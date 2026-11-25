package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.Tracking;
import com.harvesthub.mobile.ui.transporter.TUi;
import com.harvesthub.mobile.utils.OsmMaps;
import com.harvesthub.mobile.utils.TripMap;

import org.osmdroid.views.MapView;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Active Order Tracker" screen: overview of any order (escrow state, buyer, current step with the
 * full text, a milestone stepper, driver / vehicle, payment) with the real map (farm, buyer, road route, the driver's
 * GPS point during the trip). Does not auto-advance. Shares its layout with the Live Delivery screen.
 */
public class ActiveOrderTrackerActivity extends BaseOrderActivity {

    /** Milestones shown in the stepper, in workflow order (label, statuses that mean "this step is current"). */
    private static final String[][] STEPS = {
            {"Deal agreed", "NEGOTIATING"},
            {"Buyer paid the advance", "AWAITING_PAYMENT"},
            {"Vehicle assigned", "DISPATCHING", "PAUSED", "TRANSPORTER_ASSIGNED"},
            {"Harvesting", "TRANSPORTER_ACCEPTED", "HARVESTING"},
            {"Pickup & loading check", "HARVEST_READY", "ARRIVED_AT_PICKUP", "AWAITING_BUYER_LOADING_APPROVAL", "LOADING_APPROVED"},
            {"On the way to the buyer", "IN_TRANSIT"},
            {"Delivered & confirmed", "ARRIVED_AT_DESTINATION", "BALANCE_PAID", "DELIVERED"},
            {"Paid to your wallet", "COMPLETED"},
    };

    private MapView mapView;
    private TripMap trip;
    private LinearLayout header;
    private LinearLayout content;
    private TextView mapChip;

    @Override
    protected int layout() {
        return R.layout.activity_order_in_transit;
    }

    @Override
    protected boolean followStatus() {
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        OsmMaps.init(this); // before the layout with the MapView is inflated
        super.onCreate(savedInstanceState);
        if (orderId == null) return;
        header = findViewById(R.id.tripHeader);
        content = findViewById(R.id.tripContent);
        mapChip = findViewById(R.id.tvLiveChip);
        mapView = findViewById(R.id.mapFarmerTrip);
        trip = new TripMap(mapView, TUi.DARK, Color.parseColor("#93C5A4"));
        mapView.setOnTouchListener((v, e) -> {
            v.getParent().requestDisallowInterceptTouchEvent(e.getAction() != MotionEvent.ACTION_UP
                    && e.getAction() != MotionEvent.ACTION_CANCEL);
            return false;
        });
        findViewById(R.id.btnRecenter).setOnClickListener(v -> trip.focusVehicle());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }

    @Override
    protected void bind(@NonNull Order o) {
        render(o);
        api.getTracking(orderId).enqueue(new Callback<Tracking>() {
            @Override
            public void onResponse(@NonNull Call<Tracking> call, @NonNull Response<Tracking> r) {
                if (isFinishing() || isDestroyed() || !r.isSuccessful() || r.body() == null) return;
                Tracking t = r.body();
                trip.show(t);
                boolean live = "IN_TRANSIT".equals(o.getStatus()) && t.currentLatitude != null;
                mapChip.setText(live ? "● Live • vehicle on the way" : "Farm → buyer route");
            }

            @Override
            public void onFailure(@NonNull Call<Tracking> call, @NonNull Throwable t) { }
        });
    }

