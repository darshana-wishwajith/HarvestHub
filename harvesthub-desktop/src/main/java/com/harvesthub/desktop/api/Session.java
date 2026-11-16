package com.harvesthub.desktop.api;

import java.util.Set;

/**
 * The signed-in officer's JWT and profile for the lifetime of the desktop app (memory only, never written to disk).
 */
public final class Session {

    /** Roles allowed into the portal: Agricultural Officers (ADMIN is a legacy account type with the same rights). */
    public static final Set<String> PORTAL_ROLES = Set.of("OFFICER", "ADMIN");

    private static volatile String token;
    private static volatile OfficerDto officer;

    private Session() {
    }

    public static void set(String jwt, OfficerDto loggedInOfficer) {
        token = jwt;
        officer = loggedInOfficer;
    }

    public static boolean mayUsePortal(String role) {
        return role != null && PORTAL_ROLES.contains(role.toUpperCase());
    }

    public static String getToken() {
        return token;
    }

    public static OfficerDto getOfficer() {
        return officer;
    }

    public static String officerId() {
        return officer == null ? null : officer.id;
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    public static void clear() {
        token = null;
        officer = null;
    }
}
