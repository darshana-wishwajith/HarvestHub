package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Tracking;
import com.harvesthub.mobile.utils.OsmMaps;
import com.harvesthub.mobile.utils.RoadRoute;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenStreetMap (osmdroid) in the scaffold's map area, with a
 * floating info chip. Turn-by-turn directions open in Google Maps.
 */
final class RouteMap {

    private final AppCompatActivity activity;
    private final MapView map;
    private final List<Overlay> drawn = new ArrayList<>();
    private final TextView chipTitle;
    private final TextView chipSub;
    private final View chip;
    private boolean framed;
    private Tracking last;
    private boolean lastLeg;
    private List<GeoPoint> road;
    private String roadKey;

    RouteMap(AppCompatActivity activity) {
        this.activity = activity;
        OsmMaps.init(activity);
        FrameLayout host = activity.findViewById(R.id.tpMapHost);
        host.setVisibility(View.VISIBLE);
        host.removeAllViews();
        map = new MapView(activity);
        host.addView(map, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        OsmMaps.setUp(map);
        map.getController().setZoom(8.0);
        map.getController().setCenter(new GeoPoint(7.8731, 80.7718));

        // floating "18 min (12.4 km)" chip, bottom-left
        LinearLayout c = new LinearLayout(activity);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(TUi.round(Color.WHITE, TUi.dp(activity, 12)));
        c.setElevation(TUi.dp(activity, 4));
        int p = TUi.dp(activity, 10);
        c.setPadding(p + p / 2, p, p + p / 2, p);
        chipTitle = TUi.label(c, "", 14, TUi.INK, true);
        chipSub = TUi.label(c, "", 11, TUi.MUTED, false);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        lp.setMargins(TUi.dp(activity, 12), 0, 0, TUi.dp(activity, 22));
        host.addView(c, lp);
        c.setVisibility(View.GONE);
        chip = c;

        activity.getLifecycle().addObserver((LifecycleEventObserver) (owner, event) -> {
            if (event == Lifecycle.Event.ON_RESUME) map.onResume();
            else if (event == Lifecycle.Event.ON_PAUSE) map.onPause();
            else if (event == Lifecycle.Event.ON_DESTROY) map.onDetach();
        });
    }

    /** Text on the floating chip, e.g. "18 min (12.4 km)" / "Last GPS point 10:42". */
    void chip(String title, String sub) {
        chip.setVisibility(title == null ? View.GONE : View.VISIBLE);
        chipTitle.setText(title == null ? "" : "●  " + title);
        chipSub.setText(sub == null ? "" : sub);
        chipSub.setVisibility(sub == null ? View.GONE : View.VISIBLE);
    }

    /** Pickup and drop-off pins, the vehicle's last GPS point, and the current leg drawn along the roads. */
    void show(Tracking t, boolean legToDrop) {
        if (t == null) return;
        last = t;
        lastLeg = legToDrop;
        GeoPoint from = point(t.pickupLatitude, t.pickupLongitude);
        GeoPoint to = point(t.dropLatitude, t.dropLongitude);
        GeoPoint now = point(t.currentLatitude, t.currentLongitude);
        GeoPoint a = legToDrop ? from : now;
        GeoPoint b = legToDrop ? to : from;
        if (a != null && b != null) {
            String key = (legToDrop ? "D" : "P") + RoadRoute.keyOf(a, b);
            if (!key.equals(roadKey)) {
                roadKey = key;
                RoadRoute.get(a, b, (pts, isRoad) -> {
                    if (!key.equals(roadKey)) return;
                    road = pts;
                    draw(last, lastLeg);
                });
            }
        }
        draw(t, legToDrop);
    }

    private void draw(Tracking t, boolean legToDrop) {
        map.getOverlays().removeAll(drawn);
        drawn.clear();
        GeoPoint from = point(t.pickupLatitude, t.pickupLongitude);
        GeoPoint to = point(t.dropLatitude, t.dropLongitude);
        GeoPoint now = point(t.currentLatitude, t.currentLongitude);
        List<GeoPoint> pts = new ArrayList<>();
        if (legToDrop && from != null && to != null) {
            List<GeoPoint> r = road != null && roadKey != null && roadKey.startsWith("D") ? road : straight(from, to);
            if (now != null) {
                int i = RoadRoute.nearest(r, now);
                List<GeoPoint> done = new ArrayList<>(r.subList(0, i + 1));
                done.add(now);
                List<GeoPoint> todo = new ArrayList<>();
                todo.add(now);
                todo.addAll(r.subList(Math.min(i + 1, r.size() - 1), r.size()));
                line(todo, 0xFF8A9A90, true);    // still to go
                line(done, 0xFF1B4332, false);   // driven
            } else {
                line(r, 0xFF1B4332, false);
            }
        } else if (!legToDrop && now != null && from != null) {
            List<GeoPoint> r = road != null && roadKey != null && roadKey.startsWith("P") ? road : straight(now, from);
            line(r, 0xFF15803D, true);
        }
        if (from != null) {
            marker(from, "Pickup: " + (t.pickupAddress == null ? "farm" : t.pickupAddress), dot(0xFF15803D, 16));
            pts.add(from);
        }
        if (to != null && legToDrop) {
            marker(to, "Drop-off: " + (t.dropAddress == null ? "buyer" : t.dropAddress), dot(0xFFDC2626, 16));
            pts.add(to);
        }
        if (now != null) {
            marker(now, "Your last GPS point", dot(0xFF1B4332, 24));
            pts.add(now);
        }
        if (!pts.isEmpty() && !framed) {
            framed = true;
            if (pts.size() == 1) {
                map.getController().setZoom(13.0);
                map.getController().setCenter(pts.get(0));
            } else {
                final BoundingBox box = BoundingBox.fromGeoPoints(pts).increaseByScale(1.6f);
                map.post(() -> map.zoomToBoundingBox(box, false));
            }
        }
        map.invalidate();
    }

    private static GeoPoint point(Double lat, Double lng) {
        return lat == null || lng == null ? null : new GeoPoint(lat, lng);
    }

    private static List<GeoPoint> straight(GeoPoint a, GeoPoint b) {
        List<GeoPoint> p = new ArrayList<>();
        p.add(a);
        p.add(b);
        return p;
    }

    private void line(List<GeoPoint> p, int color, boolean dashed) {
        float d = activity.getResources().getDisplayMetrics().density;
        Polyline l = new Polyline(map);
        l.setPoints(p);
        l.getOutlinePaint().setColor(color);
        l.getOutlinePaint().setStrokeWidth(5 * d);
        l.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        if (dashed) l.getOutlinePaint().setPathEffect(new DashPathEffect(new float[]{10 * d, 8 * d}, 0));
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

    private Drawable dot(int color, int dp) {
        float d = activity.getResources().getDisplayMetrics().density;
        int size = Math.round(dp * d);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setColor(color);
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, p);
        return new BitmapDrawable(activity.getResources(), b);
    }

    /** Opens turn-by-turn navigation in Google Maps (the app itself does not draw turn instructions). */
    static void navigate(AppCompatActivity a, Double lat, Double lng) {
        if (lat == null || lng == null) {
            Toast.makeText(a, "No GPS point for this stop.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lng));
        i.setPackage("com.google.android.apps.maps");
        try {
            a.startActivity(i);
        } catch (Exception e) {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng)));
        }
    }
}
