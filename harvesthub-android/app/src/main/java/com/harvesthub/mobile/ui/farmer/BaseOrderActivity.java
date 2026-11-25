package com.harvesthub.mobile.ui.farmer;

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
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Common base of the farmer order-step screens. Loads the real order by id,
 * refreshes it every 10 s while open, and hands over to the matching screen when the status moves on
 * (e.g. the buyer paid, the driver accepted) — so the farmer always sees the current step.
 */
public abstract class BaseOrderActivity extends AppCompatActivity {

    private static final long POLL_MS = 10_000;

    protected ApiService api;
    protected SessionManager session;
    protected String orderId;
    protected Order order;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::load;
    private boolean visible;

    @LayoutRes
    protected abstract int layout();

    /** Shows the order. Called on every refresh. */
    protected abstract void bind(@NonNull Order o);

    /** false = this screen shows any status (tracker); true = hand over when the status changes. */
    protected boolean followStatus() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout());
        orderId = getIntent().getStringExtra(OrderFlowRouter.EXTRA_ORDER_ID);
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "No order selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        api = ApiClient.getApiService();
        session = new SessionManager(this);
        View top = findViewById(R.id.included_top_nav);
        if (top != null && top.findViewById(R.id.btnNotification) != null) {
            top.findViewById(R.id.btnNotification).setOnClickListener(v ->
                    startActivity(new Intent(this, NotificationsActivity.class)));
        }
        View nav = findViewById(R.id.included_nav);
        if (nav != null) BottomNavHelper.setup(this, nav, BottomNavHelper.Tab.LISTINGS);
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
                    Toast.makeText(BaseOrderActivity.this, "Could not load the order.", Toast.LENGTH_SHORT).show();
                    return;
                }
                order = response.body();
                if (followStatus()) {
                    Class<?> target = OrderFlowRouter.screenFor(order, session.getUserId());
                    if (target != BaseOrderActivity.this.getClass()) {
                        startActivity(OrderFlowRouter.intentFor(BaseOrderActivity.this, order, session.getUserId()));
                        finish();
                        return;
                    }
                }
                bind(order);
                if (visible) handler.postDelayed(poll, POLL_MS);
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (visible) handler.postDelayed(poll, POLL_MS);
            }
        });
    }

    /** Farmer milestone (START_HARVESTING, HARVEST_READY) through the backend state machine. */
    protected void sendEvent(String event, View button) {
        if (button != null) button.setEnabled(false);
        Map<String, String> body = Collections.singletonMap("event", event);
        api.sendOrderEvent(orderId, body).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (button != null) button.setEnabled(true);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(BaseOrderActivity.this, AuthInputUtils.errorMessage(response, "Action not possible right now."),
                            Toast.LENGTH_LONG).show();
                    load();
                    return;
                }
                order = response.body();
                startActivity(OrderFlowRouter.intentFor(BaseOrderActivity.this, order, session.getUserId()));
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (button != null) button.setEnabled(true);
                Toast.makeText(BaseOrderActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
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

    protected static String lkr(double v) {
        return String.format(Locale.US, "LKR %,.0f", v);
    }

    protected static String kg(double v) {
        return String.format(Locale.US, "%,.0f KG", v);
    }

    protected String orderLabel(Order o) {
        return "Order #" + o.shortId();
    }

    protected String driverLine(Order o) {
        return o.getTransporterName() == null ? "Driver: being assigned" : "Driver: " + o.getTransporterName();
    }

    protected String vehicleLine(Order o) {
        String label = o.getVehicleLabel() != null ? o.getVehicleLabel() : "Vehicle";
        return o.getVehiclePlate() != null ? label + " – " + o.getVehiclePlate() : label;
    }
}
