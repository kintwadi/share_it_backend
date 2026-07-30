package com.vicinity24.api.linked.store.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record StoreCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        Long parentId,
        Map<String, Object> attributeSchema
) {
}


