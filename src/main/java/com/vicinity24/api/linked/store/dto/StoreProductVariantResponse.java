package com.vicinity24.api.linked.store.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record StoreProductVariantResponse(
        Long id,
        Long storeId,
        Long productId,
        String sku,
        BigDecimal price,
        int stock,
        Map<String, Object> options,
        boolean isActive,
        Instant createdAt
) {
}


