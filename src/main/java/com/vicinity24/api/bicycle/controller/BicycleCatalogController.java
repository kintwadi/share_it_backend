package com.vicinity24.api.bicycle.controller;

import com.vicinity24.api.bicycle.dto.BicycleCatalogPageDto;
import com.vicinity24.api.bicycle.dto.BicycleDetailDto;
import com.vicinity24.api.bicycle.dto.RentToOwnConversionRequest;
import com.vicinity24.api.bicycle.dto.RentToOwnQuoteDto;
import com.vicinity24.api.bicycle.service.BicycleCatalogService;
import com.vicinity24.api.bicycle.service.RentToOwnConversionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/bikes")
public class BicycleCatalogController {

    private final BicycleCatalogService bicycleCatalogService;
    private final RentToOwnConversionService rentToOwnConversionService;

    public BicycleCatalogController(BicycleCatalogService bicycleCatalogService, RentToOwnConversionService rentToOwnConversionService) {
        this.bicycleCatalogService = bicycleCatalogService;
        this.rentToOwnConversionService = rentToOwnConversionService;
    }

    @GetMapping
    public ResponseEntity<BicycleCatalogPageDto> search(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "frameSize", required = false) String frameSize,
            @RequestParam(name = "bikeType", required = false) String bikeType,
            @RequestParam(name = "inventoryStatus", required = false) String inventoryStatus,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(bicycleCatalogService.search(search, city, frameSize, bikeType, inventoryStatus, sort, PageRequest.of(page, size)));
    }

    @GetMapping("/{listingId}")
    public ResponseEntity<BicycleDetailDto> getById(@PathVariable("listingId") UUID listingId) {
        return ResponseEntity.ok(bicycleCatalogService.getByListingId(listingId));
    }

    @GetMapping("/{listingId}/rent-to-own/quote")
    public ResponseEntity<RentToOwnQuoteDto> quote(
            @PathVariable("listingId") UUID listingId,
            @RequestParam(name = "borrowerId", required = false) UUID borrowerId
    ) {
        return ResponseEntity.ok(rentToOwnConversionService.quote(listingId, borrowerId));
    }

    @PostMapping("/{listingId}/rent-to-own/convert")
    public ResponseEntity<RentToOwnQuoteDto> convert(
            @PathVariable("listingId") UUID listingId,
            @RequestBody(required = false) RentToOwnConversionRequest request
    ) {
        RentToOwnConversionRequest payload = request != null ? request : new RentToOwnConversionRequest();
        return ResponseEntity.ok(rentToOwnConversionService.convert(
                listingId,
                payload.getBorrowerId(),
                payload.getPaymentMethod(),
                payload.getPaymentToken()
        ));
    }
}
