package com.vicinity24.api.core.recommendation.service;

import com.vicinity24.api.core.model.enums.AvailabilityStatus;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Transaction;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.ListingType;
import com.vicinity24.api.core.model.enums.UserRole;
import com.vicinity24.api.core.model.enums.UserStatus;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import com.vicinity24.api.core.recommendation.model.EvaluateItemRequest;
import com.vicinity24.api.core.recommendation.model.RecommendationResult;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.TransactionRepository;
import com.vicinity24.api.core.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RecommendationServiceTest {
    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testRecommendationLogic() {
        // Create User
        User user1 = createUser("user1@test.com");
        User user2 = createUser("user2@test.com");

        // Create Items (Drill 1, Drill 2)
        Listing drill1 = createListing(user1, "Drill 1", "Tools", ListingType.SELL, new BigDecimal("50.00"));
        Listing drill2 = createListing(user1, "Drill 2", "Tools", ListingType.SELL, new BigDecimal("45.00"));

        // User2 buys both drills (establishing similarity between Drill 1 and Drill 2)
        createTransaction(drill1, user2, new BigDecimal("50.00"));
        createTransaction(drill2, user2, new BigDecimal("45.00"));

        // Rebuild model to include these transactions
        recommendationService.rebuildModel();

        // Evaluate a new "Drill"
        EvaluateItemRequest request = new EvaluateItemRequest();
        request.setTitle("Drill");
        request.setCategory("Tools");
        request.setEstimatedValue(new BigDecimal("40.00"));

        RecommendationResult result = recommendationService.evaluate(request);

        System.out.println("Recommendation Result: " + result);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getRecommendedAction());
        // Since similar items (Drill 1, Drill 2) were SOLD, we expect SELL recommendation or at least some confidence
        // Note: Confidence might be low with only 2 transactions, but it shouldn't crash.
    }

    private User createUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setName("Test User");
        u.setPassword("password");
        u.setRole(UserRole.MEMBER);
        u.setStatus(UserStatus.ACTIVE);
        u.setVerificationStatus(VerificationStatus.VERIFIED);
        u.setJoinedDate(LocalDateTime.now());
        u.setTrustScore(100);
        u.setVouchCount(0);
        return userRepository.save(u);
    }

    private Listing createListing(User owner, String title, String category, ListingType type, BigDecimal rate) {
        Listing l = new Listing();
        l.setId(UUID.randomUUID());
        l.setOwner(owner);
        l.setTitle(title);
        l.setCategory(category);
        l.setType(type);
        l.setHourlyRate(rate);
        l.setStatus(AvailabilityStatus.AVAILABLE);
        l.setCreatedAt(LocalDateTime.now());
        return listingRepository.save(l);
    }

    private void createTransaction(Listing listing, User payer, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setId(UUID.randomUUID());
        tx.setListing(listing);
        tx.setPayer(payer);
        tx.setPayee(listing.getOwner());
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus("SUCCESS");
        transactionRepository.save(tx);
    }
}
