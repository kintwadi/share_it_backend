package com.nearshare.api.config;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.nearshare.api.repository.ReviewRepository;
import com.nearshare.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seed(UserRepository users, ListingRepository listings, ReviewRepository reviews, MessageRepository messages, PasswordEncoder encoder) {
        return args -> {
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
                            if (users.findByEmail(u.email).isEmpty()) {
                                User user = User.builder()
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
                                        .build();
                                users.save(user);
                            }
                        }
                    }

                    // Seed Listings
                    if (mockData.listings != null) {
                        for (MockListing l : mockData.listings) {
                            User owner = users.findByEmail(l.ownerEmail).orElse(null);
                            if (owner != null && listings.findByTitle(l.title).isEmpty()) {
                                Listing listing = Listing.builder()
                                        .id(UUID.randomUUID())
                                        .owner(owner)
                                        .title(l.title)
                                        .description(l.description)
                                        .type(ListingType.valueOf(l.type))
                                        .category(l.category)
                                        .hourlyRate(BigDecimal.valueOf(l.hourlyRate))
                                        .imageUrl(l.imageUrl)
                                        .gallery(l.gallery)
                                        .autoApprove(l.autoApprove)
                                        .status(AvailabilityStatus.valueOf(l.status))
                                        .location(Location.builder().lat(l.location.lat).lng(l.location.lng).build())
                                        .build();
                                listings.save(listing);
                            }
                        }
                    }

                    // Seed Reviews
                    if (mockData.reviews != null) {
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
                    if (mockData.messages != null) {
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
                    
                    System.out.println("Mock data seeded successfully from mockdata.json");
                }
            } catch (Exception e) {
                System.err.println("Failed to seed mock data: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    // Helper classes for JSON mapping
    private static class MockData {
        public List<MockUser> users;
        public List<MockListing> listings;
        public List<MockReview> reviews;
        public List<MockMessage> messages;
    }

    private static class MockUser {
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
    }

    private static class MockListing {
        public String ownerEmail;
        public String title;
        public String description;
        public String type;
        public String category;
        public double hourlyRate;
        public String imageUrl;
        public List<String> gallery;
        public boolean autoApprove;
        public String status;
        public MockLocation location;
    }

    private static class MockReview {
        public String authorEmail;
        public String targetEmail;
        public String listingTitle;
        public int rating;
        public String comment;
        public String timestamp;
    }

    private static class MockMessage {
        public String senderEmail;
        public String receiverEmail;
        public String content;
        public String timestamp;
        public boolean isRead;
    }

    private static class MockLocation {
        public double lat;
        public double lng;
    }
}