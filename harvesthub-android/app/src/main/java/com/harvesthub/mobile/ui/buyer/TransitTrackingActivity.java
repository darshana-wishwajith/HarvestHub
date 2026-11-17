package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;
import com.harvesthub.mobile.utils.OsmMaps;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer 5.2 "Live Delivery Tracking". Real GPS (decision 15 Nov): the driver's app sends its position,
 * GET /orders/{id}/tracking returns the last point, remaining distance and ETA. Refreshes every 10 s and
 * opens 6.1 when the vehicle arrives.
 */
public class TransitTrackingActivity extends BaseBuyerOrderActivity {

    private MapView map;
    private final List<org.osmdroid.views.overlay.Overlay> drawn = new ArrayList<>();
    private Tracking last;
    private boolean framed;

    @Override
    protected int layout() {
        return R.layout.activity_transit_tracking;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OsmMaps.init(this); // before the layout with the MapView is inflated
        super.onCreate(savedInstanceState);
        if (api == null) return;
        map = findViewById(R.id.mapTransit);
        OsmMaps.setUp(map);
        map.getController().setZoom(8.0);
        map.getController().setCenter(new GeoPoint(7.8731, 80.7718)); // Sri Lanka until the route loads
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderNumber, "Order " + ref(o));
        setText(R.id.tvOrderDetails, kg(o.getQuantity()) + " " + o.getCropName());
        setText(R.id.tvOrigin, "Origin: " + (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()));
        setText(R.id.tvDestination, "Destination: " + (o.getDropAddress() == null ? "Your delivery point" : o.getDropAddress()));
        setText(R.id.tvDriverName, o.getTransporterName() == null ? "Driver" : o.getTransporterName());
        setText(R.id.tvDriverVehicle, vehicleLine(o));
        findViewById(R.id.btnCallDriver).setOnClickListener(v -> dial(o.getTransporterPhone()));
        findViewById(R.id.btnViewLoadingEvidence).setOnClickListener(v -> showEvidence(o));
        loadTracking();
    }

    private void loadTracking() {
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (isFinishing() || !r.isSuccessful() || r.body() == null) return;
                last = r.body();
                bindTracking(last);
                drawMap();
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    private void bindTracking(Tracking t) {
        boolean live = t.currentLatitude != null;
        setText(R.id.tvTransitStatus, live ? "Status: In Transit" : "Status: In Transit • waiting for GPS");
        if (t.etaMinutes != null) {
            int m = t.etaMinutes;
            setText(R.id.tvArrivingTime, m >= 60 ? String.format(Locale.US, "%dh %02dm", m / 60, m % 60) : m + " mins");
        }
        if (t.remainingKm != null) setText(R.id.tvDistanceRemaining, String.format(Locale.US, "remaining: %.1f km", t.remainingKm));
        ProgressBar bar = findViewById(R.id.progressBarTransit);
        if (bar != null) bar.setProgress(t.progressPercent);
        String speed = t.speedKmh != null ? String.format(Locale.US, " • %.0f km/h", t.speedKmh) : "";
        String updated = t.lastUpdate != null ? " • updated " + time(t.lastUpdate) : "";
        setText(R.id.tvProgressPercent, t.progressPercent + " % Completed" + speed + updated);
    }

    private void drawMap() {
        if (map == null || last == null) return;
        map.getOverlays().removeAll(drawn);
        drawn.clear();
        GeoPoint from = point(last.pickupLatitude, last.pickupLongitude);
        GeoPoint to = point(last.dropLatitude, last.dropLongitude);
        GeoPoint now = point(last.currentLatitude, last.currentLongitude);
        if (from != null && to != null) {
            // planned route in light blue, the part already driven in solid blue (Figma 5.2)
            List<GeoPoint> all = new ArrayList<>();
            all.add(from);
            if (now != null) all.add(now);
            all.add(to);
            addLine(all, 0xFF93C5FD);
            if (now != null) {
                List<GeoPoint> done = new ArrayList<>();
                done.add(from);
                done.add(now);
                addLine(done, 0xFF2563EB);
            }
        }
        if (from != null) addMarker(from, "Farm (origin)", dot(0xFF1B4332, 16));
        if (to != null) addMarker(to, "Delivery point", dot(0xFFDC2626, 16));
        if (now != null) addMarker(now, last.transporterName == null ? "Vehicle" : last.transporterName, dot(0xFF2563EB, 22));
        List<GeoPoint> pts = new ArrayList<>();
        if (from != null) pts.add(from);
        if (to != null) pts.add(to);
        if (now != null) pts.add(now);
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

    private static GeoPoint point(Double lat, Double lng) {
        return lat == null || lng == null ? null : new GeoPoint(lat, lng);
    }

    private void addLine(List<GeoPoint> points, int color) {
        Polyline line = new Polyline(map);
        line.setPoints(points);
        line.getOutlinePaint().setColor(color);
        line.getOutlinePaint().setStrokeWidth(5 * getResources().getDisplayMetrics().density);
        line.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        line.setInfoWindow(null);
        map.getOverlays().add(line);
        drawn.add(line);
    }

    private void addMarker(GeoPoint at, String title, Drawable icon) {
        Marker m = new Marker(map);
        m.setPosition(at);
        m.setTitle(title);
        m.setIcon(icon);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        map.getOverlays().add(m);
        drawn.add(m);
    }

    /** Coloured dot with a white ring, sized in dp. */
    private Drawable dot(int color, int dp) {
        float d = getResources().getDisplayMetrics().density;
        int size = Math.round(dp * d);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFFFFFFFF);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setColor(color);
        c.drawCircle(size / 2f, size / 2f, size / 2f - 3 * d, p);
        return new BitmapDrawable(getResources(), b);
    }

    private void showEvidence(Order o) {
        if (o.getLoadingEvidenceUrls() == null || o.getLoadingEvidenceUrls().isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Loading evidence")
                    .setMessage("The driver submitted " + photoCount(o) + " photo(s) in their app; no uploaded copies are available.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        Intent i = new Intent(this, EvidenceViewerActivity.class);
        i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, o.getId());
        startActivity(i);
    }

    private static int photoCount(Order o) {
        Order.TransportEvent e = o.lastEvent("LOADING_EVIDENCE_SUBMITTED");
        return e == null || e.photoCount == null ? 0 : e.photoCount;
    }
}