    private void render(Order o) {
        String s = o.getStatus() == null ? "" : o.getStatus();
        boolean bad = Arrays.asList("PRELOAD_REJECTED", "RETURN_JOURNEY", "RETURNED", "CANCELLED").contains(s);

        // ---- header: order, escrow pill, crop and buyer
        header.removeAllViews();
        LinearLayout hc = TUi.card(header, Color.parseColor("#E8F5EA"), Color.parseColor("#C8E6CF"));
        ((LinearLayout.LayoutParams) ((View) hc.getParent()).getLayoutParams()).topMargin = TUi.dp(this, 8);
        LinearLayout hr = TUi.hrow(hc, 0);
        TUi.fill(hr, orderLabel(o), 20, TUi.DARK, true);
        String escrow = escrowLabel(o);
        boolean released = "FUNDS RELEASED".equals(escrow);
        TUi.tag(hr, escrow, released ? TUi.MINT : "REFUNDED".equals(escrow) ? TUi.RED_BG : Color.parseColor("#DBEAFE"),
                released ? TUi.MINT_INK : "REFUNDED".equals(escrow) ? TUi.RED : Color.parseColor("#1E3A8A"));
        TUi.text(hc, kg(o.getQuantity()) + " " + (o.getCropName() == null ? "" : o.getCropName()), 16, TUi.INK, true)
                .setPadding(0, TUi.dp(this, 8), 0, 0);
        TUi.text(hc, "Buyer: " + (o.getBuyerName() == null ? "—" : o.getBuyerName()), 13, TUi.MUTED, false);

        content.removeAllViews();

        // ---- current step: full text, never cut off
        LinearLayout cc = TUi.card(content, bad ? TUi.RED_BG : Color.WHITE, bad ? TUi.RED_BG : TUi.LINE);
        LinearLayout cr = TUi.hrow(cc, 0);
        TUi.iconTile(cr, bad ? R.drawable.ic_info_outline : R.drawable.ic_local_shipping, 44,
                bad ? Color.WHITE : TUi.SURFACE, bad ? TUi.RED : TUi.DARK, true);
        LinearLayout ct = TUi.col(cr, 1f);
        ct.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.text(ct, "CURRENT STEP", 11, TUi.MUTED, true).setLetterSpacing(0.05f);
        TUi.text(ct, stepLabel(o), 15, bad ? TUi.RED : TUi.INK, true); // wraps to as many lines as needed

        // ---- milestones
        if (!bad) {
            LinearLayout mc = TUi.card(content);
            TUi.header(mc, "Order progress", null, 0, 0);
            int current = currentStep(s);
            for (int i = 0; i < STEPS.length; i++) {
                int state = i < current || "COMPLETED".equals(s) ? 2 : i == current ? 1 : 0;
                TUi.step(mc, STEPS[i][0], state, null);
            }
        }

        // ---- driver / vehicle
        LinearLayout dc = TUi.card(content);
        LinearLayout dr = TUi.hrow(dc, 0);
        boolean hasDriver = o.getTransporterName() != null;
        TUi.iconTile(dr, R.drawable.ic_truck, 48, TUi.SURFACE, TUi.DARK, true);
        LinearLayout dt = TUi.col(dr, 1f);
        dt.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
        TextView dn = TUi.text(dt, hasDriver ? o.getTransporterName() : waitingForDriver(s), 15, TUi.INK, true);
        dn.setMaxLines(2);
        dn.setEllipsize(TextUtils.TruncateAt.END);
        TUi.text(dt, hasDriver ? vehicleLine(o) : "Vehicle class: " + (o.getVehicleLabel() == null ? "chosen by weight" : o.getVehicleLabel()),
                12, TUi.MUTED, false);
        if (hasDriver && o.getTransporterPhone() != null) {
            TUi.smallButton(dr, "Call", R.drawable.ic_call, TUi.DARK, Color.WHITE, v -> dial(o.getTransporterPhone()));
        }

        // ---- money
        Order.Payment p = o.getPayment();
        if (p != null) {
            LinearLayout pc = TUi.card(content);
            TUi.iconHeader(pc, R.drawable.ic_wallet, "Your payment", null, 0, 0);
            TUi.row(pc, "Crop value", lkr(p.cropValue));
            TUi.row(pc, "Advance (20%)", p.farmerAdvanceReleased ? lkr(p.advanceCropPortion) + " paid" : lkr(p.advanceCropPortion) + " on pickup",
                    p.farmerAdvanceReleased ? Color.parseColor("#15803D") : TUi.MUTED, true);
            TUi.row(pc, "Balance (80%)", "COMPLETED".equals(s) ? "paid" : "after delivery", "COMPLETED".equals(s)
                    ? Color.parseColor("#15803D") : TUi.MUTED, true);
        }
    }

    private static int currentStep(String s) {
        for (int i = 0; i < STEPS.length; i++) {
            List<String> st = Arrays.asList(STEPS[i]).subList(1, STEPS[i].length);
            if (st.contains(s)) {
                // the advance step is "done" once dispatch starts; NEGOTIATING / AWAITING_PAYMENT are current
                return i;
            }
        }
        return 0;
    }

    private static String waitingForDriver(String s) {
        switch (s) {
            case "TRANSPORTER_ASSIGNED": return "Request sent to nearby drivers";
            case "DISPATCHING":
            case "PAUSED": return "Finding a vehicle";
            default: return "Driver not assigned yet";
        }
    }

    static String escrowLabel(Order o) {
        String e = o.getEscrowStatus() == null ? "" : o.getEscrowStatus();
        switch (e) {
            case "ADVANCE_HELD":
            case "FULLY_PAID":
                return "FUNDS IN ESCROW";
            case "RELEASED":
                return "FUNDS RELEASED";
            case "REFUNDED":
                return "REFUNDED";
            default:
                return "AWAITING PAYMENT";
        }
    }

    static String stepLabel(Order o) {
        Order.Payment p = o.getPayment();
        switch (o.getStatus() == null ? "" : o.getStatus()) {
            case "NEGOTIATING": return "Negotiating the final deal";
            case "AWAITING_PAYMENT": return "Waiting for the buyer's advance payment";
            case "DISPATCHING": return "Buyer payment cleared – finding a vehicle";
            case "TRANSPORTER_ASSIGNED": return "Job request sent to nearby drivers – waiting for one to accept. Don't start harvesting yet.";
            case "PAUSED": return "Buyer payment cleared – no vehicle free yet, retrying every 2 minutes";
            case "TRANSPORTER_ACCEPTED": return "Transporter assigned – you can start harvesting";
            case "HARVESTING": return "Harvesting in progress";
            case "HARVEST_READY": return "Harvest ready – vehicle on the way";
            case "ARRIVED_AT_PICKUP": return "Vehicle at the farm";
            case "AWAITING_BUYER_LOADING_APPROVAL": return "Buyer checking the pre-loading photos";
            case "LOADING_APPROVED": return "Approved – loading the goods";
            case "IN_TRANSIT": return "In transit to the buyer";
            case "ARRIVED_AT_DESTINATION": return "Arrived – buyer inspecting the goods";
            case "BALANCE_PAID":
            case "DELIVERED": return "Delivered – waiting for both confirmations";
            case "COMPLETED": return "Completed – " + (p != null ? lkr(p.cropValue) : "") + " paid to your wallet";
            case "PRELOAD_REJECTED": return "Rejected before loading – Empty Trip Fee "
                    + (p != null ? lkr(p.emptyTripFee) : "") + " charged";
            case "RETURN_JOURNEY": return "Rejected after delivery – you keep the advance";
            case "CANCELLED": return "Cancelled";
            default: return o.getStatus();
        }
    }
}
