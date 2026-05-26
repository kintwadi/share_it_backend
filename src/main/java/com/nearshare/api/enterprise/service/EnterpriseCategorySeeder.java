package com.nearshare.api.enterprise.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnterpriseCategorySeeder {
    private final EnterpriseCategoryService service;

    @PostConstruct
    public void seed() {
        service.ensureSeededFromMarkdown();
    }
}

