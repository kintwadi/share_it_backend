package com.vicinity24.api.bicycle.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FahradFuchsCheckoutRequest(
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @FutureOrPresent LocalDate endDate,
        @NotBlank String frameSizeOption,
        String paymentMethod,
        String paymentToken
) {
}
