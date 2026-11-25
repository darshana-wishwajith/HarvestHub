package com.harvesthub.mobile.ui.transporter;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.TransporterDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Matchmaking Job Offers" screen (Jobs tab). Only the requests dispatch sent to this driver
 *each with its 10-minute countdown, plus the job in progress.
 */
public class TransporterJobsActivity extends AppCompatActivity {

    private LinearLayout content;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Object[]> timers = new ArrayList<>(); // {TextView, Long expiresAt}
    private final Runnable tick = this::tick;
    private final Runnable reload = this::load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, "Jobs", TUi.Tab.JOBS);
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

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private void load() {
        handler.removeCallbacks(reload);
        ApiClient.getApiService().getTransporterDashboard(new SessionManager(this).getUserId())
                .enqueue(new Callback<TransporterDashboard>() {
                    @Override
                    public void onResponse(@NonNull Call<TransporterDashboard> call, @NonNull Response<TransporterDashboard> r) {
                        if (isFinishing()) return;
                        if (r.isSuccessful() && r.body() != null) render(r.body());
                        handler.postDelayed(reload, 20_000); // new requests arrive from dispatch
                    }

                    @Override
                    public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                        content.removeAllViews();
                        TUi.body(content, "Can't reach the server. Pull down to try again.");
                    }
                });
    }

    private TransporterDashboard last;
    private int sort; // 0 all (dispatch order), 1 shortest trip, 2 highest payout

    private void render(TransporterDashboard d) {
        last = d;
        content.removeAllViews();
        timers.clear();
        int n = d.pendingRequests.size();
        LinearLayout hr = TUi.hrow(content, 16);
        TUi.fill(hr, "Matchmaking Job Offers", 20, TUi.INK, true);
        TUi.tag(hr, "\u25CF " + n + (n == 1 ? " Match" : " Matches"), TUi.MINT, TUi.MINT_INK);
        java.util.List<TextView> chips = TUi.chips(content, new String[]{"All Matches (" + n + ")", "Shortest Trip", "Highest Payout"}, false, null);
        for (int i = 0; i < chips.size(); i++) {
            final int mode = i;
            TUi.setChip(chips.get(i), sort == i);
            chips.get(i).setOnClickListener(v -> {
                sort = mode;
                render(last);
            });
        }
        if (!d.online) TUi.pill(content, "You are offline \u2013 go online on Home to receive requests", TUi.AMBER_BG, TUi.AMBER);
        if (d.suspended) TUi.pill(content, "Suspended \u2013 no new requests until your wallet is paid back", TUi.RED_BG, TUi.RED);

        List<TransporterDashboard.Job> jobs = new ArrayList<>(d.pendingRequests);
        if (sort == 1) java.util.Collections.sort(jobs, (a, b) -> Double.compare(a.distanceKm, b.distanceKm));
        if (sort == 2) java.util.Collections.sort(jobs, (a, b) -> Double.compare(b.payout, a.payout));
        boolean first = true;
        for (TransporterDashboard.Job j : jobs) {
            if (first) priorityCard(j); else standardCard(j);
            first = false;
        }
        if (jobs.isEmpty()) {
            LinearLayout c = TUi.card(content);
            LinearLayout r = TUi.hrow(c, 0);
            r.setGravity(android.view.Gravity.TOP);
            TUi.iconTile(r, R.drawable.ic_truck, 44, TUi.SURFACE, TUi.DARK, false);
            LinearLayout t = TUi.col(r, 1f);
            t.setPadding(TUi.dp(this, 12), 0, 0, 0);
            TUi.text(t, "No job requests right now", 16, TUi.INK, true);
            TUi.text(t, "When a buyer pays for an order near you, dispatch sends it to the nearest free driver whose vehicle can carry it. "
                    + "You then have 10 minutes to accept.", 13, TUi.MUTED, false);
        }
        if (d.activeJob != null) {
            LinearLayout sr = TUi.hrow(content, 20);
            TUi.fill(sr, "Job in progress", 17, TUi.INK, true);
            TUi.tag(sr, JobStatus.label(d.activeJob.status), Color.parseColor("#7C3AED"), Color.WHITE);
            DeliveryDashboardActivity.jobCard(this, content, d.activeJob);
        }
        tick();
    }

    private static String name(TransporterDashboard.Job j) {
        return (j.cropName == null ? "Produce" : j.cropName) + " \u2022 " + String.format(Locale.US, "%,.0f kg", j.quantityKg);
    }

    /** First request: dark "PRIORITY MATCH" strip with the countdown. */
    private void priorityCard(TransporterDashboard.Job j) {
        LinearLayout c = TUi.card(content);
        c.setPadding(0, 0, 0, 0);
        ((android.view.View) c.getParent()).setClipToOutline(true);
        ((android.view.View) c.getParent()).setOnClickListener(v -> TransporterOrderRouter.open(this, j.orderId));
        LinearLayout strip = TUi.hrow(c, 0);
        strip.setBackgroundColor(TUi.DARK);
        strip.setPadding(TUi.dp(this, 16), TUi.dp(this, 9), TUi.dp(this, 16), TUi.dp(this, 9));
        TextView timer = TUi.fill(strip, "", 12, Color.WHITE, true);
        timer.setLetterSpacing(0.04f);
        timers.add(new Object[]{timer, j.expiresAt, Boolean.TRUE});
        TUi.icon(strip, R.drawable.ic_bolt_green, 16, Color.parseColor("#86EFAC"));

        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        int p = TUi.dp(this, 16);
        b.setPadding(p, p, p, p);
        c.addView(b);
        LinearLayout top = TUi.hrow(b, 0);
        TUi.iconTile(top, R.drawable.ic_package, 56, TUi.SURFACE, TUi.DARK, false);
        LinearLayout m = TUi.col(top, 1f);
        m.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
        TextView nm = TUi.text(m, name(j), 16, TUi.INK, true);
        nm.setMaxLines(1);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (j.vehicleLabel != null) TUi.pill(m, j.vehicleLabel, TUi.MINT, TUi.MINT_INK).setTextSize(11);
        LinearLayout right = TUi.col(top, 0);
        right.setGravity(android.view.Gravity.END);
        TUi.label(right, TUi.rs(j.payout), 19, TUi.DARK, true);
        TUi.pill(right, "Guaranteed", TUi.MINT, TUi.MINT_INK).setTextSize(11);
        LinearLayout box = TUi.box(b, TUi.GREY_BG, 14);
        TUi.timeline(box, new String[]{"Pickup", j.pickupAddress, j.farmerName == null ? null : "Farmer: " + j.farmerName},
                new String[]{"Drop-off", j.dropoffAddress, j.buyerName == null ? null : "Buyer: " + j.buyerName});
        if (j.pickupDate != null) TUi.text(b, "Pickup date: " + BaseTransporterOrderActivity.day(j.pickupDate), 13, TUi.DARK, true).setPadding(0, TUi.dp(this, 8), 0, 0);
        LinearLayout foot = TUi.hrow(b, 12);
        TUi.icon(foot, R.drawable.ic_location_pin, 14, TUi.INK);
        TUi.fill(foot, " " + TUi.km(j.distanceKm) + "  \u2022  " + JobOfferActivity.duration(j.distanceKm), 13, TUi.INK, false);
        TUi.label(foot, "Review Offer \u2192", 14, TUi.DARK, true);
    }

    private void standardCard(TransporterDashboard.Job j) {
        LinearLayout c = TUi.card(content);
        ((android.view.View) c.getParent()).setOnClickListener(v -> TransporterOrderRouter.open(this, j.orderId));
        LinearLayout head = TUi.hrow(c, 0);
        TUi.tag(head, "Standard Match" + (j.vehicleLabel == null ? "" : " \u00B7 " + j.vehicleLabel), TUi.GREY_BG, TUi.INK);
        TextView timer = TUi.fill(head, "", 12, TUi.MUTED, false);
        timer.setGravity(android.view.Gravity.END);
        timers.add(new Object[]{timer, j.expiresAt, Boolean.FALSE});
        LinearLayout top = TUi.hrow(c, 10);
        TUi.iconTile(top, R.drawable.ic_package, 48, TUi.SURFACE, TUi.DARK, false);
        TextView nm = TUi.fill(top, name(j), 16, TUi.INK, true);
        nm.setMaxLines(1);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        ((LinearLayout.LayoutParams) nm.getLayoutParams()).leftMargin = TUi.dp(this, 12);
        LinearLayout right = TUi.col(top, 0);
        right.setGravity(android.view.Gravity.END);
        TUi.label(right, TUi.rs(j.payout), 18, TUi.DARK, true);
        TUi.pill(right, "Escrow Protected", TUi.MINT, TUi.MINT_INK).setTextSize(11);
        LinearLayout rt = TUi.hrow(c, 10);
        TextView route = TUi.fill(rt, (j.pickupAddress == null ? "Farm" : j.pickupAddress) + "  \u2192  " + (j.dropoffAddress == null ? "Buyer" : j.dropoffAddress),
                13, TUi.INK, false);
        route.setMaxLines(1);
        route.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.label(rt, "  " + TUi.km(j.distanceKm) + " \u00B7 " + JobOfferActivity.duration(j.distanceKm), 12, TUi.MUTED, false);
        if (j.pickupDate != null) TUi.text(c, "Pickup " + BaseTransporterOrderActivity.day(j.pickupDate), 12, TUi.DARK, true).setPadding(0, TUi.dp(this, 6), 0, 0);
        TextView more = TUi.text(c, "View Details \u2192", 14, TUi.DARK, true);
        more.setGravity(android.view.Gravity.END);
        more.setPadding(0, TUi.dp(this, 10), 0, 0);
    }

    private void tick() {
        handler.removeCallbacks(tick);
        for (Object[] t : timers) {
            String left = JobOfferActivity.countdown((Long) t[1]).replace("\u23F1 ", "");
            ((TextView) t[0]).setText(Boolean.TRUE.equals(t[2]) ? "\u25CF  PRIORITY MATCH \u2022 " + left.toUpperCase(Locale.US) : left);
        }
        if (!timers.isEmpty()) handler.postDelayed(tick, 1000);
    }
}
