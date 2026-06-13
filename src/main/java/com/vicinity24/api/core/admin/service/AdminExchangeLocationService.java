package com.vicinity24.api.core.admin.service;

import com.vicinity24.api.core.admin.dto.AdminExchangeLocationDTO;
import com.vicinity24.api.core.admin.dto.AdminExchangeLocationUpsertRequest;
import com.vicinity24.api.core.dto.LocationDTO;
import com.vicinity24.api.core.model.ExchangeLocation;
import com.vicinity24.api.core.model.embeddable.Location;
import com.vicinity24.api.core.repository.ExchangeLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
public class AdminExchangeLocationService {
    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_LEN = 8;

    private final ExchangeLocationRepository exchangeLocationRepository;
    private final SecureRandom random = new SecureRandom();

    public AdminExchangeLocationService(ExchangeLocationRepository exchangeLocationRepository) {
        this.exchangeLocationRepository = exchangeLocationRepository;
    }

    @PostConstruct
    @Transactional
    public void ensureReferenceIds() {
        List<ExchangeLocation> all = exchangeLocationRepository.findAll();
        boolean changed = false;
        for (ExchangeLocation p : all) {
            if (p == null) continue;
            String ref = p.getReferenceId();
            if (ref != null && !ref.trim().isEmpty()) continue;
            p.setReferenceId(generateUniqueReferenceId());
            changed = true;
        }
        if (changed) {
            exchangeLocationRepository.saveAll(all);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminExchangeLocationDTO> listAll() {
        return exchangeLocationRepository.findAll().stream().map(this::toAdminDto).toList();
    }

    @Transactional
    public AdminExchangeLocationDTO create(AdminExchangeLocationUpsertRequest body) {
        if (body == null) throw new IllegalArgumentException("invalid_request");
        String name = normalize(body.getName());
        if (name.isEmpty()) throw new IllegalArgumentException("name_required");

        String address = normalize(body.getAddress());
        String street = normalize(body.getStreetAddress());
        String city = normalize(body.getCity());
        String postal = normalize(body.getPostalCode());
        String country = normalize(body.getCountry());
        if (address.isEmpty()) {
            address = joinAddress(street, city, postal, country);
        }
        if (address.isEmpty()) throw new IllegalArgumentException("address_required");

        Double lat = body.getLatitude();
        Double lng = body.getLongitude();
        Location location = (lat != null && lng != null) ? Location.builder().lat(lat).lng(lng).build() : null;

        boolean active = body.getActive() == null || Boolean.TRUE.equals(body.getActive());

        ExchangeLocation p = ExchangeLocation.builder()
                .id(UUID.randomUUID())
                .referenceId(generateUniqueReferenceId())
                .name(name)
                .address(address)
                .streetAddress(street.isEmpty() ? null : street)
                .city(city.isEmpty() ? null : city)
                .postalCode(postal.isEmpty() ? null : postal)
                .country(country.isEmpty() ? null : country)
                .location(location)
                .operatingTimeFrom(normalize(body.getOperatingTimeFrom()).isEmpty() ? null : normalize(body.getOperatingTimeFrom()))
                .operatingTimeTo(normalize(body.getOperatingTimeTo()).isEmpty() ? null : normalize(body.getOperatingTimeTo()))
                .active(active)
                .build();

        exchangeLocationRepository.save(p);
        return toAdminDto(p);
    }

    @Transactional
    public AdminExchangeLocationDTO update(UUID id, AdminExchangeLocationUpsertRequest body) {
        if (id == null) throw new IllegalArgumentException("id_required");
        if (body == null) throw new IllegalArgumentException("invalid_request");

        ExchangeLocation p = exchangeLocationRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("location_not_found"));

        String name = normalize(body.getName());
        if (!name.isEmpty()) p.setName(name);

        String street = normalize(body.getStreetAddress());
        String city = normalize(body.getCity());
        String postal = normalize(body.getPostalCode());
        String country = normalize(body.getCountry());
        if (!street.isEmpty()) p.setStreetAddress(street);
        if (!city.isEmpty()) p.setCity(city);
        if (!postal.isEmpty()) p.setPostalCode(postal);
        if (!country.isEmpty()) p.setCountry(country);

        String address = normalize(body.getAddress());
        if (address.isEmpty()) {
            address = joinAddress(p.getStreetAddress(), p.getCity(), p.getPostalCode(), p.getCountry());
        }
        if (!address.isEmpty()) p.setAddress(address);

        if (body.getLatitude() != null && body.getLongitude() != null) {
            if (p.getLocation() == null) p.setLocation(Location.builder().lat(body.getLatitude()).lng(body.getLongitude()).build());
            else {
                p.getLocation().setLat(body.getLatitude());
                p.getLocation().setLng(body.getLongitude());
            }
        }

        if (body.getOperatingTimeFrom() != null) {
            String opFrom = normalize(body.getOperatingTimeFrom());
            p.setOperatingTimeFrom(opFrom.isEmpty() ? null : opFrom);
        }
        if (body.getOperatingTimeTo() != null) {
            String opTo = normalize(body.getOperatingTimeTo());
            p.setOperatingTimeTo(opTo.isEmpty() ? null : opTo);
        }

        if (body.getActive() != null) p.setActive(Boolean.TRUE.equals(body.getActive()));

        exchangeLocationRepository.save(p);
        return toAdminDto(p);
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) throw new IllegalArgumentException("id_required");
        if (!exchangeLocationRepository.existsById(id)) throw new IllegalArgumentException("location_not_found");
        exchangeLocationRepository.deleteById(id);
    }

    private AdminExchangeLocationDTO toAdminDto(ExchangeLocation p) {
        return AdminExchangeLocationDTO.builder()
                .id(p.getId())
                .referenceId(p.getReferenceId())
                .name(p.getName())
                .address(p.getAddress())
                .streetAddress(p.getStreetAddress())
                .city(p.getCity())
                .postalCode(p.getPostalCode())
                .country(p.getCountry())
                .location(LocationDTO.builder()
                        .x(p.getLocation() != null ? p.getLocation().getLat() : null)
                        .y(p.getLocation() != null ? p.getLocation().getLng() : null)
                        .build())
                .operatingTimeFrom(p.getOperatingTimeFrom())
                .operatingTimeTo(p.getOperatingTimeTo())
                .active(p.isActive())
                .build();
    }

    private String generateUniqueReferenceId() {
        for (int i = 0; i < 50; i++) {
            String ref = randomRef();
            if (!exchangeLocationRepository.existsByReferenceId(ref)) return ref;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String randomRef() {
        StringBuilder sb = new StringBuilder(REF_LEN);
        for (int i = 0; i < REF_LEN; i++) {
            int idx = random.nextInt(REF_CHARS.length());
            sb.append(REF_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private String normalize(String v) {
        return v == null ? "" : v.trim();
    }

    private String joinAddress(String street, String city, String postal, String country) {
        String s = normalize(street);
        String c = normalize(city);
        String p = normalize(postal);
        String co = normalize(country);
        StringBuilder out = new StringBuilder();
        if (!s.isEmpty()) out.append(s);
        if (!c.isEmpty()) {
            if (!out.isEmpty()) out.append(", ");
            out.append(c);
        }
        if (!p.isEmpty()) {
            if (!out.isEmpty()) out.append(" ");
            out.append(p);
        }
        if (!co.isEmpty()) {
            if (!out.isEmpty()) out.append(", ");
            out.append(co);
        }
        return out.toString().trim();
    }
}
