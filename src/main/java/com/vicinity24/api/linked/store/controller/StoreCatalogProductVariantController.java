package com.vicinity24.api.linked.store.controller;

import com.vicinity24.api.linked.store.dto.StoreProductVariantRequest;
import com.vicinity24.api.linked.store.dto.StoreProductVariantResponse;
import com.vicinity24.api.linked.store.service.StoreCatalogProductVariantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/products/{productId}/variants", "/api/v1/products/{productId}/variants"})
public class StoreCatalogProductVariantController {
    private final StoreCatalogProductVariantService variantService;

    public StoreCatalogProductVariantController(StoreCatalogProductVariantService variantService) {
        this.variantService = variantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductVariantResponse create(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody StoreProductVariantRequest request
    ) {
        return variantService.create(productId, request);
    }

    @GetMapping
    public List<StoreProductVariantResponse> getAll(@PathVariable("productId") Long productId) {
        return variantService.getAll(productId);
    }

    @GetMapping("/{variantId}")
    public StoreProductVariantResponse getById(
            @PathVariable("productId") Long productId,
            @PathVariable("variantId") Long variantId
    ) {
        return variantService.getById(productId, variantId);
    }

    @PutMapping("/{variantId}")
    public StoreProductVariantResponse update(
            @PathVariable("productId") Long productId,
            @PathVariable("variantId") Long variantId,
            @Valid @RequestBody StoreProductVariantRequest request
    ) {
        return variantService.update(productId, variantId, request);
    }

    @DeleteMapping("/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("productId") Long productId, @PathVariable("variantId") Long variantId) {
        variantService.delete(productId, variantId);
    }
}


