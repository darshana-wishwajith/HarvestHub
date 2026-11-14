package com.harvesthub.mobile.utils;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Response;

/** Plain-Java helpers for the auth screens (no Android classes, so they are unit-testable). */
public final class AuthInputUtils {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private AuthInputUtils() {}

    public static boolean isEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    /**
     * Accounts store mobiles as 07XXXXXXXX. Accepts "077 123 4567", "771234567", "+94771234567", "94771234567".
     * Returns null if it is not a valid Sri Lankan mobile number.
     */
    public static String normaliseMobile(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        } else if (digits.length() == 9 && digits.startsWith("7")) {
            digits = "0" + digits;
        }
        return digits.matches("07\\d{8}") ? digits : null;
    }

    /** Email (lower-cased) or normalised mobile, or null if the input is neither. */
    public static String normaliseIdentifier(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.contains("@")) return isEmail(s) ? s.toLowerCase() : null;
        return normaliseMobile(s);
    }

    /** "kamal@gmail.com" -> "kama***@gmail.com"; short names keep their first letter only. */
    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) return email;
        String name = email.substring(0, at);
        int keep = name.length() > 4 ? 4 : 1;
        return name.substring(0, keep) + "***" + email.substring(at);
    }

    /** At least 8 characters with at least one letter and one digit (same rule as registration). */
    public static boolean isStrongEnough(String password) {
        return password != null && password.length() >= 8 && password.length() <= 128
                && password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
    }

    /** Reads {"error": "..."} or a field->message map from an error response; falls back to {@code fallback}. */
    public static String errorMessage(Response<?> response, String fallback) {
        try (ResponseBody body = response.errorBody()) {
            if (body == null) return fallback;
            String text = body.string();
            if (text == null || !text.trim().startsWith("{")) return fallback;
            JSONObject json = new JSONObject(text);
            if (json.has("error")) return json.optString("error", fallback);
            if (json.has("message")) return json.optString("message", fallback);
            Iterator<String> keys = json.keys();
            return keys.hasNext() ? json.optString(keys.next(), fallback) : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }
}
