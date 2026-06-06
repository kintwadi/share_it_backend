package com.vicinity24.api.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vicinity24.api.dto.LocationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String defaultCountryCodes;

    public LocationService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${locationiq.api.key:}") String apiKey,
            @Value("${locationiq.base-url:https://us1.locationiq.com}") String baseUrl,
            @Value("${locationiq.countrycodes:pt,de,fr,be}") String defaultCountryCodes,
            @Value("${locationiq.timeout.ms:8000}") long timeoutMs
    ) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "https://us1.locationiq.com" : baseUrl.trim();
        this.defaultCountryCodes = defaultCountryCodes == null ? "" : defaultCountryCodes.trim();
        Duration timeout = Duration.ofMillis(Math.max(1000, Math.min(30000, timeoutMs)));
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    public LocationResponse forwardGeocode(String streetAddress, String city, String postalCode, String country) {
        requireApiKey();
        String query = normalizeAddressQuery(streetAddress, city, postalCode, country);
        if (query.isBlank()) throw new IllegalArgumentException("address_required");

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/search.php")
                .queryParam("key", apiKey)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("normalizeaddress", 1)
                .queryParam("limit", 1)
                .build(true)
                .toUri();

        try {
            ResponseEntity<LocationIqSearchResult[]> response = restTemplate.getForEntity(uri, LocationIqSearchResult[].class);
            LocationIqSearchResult[] results = response.getBody();
            if (results == null || results.length == 0) throw new IllegalArgumentException("address_not_found");
            return toLocationResponse(results[0]);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("locationiq_timeout");
        } catch (RestClientException e) {
            throw new RuntimeException("locationiq_error");
        }
    }

    public LocationResponse reverseGeocode(double latitude, double longitude) {
        requireApiKey();

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/reverse.php")
                .queryParam("key", apiKey)
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("normalizeaddress", 1)
                .build(true)
                .toUri();

        try {
            ResponseEntity<LocationIqReverseResult> response = restTemplate.getForEntity(uri, LocationIqReverseResult.class);
            LocationIqReverseResult result = response.getBody();
            if (result == null) throw new IllegalArgumentException("address_not_found");
            return toLocationResponse(result);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("locationiq_timeout");
        } catch (RestClientException e) {
            throw new RuntimeException("locationiq_error");
        }
    }

    public List<LocationResponse> autocomplete(String query, String countryCodes, int limit) {
        requireApiKey();
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) return List.of();

        String cc = (countryCodes == null || countryCodes.isBlank()) ? defaultCountryCodes : countryCodes.trim();
        int lim = Math.max(1, Math.min(10, limit));

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/autocomplete.php")
                .queryParam("key", apiKey)
                .queryParam("q", q)
                .queryParam("format", "json")
                .queryParam("addressdetails", 1)
                .queryParam("normalizeaddress", 1)
                .queryParam("dedupe", 1)
                .queryParam("limit", lim)
                .queryParam("countrycodes", cc)
                .build(true)
                .toUri();

        try {
            ResponseEntity<LocationIqSearchResult[]> response = restTemplate.getForEntity(uri, LocationIqSearchResult[].class);
            LocationIqSearchResult[] results = response.getBody();
            if (results == null || results.length == 0) return List.of();
            List<LocationResponse> out = new ArrayList<>();
            for (LocationIqSearchResult r : results) {
                if (r == null) continue;
                out.add(toLocationResponse(r));
            }
            return out;
        } catch (ResourceAccessException e) {
            return List.of();
        } catch (RestClientException e) {
            return List.of();
        }
    }

    private void requireApiKey() {
        if (apiKey.isBlank()) throw new IllegalStateException("locationiq_api_key_missing");
    }

    private String normalizeAddressQuery(String streetAddress, String city, String postalCode, String country) {
        String s = streetAddress == null ? "" : streetAddress.trim();
        String c = city == null ? "" : city.trim();
        String p = postalCode == null ? "" : postalCode.trim();
        String co = country == null ? "" : country.trim();

        StringBuilder sb = new StringBuilder();
        if (!s.isBlank()) sb.append(s);
        if (!c.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(c);
        }
        if (!p.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(p);
        }
        if (!co.isBlank()) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(co);
        }
        return sb.toString();
    }

    private LocationResponse toLocationResponse(LocationIqSearchResult r) {
        Double lat = parseDouble(r.lat);
        Double lon = parseDouble(r.lon);
        LocationIqAddress a = r.address;

        String city = firstNonBlank(a != null ? a.city : null, a != null ? a.town : null, a != null ? a.village : null, a != null ? a.county : null);
        String street = joinNonBlank(" ", a != null ? a.road : null, a != null ? a.house_number : null);

        return LocationResponse.builder()
                .displayName(blankToNull(r.display_name))
                .streetAddress(blankToNull(street))
                .city(blankToNull(city))
                .postalCode(blankToNull(a != null ? a.postcode : null))
                .country(blankToNull(a != null ? a.country : null))
                .countryCode(blankToNull(a != null ? a.country_code : null))
                .latitude(lat)
                .longitude(lon)
                .build();
    }

    private LocationResponse toLocationResponse(LocationIqReverseResult r) {
        LocationIqAddress a = r.address;
        String city = firstNonBlank(a != null ? a.city : null, a != null ? a.town : null, a != null ? a.village : null, a != null ? a.county : null);
        String street = joinNonBlank(" ", a != null ? a.road : null, a != null ? a.house_number : null);

        return LocationResponse.builder()
                .displayName(blankToNull(r.display_name))
                .streetAddress(blankToNull(street))
                .city(blankToNull(city))
                .postalCode(blankToNull(a != null ? a.postcode : null))
                .country(blankToNull(a != null ? a.country : null))
                .countryCode(blankToNull(a != null ? a.country_code : null))
                .latitude(parseDouble(r.lat))
                .longitude(parseDouble(r.lon))
                .build();
    }

    private Double parseDouble(String v) {
        try {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            String s = blankToNull(v);
            if (s != null) return s;
        }
        return null;
    }

    private String joinNonBlank(String sep, String a, String b) {
        String x = blankToNull(a);
        String y = blankToNull(b);
        if (x == null) return y == null ? "" : y;
        if (y == null) return x;
        return x + (sep == null ? " " : sep) + y;
    }

    private String blankToNull(String v) {
        String s = v == null ? null : v.trim();
        if (s == null || s.isEmpty()) return null;
        return s;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocationIqSearchResult {
        public String lat;
        public String lon;
        public String display_name;
        public LocationIqAddress address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocationIqReverseResult {
        public String lat;
        public String lon;
        public String display_name;
        public LocationIqAddress address;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class LocationIqAddress {
        public String house_number;
        public String road;
        public String city;
        public String town;
        public String village;
        public String county;
        public String postcode;
        public String country;
        public String country_code;
    }
}

