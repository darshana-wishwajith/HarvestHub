package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;
import com.harvesthub.mobile.utils.OsmMaps;
import com.harvesthub.mobile.utils.TripMap;

import org.osmdroid.views.MapView;


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
    private TripMap trip;
    private Tracking last;

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
        trip = new TripMap(map, 0xFF2563EB, 0xFF93C5FD); // driven solid blue, still to go light blue (Figma 5.2)
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
        if (trip != null && last != null) trip.show(last);
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
