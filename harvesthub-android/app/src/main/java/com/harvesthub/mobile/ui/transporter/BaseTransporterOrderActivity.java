package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.TransporterEventRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Base of the transporter job-step screens. Loads the real order, refreshes it every 10 s, hands over to the
 * screen for the new status (e.g. the buyer approved the photos) and sends driver events with the phone's GPS
 * position attached (decision 15 Nov: position recorded, never blocking).
 */
public abstract class BaseTransporterOrderActivity extends AppCompatActivity {

    private static final long POLL_MS = 10_000;
    private static final int REQ_LOCATION = 61;

    protected ApiService api;
    protected SessionManager session;
    protected String orderId;
    protected Order order;
    protected LinearLayout content;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::load;
    private boolean visible;

    protected abstract String title();

    /** Rebuilds the cards for the order. Called on every refresh; {@link #content} and the action area are cleared first. */
    protected abstract void bind(@NonNull Order o, @NonNull LinearLayout content, @NonNull LinearLayout actions);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, title(), null);
        orderId = getIntent().getStringExtra(TransporterOrderRouter.EXTRA_ORDER_ID);
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "No job selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        api = ApiClient.getApiService();
        session = new SessionManager(this);
        SwipeRefreshLayout refresh = findViewById(R.id.tpRefresh);
        refresh.setOnRefreshListener(() -> {
            refresh.setRefreshing(false);
            load();
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        if (api != null) load();
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
        handler.removeCallbacks(poll);
    }

    protected void load() {
        handler.removeCallbacks(poll);
        api.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (isFinishing()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(BaseTransporterOrderActivity.this, "Could not load the job.", Toast.LENGTH_SHORT).show();
                    return;
                }
                order = response.body();
                if (handOver(order)) return;
                render();
                if (visible && pollWhileOpen()) handler.postDelayed(poll, POLL_MS);
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (visible) handler.postDelayed(poll, POLL_MS);
            }
        });
    }

    /** Screens with typed input (appeal, rating) don't rebuild every 10 s. */
    protected boolean pollWhileOpen() {
        return true;
    }

    protected void render() {
        content.removeAllViews();
        ((android.widget.TextView) findViewById(R.id.tvTpTitle)).setText(title());
        LinearLayout actions = findViewById(R.id.tpActions);
        actions.removeAllViews();
        actions.setVisibility(View.GONE);
        bind(order, content, actions);
        if (actions.getChildCount() > 0) actions.setVisibility(View.VISIBLE);
    }

    protected boolean handOver(Order o) {
        Class<?> target = TransporterOrderRouter.screenFor(o, session.getUserId());
        if (target == getClass()) return false;
        startActivity(TransporterOrderRouter.intentFor(this, o, session.getUserId()));
        finish();
        return true;
    }

    /** Sends a driver event; the current GPS position is attached when it is available within a few seconds. */
    protected void sendEvent(String event, @Nullable String note, @Nullable List<String> photoUrls, @Nullable View button) {
        if (button != null) button.setEnabled(false);
        TransporterEventRequest req = new TransporterEventRequest(session.getUserId(), event, note);
        if (photoUrls != null) {
            req.setPhotoUrls(photoUrls);
            req.setPhotoCount(photoUrls.size());
        }
        withLocation(loc -> {
            if (loc != null) req.setLocation(loc[0], loc[1]);
            api.applyTransporterEvent(orderId, req).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                    if (button != null) button.setEnabled(true);
                    if (!response.isSuccessful() || response.body() == null) {
                        Toast.makeText(BaseTransporterOrderActivity.this,
                                AuthInputUtils.errorMessage(response, "Action not possible right now."), Toast.LENGTH_LONG).show();
                        load();
                        return;
                    }
                    order = response.body();
                    onEventDone(event, order);
                }

                @Override
                public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                    if (button != null) button.setEnabled(true);
                    Toast.makeText(BaseTransporterOrderActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    protected void onEventDone(String event, Order updated) {
        if (!handOver(updated)) render();
    }

    public interface LocationCallback {
        void done(@Nullable double[] latLng);
    }

    /** Current position (or null without permission / fix). Never blocks the action for long. */
    protected void withLocation(LocationCallback cb) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            cb.done(null);
            return;
        }
        final boolean[] answered = {false};
        Runnable timeout = () -> {
            if (!answered[0]) {
                answered[0] = true;
                cb.done(null);
            }
        };
        handler.postDelayed(timeout, 6000);
        try {
            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMaxUpdateAgeMillis(30_000).build();
            LocationServices.getFusedLocationProviderClient(this).getCurrentLocation(req, null)
                    .addOnCompleteListener(t -> {
                        if (answered[0]) return;
                        answered[0] = true;
                        handler.removeCallbacks(timeout);
                        android.location.Location l = t.isSuccessful() ? t.getResult() : null;
                        cb.done(l == null ? null : new double[]{l.getLatitude(), l.getLongitude()});
                    });
        } catch (SecurityException e) {
            handler.removeCallbacks(timeout);
            if (!answered[0]) {
                answered[0] = true;
                cb.done(null);
            }
        }
    }

    protected static String rs(double v) {
        return TUi.rs(v);
    }

    protected static String time(String iso) {
        return com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(iso);
    }

    /** "Thu, 20 Nov" (+ "today"/"tomorrow") from the agreed ISO pickup / delivery date; "Not set" when missing. */
    public static String day(String iso) {
        if (iso == null || iso.length() < 10) return "Not set";
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date d = in.parse(iso.substring(0, 10));
            String out = new java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.US).format(d);
            String today = in.format(new java.util.Date());
            String tomorrow = in.format(new java.util.Date(System.currentTimeMillis() + 86_400_000L));
            if (iso.startsWith(today)) return out + " (today)";
            if (iso.startsWith(tomorrow)) return out + " (tomorrow)";
            return out;
        } catch (Exception e) {
            return iso;
        }
    }

    protected String ref() {
        return "#" + order.shortId();
    }

    protected String cargo(Order o) {
        return (o.getCropName() == null ? "Produce" : o.getCropName()) + " • " + String.format(java.util.Locale.US, "%,.0f kg", o.getQuantity());
    }
}
