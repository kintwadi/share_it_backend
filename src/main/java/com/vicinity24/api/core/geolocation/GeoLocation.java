package com.vicinity24.api.core.geolocation;

public record GeoLocation(
        Double latitude,
        Double longitude,
        String city,
        String region,
        String country,
        String ip
) {
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}

