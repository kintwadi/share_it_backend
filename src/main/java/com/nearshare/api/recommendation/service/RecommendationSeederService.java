package com.nearshare.api.recommendation.service;

import com.nearshare.api.model.Listing;
import com.nearshare.api.model.Transaction;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.ListingType;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.TransactionRepository;
import com.nearshare.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RecommendationSeederService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final TransactionRepository transactionRepository;
    private final RecommendationService recommendationService;

    @Bean
    CommandLineRunner seedRecommendationData() {
        return args -> {
            if (transactionRepository.count() > 10) {
                log.info("Recommendation data already exists, skipping seeding.");
                return;
            }
            log.info("Seeding recommendation transaction data...");
            seedTransactions();
            recommendationService.rebuildModel();
        };
    }

    @Transactional
    public void seedTransactions() {
        List<User> users = userRepository.findAll();
        List<Listing> listings = listingRepository.findAll();

        if (users.isEmpty() || listings.isEmpty()) {
            log.warn("Not enough users or listings to seed transactions.");
            return;
        }

        Random random = new Random();
        int transactionCount = 50;

        for (int i = 0; i < transactionCount; i++) {
            User payer = users.get(random.nextInt(users.size()));
            Listing listing = listings.get(random.nextInt(listings.size()));
            
            // Avoid self-transaction
            if (listing.getOwner() == null || listing.getOwner().getId() == null) continue;
            if (listing.getOwner().getId().equals(payer.getId())) continue;

            Transaction tx = Transaction.builder()
                    .id(UUID.randomUUID())
                    .listing(listing)
                    .payer(payer)
                    .payee(listing.getOwner())
                    .timestamp(LocalDateTime.now().minusDays(random.nextInt(365)))
                    .status("SUCCESS")
                    .currency("EUR")
                    .paymentMethod("CARD")
                    .borrowerPath("VERIFIED")
                    .build();

            // Set amount based on type
            if (listing.getType() == ListingType.SELL) {
                tx.setAmount(listing.getHourlyRate() != null ? listing.getHourlyRate() : BigDecimal.valueOf(random.nextInt(100) + 10));
            } else if (listing.getType() == ListingType.LEND) {
                tx.setAmount(listing.getHourlyRate() != null ? listing.getHourlyRate().multiply(BigDecimal.valueOf(random.nextInt(5) + 1)) : BigDecimal.valueOf(random.nextInt(20) + 5));
            } else {
                tx.setAmount(BigDecimal.ZERO);
            }

            transactionRepository.save(tx);
        }
        log.info("Seeded {} transactions.", transactionCount);
    }
}
