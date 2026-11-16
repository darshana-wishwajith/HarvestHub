package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Common base of the buyer order-step screens (Figma buyer 4.1 – 6.3). Loads the real order by id, refreshes it
 * every 10 s while open and hands over to the matching screen when the status moves on (e.g. the driver
 * uploaded the loading photos), so the buyer always sees the current step.
 */
public abstract class BaseBuyerOrderActivity extends AppCompatActivity {

    private static final long POLL_MS = 10_000;

    protected ApiService api;
    protected SessionManager session;
    protected String orderId;
    protected Order order;
    protected final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::load;
    private boolean visible;

    @LayoutRes
    protected abstract int layout();

    /** Shows the order. Called on every refresh. */
    protected abstract void bind(@NonNull Order o);

    /** false = this screen shows several statuses itself; true = hand over when the status changes. */
    protected boolean followStatus() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout());
        orderId = getIntent().getStringExtra(BuyerOrderRouter.EXTRA_ORDER_ID);
        if (orderId == null || orderId.isEmpty()) orderId = getIntent().getStringExtra("order_id");
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "No order selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        api = ApiClient.getApiService();
        session = new SessionManager(this);
        View back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(v -> finish());
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
                    Toast.makeText(BaseBuyerOrderActivity.this, "Could not load the order.", Toast.LENGTH_SHORT).show();
                    return;
                }
                order = response.body();
                if (followStatus() && handOver(order)) return;
                bind(order);
                if (visible) handler.postDelayed(poll, POLL_MS);
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (visible) handler.postDelayed(poll, POLL_MS);
            }
        });
    }

    /** Opens the screen for the order's status when it is not this one. */
    protected boolean handOver(Order o) {
        Class<?> target = BuyerOrderRouter.screenFor(o, session.getUserId());
        if (target == getClass()) return false;
        startActivity(BuyerOrderRouter.intentFor(this, o, session.getUserId()));
        finish();
        return true;
    }

    /** Buyer milestone through the backend state machine (APPROVE_LOADING, ACCEPT_DELIVERY, …). */
    protected void sendEvent(String event, @Nullable String reason, @Nullable String method, @Nullable View button) {
        if (button != null) button.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("event", event);
        if (reason != null) body.put("reason", reason);
        if (method != null) body.put("method", method);
        api.sendOrderEvent(orderId, body).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (button != null) button.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(BaseBuyerOrderActivity.this,
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
                Toast.makeText(BaseBuyerOrderActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Default: open the screen for the new status. */
    protected void onEventDone(String event, Order updated) {
        if (!handOver(updated)) bind(updated);
    }

    protected void dial(String phone) {
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "No phone number available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    protected void setText(@IdRes int id, CharSequence text) {
        View v = findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(text);
    }

    protected void show(@IdRes int id, boolean visible) {
        View v = findViewById(id);
        if (v != null) v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public static String rs(double v) {
        return Math.abs(v - Math.rint(v)) < 0.005
                ? String.format(Locale.US, "Rs. %,.0f", v) : String.format(Locale.US, "Rs. %,.2f", v);
    }

    public static String kg(double v) {
        return String.format(Locale.US, "%,.0f kg", v);
    }

    protected static String ref(Order o) {
        return "#" + o.shortId();
    }

    protected static String vehicleLine(Order o) {
        String label = o.getVehicleLabel() != null ? o.getVehicleLabel() : "Vehicle";
        return o.getVehiclePlate() != null ? label + " • " + o.getVehiclePlate() : label;
    }

    /** "2026-11-15T10:45:12.123" → "15 Nov, 10:45". */
    public static String time(String iso) {
        if (iso == null || iso.length() < 16) return "";
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
            java.util.Date d = in.parse(iso.substring(0, 16).replace(' ', 'T'));
            return new java.text.SimpleDateFormat("d MMM, HH:mm", Locale.US).format(d);
        } catch (Exception e) {
            return iso.substring(0, 16).replace('T', ' ');
        }
    }

    /** Full URL for a stored media path like "/uploads/x.jpg". */
    public static String mediaUrl(String path) {
        if (path == null) return null;
        if (path.startsWith("http")) return path;
        String base = com.harvesthub.mobile.BuildConfig.API_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
