package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.model.BikeSku;
import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import com.vicinity24.api.bicycle.domain.model.BikeSpecMapping;
import com.vicinity24.api.bicycle.domain.model.BikeSpecMappingId;
import com.vicinity24.api.bicycle.domain.model.BikeSpecValue;
import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import com.vicinity24.api.bicycle.repository.BikeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecAttributeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecValueRepository;
import com.vicinity24.api.core.config.tenant.TenantContextHolder;
import com.vicinity24.api.core.config.tenant.TenantRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class BikeShopCatalogSeeder {

    @Bean
    @Order(5)
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seedBikeShopCatalog(
            BikeRepository bikeRepository,
            BikeSpecAttributeRepository attributeRepository,
            BikeSpecValueRepository valueRepository,
            TenantRegistry tenantRegistry
    ) {
        return args -> {
            for (String tenantId : tenantRegistry.getTenants().keySet()) {
                TenantContextHolder.setTenantId(tenantId);
                try {
                    seedTenantCatalog(bikeRepository, attributeRepository, valueRepository, tenantId);
                } finally {
                    TenantContextHolder.clear();
                }
            }
        };
    }

    private void seedTenantCatalog(
            BikeRepository bikeRepository,
            BikeSpecAttributeRepository attributeRepository,
            BikeSpecValueRepository valueRepository,
            String tenantId
    ) {
        Map<String, BikeSpecAttribute> attributes = new LinkedHashMap<>();
        attributes.put("Frame Material", ensureAttribute(attributeRepository, tenantId, "Frame Material", false));
        attributes.put("Brake Type", ensureAttribute(attributeRepository, tenantId, "Brake Type", false));
        attributes.put("Shifting Type", ensureAttribute(attributeRepository, tenantId, "Shifting Type", false));
        attributes.put("Wheel Size", ensureAttribute(attributeRepository, tenantId, "Wheel Size", false));
        attributes.put("Motor System", ensureAttribute(attributeRepository, tenantId, "Motor System", false));
        attributes.put("Battery Range", ensureAttribute(attributeRepository, tenantId, "Battery Range", false));
        attributes.put("Bottom Bracket", ensureAttribute(attributeRepository, tenantId, "Bottom Bracket", true));
        attributes.put("Mounting Points", ensureAttribute(attributeRepository, tenantId, "Mounting Points", true));

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Pinarello",
                "X3 105 Di2",
                2025,
                BikeCategory.ROAD,
                BikeSaleType.COMPLETE_BIKE,
                new BigDecimal("5299.00"),
                "Premium endurance road build for boutique retail customers seeking long-distance comfort with race-inspired handling.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://pinarello.com/europe/en/bikes/road/endurance/pinarello-x",
                Map.of(
                        "Frame Material", List.of("Carbon"),
                        "Brake Type", List.of("Hydraulic Disc"),
                        "Shifting Type", List.of("Electronic"),
                        "Wheel Size", List.of("700c"),
                        "Bottom Bracket", List.of("T47")
                ),
                List.of(
                        new SeedSku("PIN-X3-54-BLK", "Gloss Black", "54cm", 168, 176, 558, 387, 2, BigDecimal.ZERO),
                        new SeedSku("PIN-X3-56-BLK", "Gloss Black", "56cm", 176, 184, 575, 392, 1, BigDecimal.ZERO)
                )
        );

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Allied",
                "Alfa Disc Frameset",
                2025,
                BikeCategory.ROAD,
                BikeSaleType.FRAMESET,
                new BigDecimal("3199.00"),
                "Carbon race frameset with premium paint and clean internal routing for custom builds.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://alliedcycleworks.com/products/alfa-frameset-custom",
                Map.of(
                        "Frame Material", List.of("Carbon"),
                        "Wheel Size", List.of("700c"),
                        "Bottom Bracket", List.of("T47"),
                        "Mounting Points", List.of("Minimal")
                ),
                List.of(
                        new SeedSku("ALL-ALFA-54-WHT", "Pearl White", "54cm", 168, 176, 545, 387, 1, BigDecimal.ZERO),
                        new SeedSku("ALL-ALFA-56-WHT", "Pearl White", "56cm", 176, 184, 562, 392, 1, BigDecimal.ZERO)
                )
        );

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Cervelo",
                "Aspero GRX 820",
                2025,
                BikeCategory.GRAVEL,
                BikeSaleType.COMPLETE_BIKE,
                new BigDecimal("4499.00"),
                "Fast gravel platform for mixed-terrain rides, weekend escapes, and premium all-road positioning.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://www.cervelo.com/en-US/bikes/aspero",
                Map.of(
                        "Frame Material", List.of("Carbon"),
                        "Brake Type", List.of("Hydraulic Disc"),
                        "Shifting Type", List.of("Mechanical"),
                        "Wheel Size", List.of("700c"),
                        "Bottom Bracket", List.of("BBright"),
                        "Mounting Points", List.of("Adventure Ready")
                ),
                List.of(
                        new SeedSku("CER-ASP-54-GRN", "Moss Green", "54cm", 168, 176, 580, 384, 3, BigDecimal.ZERO),
                        new SeedSku("CER-ASP-56-GRN", "Moss Green", "56cm", 176, 184, 597, 389, 2, BigDecimal.ZERO)
                )
        );

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Santa Cruz",
                "Tallboy C GX",
                2025,
                BikeCategory.MTB,
                BikeSaleType.COMPLETE_BIKE,
                new BigDecimal("5799.00"),
                "Progressive trail bike tuned for all-day speed, modern handling, and high-touch retailer service.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://www.santacruzbicycles.com/products/tallboy-gx-axs-2025",
                Map.of(
                        "Frame Material", List.of("Carbon"),
                        "Brake Type", List.of("Hydraulic Disc"),
                        "Shifting Type", List.of("Mechanical"),
                        "Wheel Size", List.of("29in"),
                        "Bottom Bracket", List.of("Threaded")
                ),
                List.of(
                        new SeedSku("SC-TBY-L-BLU", "Deep Blue", "L", 178, 188, 636, 474, 2, BigDecimal.ZERO),
                        new SeedSku("SC-TBY-XL-BLU", "Deep Blue", "XL", 188, 198, 649, 499, 1, BigDecimal.ZERO)
                )
        );

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Specialized",
                "Turbo Vado 4.0",
                2025,
                BikeCategory.E_BIKE,
                BikeSaleType.COMPLETE_BIKE,
                new BigDecimal("4299.00"),
                "Urban premium e-bike aimed at daily mobility programs with upright comfort and integrated commuter equipment.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://www.specialized.com/us/en/turbo-vado-40/p/4277441",
                Map.of(
                        "Frame Material", List.of("Aluminum"),
                        "Brake Type", List.of("Hydraulic Disc"),
                        "Shifting Type", List.of("Electronic"),
                        "Wheel Size", List.of("700c"),
                        "Motor System", List.of("Specialized 2.0"),
                        "Battery Range", List.of("Up to 120 km"),
                        "Mounting Points", List.of("Commuter Rack Ready")
                ),
                List.of(
                        new SeedSku("SPZ-VADO-M-SLV", "Satin Silver", "M", 165, 178, 610, 405, 4, BigDecimal.ZERO),
                        new SeedSku("SPZ-VADO-L-SLV", "Satin Silver", "L", 178, 190, 628, 418, 2, BigDecimal.ZERO)
                )
        );

        seedBike(
                bikeRepository,
                attributeRepository,
                valueRepository,
                tenantId,
                "Orbea",
                "Rise Frameset",
                2025,
                BikeCategory.E_BIKE,
                BikeSaleType.FRAMESET,
                new BigDecimal("3899.00"),
                "Light-assist e-MTB frameset for custom boutique builds that still need sizing and fit support.",
                "https://image.thum.io/get/width/1400/crop/900/noanimate/https://www.orbea.com/us-en/ebikes/mountain/rise/",
                Map.of(
                        "Frame Material", List.of("Carbon"),
                        "Wheel Size", List.of("29in"),
                        "Motor System", List.of("Shimano EP8 RS"),
                        "Battery Range", List.of("Up to 100 km"),
                        "Bottom Bracket", List.of("PressFit"),
                        "Mounting Points", List.of("Minimal")
                ),
                List.of(
                        new SeedSku("ORB-RISE-M-RAW", "Raw Carbon", "M", 168, 180, 619, 455, 1, BigDecimal.ZERO),
                        new SeedSku("ORB-RISE-L-RAW", "Raw Carbon", "L", 180, 192, 633, 475, 1, BigDecimal.ZERO)
                )
        );
    }

    private BikeSpecAttribute ensureAttribute(
            BikeSpecAttributeRepository attributeRepository,
            String tenantId,
            String name,
            boolean custom
    ) {
        return attributeRepository.findByTenantIdAndAttributeNameIgnoreCase(tenantId, name)
                .orElseGet(() -> attributeRepository.save(BikeSpecAttribute.builder()
                        .tenantId(tenantId)
                        .attributeName(name)
                        .isCustom(custom)
                        .build()));
    }

    private void seedBike(
            BikeRepository bikeRepository,
            BikeSpecAttributeRepository attributeRepository,
            BikeSpecValueRepository valueRepository,
            String tenantId,
            String brandName,
            String modelName,
            int modelYear,
            BikeCategory category,
            BikeSaleType saleType,
            BigDecimal basePrice,
            String description,
            String imageUrl,
            Map<String, List<String>> specs,
            List<SeedSku> skus
    ) {
        Bike bike = bikeRepository.findByTenantIdAndBrandNameIgnoreCaseAndModelNameIgnoreCaseAndModelYearAndSaleType(
                        tenantId,
                        brandName,
                        modelName,
                        modelYear,
                        saleType
                )
                .orElseGet(() -> Bike.builder()
                        .tenantId(tenantId)
                        .createdAt(LocalDateTime.now().minusDays(2))
                        .build());

        bike.setBrandName(brandName);
        bike.setModelName(modelName);
        bike.setModelYear(modelYear);
        bike.setCategory(category);
        bike.setSaleType(saleType);
        bike.setBasePrice(basePrice);
        bike.setDescription(description);
        bike.setImageUrl(imageUrl);
        bike.setIsActive(true);

        bike = bikeRepository.saveAndFlush(bike);
        bike.getSpecMappings().clear();
        bike.getSkus().clear();
        bike = bikeRepository.saveAndFlush(bike);

        for (Map.Entry<String, List<String>> entry : specs.entrySet()) {
            BikeSpecAttribute attribute = attributeRepository.findByTenantIdAndAttributeNameIgnoreCase(tenantId, entry.getKey())
                    .orElseThrow();
            for (String valueText : entry.getValue()) {
                BikeSpecValue value = valueRepository.findByAttributeIdAndValueTextIgnoreCase(attribute.getId(), valueText)
                        .orElseGet(() -> valueRepository.save(BikeSpecValue.builder()
                                .attribute(attribute)
                                .valueText(valueText)
                                .build()));
                bike.getSpecMappings().add(BikeSpecMapping.builder()
                        .id(new BikeSpecMappingId(bike.getId(), value.getId()))
                        .bike(bike)
                        .specValue(value)
                        .build());
            }
        }

        for (SeedSku sku : skus) {
            bike.getSkus().add(BikeSku.builder()
                    .bike(bike)
                    .skuCode(seedSkuCode(tenantId, sku.skuCode()))
                    .colorName(sku.colorName())
                    .sizeValue(sku.sizeValue())
                    .riderHeightMinCm(sku.riderHeightMinCm())
                    .riderHeightMaxCm(sku.riderHeightMaxCm())
                    .stackMm(sku.stackMm())
                    .reachMm(sku.reachMm())
                    .stockQuantity(sku.stockQuantity())
                    .priceModifier(sku.priceModifier())
                    .build());
        }

        bikeRepository.save(bike);
    }

    private String seedSkuCode(String tenantId, String skuCode) {
        String normalizedTenant = tenantId == null ? "tenant" : tenantId
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "")
                .toUpperCase(Locale.ROOT);
        if (normalizedTenant.isBlank()) {
            normalizedTenant = "TENANT";
        }
        return normalizedTenant + "-" + skuCode;
    }

    private record SeedSku(
            String skuCode,
            String colorName,
            String sizeValue,
            Integer riderHeightMinCm,
            Integer riderHeightMaxCm,
            Integer stackMm,
            Integer reachMm,
            Integer stockQuantity,
            BigDecimal priceModifier
    ) {
    }
}
