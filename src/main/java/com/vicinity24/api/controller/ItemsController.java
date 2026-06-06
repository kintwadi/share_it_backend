package com.vicinity24.api.controller;

import com.vicinity24.api.dto.ItemListingRequest;
import com.vicinity24.api.model.Item;
import com.vicinity24.api.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemsController {
    private final ItemService itemService;

    public ItemsController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping("/")
    public ResponseEntity<Item> create(@RequestBody ItemListingRequest req) {
        return ResponseEntity.ok(itemService.create(req));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<Item>> nearby(
            @RequestParam(name = "lat") double lat,
            @RequestParam(name = "lng") double lng,
            @RequestParam(name = "radiusKm", defaultValue = "25") double radiusKm,
            @RequestParam(name = "size", defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(itemService.findNearby(lat, lng, radiusKm, size));
    }
}

