package com.vicinity24.api.linked;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ComponentScan(basePackages = "com.vicinity24.api.linked.store")
@EntityScan(basePackages = {
        "com.vicinity24.api.core",
        "com.vicinity24.api.linked.store.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.vicinity24.api.linked.store.repository"
})
public class LinkedStoreModuleAutoConfiguration {
}
