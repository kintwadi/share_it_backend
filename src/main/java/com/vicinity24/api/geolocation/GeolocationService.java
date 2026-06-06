package com.vicinity24.api.geolocation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vicinity24.api.config.ConfigProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeolocationService {
    private static final String BASE_URL_KEY = "geolocation.freeipapi.base_url";
    private static final String DEFAULT_BASE_URL = "https://free.freeipapi.com/api/json/";

    private final ObjectMapper objectMapper;
    private final ConfigProvider config;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String devIp;
    private final Duration cacheTtl;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeolocationService(
            ObjectMapper objectMapper,
            ConfigProvider config,
            @Value("${geolocation.enabled:true}") boolean enabled,
            @Value("${geolocation.dev-ip:}") String devIp,
            @Value("${geolocation.cache-ttl-seconds:900}") long cacheTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.enabled = enabled;
        this.devIp = devIp == null ? "" : devIp.trim();
        this.cacheTtl = Duration.ofSeconds(Math.max(30, cacheTtlSeconds));
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public GeoLocation resolve(String ipAddress) {
        if (!enabled) return null;

        String ip = normalizeIp(ipAddress);
        if (ip == null && !devIp.isEmpty()) {
            ip = normalizeIp(devIp);
        }
        if (ip == null) return null;

        CacheEntry cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) return cached.value;

        GeoLocation fetched = fetch(ip);
        Duration ttl = fetched != null ? cacheTtl : Duration.ofSeconds(60);
        cache.put(ip, new CacheEntry(fetched, Instant.now().plus(ttl)));
        return fetched;
    }

    private GeoLocation fetch(String ip) {
        String baseUrl = normalizeBaseUrl(config != null ? config.getString(BASE_URL_KEY, DEFAULT_BASE_URL) : DEFAULT_BASE_URL);
        if (baseUrl == null) return null;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ip))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 200) {
                FreeIpApiResponse data = objectMapper.readValue(response.body(), FreeIpApiResponse.class);
                if (data == null) return null;
                return new GeoLocation(
                        data.latitude,
                        data.longitude,
                        blankToNull(data.cityName),
                        blankToNull(data.regionName),
                        blankToNull(data.countryName),
                        ip
                );
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String v = baseUrl == null ? null : baseUrl.trim();
        if (v == null || v.isEmpty()) return null;
        if (!v.startsWith("http://") && !v.startsWith("https://")) return null;
        return v.endsWith("/") ? v : (v + "/");
    }

    private String normalizeIp(String ip) {
        String v = ip == null ? null : ip.trim();
        if (v == null || v.isEmpty()) return null;
        if ("unknown".equalsIgnoreCase(v)) return null;
        if (v.contains(",")) v = v.split(",")[0].trim();
        return v;
    }

    private String blankToNull(String s) {
        String v = s == null ? null : s.trim();
        if (v == null || v.isEmpty()) return null;
        return v;
    }

    private record CacheEntry(GeoLocation value, Instant expiresAt) {
        boolean isExpired() {
            return expiresAt == null || Instant.now().isAfter(expiresAt);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FreeIpApiResponse {
        public String ipAddress;
        public String countryName;
        public String regionName;
        public String cityName;
        public Double latitude;
        public Double longitude;
    }
}
