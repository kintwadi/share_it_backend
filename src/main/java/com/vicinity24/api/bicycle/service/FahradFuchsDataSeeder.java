package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.service.FahradFuchsCatalogDefinitions.FahradFuchsBikeDefinition;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.embeddable.Location;
import com.vicinity24.api.core.model.enums.AvailabilityStatus;
import com.vicinity24.api.core.model.enums.ListingType;
import com.vicinity24.api.core.repository.ListingRepository;
import com.vicinity24.api.core.repository.UserRepository;
import com.vicinity24.api.core.util.GeohashUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Configuration
public class FahradFuchsDataSeeder {

    @Bean
    @Order(6)
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seedFahradFuchsStorefront(
            ListingRepository listingRepository,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            User owner = userRepository.findByEmailIgnoreCase("linda.lender@example.com")
                    .or(() -> userRepository.findAll().stream().findFirst())
                    .orElse(null);
            if (owner == null) {
                return;
            }

            List<FahradFuchsBikeDefinition> definitions = FahradFuchsCatalogDefinitions.all();
            for (int index = 0; index < definitions.size(); index++) {
                seedListing(definitions.get(index), index + 1, owner, listingRepository, jdbcTemplate);
            }
        };
    }

    private void seedListing(
            FahradFuchsBikeDefinition definition,
            int displayOrder,
            User owner,
            ListingRepository listingRepository,
            JdbcTemplate jdbcTemplate
    ) {
        UUID listingId = jdbcTemplate.query(
                "select listing_id from bicycle.fahrad_fuchs_catalog where slug = ?",
                rs -> rs.next() ? (UUID) rs.getObject("listing_id") : null,
                definition.slug()
        );

        Listing listing = listingId != null
                ? listingRepository.findById(listingId).orElse(null)
                : null;

        if (listing == null) {
            listing = Listing.builder()
                    .id(UUID.randomUUID())
                    .createdAt(LocalDateTime.now().minusHours(displayOrder))
                    .itemReference(nextItemReference(listingRepository, definition.slug()))
                    .build();
        }

        listing.setTitle(definition.title());
        listing.setDescription(definition.description());
        listing.setType(ListingType.LEND);
        listing.setCategory("Fahrad-Fuchs Demo");
        listing.setImageUrl(definition.gallery().get(0));
        listing.setGallery(definition.gallery());
        listing.setHourlyRate(definition.dailyRate());
        listing.setAutoApprove(true);
        listing.setInsuranceRequired(false);
        listing.setStatus(AvailabilityStatus.AVAILABLE);
        listing.setLocation(Location.builder().lat(49.9212).lng(8.4824).build());
        listing.setStreetAddress(FahradFuchsCatalogDefinitions.ADDRESS_LINE_1);
        listing.setCity("Gross-Gerau");
        listing.setPostalCode("64521");
        listing.setCountry("DE");
        listing.setGeohash(GeohashUtil.encode(49.9212, 8.4824, 9));
        listing.setOwner(owner);
        listing.setPartner(null);
        listing.setBorrower(null);
        listing.setPickupLocation(null);
        listing.setPickupLocationCustom(FahradFuchsCatalogDefinitions.STORE_NAME + ", " + FahradFuchsCatalogDefinitions.ADDRESS_LINE_1);
        listing.setPickupLocationStreet("Darmstaedter Strasse");
        listing.setPickupLocationHouseNumber("36");
        listing.setPickupLocationCity("Gross-Gerau");
        listing.setPickupLocationZip("64521");
        listing.setAvailableUnlimited(true);
        listing.setAvailableFrom(null);
        listing.setAvailableTo(null);
        listingRepository.save(listing);

        jdbcTemplate.update(
                """
                insert into bicycle.bike_listings (
                    listing_id,
                    frame_size,
                    bike_type,
                    assembly_buffer_minutes,
                    rent_to_own_eligible,
                    retail_purchase_price,
                    inventory_status
                ) values (?, ?, ?, ?, ?, ?, ?)
                on conflict (listing_id) do update set
                    frame_size = excluded.frame_size,
                    bike_type = excluded.bike_type,
                    assembly_buffer_minutes = excluded.assembly_buffer_minutes,
                    rent_to_own_eligible = excluded.rent_to_own_eligible,
                    retail_purchase_price = excluded.retail_purchase_price,
                    inventory_status = excluded.inventory_status
                """,
                listing.getId(),
                definition.frameOptions().stream().findFirst().map(frame -> frame.value()).orElse("M"),
                definition.bikeType().name(),
                30,
                true,
                definition.retailPrice(),
                definition.inventoryStatus().name()
        );

        jdbcTemplate.update(
                """
                insert into bicycle.fahrad_fuchs_catalog (listing_id, slug, display_order)
                values (?, ?, ?)
                on conflict (listing_id) do update set
                    slug = excluded.slug,
                    display_order = excluded.display_order
                """,
                listing.getId(),
                definition.slug(),
                displayOrder
        );
    }

    private String nextItemReference(ListingRepository listingRepository, String seed) {
        String normalized = seed == null ? "FF" : seed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        String prefix = normalized.length() >= 2 ? normalized.substring(0, 2) : (normalized + "FF").substring(0, 2);
        for (int i = 1; i < 1000; i++) {
            String candidate = "F" + prefix + String.format("%05d", i);
            if (!listingRepository.existsByItemReference(candidate)) {
                return candidate;
            }
        }
        return ("F" + UUID.randomUUID().toString().replace("-", "").substring(0, 7)).toUpperCase(Locale.ROOT);
    }
}
