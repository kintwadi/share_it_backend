package com.nearshare.api.enterprise.service;

import com.nearshare.api.enterprise.dto.EnterpriseSampleDataDTO;
import com.nearshare.api.enterprise.model.EnterpriseCategory;
import com.nearshare.api.enterprise.repository.EnterpriseCategoryRepository;
import com.nearshare.api.model.Listing;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import com.nearshare.api.partner.model.Partner;
import com.nearshare.api.partner.model.PartnerStatus;
import com.nearshare.api.partner.repository.PartnerRepository;
import com.nearshare.api.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnterpriseSampleDataService {
    private final EnterpriseCategoryRepository categories;
    private final PartnerRepository partners;
    private final ListingRepository listings;

    @Value("${settings.enterprise.sample-data.enabled:false}")
    private boolean enabled;

    @Transactional
    public EnterpriseSampleDataDTO load(boolean reset, int limit) {
        long existing = listings.countByEnterpriseOnlyTrue();
        if (!enabled) {
            return EnterpriseSampleDataDTO.builder()
                    .enabled(false)
                    .reset(reset)
                    .existingEnterpriseListings(existing)
                    .partnersCreated(0)
                    .listingsCreated(0)
                    .build();
        }

        if (reset) {
            listings.deleteByEnterpriseOnlyTrue();
            existing = 0;
        }

        if (existing > 0) {
            return EnterpriseSampleDataDTO.builder()
                    .enabled(true)
                    .reset(reset)
                    .existingEnterpriseListings(existing)
                    .partnersCreated(0)
                    .listingsCreated(0)
                    .build();
        }

        List<EnterpriseCategory> items = categories.findAllByOrderBySectorAscCategoryGroupAscItemLabelAsc();
        int capped = Math.max(0, Math.min(limit, items.size()));
        items = items.subList(0, capped);

        Random rnd = new Random(42);
        List<Partner> createdPartners = createPartners(rnd);
        partners.saveAll(createdPartners);

        int partnerIdx = 0;
        List<Listing> createdListings = new ArrayList<>();
        for (EnterpriseCategory c : items) {
            Partner p = createdPartners.get(partnerIdx % createdPartners.size());
            partnerIdx++;
            createdListings.add(buildListing(c, p, rnd));
        }
        listings.saveAll(createdListings);

        return EnterpriseSampleDataDTO.builder()
                .enabled(true)
                .reset(reset)
                .existingEnterpriseListings(0)
                .partnersCreated(createdPartners.size())
                .listingsCreated(createdListings.size())
                .build();
    }

    private List<Partner> createPartners(Random rnd) {
        String[] prefixes = {"Northbridge", "Apex", "Summit", "BlueRock", "IronGate", "Crown", "Evergreen", "Sterling", "Pioneer", "Oakridge"};
        String[] suffixes = {"Industrial", "Facilities", "Logistics", "Systems", "Holdings", "Engineering", "Group", "Services", "Supply", "Manufacturing"};
        City[] cities = {
                new City("Berlin", "Friedrichstraße 101", 52.5200, 13.4050),
                new City("Munich", "Leopoldstraße 12", 48.1351, 11.5820),
                new City("Hamburg", "Jungfernstieg 5", 53.5511, 9.9937),
                new City("Frankfurt", "Mainzer Landstraße 50", 50.1109, 8.6821),
                new City("Cologne", "Hohenzollernring 22", 50.9375, 6.9603)
        };

        List<Partner> out = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            City city = cities[i % cities.length];
            String name = prefixes[i % prefixes.length] + " " + suffixes[(i * 3) % suffixes.length];
            String email = ("sample.enterprise." + i + "@" + "enterprise.local").toLowerCase(Locale.ROOT);
            out.add(Partner.builder()
                    .id(UUID.randomUUID())
                    .name(name)
                    .email(email)
                    .phone("+49 30 0000 " + String.format("%04d", rnd.nextInt(10000)))
                    .address(city.address)
                    .city(city.city)
                    .contactPerson("Operations Desk")
                    .status(PartnerStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        }
        return out;
    }

    private Listing buildListing(EnterpriseCategory c, Partner p, Random rnd) {
        ListingType type = ListingType.LEND;
        BigDecimal rate = BigDecimal.valueOf(15 + (rnd.nextInt(20) * 5));
        String title = c.getItemLabel() + " (" + p.getName() + ")";
        String desc = "Enterprise inventory item.\n" +
                "Sector: " + c.getSector() + "\n" +
                "Group: " + c.getCategoryGroup() + "\n" +
                "Provided by: " + p.getName() + "\n" +
                "Pickup: " + p.getAddress() + ", " + p.getCity();

        City city = City.from(p.getCity());
        double jitterLat = (rnd.nextDouble() - 0.5) * 0.06;
        double jitterLng = (rnd.nextDouble() - 0.5) * 0.06;

        return Listing.builder()
                .id(UUID.randomUUID())
                .title(title)
                .description(desc)
                .type(type)
                .category(c.getItemLabel())
                .imageUrl("https://picsum.photos/seed/enterprise-" + UUID.randomUUID() + "/800/600")
                .gallery(List.of())
                .hourlyRate(rate)
                .autoApprove(true)
                .insuranceRequired(false)
                .status(AvailabilityStatus.PARTNER_ACTIVE)
                .location(Location.builder().lat(city.lat + jitterLat).lng(city.lng + jitterLng).build())
                .owner(null)
                .partner(p)
                .borrower(null)
                .pickupLocation(null)
                .pickupLocationCustom(null)
                .pickupLocationStreet(null)
                .pickupLocationHouseNumber(null)
                .pickupLocationCity(null)
                .pickupLocationZip(null)
                .createdAt(LocalDateTime.now())
                .availableUnlimited(true)
                .availableFrom(null)
                .availableTo(null)
                .enterpriseOnly(true)
                .build();
    }

    private record City(String city, String address, double lat, double lng) {
        static City from(String city) {
            if (city == null) return new City("Berlin", "Friedrichstraße 101", 52.5200, 13.4050);
            return switch (city) {
                case "Munich" -> new City("Munich", "Leopoldstraße 12", 48.1351, 11.5820);
                case "Hamburg" -> new City("Hamburg", "Jungfernstieg 5", 53.5511, 9.9937);
                case "Frankfurt" -> new City("Frankfurt", "Mainzer Landstraße 50", 50.1109, 8.6821);
                case "Cologne" -> new City("Cologne", "Hohenzollernring 22", 50.9375, 6.9603);
                default -> new City("Berlin", "Friedrichstraße 101", 52.5200, 13.4050);
            };
        }
    }
}

