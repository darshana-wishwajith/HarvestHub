package com.harvesthub.mobile.ui.transporter;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.TransporterDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma transporter "History": weekly earnings header (dashboard) and every job of this driver with its money
 * outcome – completed (95% payout), empty trip (Empty Trip Fee), rejected (damage charge / appeal).
 */
public class JobHistoryActivity extends AppCompatActivity {

    private static final String[] FILTERS = {"All trips", "Completed", "Empty trips", "Disputes", "Active"};

    private LinearLayout content;
    private LinearLayout list;
    private TransporterDashboard dash;
    private List<Order> orders = new ArrayList<>();
    private List<TextView> chips;
    private EditText search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = TUi.scaffold(this, "History", TUi.Tab.HISTORY);
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
                if (r.isSuccessful()) dash = r.body();
                ApiClient.getApiService().getTransporterOrders(me, null).enqueue(new Callback<List<Order>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> r2) {
                        orders = r2.isSuccessful() && r2.body() != null ? r2.body() : new ArrayList<>();
                        Collections.sort(orders, (a, b) -> String.valueOf(b.getCreatedAt()).compareTo(String.valueOf(a.getCreatedAt())));
                        build();
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                        build();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                content.removeAllViews();
                TUi.body(content, "Can't reach the server. Pull down to try again.");
            }
        });
    }

    private void build() {
        content.removeAllViews();
        if (dash != null) {
            LinearLayout h = TUi.card(content);
            TUi.caption(h, "Weekly net earnings");
            TUi.big(h, TUi.rs(dash.earningsWeek), TUi.DARK);
            TUi.row(h, "Trips this week", String.valueOf(dash.tripsThisWeek));
            TUi.row(h, "Completed trips (all time)", String.valueOf(dash.tripsCompleted));
            TUi.row(h, "Empty Trip Fees", "+" + TUi.rs(dash.emptyTripTotal), TUi.ACCENT, false);
            if (dash.ratingAverage != null) {
                TUi.row(h, "Your rating", String.format(Locale.US, "★ %.1f (%d)", dash.ratingAverage, dash.ratingCount == null ? 0 : dash.ratingCount));
            }
        }
        TUi.title(content, "Trip History").setPadding(0, TUi.dp(this, 16), 0, 0);
        search = TUi.input(content, "Search orders, crops, farms or buyers…", false);
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { fill(); }
            public void afterTextChanged(android.text.Editable s) { }
        });
        chips = TUi.chips(content, FILTERS, false, this::fill);
        TUi.setChip(chips.get(0), true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        content.addView(list);
        fill();
    }

    private void fill() {
        list.removeAllViews();
        int filter = 0;
        for (int i = 0; i < chips.size(); i++) if (TUi.isOn(chips.get(i))) filter = i;
        String q = search.getText().toString().trim().toLowerCase(Locale.US);
        int shown = 0;
        for (Order o : orders) {
            String s = o.getStatus() == null ? "" : o.getStatus();
            boolean ok;
            switch (filter) {
                case 1: ok = "COMPLETED".equals(s); break;
                case 2: ok = "PRELOAD_REJECTED".equals(s); break;
                case 3: ok = "RETURN_JOURNEY".equals(s) || "RETURNED".equals(s) || o.isDisputeOpen(); break;
                case 4: ok = !"COMPLETED".equals(s) && !"PRELOAD_REJECTED".equals(s) && !"RETURN_JOURNEY".equals(s)
                        && !"RETURNED".equals(s) && !"CANCELLED".equals(s); break;
                default: ok = true;
            }
            if (!q.isEmpty()) {
                String hay = (o.getId() + " " + o.getCropName() + " " + o.getFarmerName() + " " + o.getBuyerName() + " "
                        + o.getPickupAddress() + " " + o.getDropAddress()).toLowerCase(Locale.US);
                ok = ok && hay.contains(q);
            }
            if (!ok) continue;
            shown++;
            card(o, s);
        }
        if (shown == 0) TUi.body(list, orders.isEmpty() ? "No trips yet." : "No trips match.");
    }

    private void card(Order o, String s) {
        LinearLayout c = TUi.card(list);
        boolean done = "COMPLETED".equals(s), empty = "PRELOAD_REJECTED".equals(s), rejected = "RETURN_JOURNEY".equals(s) || "RETURNED".equals(s);
        TUi.pill(c, "#" + o.shortId() + " • " + JobStatus.label(s),
                done ? TUi.SURFACE : empty ? TUi.AMBER_BG : rejected ? TUi.RED_BG : TUi.GREY_BG,
                done ? TUi.DARK : empty ? TUi.AMBER : rejected ? TUi.RED : TUi.INK);
        TUi.route(c, o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress(),
                o.getBuyerName() == null ? "Buyer" : o.getBuyerName(), o.getDropAddress());
        TUi.body(c, String.format(Locale.US, "%s • %,.0f kg%s", o.getCropName(), o.getQuantity(),
                o.getActualDeliveryDate() == null ? "" : " • " + com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(o.getActualDeliveryDate())));
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        if (done) {
            TUi.row(c, "Gross freight fee", TUi.rs(fee));
            TUi.row(c, "5% platform commission", "-" + TUi.rs(fee - o.driverPayout()), TUi.RED, false);
            TUi.row(c, "Net transporter payout", TUi.rs(o.driverPayout()), TUi.ACCENT, true);
        } else if (empty) {
            TUi.row(c, "Empty Trip Fee credited", "+" + TUi.rs(o.getPayment() == null ? 0 : o.getPayment().emptyTripFee), TUi.ACCENT, true);
        } else if (rejected) {
            TUi.row(c, "Damage charge", "-" + TUi.rs(o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion), TUi.RED, true);
        } else {
            TUi.row(c, "Payout on delivery", TUi.rs(o.driverPayout()), TUi.DARK, true);
        }
        c.setOnClickListener(v -> TransporterOrderRouter.open(this, o.getId()));
        if (rejected) TUi.danger(c, "View resolution case", v -> startActivity(new android.content.Intent(this, DisputeCaseActivity.class)
                .putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, o.getId())));
        else TUi.secondary(c, "Open", v -> TransporterOrderRouter.open(this, o.getId()));
    }
}
