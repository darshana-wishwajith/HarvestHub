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
 * Figma "Matchmaking Job Offers" (Jobs tab). Decision 15 Nov: only the requests dispatch sent to this driver
 * (nearest-driver workflow), each with its 10-minute countdown, plus the job in progress.
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

    private void render(TransporterDashboard d) {
        content.removeAllViews();
        timers.clear();
        TUi.title(content, "Matchmaking Job Offers").setPadding(0, TUi.dp(this, 16), 0, 0);
        TUi.pill(content, d.pendingRequests.size() + (d.pendingRequests.size() == 1 ? " request for you" : " requests for you"), TUi.SURFACE, TUi.DARK);
        if (!d.online) TUi.pill(content, "You are offline – go online on Home to receive requests", TUi.AMBER_BG, TUi.AMBER);
        if (d.suspended) TUi.pill(content, "Suspended – no new requests until your wallet is paid back", TUi.RED_BG, TUi.RED);

        boolean first = true;
        for (TransporterDashboard.Job j : d.pendingRequests) {
            LinearLayout c = first ? TUi.card(content, Color.WHITE, TUi.DARK) : TUi.card(content);
            TextView timer = TUi.pill(c, "", first ? TUi.DARK : TUi.GREY_BG, first ? Color.WHITE : TUi.INK);
            timers.add(new Object[]{timer, j.expiresAt});
            TUi.text(c, j.cropName + " • " + String.format(Locale.US, "%,.0f kg", j.quantityKg), 17, TUi.INK, true);
            TUi.big(c, TUi.rs(j.payout), TUi.DARK).setTextSize(24);
            TUi.pill(c, "Escrow protected – paid on delivery", TUi.SURFACE, TUi.DARK);
            TUi.route(c, "Pickup", j.pickupAddress, "Drop-off", j.dropoffAddress);
            TUi.body(c, TUi.km(j.distanceKm) + " • about " + JobOfferActivity.duration(j.distanceKm) + (j.vehicleLabel == null ? "" : " • " + j.vehicleLabel));
            TUi.primary(c, "Review Offer →", v -> TransporterOrderRouter.open(this, j.orderId));
            first = false;
        }
        if (d.pendingRequests.isEmpty()) {
            LinearLayout c = TUi.card(content);
            TUi.text(c, "No job requests right now", 16, TUi.INK, true);
            TUi.body(c, "When a buyer pays for an order near you, dispatch sends it to the nearest free driver whose vehicle can carry it. You then have 10 minutes to accept.");
        }

        if (d.activeJob != null) {
            TUi.title(content, "Job in progress").setPadding(0, TUi.dp(this, 18), 0, 0);
            TransporterDashboard.Job j = d.activeJob;
            LinearLayout c = TUi.card(content);
            TUi.pill(c, JobStatus.label(j.status), TUi.SURFACE, TUi.DARK);
            TUi.text(c, j.cropName + " • " + String.format(Locale.US, "%,.0f kg", j.quantityKg), 16, TUi.INK, true);
            TUi.route(c, "Pickup", j.pickupAddress, "Drop-off", j.dropoffAddress);
            TUi.primary(c, "Continue →", v -> TransporterOrderRouter.open(this, j.orderId));
        }
        tick();
    }

    private void tick() {
        handler.removeCallbacks(tick);
        for (Object[] t : timers) {
            ((TextView) t[0]).setText(JobOfferActivity.countdown((Long) t[1]));
        }
        if (!timers.isEmpty()) handler.postDelayed(tick, 1000);
    }
}
