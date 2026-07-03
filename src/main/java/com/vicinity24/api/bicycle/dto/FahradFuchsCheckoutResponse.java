package com.vicinity24.api.bicycle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FahradFuchsCheckoutResponse(
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
        String paymentMethod,
        String status,
        FahradFuchsStoreDto store
) {
}
