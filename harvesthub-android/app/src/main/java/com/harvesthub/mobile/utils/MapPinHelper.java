package com.harvesthub.mobile.utils;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

/**
 * Shared "pin on a map" helpers for the OpenStreetMap screens: one search box that accepts a place name,
 * coordinates or a Google Maps link; "Check this pin in Google Maps"; coordinates copied from Google Maps;
 * reverse-geocoded address without the Google plus code.
 */
public final class MapPinHelper {

    private MapPinHelper() {
    }

    public interface SearchResult {
        void onPoint(double lat, double lng);

        /** Nothing found / no connection: show {@code message} to the user. */
        void onNotFound(String message);
    }

    public static boolean insideSriLanka(double lat, double lng) {
        return lat >= 5.7 && lat <= 10.1 && lng >= 79.3 && lng <= 82.1;
    }

    /**
     * Resolves what the user typed. Coordinates answer at once; short links and place names are looked up on
     * {@code executor} and answered on the main thread.
     */
    public static void search(Context context, String query, ExecutorService executor, SearchResult result) {
        Handler main = new Handler(Looper.getMainLooper());
        double[] point = CoordinateParser.parse(query);
        if (point != null) {
            result.onPoint(point[0], point[1]);
            return;
        }
        if (CoordinateParser.isShortMapsLink(query)) {
            String start = query.startsWith("http") ? query : "https://" + query;
            executor.execute(() -> {
                double[] p = followShortLink(start);
                main.post(() -> {
                    if (p != null) result.onPoint(p[0], p[1]);
                    else result.onNotFound("Could not read coordinates from this link. In Google Maps, press and hold the place and copy the numbers.");
                });
            });
            return;
        }
        if (CoordinateParser.isLink(query)) {
            result.onNotFound("This link has no coordinates. In Google Maps, press and hold the place, then copy the numbers shown.");
            return;
        }
        if (!Geocoder.isPresent()) {
            result.onNotFound("Place search is not available on this phone. Drag the map or paste coordinates.");
            return;
        }
        Context app = context.getApplicationContext();
        executor.execute(() -> {
            List<Address> found = null;
            boolean failed = false;
            try {   // limit results to Sri Lanka
                found = new Geocoder(app, Locale.getDefault()).getFromLocationName(query + ", Sri Lanka", 1, 5.7, 79.3, 10.1, 82.1);
            } catch (Exception e) {
                failed = true;
            }
            List<Address> f = found;
            boolean error = failed;
            main.post(() -> {
                if (f != null && !f.isEmpty()) result.onPoint(f.get(0).getLatitude(), f.get(0).getLongitude());
                else result.onNotFound(error ? "Search needs an internet connection. Try again or drag the map."
                        : "No place found for \"" + query + "\". Try a nearby town.");
            });
        });
    }

    /** maps.app.goo.gl/... share links redirect to a full link that contains the coordinates. */
    private static double[] followShortLink(String url) {
        try {
            for (int i = 0; i < 5 && url != null; i++) {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setInstanceFollowRedirects(false);
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                String next = c.getHeaderField("Location");
                c.disconnect();
                double[] p = CoordinateParser.parse(next);
                if (p != null) return p;
                url = next;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Address line for a point (run off the main thread), without a leading Google plus code; null if unknown. */
    public static String addressFor(Context context, double lat, double lng) {
        try {
            List<Address> list = new Geocoder(context.getApplicationContext(), Locale.getDefault()).getFromLocation(lat, lng, 1);
            if (list == null || list.isEmpty()) return null;
            String line = list.get(0).getAddressLine(0);
            if (line == null) return null;
            line = line.replaceFirst("^[23456789CFGHJMPQRVWX]{4,8}\\+[23456789CFGHJMPQRVWX]{2,3},?\\s*", "").trim();
            return line.isEmpty() ? null : line;
        } catch (Exception e) {
            return null;
        }
    }

    /** Opens the point in the Google Maps app (or the browser) so the user can check it on satellite view. */
    public static void openInGoogleMaps(Activity activity, double lat, double lng) {
        String coords = String.format(Locale.US, "%.6f,%.6f", lat, lng);
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + coords)));
            Toast.makeText(activity, "To move the pin: in Google Maps press and hold the right place, copy the coordinates and come back.",
                    Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, "No app found to open Google Maps.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Coordinates in Sri Lanka currently on the clipboard (e.g. copied in Google Maps), or null.
     * Android only allows this while the app has window focus - call it from onWindowFocusChanged.
     */
    public static double[] copiedCoordinates(Context context) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip()) return null;
            ClipData clip = cm.getPrimaryClip();
            if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) return null;
            double[] p = CoordinateParser.parse(clip.getItemAt(0).getText().toString());
            return p != null && insideSriLanka(p[0], p[1]) ? p : null;
        } catch (Exception e) {
            return null;
        }
    }
}
