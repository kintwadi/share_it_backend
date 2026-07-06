package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class BikeCatalogImageService {

    public String buildPreviewImage(Bike bike) {
        if (bike.getImageUrl() != null && !bike.getImageUrl().isBlank()) {
            return bike.getImageUrl();
        }

        Theme theme = switch (bike.getCategory()) {
            case ROAD -> new Theme("4c1d95", "ede9fe", "Road");
            case GRAVEL -> new Theme("92400e", "ffedd5", "Gravel");
            case MTB -> new Theme("164e63", "cffafe", "MTB");
            case E_BIKE -> new Theme("14532d", "dcfce7", "E-Bike");
        };
        String text = bike.displayName() + "\n" + theme.label() + "  " + bike.getModelYear();
        return "https://placehold.co/1200x800/" + theme.background() + "/" + theme.foreground()
                + "/png?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    public String buildSpecChip(BikeCategory category) {
        return switch (category) {
            case ROAD -> "Race-tuned";
            case GRAVEL -> "All-road";
            case MTB -> "Trail-ready";
            case E_BIKE -> "Pedal assist";
        };
    }

    private record Theme(String background, String foreground, String label) {
    }
}
