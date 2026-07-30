package com.vicinity24.api.linked.store.dto;

import java.util.List;
import java.util.Map;

public record StoreCategoryResponse(
        Long id,
        Long storeId,
        Long parentId,
        String name,
        String slug,
        Map<String, Object> attributeSchema,
        List<StoreCategoryResponse> children
) {
}


