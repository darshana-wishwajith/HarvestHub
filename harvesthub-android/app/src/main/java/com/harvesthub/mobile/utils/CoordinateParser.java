package com.harvesthub.mobile.utils;

import java.net.URLDecoder;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a latitude/longitude from what a farmer pastes into the map search box:
 * plain decimal coordinates ("8.35461, 80.50211"), degrees-minutes-seconds as Google Maps shows them
 * (8°21'16.6"N 80°30'07.6"E) or a full Google Maps link. Pure Java so it can be unit tested.
 */
public final class CoordinateParser {

    private CoordinateParser() {
    }

    private static final String DEC = "(-?\\d{1,3}\\.\\d+)";
    /** Exact place pin inside a Google Maps "place" link. */
    private static final Pattern PLACE_PIN = Pattern.compile("!3d" + DEC + "!4d" + DEC);
    /** ?q=lat,lng / ?query=lat,lng / ?ll=... / ?destination=... */
    private static final Pattern QUERY = Pattern.compile("[?&](?:q|query|ll|center|destination|daddr)=(?:loc:)?" + DEC + ",\\s*" + DEC);
    /** Camera position in a link: /@lat,lng,17z */
    private static final Pattern AT = Pattern.compile("@" + DEC + "," + DEC);
    /** Plain "lat, lng" (at least 3 decimals so it is not confused with other numbers). */
    private static final Pattern PLAIN = Pattern.compile("(?<![\\d.])(-?\\d{1,2}\\.\\d{3,})\\s*[,;\\s]\\s*(-?\\d{1,3}\\.\\d{3,})(?![\\d.])");
    /** 8°21'16.6"N 80°30'07.6"E */
    private static final Pattern DMS = Pattern.compile(
            "(\\d{1,2})\\s*°\\s*(\\d{1,2})\\s*['′]\\s*([\\d.]+)\\s*(?:\"|″|'')?\\s*([NS])\\W*"
                    + "(\\d{1,3})\\s*°\\s*(\\d{1,2})\\s*['′]\\s*([\\d.]+)\\s*(?:\"|″|'')?\\s*([EW])",
            Pattern.CASE_INSENSITIVE);

    /** @return {lat, lng} or null when the text holds no coordinates. */
    public static double[] parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        if (s.isEmpty()) return null;
        try {
            s = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8");
        } catch (Exception ignored) {
            // keep the raw text
        }
        double[] r;
        if ((r = match(PLACE_PIN, s)) != null) return r;
        if ((r = match(QUERY, s)) != null) return r;
        if ((r = match(AT, s)) != null) return r;
        if ((r = matchDms(s)) != null) return r;
        return match(PLAIN, s);
    }

    /** Short share links (maps.app.goo.gl/...) hold no coordinates until they are followed. */
    public static boolean isShortMapsLink(String text) {
        if (text == null) return false;
        String s = text.toLowerCase(Locale.ROOT);
        return s.contains("maps.app.goo.gl/") || s.contains("goo.gl/maps/");
    }

    public static boolean isLink(String text) {
        return text != null && text.trim().toLowerCase(Locale.ROOT).matches("^(https?://|www\\.|maps\\.).*");
    }

    private static double[] match(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        return valid(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
    }

    private static double[] matchDms(String s) {
        Matcher m = DMS.matcher(s);
        if (!m.find()) return null;
        double lat = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / 60.0 + Double.parseDouble(m.group(3)) / 3600.0;
        double lng = Integer.parseInt(m.group(5)) + Integer.parseInt(m.group(6)) / 60.0 + Double.parseDouble(m.group(7)) / 3600.0;
        if (m.group(4).equalsIgnoreCase("S")) lat = -lat;
        if (m.group(8).equalsIgnoreCase("W")) lng = -lng;
        return valid(lat, lng);
    }

    private static double[] valid(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) return null;
        return new double[]{lat, lng};
    }
}
