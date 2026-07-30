package com.vicinity24.api.linked.store.controller;

import com.vicinity24.api.linked.store.dto.StoreResponse;
import com.vicinity24.api.linked.store.dto.StoreUpsertRequest;
import com.vicinity24.api.linked.store.service.StoreCatalogStoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/stores", "/api/v1/stores"})
public class StoreCatalogStoreController {
    private final StoreCatalogStoreService storeService;

    public StoreCatalogStoreController(StoreCatalogStoreService storeService) {
        this.storeService = storeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoreResponse create(@Valid @RequestBody StoreUpsertRequest request) {
        return storeService.create(request);
    }

    @GetMapping("/{id}")
    public StoreResponse getById(@PathVariable("id") Long id) {
        return storeService.getById(id);
    }

    @GetMapping
    public List<StoreResponse> getAll() {
        return storeService.getAll();
    }
}


