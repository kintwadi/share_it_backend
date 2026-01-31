package com.nearshare.api.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seed(UserRepository users, ListingRepository listings, ReviewRepository reviews, MessageRepository messages, PasswordEncoder encoder) {
        return args -> {
            User linda = users.findByEmail("linda.lender@example.com").orElseGet(() -> users.save(
                User.builder().id(UUID.randomUUID()).name("Linda Lender").email("linda.lender@example.com").password(encoder.encode("password123")).phone("123-456-7890").address("1 Main St").avatarUrl("https://images.unsplash.com/photo-1544005313-94ddf0286df2").trustScore(98).vouchCount(156).verificationStatus(VerificationStatus.VERIFIED).location(Location.builder().lat(0.002).lng(0.002).build()).joinedDate(LocalDateTime.of(2021, 5, 15, 0, 0)).status(UserStatus.ACTIVE).role(UserRole.LENDER).build()
            ));
            linda.setPassword(encoder.encode("password123"));
            linda.setTwoFactorEnabled(false);
            users.save(linda);

            User bob = users.findByEmail("bob.borrower@example.com").orElseGet(() -> users.save(
                User.builder().id(UUID.randomUUID()).name("Bob Borrower").email("bob.borrower@example.com").password(encoder.encode("password123")).phone("987-654-3210").address("2 Main St").avatarUrl("https://images.unsplash.com/photo-1502767089025-6572583495b0").trustScore(75).vouchCount(45).verificationStatus(VerificationStatus.UNVERIFIED).location(Location.builder().lat(0.003).lng(0.003).build()).joinedDate(LocalDateTime.of(2022, 1, 10, 0, 0)).status(UserStatus.ACTIVE).role(UserRole.BORROWER).build()
            ));
            bob.setPassword(encoder.encode("password123"));
            bob.setTwoFactorEnabled(false);
            users.save(bob);

            User admin = users.findByEmail("admin@nearshare.local").orElseGet(() -> users.save(
                User.builder().id(UUID.randomUUID()).name("Alice Admin").email("admin@nearshare.local").password(encoder.encode("password123")).phone("+1 (800) 555-9999").address("NearShare HQ").avatarUrl("https://images.unsplash.com/photo-1573496359142-b8d87734a5a2").trustScore(100).vouchCount(500).verificationStatus(VerificationStatus.VERIFIED).location(Location.builder().lat(0.0).lng(0.0).build()).joinedDate(LocalDateTime.of(2020, 1, 1, 0, 0)).status(UserStatus.ACTIVE).role(UserRole.ADMIN).build()
            ));
            admin.setPassword(encoder.encode("password123"));
            admin.setTwoFactorEnabled(false);
            users.save(admin);

            User newNeighbor = users.findByEmail("new.neighbor@example.com").orElseGet(() -> users.save(
                User.builder().id(UUID.randomUUID()).name("New Neighbor").email("new.neighbor@example.com").password(encoder.encode("password123")).phone("").address("").avatarUrl("https://images.unsplash.com/photo-1599566150163-29194dcaad36").trustScore(10).vouchCount(0).verificationStatus(VerificationStatus.UNVERIFIED).location(Location.builder().lat(0.005).lng(-0.005).build()).joinedDate(LocalDateTime.of(2023, 11, 1, 0, 0)).status(UserStatus.ACTIVE).role(UserRole.MEMBER).build()
            ));
            newNeighbor.setPassword(encoder.encode("password123"));
            newNeighbor.setTwoFactorEnabled(false);
            users.save(newNeighbor);

            if (listings.count() == 0) {
                Listing tileCutter = Listing.builder().id(UUID.randomUUID()).title("Professional Tile Cutter").description("Manual tile cutter").type(ListingType.GOODS).category("Tools").imageUrl("https://example.com/tile-cutter.jpg").gallery(List.of()).hourlyRate(new java.math.BigDecimal("15.0")).autoApprove(false).status(AvailabilityStatus.AVAILABLE).location(Location.builder().lat(10.0).lng(10.0).build()).owner(linda).borrower(null).build();
                listings.save(tileCutter);
                Message m = Message.builder().id(UUID.randomUUID()).content("Hi, is this available?").timestamp(LocalDateTime.now()).isRead(true).sender(bob).receiver(linda).build();
                messages.save(m);
                Review r = Review.builder().id(UUID.randomUUID()).rating(5).comment("Great experience!").timestamp(LocalDateTime.now()).author(bob).targetUser(linda).listing(tileCutter).build();
                reviews.save(r);
            }
        };
    }
}