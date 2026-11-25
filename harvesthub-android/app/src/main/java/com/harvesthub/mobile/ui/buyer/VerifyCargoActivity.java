package com.harvesthub.mobile.ui.buyer;

import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.RemoteImage;

import java.util.List;
import java.util.Locale;

/**
 * "Pre-Loading Evidence Review" screen. Shows the driver's real loading photos and GPS, with the
 * server's 15-minute deadline.
 * Approve → LOADING_APPROVED. Reject → PRELOAD_REJECTED: 100% refund, Empty Trip Fee charged to the farmer.
 */
public class VerifyCargoActivity extends BaseBuyerOrderActivity {

    private final Runnable tick = this::updateTimer;
    private int photoIndex;

    @Override
    protected int layout() {
        return R.layout.activity_verify_cargo;
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvCargoVehicle, o.getVehicleLabel() == null ? "Vehicle" : o.getVehicleLabel());
        setText(R.id.tvCargoCarrier, (o.getTransporterName() == null ? "" : o.getTransporterName())
                + (o.getVehiclePlate() == null ? "" : " • " + o.getVehiclePlate()));

        Order.TransportEvent ev = o.lastEvent("LOADING_EVIDENCE_SUBMITTED");
        if (ev != null && ev.latitude != null && ev.longitude != null) {
            setText(R.id.tvTelemetryLat, String.format(Locale.US, "%.4f° %s", Math.abs(ev.latitude), ev.latitude >= 0 ? "N" : "S"));
            setText(R.id.tvTelemetryLong, String.format(Locale.US, "%.4f° %s", Math.abs(ev.longitude), ev.longitude >= 0 ? "E" : "W"));
        } else {
            setText(R.id.tvTelemetryLat, "—");
            setText(R.id.tvTelemetryLong, "—");
        }
        setText(R.id.tvTelemetryAltLabel, "TIME");
        setText(R.id.tvTelemetryAlt, o.getLoadingEvidenceAt() == null ? "—" : time(o.getLoadingEvidenceAt()));

        Order.Payment p = o.getPayment();
        if (p != null) setText(R.id.tvEscrowProtectedText, "Smart Escrow Lock: " + rs(p.advanceDue) + " Protected");

        showPhoto(o);
        ImageView iv = findViewById(R.id.ivCargoEvidence);
        iv.setOnClickListener(v -> {
            photoIndex++;
            showPhoto(o);
        });

        findViewById(R.id.btnApproveLoad).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Approve loading?")
                .setMessage("You confirm the crops in the photos match your order. The driver will load and start the journey, "
                        + "and the farmer receives the 20% advance.")
                .setPositiveButton("Approve & Load", (d, w) -> sendEvent("APPROVE_LOADING", null, null, v))
                .setNegativeButton("Cancel", null).show());
        findViewById(R.id.btnRejectCargo).setOnClickListener(v -> askReject());
        updateTimer();
    }

    private void showPhoto(Order o) {
        List<String> urls = o.getLoadingEvidenceUrls();
        ImageView iv = findViewById(R.id.ivCargoEvidence);
        if (urls == null || urls.isEmpty()) {
            setText(R.id.tvEvidencePager, "Photos were taken in the driver's app (not uploaded)");
            return;
        }
        int i = Math.floorMod(photoIndex, urls.size());
        RemoteImage.load(iv, mediaUrl(urls.get(i)));
        setText(R.id.tvEvidencePager, String.format(Locale.US, "Photo %d of %d%s", i + 1, urls.size(), urls.size() > 1 ? " • tap for next" : ""));
    }

    private void askReject() {
        EditText reason = new EditText(this);
        reason.setHint("What is wrong? (e.g. wrong grade, rotten)");
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        reason.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("Reject the cargo?")
                .setMessage("You get a 100% refund including the 3% fee. The farmer pays the Empty Trip Fee to the transporter. "
                        + "This cannot be undone.")
                .setView(reason)
                .setPositiveButton("Reject Cargo", (d, w) -> {
                    String r = reason.getText().toString().trim();
                    sendEvent("REJECT_LOADING", r.isEmpty() ? "Crop does not match the order" : r, null, findViewById(R.id.btnRejectCargo));
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateTimer() {
        handler.removeCallbacks(tick);
        if (order == null || order.getLoadingApprovalDeadline() == null) {
            setText(R.id.tvExpiringTimer, "—");
            return;
        }
        long left = order.getLoadingApprovalDeadline() - System.currentTimeMillis();
        if (left <= 0) {
            setText(R.id.tvExpiringTimer, "auto-approving…");
            return; // the next poll picks up LOADING_APPROVED and moves on
        }
        long s = left / 1000;
        setText(R.id.tvExpiringTimer, String.format(Locale.US, "%02d:%02d mins", s / 60, s % 60));
        handler.postDelayed(tick, 1000);
    }
}
