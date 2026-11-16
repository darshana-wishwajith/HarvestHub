package com.harvesthub.backend.service;

import java.util.Set;

/** Validation of the transporter vehicle details (Figma transporter "Vehicle Profile", decisions 15 Nov). */
public final class TransporterProfile {

    /** Backend vehicle classes (MarketplaceRules.vehicles) – user kept these instead of the Figma 500/1,000/3,000/5,000 kg cards. */
    public static final Set<String> VEHICLE_TYPES = Set.of("THREE_WHEELER", "MINI_LORRY", "LORRY", "TRUCK");
    /** Label only, not used in dispatch. */
    public static final Set<String> BODY_TYPES = Set.of("REEFER", "FLATBED", "BOX");

    private TransporterProfile() {}

    public static String normalisePlate(String plate) {
        return plate == null ? null : plate.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    /** Sri Lankan plates: optional province letters, 2-3 letters or digits, dash, 4 digits (e.g. "WP ND-4892", "65-1234"). */
    public static void checkPlate(String plate) {
        String p = normalisePlate(plate);
        if (p == null || p.isEmpty()) throw new IllegalStateException("Enter the licence plate number");
        if (!p.matches("^([A-Z]{2} )?[A-Z0-9]{2,3}[- ]?\\d{4}$")) {
            throw new IllegalStateException("Licence plate must look like WP ND-4892");
        }
    }

    public static void checkBody(String body) {
        if (body != null && !body.isBlank() && !BODY_TYPES.contains(body.trim().toUpperCase())) {
            throw new IllegalStateException("Vehicle configuration must be REEFER, FLATBED or BOX");
        }
    }
}
