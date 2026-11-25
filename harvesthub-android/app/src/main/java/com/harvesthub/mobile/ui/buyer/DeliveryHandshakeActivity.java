package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.widget.Button;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Delivery Handshake" screen (BALANCE_PAID, DELIVERED, COMPLETED). Funds are released when both the
 * driver ("Delivery Completed") and the buyer ("Goods Received") confirm. "Report Issue with Handover" opens a
 * dispute ticket for the officers; money stays as settled.
 */
public class DeliveryHandshakeActivity extends BaseBuyerOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_delivery_handshake;
    }

    @Override
    protected void bind(@NonNull Order o) {
        boolean completed = "COMPLETED".equals(o.getStatus());
        boolean iConfirmed = o.isBuyerConfirmedReceipt() || completed;
        boolean driverConfirmed = o.isTransporterConfirmedDelivery() || completed;

        setText(R.id.tvOrderIdPill, ref(o));
        setText(R.id.tvHandshakeStatus, completed ? "Status: Completed – funds released"
                : iConfirmed ? "Status: Waiting for the driver" : "Status: Pending Buyer Confirmation");
        setText(R.id.tvDriverName, "Driver: " + (o.getTransporterName() == null ? "-" : o.getTransporterName()));
        setText(R.id.tvDriverVehicle, vehicleLine(o));
        setText(R.id.tvCropDetails, kg(o.getQuantity()) + " " + o.getCropName());
        setText(R.id.tvDriverVerified, driverConfirmed ? "Verified" : "Pending");
        setText(R.id.tvDriverSignal, driverConfirmed ? "Transmitted: Delivery Completed" : "Waiting for: Delivery Completed");
        setText(R.id.tvDriverSignalSub, driverConfirmed ? "The driver confirmed the offload"
                : "The driver confirms the offload in their app");

        Order.Payment p = o.getPayment();
        if (p != null) {
            setText(R.id.tvFarmerEscrow, rs(p.balanceDue));
            setText(R.id.tvDriverFee, rs(p.transportFee));
            setText(R.id.tvTotalRelease, rs(p.balanceDue + p.transportFee));
        }
        setText(R.id.tvSettlementBadge, completed ? "Released" : "On 2-way confirm");
        setText(R.id.tvHandshakeNote, completed
                ? (p != null && p.settlementNote != null ? p.settlementNote : "All payments were released.")
                : "Confirming releases the 80% crop balance to the farmer and the transport fee to the driver "
                + "(5% platform commission deducted). This cannot be undone.");

        Button confirm = findViewById(R.id.btnConfirmGoodsReceived);
        boolean rated = o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId());
        if (completed) {
            confirm.setText(rated ? "Back to Orders" : "Rate Farmer & Driver");
            confirm.setEnabled(true);
            confirm.setOnClickListener(v -> {
                if (rated) {
                    Intent i = new Intent(this, BuyerDashboardActivity.class);
                    i.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.ORDERS.name());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    finish();
                } else {
                    Intent i = new Intent(this, BuyerRatingActivity.class);
                    i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, o.getId());
                    startActivity(i);
                }
            });
        } else if (iConfirmed) {
            confirm.setText("Goods received – waiting for the driver");
            confirm.setEnabled(false);
        } else {
            confirm.setText("Confirm Goods Received");
            confirm.setEnabled(true);
            confirm.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Confirm goods received?")
                    .setMessage("Only confirm after the goods are offloaded and checked.")
                    .setPositiveButton("Confirm", (d, w) -> sendEvent("GOODS_RECEIVED", null, null, confirm))
                    .setNegativeButton("Cancel", null).show());
        }

        TextView report = findViewById(R.id.tvReportIssue);
        report.setText(o.isDisputeOpen() ? "Issue reported – an officer is reviewing it" : "Report Issue with Handover");
        report.setEnabled(!o.isDisputeOpen());
        report.setOnClickListener(v -> askDispute());
    }

    @Override
    protected boolean handOver(Order o) {
        // stay here after confirming: COMPLETED is shown on this screen until the buyer leaves
        if ("COMPLETED".equals(o.getStatus()) || "BALANCE_PAID".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus())) return false;
        return super.handOver(o);
    }

    private void askDispute() {
        String[] reasons = {"Short weight", "Quality not as agreed", "Damaged on offload", "Wrong crop or grade", "Other"};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        Spinner reason = new Spinner(this);
        reason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, reasons));
        EditText details = new EditText(this);
        details.setHint("Details (optional)");
        details.setMinLines(2);
        box.addView(reason);
        box.addView(details);
        new AlertDialog.Builder(this)
                .setTitle("Report an issue")
                .setMessage("An agricultural officer reviews the ticket. Payments are not reversed automatically.")
                .setView(box)
                .setPositiveButton("Send", (d, w) -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("reason", (String) reason.getSelectedItem());
                    body.put("details", details.getText().toString().trim());
                    api.openDispute(orderId, body).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> r) {
                            Toast.makeText(DeliveryHandshakeActivity.this, r.isSuccessful()
                                    ? "Issue reported. An officer will review it."
                                    : AuthInputUtils.errorMessage(r, "Could not report the issue."), Toast.LENGTH_LONG).show();
                            load();
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                            Toast.makeText(DeliveryHandshakeActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null).show();
    }
}
