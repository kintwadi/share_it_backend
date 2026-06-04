package com.nearshare.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.Message;
import com.nearshare.api.model.Review;
import com.nearshare.api.model.User;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import com.nearshare.api.model.enums.UserRole;
import com.nearshare.api.model.enums.UserStatus;
import com.nearshare.api.model.enums.VerificationStatus;
import com.nearshare.api.repository.ListingRepository;
import com.nearshare.api.repository.MessageRepository;
import com.nearshare.api.repository.ExchangeLocationRepository;
import com.nearshare.api.repository.ReviewRepository;
import com.nearshare.api.repository.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.nearshare.api.model.Subscription;
import com.nearshare.api.repository.SubscriptionRepository;

@Service
public class MockDataSeederService {

    private final UserRepository users;
    private final ListingRepository listings;
    private final ReviewRepository reviews;
    private final MessageRepository messages;
    private final SubscriptionRepository subscriptions;
    private final ExchangeLocationRepository pickupLocations;
    private final PasswordEncoder encoder;

    public MockDataSeederService(UserRepository users, ListingRepository listings, ReviewRepository reviews, MessageRepository messages, SubscriptionRepository subscriptions, ExchangeLocationRepository pickupLocations, PasswordEncoder encoder) {
        this.users = users;
        this.listings = listings;
        this.reviews = reviews;
        this.messages = messages;
        this.subscriptions = subscriptions;
        this.pickupLocations = pickupLocations;
        this.encoder = encoder;
    }

