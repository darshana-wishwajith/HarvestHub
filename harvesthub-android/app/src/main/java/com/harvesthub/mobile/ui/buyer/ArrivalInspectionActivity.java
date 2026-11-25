package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.RemoteImage;

import java.util.Locale;

/**
 * "Post-Delivery Action" screen (ARRIVED_AT_DESTINATION). Compare the goods with the loading photo,
 * then Accept & Pay Balance (80%, card or wallet → 4.2 checkout in balance mode) or Reject (Transit Damage):
 * full refund, the transporter pays the crop advance.
 */
public class ArrivalInspectionActivity extends BaseBuyerOrderActivity {

    @Override
    protected int layout() {
        return R.layout.activity_arrival_inspection;
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvOrderIdPill, ref(o));
        Order.Payment p = o.getPayment();
        if (p != null) {
            setText(R.id.tvRemainingBalance, rs(p.balanceDue));
            setText(R.id.tvSettlementNote, String.format(Locale.US,
                    "%s (20%% crop + transport + fee) is already in escrow. Paying the balance lets the farmer and driver be paid "
                            + "once you confirm the goods were received.", rs(p.advanceDue)));
        }
        ImageView iv = findViewById(R.id.ivBaselineCargo);
        if (o.getLoadingEvidenceUrls() != null && !o.getLoadingEvidenceUrls().isEmpty()) {
            RemoteImage.load(iv, mediaUrl(o.getLoadingEvidenceUrls().get(0)));
            iv.setOnClickListener(v -> {
                Intent i = new Intent(this, EvidenceViewerActivity.class);
                i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, o.getId());
                startActivity(i);
            });
        }
        setText(R.id.tvBaselineMeta, o.getLoadingEvidenceAt() == null ? "Pre-Transit Baseline"
                : "Loaded " + time(o.getLoadingEvidenceAt()));

        findViewById(R.id.btnAcceptPayBalance).setOnClickListener(v ->
                startActivity(SecureCheckoutActivity.balanceIntent(this, o.getId())));
        findViewById(R.id.btnRejectDamage).setOnClickListener(v -> askReject());
    }

    private void askReject() {
        EditText reason = new EditText(this);
        reason.setHint("Describe the damage");
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        reason.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("Reject the delivery?")
                .setMessage("Use this only if the goods were damaged in transit. You get a full refund; the transporter pays "
                        + "for the crop and the goods go back to the farm. This cannot be undone.")
                .setView(reason)
                .setPositiveButton("Reject Delivery", (d, w) -> {
                    String r = reason.getText().toString().trim();
                    sendEvent("REJECT_DELIVERY", r.isEmpty() ? "Damaged in transit" : r, null, findViewById(R.id.btnRejectDamage));
                })
                .setNegativeButton("Cancel", null).show();
    }
}
