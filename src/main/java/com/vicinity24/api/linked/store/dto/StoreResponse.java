package com.vicinity24.api.linked.store.dto;

import java.time.Instant;

public record StoreResponse(
        Long id,
        String name,
        String slug,
        String bannerImageUrl,
        Instant createdAt
) {
}


