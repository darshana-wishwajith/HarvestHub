package com.harvesthub.desktop.api;

/**
 * Holds the logged-in officer's JWT and profile for the lifetime of the desktop app.
 * ApiClient reads the token from here to attach "Authorization: Bearer ..." to every call.
 */
public final class Session {

    private static String token;
    private static OfficerDto officer;

    private Session() {
    }

    public static void set(String jwt, OfficerDto loggedInOfficer) {
        token = jwt;
        officer = loggedInOfficer;
    }

    public static String getToken() {
        return token;
    }

    public static OfficerDto getOfficer() {
        return officer;
    }

    public static boolean isLoggedIn() {
        return token != null;
    }

    public static void clear() {
        token = null;
        officer = null;
    }
}
