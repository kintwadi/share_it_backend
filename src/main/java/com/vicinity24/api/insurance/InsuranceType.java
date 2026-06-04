package com.vicinity24.api.insurance;

import com.vicinity24.api.insurance.exception.InvalidInsuranceTypeException;

import java.util.Locale;

/**
 * Supported insurance types.
 */
public enum InsuranceType {
    BASIC,
    PREMIUM,
    THEFT_PROTECTION,
    EXTENDED_WARRANTY;

    /**
     * Parses an insurance type from a user-provided string.
     * Accepts values like "basic", "BASIC", "theft protection", "theft/damage protection".
     *
     * @param raw input string
     * @return insurance type
     */
    public static InsuranceType fromString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidInsuranceTypeException("insurance_type_required");
        }
        String normalized = raw.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace('/', '_');
        normalized = normalized.replaceAll("_+", "_");
        if ("THEFT".equals(normalized) || "THEFT_DAMAGE".equals(normalized) || "THEFT_DAMAGE_PROTECTION".equals(normalized)) {
            normalized = "THEFT_PROTECTION";
        }
        try {
            return InsuranceType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidInsuranceTypeException("invalid_insurance_type");
        }
    }
}

