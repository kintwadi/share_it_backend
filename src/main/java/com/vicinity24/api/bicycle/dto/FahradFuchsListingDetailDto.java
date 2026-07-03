package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FahradFuchsListingDetailDto(
        UUID listingId,
        String slug,
        String title,
        String category,
        String availabilityBadge,
        String description,
        BigDecimal dailyRate,
        BigDecimal retailPrice,
        String imageUrl,
        List<String> gallery,
        List<String> valuePoints,
        List<FahradFuchsTechnicalSpecDto> technicalSpecs,
        List<FahradFuchsFrameOptionDto> frameOptions,
        FahradFuchsStoreDto store
) {
}
