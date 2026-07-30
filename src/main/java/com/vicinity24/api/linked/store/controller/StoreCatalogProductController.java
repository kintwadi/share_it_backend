package com.vicinity24.api.linked.store.controller;

import com.vicinity24.api.linked.store.dto.StoreProductRequest;
import com.vicinity24.api.linked.store.dto.StoreProductResponse;
import com.vicinity24.api.linked.store.service.StoreCatalogProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/products", "/api/v1/products"})
public class StoreCatalogProductController {
    private final StoreCatalogProductService productService;

    public StoreCatalogProductController(StoreCatalogProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreProductResponse create(@Valid @RequestBody StoreProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public List<StoreProductResponse> getAll(@RequestParam(name = "categoryId", required = false) Long categoryId) {
        return productService.getAll(categoryId);
    }

    @GetMapping("/{id}")
    public StoreProductResponse getById(@PathVariable("id") Long id) {
        return productService.getById(id);
    }

    @PutMapping("/{id}")
    public StoreProductResponse update(@PathVariable("id") Long id, @Valid @RequestBody StoreProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        productService.delete(id);
    }
}


