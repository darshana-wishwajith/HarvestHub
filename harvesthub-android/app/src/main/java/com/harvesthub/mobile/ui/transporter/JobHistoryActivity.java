package com.harvesthub.mobile.ui.transporter;

import android.graphics.Color;
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

    private static final String[] FILTERS = {"All Trips", "Completed", "Empty Trip Fee", "Disputes", "Active"};

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
        int green = Color.parseColor("#15803D");
        if (dash != null) {
            LinearLayout h = TUi.card(content, TUi.GREY_BG, TUi.GREY_BG);
            LinearLayout hr = TUi.hrow(h, 0);
            LinearLayout ht = TUi.col(hr, 1f);
            TUi.text(ht, "WEEKLY NET EARNINGS", 11, TUi.INK, true).setLetterSpacing(0.04f);
            TUi.text(ht, TUi.rs(dash.earningsWeek), 26, TUi.DARK, true);
            TUi.iconTile(hr, R.drawable.ic_wallet, 44, TUi.MINT, TUi.DARK, true)
                    .setOnClickListener(v -> startActivity(new android.content.Intent(this, TransporterWalletActivity.class)));
            LinearLayout stats = TUi.box(h, Color.WHITE, 12);
            LinearLayout sr = TUi.hrow(stats, 0);
            stat(sr, "Trips", String.valueOf(dash.tripsCompleted), TUi.INK);
            stat(sr, "Empty Trip Fees", "+" + TUi.rs(dash.emptyTripTotal), green);
            stat(sr, "Rating", dash.ratingAverage == null ? "\u2014" : String.format(Locale.US, "\u2605 %.1f", dash.ratingAverage), TUi.INK);
        }
        LinearLayout th = TUi.hrow(content, 18);
        TUi.fill(th, "Trip History", 20, TUi.INK, true);
        TUi.tag(th, new java.text.SimpleDateFormat("MMMM yyyy", Locale.US).format(new java.util.Date()), TUi.MINT, TUi.MINT_INK);
        search = TUi.input(content, "Search orders, cargo, farms or buyers\u2026", false);
        search.setBackground(TUi.round(Color.WHITE, TUi.dp(this, 40)));
        search.setPadding(TUi.dp(this, 18), TUi.dp(this, 12), TUi.dp(this, 18), TUi.dp(this, 12));
        search.setSingleLine(true);
        search.setElevation(TUi.dp(this, 1));
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

    private void stat(LinearLayout row, String label, String value, int color) {
        LinearLayout v = TUi.col(row, 1f);
        v.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        TUi.text(v, label, 11, TUi.MUTED, true).setGravity(android.view.Gravity.CENTER);
        TUi.text(v, value, 15, color, true).setGravity(android.view.Gravity.CENTER);
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
        int green = Color.parseColor("#15803D");
        LinearLayout c = TUi.card(list);
        boolean done = "COMPLETED".equals(s), empty = "PRELOAD_REJECTED".equals(s), rejected = "RETURN_JOURNEY".equals(s) || "RETURNED".equals(s);
        LinearLayout h = TUi.hrow(c, 0);
        TUi.tag(h, "#" + o.shortId(), TUi.GREY_BG, TUi.INK);
        String when = o.getActualDeliveryDate() != null ? o.getActualDeliveryDate() : o.getCreatedAt();
        TextView date = TUi.fill(h, when == null ? "" : "  " + com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity.time(when), 12, TUi.MUTED, false);
        date.setMaxLines(1);
        TUi.tag(h, done ? "\u2713 Completed" : empty ? "Empty Trip Fee Credited" : rejected ? "\u26A0 Dispute" : JobStatus.label(s),
                done ? TUi.MINT : rejected ? TUi.RED_BG : empty ? Color.parseColor("#E7E5E4") : TUi.AMBER_BG,
                done ? TUi.MINT_INK : rejected ? TUi.RED : empty ? TUi.INK : TUi.AMBER);
        LinearLayout rb = TUi.box(c, TUi.GREY_BG, 10);
        TUi.timeline(rb, new String[]{"Pickup", o.getFarmerName() == null ? "Farm" : o.getFarmerName(), o.getPickupAddress()},
                new String[]{"Drop-off", o.getBuyerName() == null ? "Buyer" : o.getBuyerName(), o.getDropAddress()});
        LinearLayout cb = TUi.box(c, TUi.GREY_BG, 8);
        LinearLayout cr = TUi.hrow(cb, 0);
        TUi.iconTile(cr, empty ? R.drawable.ic_return_queue : R.drawable.ic_package, 40, Color.WHITE, TUi.DARK, false);
        LinearLayout ct = TUi.col(cr, 1f);
        ct.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(ct, empty ? "Empty Trip" : (o.getCropName() == null ? "Produce" : o.getCropName()), 14, TUi.INK, true);
        TUi.text(ct, empty ? "Cargo rejected at pre-loading \u2013 nothing was loaded"
                : String.format(Locale.US, "Cargo Weight: %,.0f kg", o.getQuantity()) + (o.getVehicleLabel() == null ? "" : " \u2022 " + o.getVehicleLabel()),
                12, rejected ? TUi.RED : TUi.MUTED, false);
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        LinearLayout rows = TUi.box(c, Color.WHITE, 2);
        rows.setPadding(TUi.dp(this, 4), TUi.dp(this, 8), TUi.dp(this, 4), 0);
        if (done) {
            TUi.row(rows, "Gross Freight Fee", TUi.rs(fee));
            TUi.row(rows, "5% Platform Dispatch Commission", "-" + TUi.rs(fee - o.driverPayout()), TUi.RED, false);
            TUi.row(rows, "\u2713 Net Transporter Payout", TUi.rs(o.driverPayout()), green, true);
        } else if (empty) {
            double etf = o.getPayment() == null ? 0 : o.getPayment().emptyTripFee;
            TUi.row(rows, "Empty Trip Fuel & Time Allowance", "+" + TUi.rs(etf), green, false);
            TUi.row(rows, "Net Credit Deposited", "+" + TUi.rs(etf), green, true);
        } else if (rejected) {
            double charge = o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion;
            TUi.row(rows, "Gross Freight Fee", TUi.rs(fee));
            TUi.row(rows, "Damage Charge", "-" + TUi.rs(charge), TUi.RED, false);
            TUi.row(rows, "Status", o.isDisputeOpen() ? "Pending Review" : "Charged", TUi.RED, true);
        } else {
            TUi.row(rows, "Payout on Delivery", TUi.rs(o.driverPayout()), TUi.DARK, true);
        }
        ((android.view.View) c.getParent()).setOnClickListener(v -> TransporterOrderRouter.open(this, o.getId()));
        if (rejected) {
            TUi.iconButton(c, "View Resolution Case", R.drawable.ic_dispute_appeal, Color.parseColor("#B91C1C"), Color.WHITE,
                    v -> startActivity(new android.content.Intent(this, DisputeCaseActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, o.getId())));
        } else if (!done && !empty && !"CANCELLED".equals(s)) {
            TUi.smallButton(c, "Continue \u2192", 0, TUi.DARK, Color.WHITE, v -> TransporterOrderRouter.open(this, o.getId()));
        }
    }

}
