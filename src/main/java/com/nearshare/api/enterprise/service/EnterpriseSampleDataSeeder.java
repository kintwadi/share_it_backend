package com.nearshare.api.enterprise.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnterpriseSampleDataSeeder {
    private final EnterpriseSampleDataService service;

    @Value("${settings.enterprise.seeded-data.auto-load:${settings.enterprise.sample-data.auto-load:true}}")
    private boolean autoLoad;

    @Value("${settings.enterprise.seeded-data.limit:${settings.enterprise.sample-data.limit:80}}")
    private int limit;

    @PostConstruct
    public void seed() {
        if (!autoLoad) return;
        service.load(false, limit);
    }
}
