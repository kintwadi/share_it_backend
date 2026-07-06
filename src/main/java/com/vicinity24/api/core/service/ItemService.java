package com.vicinity24.api.core.service;

import com.vicinity24.api.core.dto.ItemListingRequest;
import com.vicinity24.api.core.model.Item;
import com.vicinity24.api.core.repository.ItemRepository;
import com.vicinity24.api.core.util.GeohashUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final LocationService locationService;

    public ItemService(ItemRepository itemRepository, LocationService locationService) {
        this.itemRepository = itemRepository;
        this.locationService = locationService;
    }

    @Transactional
    public Item create(ItemListingRequest req) {
        if (req == null) throw new IllegalArgumentException("invalid_request");
        String title = safe(req.getTitle());
        if (title.isEmpty()) throw new IllegalArgumentException("title_required");
        var loc = locationService.forwardGeocode(req.getStreetAddress(), req.getCity(), req.getPostalCode(), req.getCountry());
        Double lat = loc != null ? loc.getLatitude() : null;
        Double lng = loc != null ? loc.getLongitude() : null;
        Item item = Item.builder()
                .id(UUID.randomUUID())
                .title(title)
                .latitude(lat)
                .longitude(lng)
                .streetAddress(loc != null ? loc.getStreetAddress() : safeNull(req.getStreetAddress()))
                .city(loc != null ? loc.getCity() : safeNull(req.getCity()))
                .postalCode(loc != null ? loc.getPostalCode() : safeNull(req.getPostalCode()))
                .country(loc != null ? loc.getCountry() : safeNull(req.getCountry()))
                .geohash(GeohashUtil.encode(lat, lng, 9))
                .build();
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<Item> findNearby(double borrowerLat, double borrowerLng, double radiusKm, int size) {
        var rows = itemRepository.findNearby(borrowerLat, borrowerLng, Math.max(0.1, radiusKm), PageRequest.of(0, Math.max(1, Math.min(200, size))));
        if (rows == null || rows.isEmpty()) return List.of();
        var ids = rows.stream().map(ItemRepository.ItemDistanceRow::getId).toList();
        var items = itemRepository.findAllById(ids);
        var byId = items.stream().collect(java.util.stream.Collectors.toMap(Item::getId, x -> x));
        List<Item> out = new ArrayList<>();
        for (var row : rows) {
            var item = byId.get(row.getId());
            if (item != null) out.add(item);
        }
        return out;
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private String safeNull(String v) {
        String s = safe(v);
        return s.isEmpty() ? null : s;
    }
}

