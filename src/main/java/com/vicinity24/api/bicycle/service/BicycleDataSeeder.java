package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.BicycleListing;
import com.vicinity24.api.bicycle.domain.valueobject.BikeType;
import com.vicinity24.api.bicycle.domain.valueobject.InventoryStatus;
import com.vicinity24.api.bicycle.repository.BicycleListingRepository;
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

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Configuration
public class BicycleDataSeeder {

    @Bean
    @Order(4)
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seedBicycleListings(
            BicycleListingRepository bicycleListingRepository,
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

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Riese & Muller Nevo4 Touring",
                    "Comfort-focused e-bike with integrated lights and two battery options.",
                    "https://images.unsplash.com/photo-1541625602330-2277a4c46182",
                    List.of(
                            "https://images.unsplash.com/photo-1541625602330-2277a4c46182",
                            "https://images.unsplash.com/photo-1511994298241-608e28f14fde"
                    ),
                    new BikeSeedSpec("L", BikeType.E_BIKE, 45, true, new BigDecimal("3299.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("9.50"), 50.1109, 8.6821, "Frankfurt"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Urban Arrow Family Cargo",
                    "Front-box cargo bike for city pickups with workshop prep buffer built in.",
                    "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                    List.of(
                            "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                            "https://images.unsplash.com/photo-1485965120184-e220f721d03e"
                    ),
                    new BikeSeedSpec("XL", BikeType.CARGO, 180, true, new BigDecimal("4890.00"), InventoryStatus.WORKSHOP_PREP_REQUIRED, new BigDecimal("14.00"), 50.1165, 8.6842, "Frankfurt"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Canyon Endurace CF 7",
                    "Road endurance bike tuned for long weekend rides and quick commuter loops.",
                    "https://images.unsplash.com/photo-1558981806-ec527fa84c39",
                    List.of(
                            "https://images.unsplash.com/photo-1558981806-ec527fa84c39",
                            "https://images.unsplash.com/photo-1485965120184-e220f721d03e"
                    ),
                    new BikeSeedSpec("M", BikeType.ROAD, 30, false, new BigDecimal("2199.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("7.00"), 50.1049, 8.6295, "Frankfurt"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Tern GSD S10 LX",
                    "Long-tail cargo e-bike set up for school runs, grocery hauls, and family logistics.",
                    "https://images.unsplash.com/photo-1519583272095-6433daf26b6e",
                    List.of(
                            "https://images.unsplash.com/photo-1519583272095-6433daf26b6e",
                            "https://images.unsplash.com/photo-1502741338009-cac2772e18bc"
                    ),
                    new BikeSeedSpec("One Size", BikeType.CARGO, 120, true, new BigDecimal("5699.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("16.00"), 52.5200, 13.4050, "Berlin"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Cube Kathmandu Hybrid Pro 750",
                    "Fully equipped trekking e-bike for daily commuting and long-distance weekend tours.",
                    "https://images.unsplash.com/photo-1485965120184-e220f721d03e",
                    List.of(
                            "https://images.unsplash.com/photo-1485965120184-e220f721d03e",
                            "https://images.unsplash.com/photo-1511994298241-608e28f14fde"
                    ),
                    new BikeSeedSpec("L", BikeType.TREKKING, 60, true, new BigDecimal("3899.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("11.50"), 48.1351, 11.5820, "Munich"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Specialized Sirrus X 4.0",
                    "Fast hybrid bike for city fitness rides with wider tires and stable commuter geometry.",
                    "https://images.unsplash.com/photo-1508979828023-0c2d7f8f2b88",
                    List.of(
                            "https://images.unsplash.com/photo-1508979828023-0c2d7f8f2b88",
                            "https://images.unsplash.com/photo-1519583272095-6433daf26b6e"
                    ),
                    new BikeSeedSpec("M", BikeType.CITY, 25, false, new BigDecimal("1599.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("6.50"), 53.5511, 9.9937, "Hamburg"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Cannondale Topstone 2",
                    "Gravel all-rounder ready for bikepacking weekends, mixed-surface training, and daily commuting.",
                    "https://images.unsplash.com/photo-1544191696-15693f2f1d05",
                    List.of(
                            "https://images.unsplash.com/photo-1544191696-15693f2f1d05",
                            "https://images.unsplash.com/photo-1558981806-ec527fa84c39"
                    ),
                    new BikeSeedSpec("54", BikeType.GRAVEL, 40, false, new BigDecimal("1899.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("8.00"), 50.9375, 6.9603, "Cologne"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Gazelle Ultimate C8 HMB",
                    "Step-through city e-bike with upright fit, belt drive comfort, and integrated daily-use lights.",
                    "https://images.unsplash.com/photo-1571068316344-75bc76f77890",
                    List.of(
                            "https://images.unsplash.com/photo-1571068316344-75bc76f77890",
                            "https://images.unsplash.com/photo-1485965120184-e220f721d03e"
                    ),
                    new BikeSeedSpec("M", BikeType.E_BIKE, 35, true, new BigDecimal("3099.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("10.50"), 51.2277, 6.7735, "Dusseldorf"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Omnium Cargo WiFi",
                    "Performance cargo bike with compact handling for courier routes and business deliveries.",
                    "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                    List.of(
                            "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                            "https://images.unsplash.com/photo-1519583272095-6433daf26b6e"
                    ),
                    new BikeSeedSpec("One Size", BikeType.CARGO, 90, false, new BigDecimal("3699.00"), InventoryStatus.WORKSHOP_PREP_REQUIRED, new BigDecimal("12.00"), 52.3759, 9.7320, "Hanover"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Scott Aspect 940",
                    "Front-suspension mountain bike for forest paths, beginner trail rides, and rugged city shortcuts.",
                    "https://images.unsplash.com/photo-1511994298241-608e28f14fde",
                    List.of(
                            "https://images.unsplash.com/photo-1511994298241-608e28f14fde",
                            "https://images.unsplash.com/photo-1541625602330-2277a4c46182"
                    ),
                    new BikeSeedSpec("L", BikeType.MOUNTAIN, 45, false, new BigDecimal("1299.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("7.50"), 51.0504, 13.7373, "Dresden"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Woom Original 4",
                    "Lightweight kids bike with confident handling for school rides and park loops.",
                    "https://images.unsplash.com/photo-1502741338009-cac2772e18bc",
                    List.of(
                            "https://images.unsplash.com/photo-1502741338009-cac2772e18bc",
                            "https://images.unsplash.com/photo-1571068316344-75bc76f77890"
                    ),
                    new BikeSeedSpec("20 in", BikeType.KIDS, 20, false, new BigDecimal("599.00"), InventoryStatus.PREORDER, new BigDecimal("4.50"), 48.7758, 9.1829, "Stuttgart"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Brompton Electric C Line",
                    "Compact folding e-bike for apartment living, rail commuting, and office-ready storage.",
                    "https://images.unsplash.com/photo-1517649763962-0c623066013b",
                    List.of(
                            "https://images.unsplash.com/photo-1517649763962-0c623066013b",
                            "https://images.unsplash.com/photo-1508979828023-0c2d7f8f2b88"
                    ),
                    new BikeSeedSpec("One Size", BikeType.E_BIKE, 25, true, new BigDecimal("3499.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("12.50"), 53.0793, 8.8017, "Bremen"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Orbea Orca M30",
                    "Light climbing road bike for fast training loops, alpine weekends, and sportive riders.",
                    "https://images.unsplash.com/photo-1485965120184-e220f721d03e",
                    List.of(
                            "https://images.unsplash.com/photo-1485965120184-e220f721d03e",
                            "https://images.unsplash.com/photo-1558981806-ec527fa84c39"
                    ),
                    new BikeSeedSpec("56", BikeType.ROAD, 30, false, new BigDecimal("2599.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("8.50"), 49.4521, 11.0767, "Nuremberg"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Kalkhoff Image 5.B Advance",
                    "Step-through commuter e-bike with upright comfort, rack capacity, and weather-ready accessories.",
                    "https://images.unsplash.com/photo-1571068316344-75bc76f77890",
                    List.of(
                            "https://images.unsplash.com/photo-1571068316344-75bc76f77890",
                            "https://images.unsplash.com/photo-1511994298241-608e28f14fde"
                    ),
                    new BikeSeedSpec("M", BikeType.E_BIKE, 35, true, new BigDecimal("2799.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("9.00"), 51.3397, 12.3731, "Leipzig"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Marin DSX FS",
                    "Flat-bar gravel bike tuned for rough commutes, fitness rides, and fast canal-path exploration.",
                    "https://images.unsplash.com/photo-1544191696-15693f2f1d05",
                    List.of(
                            "https://images.unsplash.com/photo-1544191696-15693f2f1d05",
                            "https://images.unsplash.com/photo-1508979828023-0c2d7f8f2b88"
                    ),
                    new BikeSeedSpec("M", BikeType.GRAVEL, 40, false, new BigDecimal("1699.00"), InventoryStatus.ON_FLOOR_ASSEMBLED, new BigDecimal("7.20"), 51.4556, 7.0116, "Essen"),
                    jdbcTemplate
            );

            seedBike(
                    bicycleListingRepository,
                    listingRepository,
                    owner,
                    "Benno Boost 10D",
                    "High-capacity compact cargo bike for delivery shifts and everyday family transport.",
                    "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                    List.of(
                            "https://images.unsplash.com/photo-1507035895480-2b3156c31fc8",
                            "https://images.unsplash.com/photo-1519583272095-6433daf26b6e"
                    ),
                    new BikeSeedSpec("One Size", BikeType.CARGO, 110, true, new BigDecimal("4299.00"), InventoryStatus.WORKSHOP_PREP_REQUIRED, new BigDecimal("13.50"), 49.0069, 8.4037, "Karlsruhe"),
                    jdbcTemplate
            );
        };
    }

    private void seedBike(
            BicycleListingRepository bicycleListingRepository,
            ListingRepository listingRepository,
            User owner,
            String title,
            String description,
            String imageUrl,
            List<String> gallery,
            BikeSeedSpec spec,
            JdbcTemplate jdbcTemplate
    ) {
        Listing existing = listingRepository.findAll().stream()
                .filter(candidate -> candidate.getTitle() != null && candidate.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);

        String seededImageUrl = buildSeedImage(title, spec, spec.city());
        List<String> seededGallery = List.of(
                seededImageUrl,
                buildSeedImage(title, spec, spec.bikeType().name())
        );

        Listing listing = existing != null
                ? existing
                : Listing.builder()
                .id(UUID.randomUUID())
                .createdAt(LocalDateTime.now().minusDays(1))
                .itemReference(nextItemReference(listingRepository, title))
                .build();

        listing.setTitle(title);
        listing.setDescription(description);
        listing.setType(ListingType.LEND);
        listing.setCategory("Bikes");
        listing.setImageUrl(seededImageUrl);
        listing.setGallery(seededGallery);
        listing.setHourlyRate(spec.hourlyRate());
        listing.setAutoApprove(true);
        listing.setInsuranceRequired(false);
        listing.setStatus(AvailabilityStatus.AVAILABLE);
        listing.setLocation(Location.builder().lat(spec.lat()).lng(spec.lng()).build());
        listing.setStreetAddress("Bike Hub 1");
        listing.setCity(spec.city());
        listing.setPostalCode("60311");
        listing.setCountry("DE");
        listing.setGeohash(GeohashUtil.encode(spec.lat(), spec.lng(), 9));
        listing.setOwner(owner);
        listing.setPartner(null);
        listing.setBorrower(null);
        listing.setPickupLocation(null);
        listing.setPickupLocationCustom("Bike Hub 1, " + spec.city());
        listing.setPickupLocationStreet("Bike Hub");
        listing.setPickupLocationHouseNumber("1");
        listing.setPickupLocationCity(spec.city());
        listing.setPickupLocationZip("60311");
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
                spec.frameSize(),
                spec.bikeType().name(),
                spec.assemblyBufferMinutes(),
                spec.rentToOwnEligible(),
                spec.retailPurchasePrice(),
                spec.inventoryStatus().name()
        );
    }

    private String nextItemReference(ListingRepository listingRepository, String seed) {
        String normalized = seed == null ? "BK" : seed.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "");
        String prefix = normalized.length() >= 2 ? normalized.substring(0, 2) : (normalized + "BK").substring(0, 2);
        for (int i = 1; i < 1000; i++) {
            String candidate = "B" + prefix + String.format("%05d", i);
            if (!listingRepository.existsByItemReference(candidate)) {
                return candidate;
            }
        }
        return ("B" + UUID.randomUUID().toString().replace("-", "").substring(0, 7)).toUpperCase(Locale.ROOT);
    }

    private String buildSeedImage(String title, BikeSeedSpec spec, String accentLabel) {
        BikeImageTheme theme = themeFor(spec.bikeType());
        String text = shortLabel(title, 28) + "\n" + theme.label() + "  " + shortLabel(formatSeedLabel(accentLabel), 18);
        return "https://placehold.co/1200x800/" + theme.background() + "/" + theme.foreground()
                + "/png?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private BikeImageTheme themeFor(BikeType bikeType) {
        return switch (bikeType) {
            case CARGO -> new BikeImageTheme("0f172a", "e2e8f0", "Cargo Bike");
            case E_BIKE -> new BikeImageTheme("14532d", "dcfce7", "E-Bike");
            case ROAD -> new BikeImageTheme("4c1d95", "ede9fe", "Road Bike");
            case GRAVEL -> new BikeImageTheme("92400e", "ffedd5", "Gravel Bike");
            case MOUNTAIN -> new BikeImageTheme("164e63", "cffafe", "Mountain Bike");
            case KIDS -> new BikeImageTheme("9a3412", "ffedd5", "Kids Bike");
            case CITY -> new BikeImageTheme("1d4ed8", "dbeafe", "City Bike");
            case TREKKING -> new BikeImageTheme("0f766e", "ccfbf1", "Trekking Bike");
            case OTHER -> new BikeImageTheme("475569", "e2e8f0", "Bike");
        };
    }

    private String formatSeedLabel(String value) {
        if (value == null || value.isBlank()) {
            return "Bike";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ').trim();
        StringBuilder formatted = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                formatted.append(part.substring(1));
            }
        }
        return formatted.isEmpty() ? "Bike" : formatted.toString();
    }

    private String shortLabel(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Bike";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength - 1) + "…" : normalized;
    }

    private record BikeSeedSpec(
            String frameSize,
            BikeType bikeType,
            Integer assemblyBufferMinutes,
            boolean rentToOwnEligible,
            BigDecimal retailPurchasePrice,
            InventoryStatus inventoryStatus,
            BigDecimal hourlyRate,
            double lat,
            double lng,
            String city
    ) {
    }

    private record BikeImageTheme(
            String background,
            String foreground,
            String label
    ) {
    }
}
