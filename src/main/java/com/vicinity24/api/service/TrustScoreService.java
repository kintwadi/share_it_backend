package com.vicinity24.api.service;

import com.vicinity24.api.model.Transaction;
import com.vicinity24.api.model.User;
import com.vicinity24.api.model.Listing;
import com.vicinity24.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrustScoreService {

    private final UserRepository userRepository;

    public TrustScoreService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void updateTrustScore(User user, Listing listing) {
        int change = calculateTrustScoreChange(listing);
        int newScore = Math.max(0, Math.min(100, user.getTrustScore() + change));
        
        user.setTrustScore(newScore);
        updateTier(user);
        
        userRepository.save(user);
    }

    @Transactional
    public void updateTrustScore(User user, Transaction transaction) {
        int change = calculateTrustScoreChange(transaction.getListing());
        int newScore = Math.max(0, Math.min(100, user.getTrustScore() + change));
        
        user.setTrustScore(newScore);
        updateTier(user);
        
        userRepository.save(user);
    }

    @Transactional
    public void updateTrustScoreForSubscription(User user, String planType) {
        int newScore = user.getTrustScore();
        
        if ("plus".equalsIgnoreCase(planType) || "pro".equalsIgnoreCase(planType)) {
            newScore = 50;
        } else if ("starter".equalsIgnoreCase(planType)) {
            newScore = 10;
        }
        
        user.setTrustScore(newScore);
        updateTier(user);
        
        userRepository.save(user);
    }

    private void updateTier(User user) {
        int score = user.getTrustScore();
        if (score >= 40) {
            user.setTrustTier("trusted");
        } else if (score >= 20) {
            user.setTrustTier("verified");
        } else if (score >= 5) {
            user.setTrustTier("active");
        } else {
            user.setTrustTier("new");
        }
    }

    private int calculateTrustScoreChange(com.vicinity24.api.model.Listing listing) {
        int basePoints = 0;
        
        if (listing != null) {
            switch (listing.getType()) {
                case GIVE: basePoints = 5; break;
                case SELL: basePoints = 3; break;
                case LEND: 
                case GOODS: // Treat legacy GOODS as LEND
                    basePoints = 8; break;
                case SKILL: basePoints = 4; break;
                default: basePoints = 0;
            }
        }

        // Additional bonuses can be added here
        // e.g., verified handoff, on-time return
        
        return Math.max(-25, Math.min(25, basePoints));
    }
}
