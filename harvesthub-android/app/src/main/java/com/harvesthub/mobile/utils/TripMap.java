package com.harvesthub.mobile.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.harvesthub.mobile.model.Tracking;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Live trip on an OSM map for the farmer and buyer: farm pin, delivery pin, the road route between them
 * (light = still to go, solid = already driven) and the vehicle's last GPS point. Call {@link #show} on every
 * tracking refresh; the road route is fetched once and cached by {@link RoadRoute}.
 */
public final class TripMap {

    private final MapView map;
    private final List<Overlay> drawn = new ArrayList<>();
    private final int doneColor;
    private final int todoColor;
    private boolean framed;
    private Tracking last;
    private List<GeoPoint> road;
    private String roadKey;

    public TripMap(@NonNull MapView map, int doneColor, int todoColor) {
        this.map = map;
        this.doneColor = doneColor;
        this.todoColor = todoColor;
        OsmMaps.setUp(map);
        map.getController().setZoom(8.0);
        map.getController().setCenter(new GeoPoint(7.8731, 80.7718)); // Sri Lanka until the route loads
    }

    public void show(@Nullable Tracking t) {
        if (t == null) return;
        last = t;
        GeoPoint from = point(t.pickupLatitude, t.pickupLongitude);
        GeoPoint to = point(t.dropLatitude, t.dropLongitude);
        if (from != null && to != null) {
            String key = from + "|" + to;
            if (!key.equals(roadKey)) {
                roadKey = key;
                RoadRoute.get(from, to, (pts, isRoad) -> {
                    road = pts;
                    draw(last);
                });
            }
        }
        draw(t);
    }

    private void draw(Tracking t) {
        map.getOverlays().removeAll(drawn);
        drawn.clear();
        GeoPoint from = point(t.pickupLatitude, t.pickupLongitude);
        GeoPoint to = point(t.dropLatitude, t.dropLongitude);
        GeoPoint now = point(t.currentLatitude, t.currentLongitude);
        List<GeoPoint> route = road;
        if (route == null && from != null && to != null) {
            route = new ArrayList<>();
            route.add(from);
            route.add(to);
        }
        if (route != null) {
            line(route, todoColor);
            if (now != null && road != null) {
                int i = RoadRoute.nearest(road, now);
                List<GeoPoint> done = new ArrayList<>(road.subList(0, i + 1));
                done.add(now);
                if (done.size() >= 2) line(done, doneColor);
            }
        }
        List<GeoPoint> pts = new ArrayList<>();
        Context c = map.getContext();
        if (from != null) {
            marker(from, "Farm: " + (t.pickupAddress == null ? "pickup" : t.pickupAddress), dot(c, 0xFF15803D, 16));
            pts.add(from);
        }
        if (to != null) {
            marker(to, "Buyer: " + (t.dropAddress == null ? "delivery point" : t.dropAddress), dot(c, 0xFFDC2626, 16));
            pts.add(to);
        }
        if (now != null) {
            marker(now, t.transporterName == null ? "Vehicle" : t.transporterName, dot(c, doneColor, 24));
            pts.add(now);
        }
        if (!pts.isEmpty() && !framed) {
            framed = true;
            if (pts.size() == 1) {
                map.getController().setZoom(13.0);
                map.getController().setCenter(pts.get(0));
            } else {
                final BoundingBox box = BoundingBox.fromGeoPoints(pts).increaseByScale(1.5f);
                map.post(() -> map.zoomToBoundingBox(box, false));
            }
        }
        map.invalidate();
    }

    /** Re-centres on the vehicle (or the route if there is no GPS point yet). */
    public void focusVehicle() {
        if (last == null) return;
        GeoPoint now = point(last.currentLatitude, last.currentLongitude);
        if (now != null) {
            map.getController().animateTo(now, 14.0, 600L);
        } else {
            framed = false;
            draw(last);
        }
    }

    private static GeoPoint point(Double lat, Double lng) {
        return lat == null || lng == null ? null : new GeoPoint(lat, lng);
    }

    private void line(List<GeoPoint> points, int color) {
        Polyline l = new Polyline(map);
        l.setPoints(points);
        float d = map.getContext().getResources().getDisplayMetrics().density;
        l.getOutlinePaint().setColor(color);
        l.getOutlinePaint().setStrokeWidth(5 * d);
        l.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        l.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        l.setInfoWindow(null);
        map.getOverlays().add(l);
        drawn.add(l);
    }

    private void marker(GeoPoint at, String title, Drawable icon) {
        Marker m = new Marker(map);
        m.setPosition(at);
        m.setTitle(title);
        m.setIcon(icon);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(m);
        drawn.add(m);
    }

    /** Coloured dot with a white ring, sized in dp. */
    public static Drawable dot(Context ctx, int color, int dp) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int size = Math.round(dp * d);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setColor(color);
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, p);
        return new BitmapDrawable(ctx.getResources(), b);
    }
}
