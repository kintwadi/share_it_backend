package com.vicinity24.api.core.recommendation.service;

import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Transaction;
import com.vicinity24.api.core.model.enums.ListingType;
import com.vicinity24.api.core.recommendation.model.EvaluateItemRequest;
import com.vicinity24.api.core.recommendation.model.RecommendationResult;
import com.vicinity24.api.core.recommendation.model.SimilarItem;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.recommender.ItemBasedRecommender;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final MahoutIdService mahoutIdService;

    private ItemBasedRecommender recommender;
    private DataModel dataModel;

    @PostConstruct
    public void init() {
        rebuildModel();
    }

    public void rebuildModel() {
        log.info("Rebuilding Mahout recommendation model...");
        try {
            List<Transaction> transactions = transactionRepository.findAll();
            
            // Map: UserID -> List<Preference>
            Map<Long, List<Preference>> userPrefs = new HashMap<>();

            for (Transaction tx : transactions) {
                if (tx.getPayer() == null || tx.getListing() == null) continue;

                Long userId = mahoutIdService.getMahoutId(tx.getPayer().getId(), "USER");
                Long itemId = mahoutIdService.getMahoutId(tx.getListing().getId(), "LISTING");
                
                // Preference value: 5.0 for SELL, 3.0 for LEND, 1.0 for GIVE
                // Or just 1.0 for interaction. Let's use transaction amount as proxy for "value"?
                // No, implicit feedback usually 1.0. Let's stick to 1.0 for "interacted".
                // But we want to know IF we should sell/lend.
                // The "similarity" finds items that are "co-transacted".
                
                userPrefs.computeIfAbsent(userId, k -> new ArrayList<>())
                        .add(new GenericPreference(userId, itemId, 1.0f));
            }

            FastByIDMap<PreferenceArray> fastUserData = new FastByIDMap<>();
            for (Map.Entry<Long, List<Preference>> entry : userPrefs.entrySet()) {
                fastUserData.put(entry.getKey(), new GenericUserPreferenceArray(entry.getValue()));
            }

            this.dataModel = new GenericDataModel(fastUserData);
            ItemSimilarity similarity = new LogLikelihoodSimilarity(this.dataModel);
            this.recommender = new GenericItemBasedRecommender(this.dataModel, similarity);
            
            log.info("Model rebuilt with {} users and {} transactions.", userPrefs.size(), transactions.size());

        } catch (Exception e) {
            log.error("Failed to build recommendation model", e);
        }
    }

    public RecommendationResult evaluate(EvaluateItemRequest request) {
        // 1. Find candidates in DB based on Title/Category
        List<Listing> candidates = findCandidates(request.getTitle(), request.getCategory());
        
        if (candidates.isEmpty()) {
            return RecommendationResult.builder()
                    .recommendedAction("GIVE") // Default
                    .confidenceScore(0.0)
                    .reasoning("No historical data found for similar items.")
                    .similarItems(Collections.emptyList())
                    .build();
        }

        // 2. Expand candidates using Mahout (Find items similar to these candidates)
        Set<Listing> expandedCandidates = new HashSet<>(candidates);
        
        // Limit expansion to top 5 matches to avoid perf hit
        for (Listing candidate : candidates.subList(0, Math.min(candidates.size(), 5))) {
            Long itemId = mahoutIdService.getMahoutId(candidate.getId(), "LISTING");
            try {
                // If item is in model
                if (dataModel.getNumUsersWithPreferenceFor(itemId) > 0) { // Check if exists
                     List<RecommendedItem> similar = recommender.mostSimilarItems(itemId, 5);
                     for (RecommendedItem rec : similar) {
                         UUID entityId = mahoutIdService.getEntityId(rec.getItemID(), "LISTING");
                         if (entityId != null) {
                             listingRepository.findById(entityId).ifPresent(expandedCandidates::add);
                         }
                     }
                }
            } catch (Exception e) {
                // Ignore if item not in model
            }
        }

        // 3. Analyze transactions of all expanded candidates
        int sellCount = 0;
        int lendCount = 0;
        int giveCount = 0;
        BigDecimal totalSellPrice = BigDecimal.ZERO;
        BigDecimal totalLendRate = BigDecimal.ZERO;
        int sellPrices = 0;
        int lendRates = 0;

        List<SimilarItem> similarItemsList = new ArrayList<>();

        for (Listing item : expandedCandidates) {
            // Find transactions for this item
            // Ideally repository should support findAllByListingIn(List<Listing>)
            // For now, assuming Listing has status or we check its type.
            // Requirement says "Find similar items ... previously lent or sold".
            // So we look at the Item's type or history.
            
            // Simplified: Look at Listing Type.
            if (item.getType() == ListingType.SELL) {
                sellCount++;
                if (item.getHourlyRate() != null) { // Assuming hourlyRate stores price for SELL too? 
                    // Actually Listing has hourlyRate. Usually SELL uses 'price' but Listing model has hourlyRate.
                    // Let's assume hourlyRate is used for value, or maybe 'amount' in transaction.
                    // Let's look at Transaction history for price accuracy.
                    List<Transaction> txs = transactionRepository.findByListingId(item.getId());
                    if (!txs.isEmpty()) {
                        for(Transaction tx : txs) {
                             if(tx.getAmount() != null) {
                                 totalSellPrice = totalSellPrice.add(tx.getAmount());
                                 sellPrices++;
                             }
                        }
                    } else {
                        // Fallback to listing price if no tx
                        if (item.getHourlyRate() != null) {
                             totalSellPrice = totalSellPrice.add(item.getHourlyRate());
                             sellPrices++;
                        }
                    }
                }
            } else if (item.getType() == ListingType.LEND) {
                lendCount++;
                if (item.getHourlyRate() != null) {
                    totalLendRate = totalLendRate.add(item.getHourlyRate());
                    lendRates++;
                }
            } else {
                giveCount++;
            }

            if (similarItemsList.size() < 5) {
                similarItemsList.add(new SimilarItem(
                    item.getId(),
                    item.getTitle(),
                    item.getType().toString(),
                    item.getHourlyRate()
                ));
            }
        }

        // 4. Decision Logic
        String action = "GIVE";
        BigDecimal suggestedPrice = BigDecimal.ZERO;
        String reason = "Most similar items are given away.";
        
        int total = sellCount + lendCount + giveCount;
        double confidence = total > 0 ? (double) Math.max(sellCount, Math.max(lendCount, giveCount)) / total : 0.0;

        if (sellCount > lendCount && sellCount > giveCount) {
            action = "SELL";
            if (sellPrices > 0) {
                suggestedPrice = totalSellPrice.divide(BigDecimal.valueOf(sellPrices), 2, RoundingMode.HALF_UP);
            }
            reason = "High demand for buying this type of item.";
        } else if (lendCount > sellCount && lendCount > giveCount) {
            action = "LEND";
            if (lendRates > 0) {
                suggestedPrice = totalLendRate.divide(BigDecimal.valueOf(lendRates), 2, RoundingMode.HALF_UP);
            }
            reason = "This item is frequently borrowed.";
        }

        return RecommendationResult.builder()
                .recommendedAction(action)
                .suggestedPrice(suggestedPrice)
                .confidenceScore(confidence)
                .reasoning(reason)
                .similarItems(similarItemsList)
                .build();
    }

    private List<Listing> findCandidates(String title, String category) {
        // Simple search: Category match AND Title contains keywords
        // Need to add method to ListingRepository
        return listingRepository.findByCategoryAndTitleContainingIgnoreCase(category, title != null ? title.split(" ")[0] : "");
    }
}
