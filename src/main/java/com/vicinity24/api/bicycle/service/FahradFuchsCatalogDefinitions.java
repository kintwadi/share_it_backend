package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.valueobject.BikeType;
import com.vicinity24.api.bicycle.domain.valueobject.InventoryStatus;
import com.vicinity24.api.bicycle.dto.FahradFuchsFrameOptionDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsStoreDto;
import com.vicinity24.api.bicycle.dto.FahradFuchsTechnicalSpecDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FahradFuchsCatalogDefinitions {

    public static final String STORE_NAME = "Fahrad-Fuchs";
    public static final String ADDRESS_LINE_1 = "Darmstaedter Strasse 36";
    public static final String CITY_LINE = "64521 Gross-Gerau";
    public static final String PHONE = "06152 - 55 795";
    public static final String EMAIL = "info@fahrrad-fuchs.de";
    public static final String MAP_URL = "https://maps.google.com/?q=Darmstaedter+Strasse+36+64521+Gross-Gerau";
    public static final String AVAILABILITY_BADGE = "Available for Try-Before-You-Buy";

    private FahradFuchsCatalogDefinitions() {
    }

    public static FahradFuchsStoreDto store() {
        return new FahradFuchsStoreDto(
                STORE_NAME,
                ADDRESS_LINE_1,
                CITY_LINE,
                PHONE,
                EMAIL,
                MAP_URL,
                List.of(
                        "Mon: 10:00-12:00 and 14:00-18:00",
                        "Tue: 10:00-12:00 and 14:00-18:00",
                        "Wed: Closed",
                        "Thu: 10:00-12:00 and 14:00-18:00",
                        "Fri: 10:00-12:00 and 14:00-18:00",
                        "Sat: 10:00-13:00",
                        "Sun: Closed"
                )
        );
    }

    public static List<FahradFuchsBikeDefinition> all() {
        return List.of(
                new FahradFuchsBikeDefinition(
                        "specialized-turbo-tero-3",
                        "Specialized Turbo Tero 3.0",
                        "E-Bike",
                        BikeType.E_BIKE,
                        InventoryStatus.ON_FLOOR_ASSEMBLED,
                        new BigDecimal("45.00"),
                        new BigDecimal("3599.00"),
                        "Perfect for handling hilly daily commutes without arriving overheated.",
                        "Perfect for handling hilly daily commutes without sweating.",
                        List.of(
                                "Perfect for handling hilly daily commutes without sweating.",
                                "Up to 90 miles of lithium-ion battery assistance per charge.",
                                "Comfortable, upright frame geometry for long-duration riding comfort."
                        ),
                        List.of(
                                new FahradFuchsTechnicalSpecDto("Motor", "Specialized 2.0E mid-drive"),
                                new FahradFuchsTechnicalSpecDto("Battery", "530 Wh internal battery"),
                                new FahradFuchsTechnicalSpecDto("Brakes", "Hydraulic disc brakes"),
                                new FahradFuchsTechnicalSpecDto("Drivetrain", "Shimano 9-speed LinkGlide"),
                                new FahradFuchsTechnicalSpecDto("Weight", "Approx. 24.5 kg")
                        ),
                        List.of(
                                new FahradFuchsFrameOptionDto("M", "Medium (Fits riders 5'6\" to 5'10\")"),
                                new FahradFuchsFrameOptionDto("L", "Large (Fits riders 5'10\" to 6'2\")")
                        ),
                        gallery(
                                "https://images.unsplash.com/photo-1571068316344-75bc76f77890?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1511994298241-608e28f14fde?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1541625602330-2277a4c46182?auto=format&fit=crop&w=1400&q=80"
                        )
                ),
                new FahradFuchsBikeDefinition(
                        "kalkhoff-image-5b-advance",
                        "Kalkhoff Image 5.B Advance",
                        "Comfort E-Bike",
                        BikeType.E_BIKE,
                        InventoryStatus.ON_FLOOR_ASSEMBLED,
                        new BigDecimal("39.00"),
                        new BigDecimal("2799.00"),
                        "Upright city e-bike with integrated rack capacity and everyday comfort.",
                        "Ideal for comfortable city rides, errands, and upright all-day support.",
                        List.of(
                                "Relaxed, step-through design for easy starts and stops in town.",
                                "Integrated lights, rack, and fenders for real daily practicality.",
                                "Smooth pedal assistance for city loops and relaxed weekend rides."
                        ),
                        List.of(
                                new FahradFuchsTechnicalSpecDto("Motor", "Bosch Performance Line"),
                                new FahradFuchsTechnicalSpecDto("Battery", "625 Wh Bosch PowerTube"),
                                new FahradFuchsTechnicalSpecDto("Brakes", "Shimano hydraulic disc"),
                                new FahradFuchsTechnicalSpecDto("Drivetrain", "8-speed Shimano Nexus"),
                                new FahradFuchsTechnicalSpecDto("Frame", "Wave aluminum comfort frame")
                        ),
                        List.of(
                                new FahradFuchsFrameOptionDto("S", "Small (Fits riders 5'2\" to 5'6\")"),
                                new FahradFuchsFrameOptionDto("M", "Medium (Fits riders 5'6\" to 5'10\")"),
                                new FahradFuchsFrameOptionDto("L", "Large (Fits riders 5'10\" to 6'1\")")
                        ),
                        gallery(
                                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1571068316344-75bc76f77890?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1511994298241-608e28f14fde?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=1400&q=80"
                        )
                ),
                new FahradFuchsBikeDefinition(
                        "orbea-orca-m30",
                        "Orbea Orca M30",
                        "Road Bike",
                        BikeType.ROAD,
                        InventoryStatus.ON_FLOOR_ASSEMBLED,
                        new BigDecimal("32.00"),
                        new BigDecimal("2599.00"),
                        "Lightweight carbon road bike for fast weekend loops and sportive riders.",
                        "Great for fast solo training rides and weekend club runs.",
                        List.of(
                                "Responsive carbon frame tuned for climbing and quick accelerations.",
                                "Road-focused geometry with stable handling for long distance efforts.",
                                "Ideal for riders comparing lightweight endurance-to-race options."
                        ),
                        List.of(
                                new FahradFuchsTechnicalSpecDto("Frame", "Orca carbon monocoque"),
                                new FahradFuchsTechnicalSpecDto("Groupset", "Shimano 105 12-speed"),
                                new FahradFuchsTechnicalSpecDto("Brakes", "Hydraulic disc brakes"),
                                new FahradFuchsTechnicalSpecDto("Wheelset", "Tubeless-ready alloy wheels"),
                                new FahradFuchsTechnicalSpecDto("Weight", "Approx. 8.6 kg")
                        ),
                        List.of(
                                new FahradFuchsFrameOptionDto("53", "Medium (Fits riders 5'6\" to 5'10\")"),
                                new FahradFuchsFrameOptionDto("57", "Large (Fits riders 5'10\" to 6'2\")")
                        ),
                        gallery(
                                "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1485965120184-e220f721d03e?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1541625602330-2277a4c46182?auto=format&fit=crop&w=1400&q=80"
                        )
                ),
                new FahradFuchsBikeDefinition(
                        "cannondale-topstone-2",
                        "Cannondale Topstone 2",
                        "Gravel Bike",
                        BikeType.GRAVEL,
                        InventoryStatus.ON_FLOOR_ASSEMBLED,
                        new BigDecimal("34.00"),
                        new BigDecimal("1899.00"),
                        "Versatile gravel all-rounder for bike paths, mixed terrain, and longer test routes.",
                        "Confident on asphalt, canal paths, and rougher mixed-surface loops.",
                        List.of(
                                "Stable off-road handling for riders exploring gravel for the first time.",
                                "Wide tire clearance and confident disc braking on broken surfaces.",
                                "A smart option for long mixed-terrain days and light bikepacking trials."
                        ),
                        List.of(
                                new FahradFuchsTechnicalSpecDto("Frame", "SmartForm C2 alloy frame"),
                                new FahradFuchsTechnicalSpecDto("Groupset", "Shimano GRX 10-speed"),
                                new FahradFuchsTechnicalSpecDto("Brakes", "Hydraulic disc brakes"),
                                new FahradFuchsTechnicalSpecDto("Tires", "700x37c mixed-surface tires"),
                                new FahradFuchsTechnicalSpecDto("Mounts", "Rack and fender ready")
                        ),
                        List.of(
                                new FahradFuchsFrameOptionDto("54", "Medium (Fits riders 5'6\" to 5'10\")"),
                                new FahradFuchsFrameOptionDto("58", "Large (Fits riders 5'10\" to 6'2\")")
                        ),
                        gallery(
                                "https://images.unsplash.com/photo-1517649763962-0c623066013b?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1558981806-ec527fa84c39?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1511994298241-608e28f14fde?auto=format&fit=crop&w=1400&q=80",
                                "https://images.unsplash.com/photo-1541625602330-2277a4c46182?auto=format&fit=crop&w=1400&q=80"
                        )
                )
        );
    }

    public static Map<String, FahradFuchsBikeDefinition> bySlug() {
        return all().stream().collect(Collectors.toMap(FahradFuchsBikeDefinition::slug, Function.identity()));
    }

    private static List<String> gallery(String first, String second, String third, String fourth) {
        return List.of(first, second, third, fourth);
    }

    public record FahradFuchsBikeDefinition(
            String slug,
            String title,
            String category,
            BikeType bikeType,
            InventoryStatus inventoryStatus,
            BigDecimal dailyRate,
            BigDecimal retailPrice,
            String description,
            String teaser,
            List<String> valuePoints,
            List<FahradFuchsTechnicalSpecDto> technicalSpecs,
            List<FahradFuchsFrameOptionDto> frameOptions,
            List<String> gallery
    ) {
    }
}
