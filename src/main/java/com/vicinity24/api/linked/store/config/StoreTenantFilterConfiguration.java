package com.vicinity24.api.linked.store.config;

import com.vicinity24.api.linked.store.filter.StoreRequestTenantFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class StoreTenantFilterConfiguration {
    @Bean
    public StoreRequestTenantFilter storeRequestTenantFilter() {
        return new StoreRequestTenantFilter();
    }

    @Bean
    public FilterRegistrationBean<StoreRequestTenantFilter> storeRequestTenantFilterRegistration(
            StoreRequestTenantFilter filter
    ) {
        FilterRegistrationBean<StoreRequestTenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setName("storeRequestTenantFilter");
        registration.addUrlPatterns(
                "/api/stores", "/api/stores/*",
                "/api/v1/stores", "/api/v1/stores/*",
                "/api/categories", "/api/categories/*",
                "/api/v1/categories", "/api/v1/categories/*",
                "/api/products", "/api/products/*",
                "/api/v1/products", "/api/v1/products/*",
                "/api/variants", "/api/variants/*",
                "/api/v1/variants", "/api/v1/variants/*"
        );
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 25);
        return registration;
    }
}


