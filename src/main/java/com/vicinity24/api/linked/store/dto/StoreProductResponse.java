package com.vicinity24.api.linked.store.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record StoreProductResponse(
        Long id,
        Long storeId,
        String sku,
        String name,
        String description,
        BigDecimal basePrice,
        String currency,
        Long categoryId,
        Map<String, Object> properties,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}


