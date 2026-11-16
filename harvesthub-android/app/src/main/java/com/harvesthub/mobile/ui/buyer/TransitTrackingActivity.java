package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;

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

    private GoogleMap map;
    private Tracking last;
    private boolean framed;

    @Override
    protected int layout() {
        return R.layout.activity_transit_tracking;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (api == null) return;
        SupportMapFragment f = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapTransit);
        if (f != null) f.getMapAsync(m -> {
            map = m;
            map.getUiSettings().setZoomControlsEnabled(true);
            drawMap();
        });
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
        map.clear();
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        int points = 0;
        LatLng from = null, to = null, now = null;
        if (last.pickupLatitude != null && last.pickupLongitude != null) {
            from = new LatLng(last.pickupLatitude, last.pickupLongitude);
            map.addMarker(new MarkerOptions().position(from).title("Farm (origin)")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            b.include(from);
            points++;
        }
        if (last.dropLatitude != null && last.dropLongitude != null) {
            to = new LatLng(last.dropLatitude, last.dropLongitude);
            map.addMarker(new MarkerOptions().position(to).title("Delivery point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            b.include(to);
            points++;
        }
        if (last.currentLatitude != null && last.currentLongitude != null) {
            now = new LatLng(last.currentLatitude, last.currentLongitude);
            map.addMarker(new MarkerOptions().position(now).title(last.transporterName == null ? "Vehicle" : last.transporterName)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            b.include(now);
            points++;
        }
        if (from != null && to != null) {
            PolylineOptions line = new PolylineOptions().color(0xFF1B4332).width(8f).add(from);
            if (now != null) line.add(now);
            line.add(to);
            map.addPolyline(line);
        }
        if (points > 0 && !framed) {
            framed = true;
            try {
                map.moveCamera(points == 1 ? CameraUpdateFactory.newLatLngZoom(from != null ? from : to != null ? to : now, 12)
                        : CameraUpdateFactory.newLatLngBounds(b.build(), 120));
            } catch (IllegalStateException e) { // map not laid out yet
                framed = false;
            }
        }
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
