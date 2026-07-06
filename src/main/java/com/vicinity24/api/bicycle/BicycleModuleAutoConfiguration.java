package com.vicinity24.api.bicycle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan(basePackages = "com.vicinity24.api.bicycle")
@EntityScan(basePackages = {
        "com.vicinity24.api.core",
        "com.vicinity24.api.bicycle.domain.model"
})
@EnableJpaRepositories(basePackages = {
        "com.vicinity24.api.core.repository",
        "com.vicinity24.api.core.partner.repository",
        "com.vicinity24.api.core.recommendation.repository",
        "com.vicinity24.api.core.config",
        "com.vicinity24.api.bicycle.repository"
})
public class BicycleModuleAutoConfiguration {
}