    @Transactional
    public String seedMockData() {
        boolean hadAnyRecords = users.count() > 0 || listings.count() > 0 || reviews.count() > 0 || messages.count() > 0;

        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // Support for Java 8 Time, etc.

        try {
            ClassPathResource resource = new ClassPathResource("mockdata.json");
            if (resource.exists()) {
                InputStream inputStream = resource.getInputStream();
                MockData mockData = mapper.readValue(inputStream, MockData.class);

                // Seed Users
                if (mockData.users != null) {
                    for (MockUser u : mockData.users) {
                        User user = null;
                        if (users.findByEmail(u.email).isEmpty()) {
                            user = User.builder()
                                    .id(UUID.randomUUID())
                                    .name(u.name)
                                    .email(u.email)
                                    .password(encoder.encode(u.password))
                                    .role(UserRole.valueOf(u.role))
                                    .phone(u.phone)
                                    .address(u.address)
                                    .avatarUrl(u.avatarUrl)
                                    .trustScore(u.trustScore)
                                    .vouchCount(u.vouchCount)
                                    .verificationStatus(VerificationStatus.valueOf(u.verificationStatus))
                                    .joinedDate(LocalDateTime.parse(u.joinedDate))
                                    .status(UserStatus.ACTIVE)
                                    .location(Location.builder().lat(u.location.lat).lng(u.location.lng).build())
                                    .twoFactorEnabled(false)
                                    .emailVerified(true)
                                    .build();
                            users.save(user);
                        } else {
                            user = users.findByEmail(u.email).get();
                            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                                user.setEmailVerified(true);
                                users.save(user);
                            }
                        }

                        // Seed Subscription for User
                        if (user != null && u.subscription != null) {
                            if (subscriptions.findFirstByUserOrderByCreatedAtDesc(user).isEmpty()) {
                                Subscription sub = Subscription.builder()
                                        .id(UUID.randomUUID())
                                        .user(user)
                                        .planType(u.subscription.planType)
                                        .status(u.subscription.status)
                                        .createdAt(LocalDateTime.now())
                                        .autoChargeAmountCents(u.subscription.autoChargeAmountCents)
                                        .autoChargeDate(LocalDateTime.now().plusDays(30))
                                        .build();
                                subscriptions.save(sub);
                            }
                        }
                    }
                }

                // Seed Listings
                if (mockData.listings != null && listings.count() == 0) {
                    List<com.nearshare.api.model.ExchangeLocation> pickupList = pickupLocations.findAll();
                    for (MockListing l : mockData.listings) {
                        User owner = users.findByEmail(l.ownerEmail).orElse(null);
                        if (owner != null && listings.findByTitle(l.title).isEmpty()) {
                            User borrower = l.borrowerEmail != null ? users.findByEmail(l.borrowerEmail).orElse(null) : null;
                            com.nearshare.api.model.ExchangeLocation pickup = null;
                            if (l.pickupLocationId != null) {
                                pickup = pickupLocations.findById(UUID.fromString(l.pickupLocationId)).orElse(null);
                            } else if (l.pickupLocationName != null) {
                                String n = l.pickupLocationName.trim();
                                pickup = pickupList.stream().filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(n)).findFirst().orElse(null);
                            }
                            Listing listing = Listing.builder()
                                    .id(UUID.randomUUID())
                                    .owner(owner)
                                    .borrower(borrower)
                                    .title(l.title)
                                    .description(l.description)
                                    .type(ListingType.valueOf(l.type))
                                    .category(l.category)
                                    .hourlyRate(BigDecimal.valueOf(l.hourlyRate))
                                    .imageUrl(l.imageUrl)
                                    .gallery(l.gallery != null ? l.gallery : Collections.emptyList())
                                    .autoApprove(l.autoApprove)
                                    .insuranceRequired(l.insuranceRequired)
                                    .status(AvailabilityStatus.valueOf(l.status))
                                    .location(Location.builder().lat(l.location.lat).lng(l.location.lng).build())
                                    .pickupLocation(pickup)
                                    .pickupLocationCustom(l.pickupLocationCustom)
                                    .pickupLocationStreet(l.pickupLocationStreet)
                                    .pickupLocationHouseNumber(l.pickupLocationHouseNumber)
                                    .pickupLocationCity(l.pickupLocationCity)
                                    .pickupLocationZip(l.pickupLocationZip)
                                    .createdAt(l.createdAt != null ? LocalDateTime.parse(l.createdAt) : LocalDateTime.now())
                                    .build();
                            listings.save(listing);
                        }
                    }
                }

                // Seed Reviews
                if (mockData.reviews != null && reviews.count() == 0) {
                    for (MockReview r : mockData.reviews) {
                        User author = users.findByEmail(r.authorEmail).orElse(null);
                        User target = users.findByEmail(r.targetEmail).orElse(null);
                        Listing listing = listings.findByTitle(r.listingTitle).stream().findFirst().orElse(null);

                        if (author != null && target != null && listing != null) {
                            Review review = Review.builder()
                                    .id(UUID.randomUUID())
                                    .author(author)
                                    .targetUser(target)
                                    .listing(listing)
                                    .rating(r.rating)
                                    .comment(r.comment)
                                    .timestamp(LocalDateTime.parse(r.timestamp))
                                    .build();
                            reviews.save(review);
                        }
                    }
                }

                // Seed Messages
                if (mockData.messages != null && messages.count() == 0) {
                    for (MockMessage m : mockData.messages) {
                        User sender = users.findByEmail(m.senderEmail).orElse(null);
                        User receiver = users.findByEmail(m.receiverEmail).orElse(null);

                        if (sender != null && receiver != null) {
                            Message message = Message.builder()
                                    .id(UUID.randomUUID())
                                    .sender(sender)
                                    .receiver(receiver)
                                    .content(m.content)
                                    .timestamp(LocalDateTime.parse(m.timestamp))
                                    .isRead(m.isRead)
                                    .build();
                            messages.save(message);
                        }
                    }
                }
                
                if (hadAnyRecords) {
                    return "Mock data seeding completed (some records already existed)";
                }
                return "Mock data seeded successfully from mockdata.json";
            } else {
                return "mockdata.json not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to seed mock data: " + e.getMessage();
        }
    }

    // Helper classes for JSON mapping
    public static class MockData {
        public List<MockUser> users;
        public List<MockListing> listings;
        public List<MockReview> reviews;
        public List<MockMessage> messages;
    }

    public static class MockUser {
        public String name;
        public String email;
        public String password;
        public String role;
        public String phone;
        public String address;
        public String avatarUrl;
        public int trustScore;
        public int vouchCount;
        public String verificationStatus;
        public String joinedDate;
        public MockLocation location;
        public MockSubscription subscription;
    }

    public static class MockSubscription {
        public String planType;
        public String status;
        public int autoChargeAmountCents;
    }

    public static class MockListing {
        public String ownerEmail;
        public String borrowerEmail;
        public String title;
        public String description;
        public String type;
        public String category;
        public double hourlyRate;
        public String imageUrl;
        public List<String> gallery;
        public boolean autoApprove;
        public boolean insuranceRequired;
        public String status;
        public MockLocation location;
        public String createdAt;
        public String pickupLocationId;
        public String pickupLocationName;
        public String pickupLocationCustom;
        public String pickupLocationStreet;
        public String pickupLocationHouseNumber;
        public String pickupLocationCity;
        public String pickupLocationZip;
    }

    public static class MockReview {
        public String authorEmail;
        public String targetEmail;
        public String listingTitle;
        public int rating;
        public String comment;
        public String timestamp;
    }

    public static class MockMessage {
        public String senderEmail;
        public String receiverEmail;
        public String content;
        public String timestamp;
        public boolean isRead;
    }

    public static class MockLocation {
        public double lat;
        public double lng;
    }
}
