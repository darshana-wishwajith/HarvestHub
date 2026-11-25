package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
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
 * "Dispute Details & Photo Log" screen + "Dispute Management": pre-loading evidence vs the buyer's rejection,
 * the money summary, and the status of the driver's appeal tickets (OPEN → RESOLVED by an officer).
 */
public class DisputeCaseActivity extends BaseTransporterOrderActivity {

    private List<Map<String, Object>> tickets = new ArrayList<>();

    @Override
    protected String title() {
        return "Dispute Details & Photo Log";
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
        int green = Color.parseColor("#15803D");
        String photo = o.getLoadingEvidenceUrls() == null || o.getLoadingEvidenceUrls().isEmpty() ? null : o.getLoadingEvidenceUrls().get(0);
        double fee = o.getPayment() == null ? 0 : o.getPayment().transportFee;
        double charge = o.getPayment() == null ? 0 : o.getPayment().advanceCropPortion;
        boolean rejected = "RETURN_JOURNEY".equals(o.getStatus()) || "RETURNED".equals(o.getStatus());

        LinearLayout head = TUi.card(c);
        LinearLayout hr = TUi.hrow(head, 0);
        hr.setGravity(android.view.Gravity.TOP);
        TUi.thumb(hr, photo, 72, 72);
        LinearLayout ht = TUi.col(hr, 1f);
        ht.setPadding(TUi.dp(this, 12), 0, 0, 0);
        LinearLayout h1 = TUi.hrow(ht, 0);
        TextView fn = TUi.fill(h1, (o.getFarmerName() == null ? "FARM" : o.getFarmerName()).toUpperCase(java.util.Locale.US), 11, green, true);
        fn.setMaxLines(1);
        TUi.label(h1, "Trip " + ref(), 11, TUi.MUTED, true);
        TUi.text(ht, cargo(o), 17, TUi.INK, true);
        TUi.text(ht, (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()) + " \u2192 " + (o.getDropAddress() == null ? "Buyer" : o.getDropAddress()),
                12, TUi.MUTED, false);

        boolean anyOpen = false;
        for (Map<String, Object> t : tickets) if ("OPEN".equals(t.get("status"))) anyOpen = true;
        LinearLayout tl = TUi.card(c);
        TUi.iconHeader(tl, R.drawable.ic_trending_up, "Case Progress & Timeline", anyOpen ? "Officer Review" : tickets.isEmpty() ? "No Appeal" : "Closed",
                anyOpen ? TUi.MINT : TUi.GREY_BG, anyOpen ? TUi.MINT_INK : TUi.INK);
        List<String[]> stops = new ArrayList<>();
        if (rejected) {
            stops.add(new String[]{time(o.reachedAt("RETURN_JOURNEY")), "Delivery Rejected & Charge Applied",
                    "-" + rs(charge) + " damage charge applied to your wallet"});
        }
        List<String> claimPhotos = new ArrayList<>();
        for (Map<String, Object> t : tickets) {
            boolean resolved = "RESOLVED".equals(String.valueOf(t.get("status")));
            String who = String.valueOf(t.get("raisedByRole"));
            String title = ("TRANSPORTER".equals(who) ? "Your Appeal" : "Issue from " + who.toLowerCase(java.util.Locale.US)) + " \u2013 " + t.get("reason");
            String sub = (t.get("details") == null ? "" : "\u201C" + t.get("details") + "\u201D ")
                    + (resolved ? (t.get("resolutionNote") == null ? "Resolved by an officer." : "Officer: " + t.get("resolutionNote")) : "Waiting for officer review.");
            stops.add(new String[]{time((String) t.get("createdAt")), title, sub});
            Object photos = t.get("photoUrls");
            if (photos instanceof List) for (Object p : (List<?>) photos) claimPhotos.add(String.valueOf(p));
        }
        stops.add(new String[]{anyOpen ? "In progress" : "Final", anyOpen ? "HarvestHub Officer Review" : "Case Closure",
                anyOpen ? "An agricultural officer is reviewing the evidence." : tickets.isEmpty() ? "No appeal filed for this trip." : "All tickets on this trip are resolved."});
        LinearLayout tb = TUi.box(tl, Color.WHITE, 6);
        tb.setPadding(0, TUi.dp(this, 10), 0, 0);
        TUi.timeline(tb, stops.toArray(new String[0][]));

        LinearLayout ev = TUi.card(c);
        TUi.text(ev, "Evidence Comparison", 19, TUi.DARK, true);
        TUi.text(ev, "Dispatch verification vs delivery claim", 12, TUi.MUTED, false);
        LinearLayout pre = TUi.box(ev, TUi.GREY_BG, 12);
        TUi.text(pre, "Pre-Loading Camera Log", 15, TUi.INK, true);
        TUi.pill(pre, "\u2713 Buyer Approved Pre-Transit", TUi.MINT, TUi.MINT_INK).setTextSize(11);
        TUi.photos(pre, o.getLoadingEvidenceUrls());
        if (o.getLoadingEvidenceAt() != null) {
            LinearLayout tr = TUi.hrow(pre, 8);
            TUi.icon(tr, R.drawable.ic_clock, 14, TUi.INK);
            TUi.fill(tr, " Timestamp: " + time(o.getLoadingEvidenceAt()), 12, TUi.INK, false);
        }
        LinearLayout vs = TUi.hrow(ev, 10);
        vs.setGravity(android.view.Gravity.CENTER);
        TUi.tag(vs, "\u21C5 VERIFY VS CLAIM", TUi.GREY_BG, TUi.INK);
        LinearLayout post = TUi.box(ev, TUi.GREY_BG, 10);
        TUi.text(post, "Post-Delivery Buyer Claim", 15, TUi.INK, true);
        TUi.pill(post, "\u26A0 Damage Claimed at Drop-off", TUi.RED_BG, TUi.RED).setTextSize(11);
        TUi.text(post, CargoRejectedActivity.reason(o), 13, TUi.INK, false).setPadding(0, TUi.dp(this, 8), 0, 0);
        if (!claimPhotos.isEmpty()) TUi.photos(post, claimPhotos);

        LinearLayout m = TUi.card(c);
        TUi.iconHeader(m, R.drawable.ic_wallet, "Financial Escrow Summary", null, 0, 0);
        LinearLayout mb = TUi.box(m, TUi.GREY_BG, 12);
        TUi.row(mb, "Gross Freight Fee", rs(fee));
        TUi.row(mb, "Payout for This Trip", rs(rejected ? 0 : o.driverPayout()), rejected ? TUi.RED : TUi.INK, false);
        if (rejected) {
            LinearLayout rb = TUi.box(m, TUi.RED_BG, 8);
            LinearLayout rr = TUi.hrow(rb, 0);
            TUi.icon(rr, R.drawable.ic_lock, 16, TUi.RED);
            TextView rl = TUi.fill(rr, "Disputed Liability Charge", 13, TUi.RED, true);
            ((LinearLayout.LayoutParams) rl.getLayoutParams()).leftMargin = TUi.dp(this, 8);
            TUi.label(rr, "-" + rs(charge), 18, TUi.RED, true);
            TUi.text(rb, "Stays on your wallet unless an officer decides otherwise.", 12, TUi.RED, false).setPadding(0, TUi.dp(this, 4), 0, 0);
        }

        if (rejected) {
            boolean myOpen = false;
            for (Map<String, Object> t : tickets) if ("OPEN".equals(t.get("status")) && "TRANSPORTER".equals(t.get("raisedByRole"))) myOpen = true;
            if (!myOpen) TUi.iconButton(actions, "Appeal Dispute with Transporter Log", R.drawable.ic_dispute_appeal, TUi.DARK, Color.WHITE, v -> startActivity(
                    new Intent(this, AppealDisputeActivity.class).putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId)));
        }
        TUi.secondary(actions, "\u2190 Back to Trip History", v -> {
            startActivity(new Intent(this, JobHistoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });
    }

}
