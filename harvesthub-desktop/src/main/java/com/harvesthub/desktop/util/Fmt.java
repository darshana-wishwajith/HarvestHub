package com.harvesthub.desktop.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display formatting shared by every screen. Missing values become a dash, never an invented number. */
public final class Fmt {

    public static final String NONE = "—";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private Fmt() {
    }

    // ---- JSON access -----------------------------------------------------------------------

    public static String text(JsonNode n, String field) {
        if (n == null || !n.hasNonNull(field)) return null;
        String s = n.get(field).asText();
        return s.isBlank() ? null : s;
    }

    public static String textOr(JsonNode n, String field, String fallback) {
        String s = text(n, field);
        return s == null ? fallback : s;
    }

    public static Double num(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) && n.get(field).isNumber() ? n.get(field).asDouble() : null;
    }

    public static boolean bool(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) && n.get(field).asBoolean();
    }

    public static long count(JsonNode n, String field) {
        return n != null && n.hasNonNull(field) ? n.get(field).asLong() : 0;
    }

    // ---- numbers --------------------------------------------------------------------------

    public static String money(Double v) {
        if (v == null) return NONE;
        DecimalFormat f = new DecimalFormat(Math.abs(v - Math.rint(v)) < 0.005 ? "#,##0" : "#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return (v < 0 ? "- Rs. " : "Rs. ") + f.format(Math.abs(v));
    }

    public static String kg(Double v) {
        if (v == null) return NONE;
        return new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)).format(v) + " kg";
    }

    public static String perKg(Double v) {
        return v == null ? NONE : money(v) + " / kg";
    }

    public static String metres(Double m) {
        if (m == null) return NONE;
        return m >= 1000 ? String.format(Locale.ENGLISH, "%.1f km", m / 1000) : String.format(Locale.ENGLISH, "%.0f m", m);
    }

    // ---- dates (server sends ISO local date-time, Sri Lanka time) ---------------------------------

    public static LocalDateTime parse(String iso) {
        if (iso == null || iso.isBlank()) return null;
        String t = iso.trim().replace(' ', 'T');
        int dot = t.indexOf('.');
        if (dot > 0) t = t.substring(0, dot);
        try {
            if (t.length() == 10) return LocalDate.parse(t).atStartOfDay();
            if (t.length() == 16) t = t + ":00";
            return LocalDateTime.parse(t.substring(0, Math.min(19, t.length())));
        } catch (Exception e) {
            return null;
        }
    }

    public static String date(String iso) {
        LocalDateTime d = parse(iso);
        return d == null ? NONE : DATE.format(d);
    }

    public static String dateTime(String iso) {
        LocalDateTime d = parse(iso);
        return d == null ? NONE : DATE_TIME.format(d);
    }

    /** "just now", "12 min ago", "3 h ago", "yesterday, 4:10 PM", "12 Nov 2026". */
    public static String ago(String iso, LocalDateTime now) {
        LocalDateTime d = parse(iso);
        if (d == null) return NONE;
        Duration diff = Duration.between(d, now);
        long min = diff.toMinutes();
        if (min < 0) return DATE_TIME.format(d);
        if (min < 1) return "just now";
        if (min < 60) return min + " min ago";
        if (min < 24 * 60 && d.toLocalDate().equals(now.toLocalDate())) return (min / 60) + " h ago";
        if (d.toLocalDate().equals(now.toLocalDate().minusDays(1))) return "yesterday, " + TIME.format(d);
        long days = diff.toDays();
        if (days < 7) return days + " days ago";
        return DATE.format(d);
    }

    public static String ago(String iso) {
        return ago(iso, LocalDateTime.now());
    }

    // ---- labels --------------------------------------------------------------------------------

    public static String title(String constant) {
        if (constant == null || constant.isBlank()) return NONE;
        String s = constant.replace('_', ' ').toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String role(String role) {
        if (role == null) return NONE;
        return switch (role.toUpperCase(Locale.ENGLISH)) {
            case "FARMER" -> "Farmer";
            case "BUYER" -> "Buyer";
            case "TRANSPORTER" -> "Transporter";
            case "OFFICER", "ADMIN" -> "Agricultural Officer";
            default -> title(role);
        };
    }

    /** Order status → words the officer understands. */
    public static String orderStatus(String s) {
        if (s == null) return NONE;
        return switch (s) {
            case "NEGOTIATING" -> "Negotiating";
            case "AWAITING_PAYMENT" -> "Waiting for advance";
            case "PROCESSING_DISPATCH", "DISPATCHING" -> "Finding a driver";
            case "TRANSPORTER_ASSIGNED" -> "Driver requested";
            case "TRANSPORTER_ACCEPTED" -> "Driver accepted";
            case "HARVESTING" -> "Harvesting";
            case "HARVEST_READY" -> "Harvest ready";
            case "ARRIVED_AT_PICKUP" -> "Driver at the farm";
            case "AWAITING_BUYER_LOADING_APPROVAL" -> "Buyer checking loading photos";
            case "LOADING_APPROVED" -> "Loading approved";
            case "IN_TRANSIT" -> "In transit";
            case "ARRIVED_AT_DESTINATION" -> "Arrived at buyer";
            case "BALANCE_PAID" -> "Balance paid";
            case "COMPLETED" -> "Completed";
            case "PRELOAD_REJECTED" -> "Rejected at loading";
            case "RETURN_JOURNEY" -> "Rejected on delivery – returning";
            case "RETURNED" -> "Returned to farm";
            case "CANCELLED" -> "Cancelled";
            default -> title(s);
        };
    }

    public static String decision(String d) {
        if (d == null) return NONE;
        return switch (d) {
            case "APPROVED" -> "Approved";
            case "RETAKE" -> "Retake requested";
            case "REJECTED" -> "Rejected";
            default -> title(d);
        };
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "HH";
        String[] p = name.trim().split("\\s+");
        if (p.length == 1) return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase(Locale.ENGLISH);
        return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase(Locale.ENGLISH);
    }

    public static String orDash(String s) {
        return s == null || s.isBlank() ? NONE : s;
    }
}
