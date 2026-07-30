package com.vicinity24.api.linked.store.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreUpsertRequest(
        @NotBlank String name,
        @NotBlank String slug
) {
}


