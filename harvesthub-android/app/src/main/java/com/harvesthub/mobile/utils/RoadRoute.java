package com.harvesthub.mobile.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Road-following route between two points for the map screens (instead of a straight line).
 * Uses the public OSRM routing service on OpenStreetMap roads (same data as the map tiles), through its own
 * plain HTTP client, so the user's login token is never sent there. Results are cached per point pair;
 * if the service can't be reached the caller gets the straight line, so the map always shows something.
 */
public final class RoadRoute {

    public interface Done {
        /** Called on the main thread. road = false means the straight-line fallback. */
        void onRoute(@NonNull List<GeoPoint> points, boolean road);
    }

    private static final String BASE = "https://router.project-osrm.org/route/v1/driving/";
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<String, List<GeoPoint>> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, List<GeoPoint>>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, List<GeoPoint>> e) {
                    return size() > 24;
                }
            });

    private RoadRoute() {
    }

    /** Route a → b along roads. Points are rounded to ~100 m so a moving vehicle doesn't refetch every update. */
    public static void get(@NonNull GeoPoint a, @NonNull GeoPoint b, @NonNull Done done) {
        final String key = keyOf(a, b);
        List<GeoPoint> hit = CACHE.get(key);
        if (hit != null) {
            done.onRoute(hit, true);
            return;
        }
        final List<GeoPoint> straight = new ArrayList<>();
        straight.add(a);
        straight.add(b);
        String url = BASE + String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f",
                a.getLongitude(), a.getLatitude(), b.getLongitude(), b.getLatitude())
                + "?overview=full&geometries=geojson";
        Request req = new Request.Builder().url(url)
                .header("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME).build();
        HTTP.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MAIN.post(() -> done.onRoute(straight, false));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                List<GeoPoint> pts = null;
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful() && body != null) pts = parse(body.string());
                } catch (Exception ignored) {
                }
                final List<GeoPoint> out = pts;
                if (out != null) {
                    out.set(0, a); // join exactly to the pins
                    out.add(b);
                    CACHE.put(key, out);
                }
                MAIN.post(() -> done.onRoute(out != null ? out : straight, out != null));
            }
        });
    }

    /** Pair key rounded to ~100 m. */
    public static String keyOf(@NonNull GeoPoint a, @NonNull GeoPoint b) {
        return String.format(Locale.US, "%.3f,%.3f;%.3f,%.3f", a.getLongitude(), a.getLatitude(), b.getLongitude(), b.getLatitude());
    }

    static List<GeoPoint> parse(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"Ok".equals(root.optString("code"))) return null;
        JSONArray routes = root.getJSONArray("routes");
        if (routes.length() == 0) return null;
        JSONArray coords = routes.getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");
        if (coords.length() < 2) return null;
        List<GeoPoint> pts = new ArrayList<>(coords.length() + 1);
        for (int i = 0; i < coords.length(); i++) {
            JSONArray c = coords.getJSONArray(i);
            pts.add(new GeoPoint(c.getDouble(1), c.getDouble(0)));
        }
        return pts;
    }

    /** Index of the route point closest to p (to split a route into "driven" and "still to go"). */
    public static int nearest(@NonNull List<GeoPoint> route, @NonNull GeoPoint p) {
        int best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            GeoPoint q = route.get(i);
            double dLat = q.getLatitude() - p.getLatitude();
            double dLng = (q.getLongitude() - p.getLongitude()) * Math.cos(Math.toRadians(p.getLatitude()));
            double d = dLat * dLat + dLng * dLng;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
