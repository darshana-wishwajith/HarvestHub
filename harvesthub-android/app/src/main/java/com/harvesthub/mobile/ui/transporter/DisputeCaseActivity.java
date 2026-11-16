package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma "Dispute Details & Photo Log" + "Dispute Management": pre-loading evidence vs the buyer's rejection,
 * the money summary, and the status of the driver's appeal tickets (OPEN → RESOLVED by an officer).
 */
public class DisputeCaseActivity extends BaseTransporterOrderActivity {

    private List<Map<String, Object>> tickets = new ArrayList<>();

    @Override
    protected String title() {
        return "Dispute Case";
    }

    @Override
    protected boolean handOver(Order o) {
        return false;
    }

    @Override
    protected void load() {
        super.load();
        api.getDisputes(orderId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    tickets = r.body();
                    if (order != null) render();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) { }
        });
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout head = TUi.card(c);
        TUi.caption(head, "Trip " + ref());
        TUi.text(head, cargo(o), 18, TUi.INK, true);
        TUi.body(head, (o.getFarmerName() == null ? "" : o.getFarmerName()) + " → " + (o.getBuyerName() == null ? "" : o.getBuyerName()));

        LinearLayout tl = TUi.card(c);
        TUi.title(tl, "Case progress");
        TUi.step(tl, "Delivery rejected " + time(o.getActualDeliveryDate()), 2, null);
        TUi.step(tl, "Charge applied to your wallet", 2, null);
        if (tickets.isEmpty()) {
            TUi.step(tl, "No appeal filed", 0, null);
        }
        for (Map<String, Object> t : tickets) {
            String status = String.valueOf(t.get("status"));
            boolean resolved = "RESOLVED".equals(status);
            String who = String.valueOf(t.get("raisedByRole"));
            TUi.step(tl, ("TRANSPORTER".equals(who) ? "Your appeal" : "Issue from " + who.toLowerCase()) + " – " + t.get("reason")
                    + " (" + time((String) t.get("createdAt")) + ")", resolved ? 2 : 1, resolved ? "Resolved" : "Officer review");
            if (t.get("details") != null) TUi.body(tl, "“" + t.get("details") + "”");
            if (resolved && t.get("resolutionNote") != null) TUi.body(tl, "Officer: " + t.get("resolutionNote"));
            Object photos = t.get("photoUrls");
            if (photos instanceof List && !((List<?>) photos).isEmpty()) {
                List<String> urls = new ArrayList<>();
                for (Object p : (List<?>) photos) urls.add(String.valueOf(p));
                TUi.photos(tl, urls);
            }
        }

        LinearLayout ev = TUi.card(c);
        TUi.title(ev, "Evidence comparison");
        TUi.caption(ev, "Pre-loading photos • approved by the buyer");
        TUi.photos(ev, o.getLoadingEvidenceUrls());
        if (o.getLoadingEvidenceAt() != null) TUi.row(ev, "Taken", time(o.getLoadingEvidenceAt()));
        TUi.caption(ev, "Buyer's claim at drop-off");
        TUi.body(ev, CargoRejectedActivity.reason(o));

        double charge = o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion;
        LinearLayout m = TUi.card(c);
        TUi.title(m, "Money summary");
        TUi.row(m, "Gross freight fee", rs(o.getPayment() == null ? 0 : o.getPayment().transportFee));
        TUi.row(m, "Payout for this trip", rs(0), TUi.RED, false);
        TUi.row(m, "Damage charge", "-" + rs(charge), TUi.RED, true);

        if ("RETURN_JOURNEY".equals(o.getStatus()) || "RETURNED".equals(o.getStatus())) {
            boolean myOpen = false;
            for (Map<String, Object> t : tickets) if ("OPEN".equals(t.get("status")) && "TRANSPORTER".equals(t.get("raisedByRole"))) myOpen = true;
            if (!myOpen) TUi.primary(actions, "Appeal with transporter log", v -> startActivity(
                    new Intent(this, AppealDisputeActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
        }
        TUi.secondary(actions, "← Back to Trip History", v -> {
            startActivity(new Intent(this, JobHistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }
}
