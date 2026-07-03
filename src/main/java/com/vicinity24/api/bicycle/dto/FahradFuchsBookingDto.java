package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FahradFuchsBookingDto(
        UUID bookingId,
        String bookingReference,
        UUID listingId,
        String bikeSlug,
        String bikeTitle,
        LocalDate startDate,
        LocalDate endDate,
        String frameSizeOption,
        BigDecimal totalAmount,
        String currency,
        String status,
        String imageUrl,
        LocalDateTime createdAt
) {
}
