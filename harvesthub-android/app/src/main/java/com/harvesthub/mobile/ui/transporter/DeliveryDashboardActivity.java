package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.LocationServices;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.TransporterDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma transporter Home: online switch (decision 15 Nov), vehicle, wallet + earnings, current job, the job
 * request waiting for an answer, recent payouts. Everything comes from GET /dashboard/transporter/{id}.
 */
public class DeliveryDashboardActivity extends AppCompatActivity {

    private LinearLayout content;
    private TransporterDashboard data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, "HarvestHub", TUi.Tab.HOME);
        SwipeRefreshLayout refresh = findViewById(R.id.tpRefresh);
        refresh.setOnRefreshListener(() -> {
            refresh.setRefreshing(false);
            load();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String me = new SessionManager(this).getUserId();
        ApiClient.getApiService().getTransporterDashboard(me).enqueue(new Callback<TransporterDashboard>() {
            @Override
            public void onResponse(@NonNull Call<TransporterDashboard> call, @NonNull Response<TransporterDashboard> r) {
                if (isFinishing()) return;
                if (!r.isSuccessful() || r.body() == null) {
                    content.removeAllViews();
                    TUi.body(content, AuthInputUtils.errorMessage(r, "Could not load your dashboard."));
                    return;
                }
                data = r.body();
                render();
                if (data.activeJob != null && "IN_TRANSIT".equals(data.activeJob.status)) {
                    TransporterTrackingService.start(DeliveryDashboardActivity.this);
                }
            }

            @Override
            public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                content.removeAllViews();
                TUi.body(content, "Can't reach the server. Pull down to try again.");
            }
        });
    }

    private void render() {
        content.removeAllViews();
        TransporterDashboard d = data;

        if (d.suspended) {
            LinearLayout s = TUi.banner(content, TUi.RED_BG, TUi.RED, "Account suspended",
                    "Your wallet is " + TUi.rs(d.amountOwed) + " below zero after a damage charge. You get no new jobs until it is paid back.");
            TUi.button(s, "Pay back now", TUi.RED, Color.WHITE, v -> startActivity(new Intent(this, AccountSuspendedActivity.class)));
        }

        // availability
        LinearLayout avail = TUi.card(content);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);
        avail.addView(top);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        top.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TUi.text(texts, d.online ? "● Available for Hauls" : "○ Offline", 17, d.online ? TUi.DARK : TUi.MUTED, true);
        TUi.text(texts, d.online ? (d.busy ? "Online • On a job" : "Online • Receiving job requests") : "You will not receive job requests",
                13, d.online ? TUi.ACCENT : TUi.MUTED, false);
        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(d.online);
        sw.setOnCheckedChangeListener((b, on) -> setOnline(on, sw));
        top.addView(sw);
        TUi.row(avail, "This week", TUi.rs(d.earningsWeek) + " • " + d.tripsThisWeek + " trips");

        // vehicle
        LinearLayout v = TUi.card(content);
        TUi.text(v, (d.vehicleLabel == null ? "Vehicle not set" : d.vehicleLabel) + (d.vehiclePlate == null ? "" : "  •  " + d.vehiclePlate), 16, TUi.INK, true);
        TUi.body(v, String.format(Locale.US, "Max payload: %,.0f kg", d.capacityKg) + (d.vehicleBodyType == null ? "" : " • " + body(d.vehicleBodyType)));
        if (!d.licenceUploaded) TUi.pill(v, "Driving licence photo missing – add it in Settings", TUi.AMBER_BG, TUi.AMBER);

        // wallet + earnings
        LinearLayout w = TUi.card(content);
        TUi.caption(w, "Available payout balance");
        TUi.big(w, TUi.rs(d.balance) + " LKR", d.balance < 0 ? TUi.RED : TUi.DARK);
        TUi.primary(w, "Wallet", x -> startActivity(new Intent(this, TransporterWalletActivity.class)));
        TUi.row(w, "Gross freight (completed trips)", TUi.rs(d.grossFreightTotal));
        TUi.row(w, "5% platform commission", "-" + TUi.rs(d.commissionTotal), TUi.RED, false);
        if (d.emptyTripTotal > 0) TUi.row(w, "Empty Trip Fees received", "+" + TUi.rs(d.emptyTripTotal), TUi.ACCENT, false);
        if (d.damageChargesTotal > 0) TUi.row(w, "Damage charges", "-" + TUi.rs(d.damageChargesTotal), TUi.RED, false);
        TUi.row(w, "Total earned", TUi.rs(d.earningsTotal), TUi.DARK, true);
        TUi.row(w, "Today", TUi.rs(d.earningsToday));

        // job request waiting for an answer
        if (!d.pendingRequests.isEmpty()) {
            TransporterDashboard.Job j = d.pendingRequests.get(0);
            LinearLayout p = TUi.banner(content, TUi.DARK, Color.WHITE, "New job request" + (d.pendingRequests.size() > 1 ? "s (" + d.pendingRequests.size() + ")" : ""),
                    j.cropName + " • " + String.format(Locale.US, "%,.0f kg", j.quantityKg) + " • " + TUi.km(j.distanceKm) + " • payout " + TUi.rs(j.payout));
            TUi.button(p, "Review offer", TUi.ACCENT, Color.WHITE, x -> startActivity(new Intent(this, TransporterJobsActivity.class)));
        }

        // current job
        TUi.title(content, "Current Job").setPadding(0, TUi.dp(this, 18), 0, 0);
        if (d.activeJob == null) {
            TUi.body(content, d.online ? "No active job. New requests appear here and in Jobs." : "Go online to receive jobs.");
        } else {
            TransporterDashboard.Job j = d.activeJob;
            LinearLayout c = TUi.card(content);
            TUi.pill(c, JobStatus.label(j.status), TUi.SURFACE, TUi.DARK);
            TUi.text(c, j.cropName + " • " + String.format(Locale.US, "%,.0f kg", j.quantityKg), 16, TUi.INK, true);
            TUi.body(c, (j.pickupAddress == null ? "Farm" : j.pickupAddress) + " → " + (j.dropoffAddress == null ? "Buyer" : j.dropoffAddress));
            TUi.row(c, "Order #" + shortId(j.orderId), TUi.rs(j.payout), TUi.DARK, true);
            TUi.primary(c, "Continue →", x -> TransporterOrderRouter.open(this, j.orderId));
        }

        // recent payouts
        TUi.title(content, "Recent Payouts").setPadding(0, TUi.dp(this, 18), 0, 0);
        if (d.recentPayouts.isEmpty()) TUi.body(content, "No payouts yet.");
        LinearLayout list = d.recentPayouts.isEmpty() ? null : TUi.card(content);
        for (int i = 0; i < Math.min(5, d.recentPayouts.size()); i++) {
            TransporterDashboard.Payout p = d.recentPayouts.get(i);
            TextView val = TUi.row(list, p.title + "\n" + com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(p.timestamp),
                    TUi.signedRs(p.amount), p.amount >= 0 ? TUi.ACCENT : TUi.RED, false);
            val.setTextSize(15);
        }
    }

    private void setOnline(boolean on, SwitchCompat sw) {
        Map<String, Object> body = new HashMap<>();
        body.put("online", on);
        Runnable send = () -> ApiClient.getApiService().setAvailability(body).enqueue(new Callback<TransporterDashboard>() {
            @Override
            public void onResponse(@NonNull Call<TransporterDashboard> call, @NonNull Response<TransporterDashboard> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    Toast.makeText(DeliveryDashboardActivity.this, AuthInputUtils.errorMessage(r, "Could not change your status."), Toast.LENGTH_LONG).show();
                    load();
                    return;
                }
                data = r.body();
                render();
            }

            @Override
            public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                Toast.makeText(DeliveryDashboardActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                load();
            }
        });
        // going online also refreshes the position used for "nearest driver" dispatch
        if (on && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnCompleteListener(t -> {
                    if (t.isSuccessful() && t.getResult() != null) {
                        body.put("latitude", t.getResult().getLatitude());
                        body.put("longitude", t.getResult().getLongitude());
                    }
                    send.run();
                });
                return;
            } catch (SecurityException ignored) { }
        }
        send.run();
    }

    static String body(String type) {
        if (type == null) return "";
        switch (type) {
            case "REEFER": return "Refrigerated (reefer)";
            case "FLATBED": return "Flatbed";
            case "BOX": return "Box truck";
            default: return type;
        }
    }

    static String shortId(String id) {
        if (id == null) return "";
        return (id.length() > 6 ? id.substring(id.length() - 6) : id).toUpperCase(Locale.US);
    }
}
