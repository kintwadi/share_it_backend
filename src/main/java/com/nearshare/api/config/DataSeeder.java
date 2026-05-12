package com.nearshare.api.config;

import com.nearshare.api.model.Category;
import com.nearshare.api.model.PickupLocation;
import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.repository.CategoryRepository;
import com.nearshare.api.repository.PickupLocationRepository;
import com.nearshare.api.service.MockDataSeederService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seed(MockDataSeederService mockDataSeederService) {
        return args -> {
            String result = mockDataSeederService.seedMockData();
            System.out.println(result);
        };
    }

    @Bean
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seedPickupLocations(PickupLocationRepository pickupLocationRepository) {
        return args -> {
            if (pickupLocationRepository.count() > 0) {
                return;
            }
            pickupLocationRepository.save(PickupLocation.builder()
                    .id(java.util.UUID.randomUUID())
                    .name("Concierge (building front desk)")
                    .address("Frankfurterstr 2455, Frankfurt")
                    .location(Location.builder().lat(50.1109).lng(8.6821).build())
                    .active(true)
                    .build());
            pickupLocationRepository.save(PickupLocation.builder()
                    .id(java.util.UUID.randomUUID())
                    .name("Local bakery partner")
                    .address("Leipzigerstr 102, Frankfurt")
                    .location(Location.builder().lat(50.1180).lng(8.6512).build())
                    .active(true)
                    .build());
            pickupLocationRepository.save(PickupLocation.builder()
                    .id(java.util.UUID.randomUUID())
                    .name("Public meetup spot")
                    .address("Konstablerwache 5, Frankfurt")
                    .location(Location.builder().lat(50.1147).lng(8.6873).build())
                    .active(true)
                    .build());
        };
    }

    @Bean
    @ConditionalOnProperty(name = "seeding.enabled", havingValue = "true")
    CommandLineRunner seedCategories(CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() > 0) {
                return;
            }
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Tools")
                    .labelKey("category.tools")
                    .active(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Gardening")
                    .labelKey("category.gardening")
                    .active(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Kitchen")
                    .labelKey("category.kitchen")
                    .active(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Outdoors")
                    .labelKey("category.outdoors")
                    .active(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Music")
                    .labelKey("category.music")
                    .active(true)
                    .build());
            categoryRepository.save(Category.builder()
                    .id(java.util.UUID.randomUUID())
                    .code("Misc")
                    .labelKey("category.misc")
                    .active(true)
                    .build());
        };
    }
}
