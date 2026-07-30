package com.vicinity24.api.linked.store.controller;

import com.vicinity24.api.linked.store.dto.StoreCategoryRequest;
import com.vicinity24.api.linked.store.dto.StoreCategoryResponse;
import com.vicinity24.api.linked.store.service.StoreCatalogCategoryService;
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
@RequestMapping({"/api/categories", "/api/v1/categories"})
public class StoreCatalogCategoryController {
    private final StoreCatalogCategoryService categoryService;

    public StoreCatalogCategoryController(StoreCatalogCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreCategoryResponse create(@Valid @RequestBody StoreCategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping
    public List<StoreCategoryResponse> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/{id}")
    public StoreCategoryResponse getById(@PathVariable("id") Long id) {
        return categoryService.getById(id);
    }

    @PutMapping("/{id}")
    public StoreCategoryResponse update(@PathVariable("id") Long id, @Valid @RequestBody StoreCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
    }
}


