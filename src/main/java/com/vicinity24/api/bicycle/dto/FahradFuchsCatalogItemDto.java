package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FahradFuchsCatalogItemDto(
        UUID listingId,
        String slug,
        String title,
        String category,
        String teaser,
        String availabilityBadge,
        BigDecimal dailyRate,
        BigDecimal retailPrice,
        String imageUrl,
        List<String> highlights
) {
}
