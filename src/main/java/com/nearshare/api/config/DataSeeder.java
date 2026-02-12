package com.nearshare.api.config;

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
}
