package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
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
        int green = Color.parseColor("#15803D");

        if (d.suspended) {
            LinearLayout s = TUi.notice(content, R.drawable.ic_wallet_frozen, TUi.RED_BG, TUi.RED, "Account suspended",
                    "Your wallet is " + TUi.rs(d.amountOwed) + " below zero after a damage charge. You get no new jobs until it is paid back.");
            TUi.smallButton(s, "Pay back now", 0, TUi.RED, Color.WHITE, v -> startActivity(new Intent(this, AccountSuspendedActivity.class)));
        }

        // availability
        LinearLayout avail = TUi.card(content);
        LinearLayout top = TUi.hrow(avail, 0);
        TUi.label(top, "\u25CF", 14, d.online ? green : TUi.MUTED, false);
        LinearLayout texts = TUi.col(top, 1f);
        texts.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(texts, d.online ? "Available for Hauls" : "Offline", 16, TUi.INK, true);
        TUi.text(texts, d.online ? (d.busy ? "Online \u2022 On a job" : "Online \u2022 Receiving Offers") : "You will not receive job requests",
                12, d.online ? green : TUi.MUTED, true);
        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(d.online);
        sw.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
        sw.setTrackTintList(android.content.res.ColorStateList.valueOf(d.online ? TUi.DARK : Color.parseColor("#CBD5E1")));
        sw.setOnCheckedChangeListener((b, on) -> setOnline(on, sw));
        top.addView(sw);
        LinearLayout wk = TUi.box(avail, TUi.GREY_BG, 12);
        LinearLayout wr = TUi.hrow(wk, 0);
        TUi.icon(wr, R.drawable.ic_navigation_pointer, 16, TUi.INK);
        TextView week = TUi.fill(wr, "+" + TUi.rs(d.earningsWeek) + " this week", 13, TUi.INK, true);
        ((LinearLayout.LayoutParams) week.getLayoutParams()).leftMargin = TUi.dp(this, 8);
        TUi.tag(wr, d.tripsThisWeek + (d.tripsThisWeek == 1 ? " TRIP" : " TRIPS"), TUi.MINT, TUi.MINT_INK);

        // vehicle
        LinearLayout v = TUi.card(content);
        LinearLayout vr = TUi.hrow(v, 0);
        TUi.iconTile(vr, R.drawable.ic_truck, 48, TUi.MINT, TUi.DARK, false);
        LinearLayout vt = TUi.col(vr, 1f);
        vt.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
        LinearLayout nameRow = TUi.hrow(vt, 0);
        TUi.label(nameRow, d.vehicleLabel == null ? "Vehicle not set" : d.vehicleLabel, 16, TUi.INK, true);
        if (d.licenceUploaded) {
            ImageView ok = TUi.icon(nameRow, R.drawable.ic_verified_badge, 18, 0);
            ((LinearLayout.LayoutParams) ok.getLayoutParams()).leftMargin = TUi.dp(this, 6);
        }
        TUi.text(vt, String.format(Locale.US, "Max Payload: %,.0f kg", d.capacityKg) + (d.vehicleBodyType == null ? "" : " \u2022 " + body(d.vehicleBodyType)),
                12, TUi.MUTED, false);
        if (d.vehiclePlate != null && !d.vehiclePlate.isEmpty()) TUi.tag(vr, d.vehiclePlate, TUi.GREY_BG, TUi.INK);
        if (!d.licenceUploaded) TUi.pill(v, "Driving licence photo missing \u2013 add it in Settings", TUi.AMBER_BG, TUi.AMBER);

        // wallet + earnings
        LinearLayout w = TUi.card(content);
        LinearLayout wh = TUi.hrow(w, 0);
        TUi.fill(wh, "Available Payout Balance", 13, TUi.MUTED, false).setTypeface(TUi.semi(this));
        TUi.tag(wh, "Auto-Sync", TUi.MINT, TUi.MINT_INK);
        LinearLayout br = TUi.hrow(w, 2);
        br.setGravity(android.view.Gravity.BOTTOM);
        TUi.label(br, TUi.rs(d.balance), 32, d.balance < 0 ? TUi.RED : TUi.INK, true);
        TUi.label(br, "  LKR", 12, TUi.DARK, true).setPadding(0, 0, 0, TUi.dp(this, 6));
        LinearLayout btns = TUi.hrow(w, 4);
        half(TUi.iconButton(btns, "Wallet", R.drawable.ic_bolt_green, TUi.DARK, Color.WHITE,
                x -> startActivity(new Intent(this, TransporterWalletActivity.class))), false);
        half(TUi.iconButton(btns, "Trip History", R.drawable.transporter_ic_bottom_history, Color.parseColor("#E9E7E5"), TUi.INK,
                x -> startActivity(new Intent(this, JobHistoryActivity.class))), true);
        LinearLayout eb = TUi.box(w, TUi.GREY_BG, 14);
        TUi.row(eb, "Gross Hauling Earnings", "+" + TUi.rs(d.grossFreightTotal));
        TUi.row(eb, "5% Platform Commission", "-" + TUi.rs(d.commissionTotal));
        if (d.emptyTripTotal > 0) TUi.row(eb, "Empty Trip Fees (credit)", "+" + TUi.rs(d.emptyTripTotal), green, false);
        if (d.damageChargesTotal > 0) TUi.row(eb, "Damage charges", "-" + TUi.rs(d.damageChargesTotal), TUi.RED, false);
        LinearLayout tot = TUi.box(eb, Color.WHITE, 6);
        TUi.row(tot, "Total Delivery Fees", TUi.rs(d.earningsTotal), TUi.DARK, true);

        // job request waiting for an answer
        if (!d.pendingRequests.isEmpty()) {
            TransporterDashboard.Job j = d.pendingRequests.get(0);
            LinearLayout p = TUi.card(content, TUi.DARK, TUi.DARK);
            TUi.text(p, "\u25CF  NEW JOB REQUEST" + (d.pendingRequests.size() > 1 ? "S (" + d.pendingRequests.size() + ")" : ""), 12,
                    Color.parseColor("#86EFAC"), true);
            TUi.text(p, j.cropName + " \u2022 " + String.format(Locale.US, "%,.0f kg", j.quantityKg), 16, Color.WHITE, true);
            TUi.text(p, TUi.km(j.distanceKm) + " \u2022 payout " + TUi.rs(j.payout), 13, Color.parseColor("#D1FAE5"), false);
            TUi.smallButton(p, "Review Offer \u2192", 0, TUi.MINT, TUi.MINT_INK, x -> startActivity(new Intent(this, TransporterJobsActivity.class)));
        }

        // current job
        LinearLayout sr = TUi.hrow(content, 20);
        TUi.fill(sr, "Current Job", 17, TUi.INK, true);
        if (d.activeJob != null) TUi.tag(sr, JobStatus.label(d.activeJob.status), Color.parseColor("#7C3AED"), Color.WHITE);
        if (d.activeJob == null) {
            TUi.body(content, d.online ? "No active job. New requests appear here and in Jobs." : "Go online to receive jobs.");
        } else {
            jobCard(this, content, d.activeJob);
        }

        // recent payouts
        TextView all = TUi.sectionTitle(content, "Recent Payouts", "View All \u203A");
        all.setOnClickListener(x -> startActivity(new Intent(this, TransporterWalletActivity.class)));
        if (d.recentPayouts.isEmpty()) TUi.body(content, "No payouts yet.");
        for (int i = 0; i < Math.min(5, d.recentPayouts.size()); i++) {
            TransporterDashboard.Payout p = d.recentPayouts.get(i);
            LinearLayout pc = TUi.card(content);
            ((LinearLayout.LayoutParams) ((android.view.View) pc.getParent()).getLayoutParams()).topMargin = TUi.dp(this, 10);
            LinearLayout pr = TUi.hrow(pc, 0);
            boolean plus = p.amount >= 0;
            TUi.iconTile(pr, plus ? R.drawable.ic_tractor : R.drawable.ic_warning_triangle_red, 40, plus ? TUi.MINT : TUi.RED_BG, plus ? TUi.DARK : TUi.RED, true);
            LinearLayout pt = TUi.col(pr, 1f);
            pt.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
            TextView title = TUi.text(pt, p.title, 14, TUi.INK, true);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TUi.text(pt, "Settled " + com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(p.timestamp), 12, TUi.MUTED, false);
            LinearLayout pv = TUi.col(pr, 0);
            pv.setGravity(android.view.Gravity.END);
            TUi.label(pv, TUi.signedRs(p.amount), 15, plus ? green : TUi.RED, true);
            if (p.type != null) TUi.tag(pv, pretty(p.type), TUi.GREY_BG, TUi.INK);
        }
    }

    private void half(com.google.android.material.button.MaterialButton b, boolean second) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, TUi.dp(this, 52), 1f);
        lp.topMargin = TUi.dp(this, 8);
        if (second) lp.leftMargin = TUi.dp(this, 8);
        b.setLayoutParams(lp);
    }

    private static String pretty(String type) {
        String t = type.replace('_', ' ').toLowerCase(Locale.US);
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Figma "Current Job" card: truck tile, cargo, route, payout and Continue. */
    static void jobCard(android.app.Activity a, LinearLayout parent, TransporterDashboard.Job j) {
        LinearLayout c = TUi.card(parent);
        ((android.view.View) c.getParent()).setOnClickListener(x -> TransporterOrderRouter.open(a, j.orderId));
        LinearLayout r = TUi.hrow(c, 0);
        TUi.iconTile(r, R.drawable.ic_truck, 48, TUi.SURFACE, TUi.DARK, false);
        LinearLayout m = TUi.col(r, 1f);
        m.setPadding(TUi.dp(a, 12), 0, TUi.dp(a, 8), 0);
        TextView name = TUi.text(m, j.cropName, 16, TUi.INK, true);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView route = TUi.text(m, (j.pickupAddress == null ? "Farm" : j.pickupAddress) + " \u2192 " + (j.dropoffAddress == null ? "Buyer" : j.dropoffAddress),
                13, TUi.MUTED, false);
        route.setMaxLines(1);
        route.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.text(m, "Order #" + shortId(j.orderId) + " \u2022 " + String.format(Locale.US, "%,.0f kg", j.quantityKg), 12, TUi.MUTED, false);
        if (j.pickupDate != null) TUi.text(m, "Pickup " + BaseTransporterOrderActivity.day(j.pickupDate), 12, TUi.DARK, true);
        LinearLayout right = TUi.col(r, 0);
        right.setGravity(android.view.Gravity.END);
        TUi.label(right, TUi.rs(j.payout), 15, TUi.INK, true);
        TUi.smallButton(right, "Continue \u2192", 0, TUi.DARK, Color.WHITE, x -> TransporterOrderRouter.open(a, j.orderId));
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
