package com.vicinity24.api.core.service;

import com.vicinity24.api.core.dto.ActivityDTO;
import com.vicinity24.api.core.model.Subscription;
import com.vicinity24.api.core.dto.UpdateProfileRequest;
import com.vicinity24.api.core.dto.UserDTO;
import com.vicinity24.api.core.dto.UserSummaryDTO;
import com.vicinity24.api.core.dto.LocationDTO;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.UserRole;
import com.vicinity24.api.core.model.enums.UserStatus;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import com.vicinity24.api.core.repository.DeviceRepository;
import com.vicinity24.api.core.repository.UserRepository;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.MessageRepository;
import com.vicinity24.api.core.repository.PasswordResetTokenRepository;
import com.vicinity24.api.core.repository.RecommendationDismissRepository;
import com.vicinity24.api.core.repository.ReportRepository;
import com.vicinity24.api.core.repository.ReviewRepository;
import com.vicinity24.api.core.repository.SubscriptionRepository;
import com.vicinity24.api.core.repository.SubscriptionVerificationCodeRepository;
import com.vicinity24.api.core.repository.TransactionRepository;
import com.vicinity24.api.core.partner.repository.PartnerAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ListingRepository listingRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final RecommendationDismissRepository recommendationDismissRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionVerificationCodeRepository subscriptionVerificationCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;
    private final PartnerAdminRepository partnerAdminRepository;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ListingRepository listingRepository,
            MessageRepository messageRepository,
            ReviewRepository reviewRepository,
            ReportRepository reportRepository,
            RecommendationDismissRepository recommendationDismissRepository,
            SubscriptionRepository subscriptionRepository,
            SubscriptionVerificationCodeRepository subscriptionVerificationCodeRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            TransactionRepository transactionRepository,
            DeviceRepository deviceRepository,
            PartnerAdminRepository partnerAdminRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.listingRepository = listingRepository;
        this.messageRepository = messageRepository;
        this.reviewRepository = reviewRepository;
        this.reportRepository = reportRepository;
        this.recommendationDismissRepository = recommendationDismissRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionVerificationCodeRepository = subscriptionVerificationCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.transactionRepository = transactionRepository;
        this.deviceRepository = deviceRepository;
        this.partnerAdminRepository = partnerAdminRepository;
    }

    public List<ActivityDTO> getActivity(User user) {
        List<ActivityDTO> activity = new java.util.ArrayList<>();

        // Account created
        if (user.getJoinedDate() != null) {
            activity.add(new ActivityDTO(
                "Joined Vicinity24",
                "Account created",
                user.getJoinedDate(),
                "account"
            ));
        }

        // Listings
        List<Listing> listings = listingRepository.findByOwner(user);
        for (Listing l : listings) {
            if (l.getCreatedAt() != null) {
                activity.add(new ActivityDTO(
                    "Listing published",
                    "Published: " + l.getTitle(),
                    l.getCreatedAt(),
                    "listing"
                ));
            }
        }

        // Subscriptions
        List<Subscription> subs = subscriptionRepository.findByUser(user);
        for (Subscription s : subs) {
            if (s.getCreatedAt() != null) {
                activity.add(new ActivityDTO(
                    "Subscription updated",
                    "Plan: " + s.getPlanType(),
                    s.getCreatedAt(),
                    "subscription"
                ));
            }
        }

        // Sort by date desc
        activity.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        // Filter last 30 days
        java.time.LocalDateTime thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30);
        return activity.stream()
            .filter(a -> a.getDate().isAfter(thirtyDaysAgo))
            .collect(java.util.stream.Collectors.toList());
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("user_not_found"));
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
    }

    public UserDTO me(User user) {
        return toDTO(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public UserDTO updateProfile(User user, UpdateProfileRequest request) {
        if (request.getName() != null) user.setName(request.getName());
        if (request.getDisplayName() != null) user.setDisplayName(request.getDisplayName());
        if (request.getAvatarUrl() != null) {
            String v = request.getAvatarUrl();
            String trimmed = v == null ? "" : v.trim();
            if (trimmed.toLowerCase().startsWith("data:")) {
                throw new IllegalArgumentException("avatar_url_invalid");
            }
            if (trimmed.length() > 2048) {
                throw new IllegalArgumentException("avatar_url_too_long");
            }
            user.setAvatarUrl(v);
        }
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getProfileVisible() != null) user.setProfileVisible(request.getProfileVisible());
        if (request.getShowRatings() != null) user.setShowRatings(request.getShowRatings());
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO updateAvatar(User user, String avatarUrl) {
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return toDTO(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("invalid_old_password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserDTO> allUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    public List<UserSummaryDTO> contacts(User current) {
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(current.getId()))
                .map(u -> UserSummaryDTO.builder().id(u.getId()).name(u.getName()).trustScore(u.getTrustScore()).avatarUrl(u.getAvatarUrl()).build())
                .toList();
    }

    public UserDTO vouch(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVouchCount(user.getVouchCount() + 1);
        user.setTrustScore(Math.min(100, user.getTrustScore() + 1));
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO verificationRequest(User user, String phone, String address) {
        user.setVerificationStatus(VerificationStatus.PENDING);
        user.setPhone(phone);
        user.setAddress(address);
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO approveVerification(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        user.setTrustScore(Math.min(100, user.getTrustScore() + 5));
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO revokeVerification(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setVerificationStatus(VerificationStatus.UNVERIFIED);
        userRepository.save(user);
        return toDTO(user);
    }

    public UserDTO setStatus(UUID id, UserStatus status) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));
        user.setStatus(status);
        userRepository.save(user);
        return toDTO(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("user_not_found"));

        partnerAdminRepository.deleteAllByUserId(user.getId());
        deviceRepository.deleteByUser(user);
        passwordResetTokenRepository.invalidateUserTokens(user.getId());
        subscriptionVerificationCodeRepository.deleteAllForUser(user);

        var subscriptions = subscriptionRepository.findByUser(user);
        if (!subscriptions.isEmpty()) {
            subscriptionRepository.deleteAll(subscriptions);
        }

        var messages = messageRepository.findBySenderOrReceiver(user, user);
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }

        var authoredReviews = reviewRepository.findByAuthor(user);
        if (!authoredReviews.isEmpty()) {
            reviewRepository.deleteAll(authoredReviews);
        }

        var receivedReviews = reviewRepository.findByTargetUser(user);
        if (!receivedReviews.isEmpty()) {
            reviewRepository.deleteAll(receivedReviews);
        }

        var reports = reportRepository.findByReporter(user);
        if (!reports.isEmpty()) {
            reportRepository.deleteAll(reports);
        }

        var dismissals = recommendationDismissRepository.findByUser(user);
        if (!dismissals.isEmpty()) {
            recommendationDismissRepository.deleteAll(dismissals);
        }

        var payerTx = transactionRepository.findByPayerId(user.getId());
        if (!payerTx.isEmpty()) {
            transactionRepository.deleteAll(payerTx);
        }

        var payeeTx = transactionRepository.findByPayeeId(user.getId());
        if (!payeeTx.isEmpty()) {
            transactionRepository.deleteAll(payeeTx);
        }

        List<Listing> ownedListings = listingRepository.findByOwner(user);
        for (Listing l : ownedListings) {
            var listingReviews = reviewRepository.findByListing(l);
            if (!listingReviews.isEmpty()) {
                reviewRepository.deleteAll(listingReviews);
            }
            var listingReports = reportRepository.findByListing(l);
            if (!listingReports.isEmpty()) {
                reportRepository.deleteAll(listingReports);
            }
            var listingDismissals = recommendationDismissRepository.findByListing(l);
            if (!listingDismissals.isEmpty()) {
                recommendationDismissRepository.deleteAll(listingDismissals);
            }
            var listingTxs = transactionRepository.findByListingId(l.getId());
            if (!listingTxs.isEmpty()) {
                transactionRepository.deleteAll(listingTxs);
            }
            listingRepository.delete(l);
        }

        List<Listing> borrowedListings = listingRepository.findByBorrower(user);
        for (Listing l : borrowedListings) {
            if (user.getId().equals(l.getBorrower() != null ? l.getBorrower().getId() : null)) {
                l.setBorrower(null);
                listingRepository.save(l);
            }
        }

        userRepository.delete(user);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole() : UserRole.MEMBER)
                .avatarUrl(user.getAvatarUrl())
                .trustScore(user.getTrustScore())
                .vouchCount(user.getVouchCount())
                .verificationStatus(user.getVerificationStatus())
                .location(LocationDTO.builder().x(user.getLocation() != null ? user.getLocation().getLat() : null).y(user.getLocation() != null ? user.getLocation().getLng() : null).build())
                .joinedDate(user.getJoinedDate() != null ? user.getJoinedDate().toLocalDate().toString() : null)
                .phone(user.getPhone())
                .address(user.getAddress())
                .twoFactorEnabled(Boolean.TRUE.equals(user.getTwoFactorEnabled()))
                .profileVisible(user.getProfileVisible())
                .showRatings(user.getShowRatings())
                .adminScope(user.getAdminScope())
                .emailVerified(Boolean.TRUE.equals(user.getEmailVerified()))
                .build();
    }
}
