package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.graphics.Color;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "2-Way Handshake Complete" screen + trip wrap-up rating. The driver rates the farmer and the buyer
 * 1–5 stars; highlight chips and the optional note are saved in the rating comment.
 */
public class RateExperienceActivity extends BaseTransporterOrderActivity {

    private static final String[] FARM_CHIPS = {"Fresh Harvest", "On-Time Loading", "Accurate Weight", "Well Packed"};
    private static final String[] BUYER_CHIPS = {"Fast Unloading", "Smooth Handshake", "Helpful Team", "Easy Access"};

    private List<TextView> farmChips, buyerChips;
    private EditText note;
    private boolean built;

    @Override
    protected String title() {
        return "Trip Complete";
    }

    @Override
    protected boolean pollWhileOpen() {
        return false;
    }

    @Override
    protected void render() {
        if (built) return; // keep what the driver typed
        built = true;
        super.render();
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        int green = Color.parseColor("#15803D");
        LinearLayout ok = TUi.card(c, Color.parseColor("#D7F5DD"), Color.parseColor("#D7F5DD"));
        LinearLayout or = TUi.hrow(ok, 0);
        TUi.iconTile(or, R.drawable.ic_check, 34, green, Color.WHITE, true);
        LinearLayout ot = TUi.col(or, 1f);
        ot.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(ot, "2-Way Handshake Complete", 19, TUi.DARK, true);
        TUi.text(ot, "WALLET CREDITED INSTANTLY", 11, green, true).setLetterSpacing(0.04f);
        LinearLayout dep = TUi.box(ok, Color.WHITE, 12);
        LinearLayout d1 = TUi.hrow(dep, 0);
        TUi.fill(d1, "Amount Deposited", 13, TUi.INK, false);
        TUi.label(d1, "+" + rs(o.driverPayout()), 20, TUi.DARK, true);
        LinearLayout d2 = TUi.hrow(dep, 2);
        TUi.fill(d2, "Order " + ref(), 12, TUi.INK, true);
        TUi.label(d2, "\u2713 Escrow Cleared", 12, green, true);

        boolean rated = o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId());
        if (rated) {
            TUi.body(c, "You already rated this trip. Thank you!");
            TUi.iconButton(actions, "Back to Home", R.drawable.transporter_ic_bottom_home, TUi.DARK, Color.WHITE, v -> home());
            return;
        }
        LinearLayout sr = TUi.hrow(c, 16);
        TUi.fill(sr, "\u2713  STEP 2 OF 2: TRIP WRAP-UP", 12, TUi.INK, true).setLetterSpacing(0.03f);
        TUi.tag(sr, "2 Parties to Review", TUi.MINT, TUi.MINT_INK);

        farmChips = party(c, R.drawable.ic_tractor, "Origin Dispatcher", TUi.MINT, TUi.MINT_INK,
                o.getFarmerName() == null ? "Farmer" : o.getFarmerName(),
                (o.getPickupAddress() == null ? "" : o.getPickupAddress() + " \u2022 ") + (o.getCropName() == null ? "" : o.getCropName()),
                0, "Highlights from pickup:", FARM_CHIPS);
        buyerChips = party(c, R.drawable.ic_dock_terminal, "Consignee", Color.parseColor("#DBEAFE"), Color.parseColor("#1E3A8A"),
                o.getBuyerName() == null ? "Buyer" : o.getBuyerName(), o.getDropAddress() == null ? "" : o.getDropAddress(),
                1, "Highlights from delivery:", BUYER_CHIPS);

        LinearLayout n = TUi.card(c);
        TUi.iconHeader(n, R.drawable.ic_chat_bubble_outline, "Optional Dispatch Notes", "Optional", Color.WHITE, TUi.MUTED).setTextSize(12);
        note = TUi.input(n, "Add optional notes (e.g. road conditions, loading or unloading speed)\u2026", true);
        note.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(300)});
        TextView max = TUi.text(n, "Max 300 chars", 11, TUi.MUTED, true);
        max.setGravity(android.view.Gravity.END);
        max.setPadding(0, TUi.dp(this, 6), 0, 0);

        TUi.iconButton(actions, "Submit Feedback", R.drawable.ic_check, TUi.DARK, Color.WHITE, v -> submit((MaterialButton) v));
        TextView skip = TUi.link(actions, "Skip to Home Dashboard", TUi.MUTED, v -> home());
        skip.setPaintFlags(skip.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }

    private final int[] stars = {5, 5};
    private static final String[] WORDS = {"", "Poor", "Fair", "Good", "Very Good", "Excellent"};

    /** One rating card: tag, name, star box with a caption, highlight chips. */
    private List<TextView> party(LinearLayout c, int iconRes, String tagText, int tagBg, int tagFg, String name, String sub,
                                 int row, String chipsLabel, String[] chipLabels) {
        LinearLayout card = TUi.card(c);
        LinearLayout h = TUi.hrow(card, 0);
        TUi.iconTile(h, iconRes, 48, TUi.SURFACE, TUi.DARK, false);
        LinearLayout t = TUi.col(h, 1f);
        t.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.pill(t, tagText, tagBg, tagFg).setTextSize(11);
        TextView nm = TUi.text(t, name, 19, TUi.INK, true);
        nm.setMaxLines(1);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (!sub.isEmpty()) TUi.text(t, sub, 12, TUi.MUTED, false);

        LinearLayout box = TUi.box(card, TUi.GREY_BG, 12);
        box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout sr = new LinearLayout(this);
        sr.setGravity(android.view.Gravity.CENTER);
        box.addView(sr);
        TextView caption = TUi.text(box, "", 14, TUi.DARK, true);
        caption.setGravity(android.view.Gravity.CENTER);
        caption.setPadding(0, TUi.dp(this, 6), 0, 0);
        TextView hint = TUi.text(box, "Tap to adjust rating", 12, TUi.MUTED, false);
        hint.setGravity(android.view.Gravity.CENTER);
        final android.widget.ImageView[] views = new android.widget.ImageView[5];
        final Runnable paint = () -> {
            for (int i = 0; i < 5; i++) {
                views[i].setImageResource(i < stars[row] ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                views[i].setColorFilter(i < stars[row] ? Color.parseColor("#15803D") : Color.parseColor("#9CA3AF"));
            }
            caption.setText(WORDS[stars[row]] + " (" + stars[row] + ".0 / 5.0)");
        };
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            android.widget.ImageView iv = new android.widget.ImageView(this);
            int p = TUi.dp(this, 5);
            iv.setPadding(p, p, p, p);
            iv.setOnClickListener(v -> {
                stars[row] = value;
                paint.run();
            });
            sr.addView(iv, new LinearLayout.LayoutParams(TUi.dp(this, 44), TUi.dp(this, 44)));
            views[i] = iv;
        }
        paint.run();
        TUi.text(card, chipsLabel, 13, TUi.INK, false).setPadding(0, TUi.dp(this, 12), 0, 0);
        return TUi.chips(card, chipLabels, true, null);
    }

    private String comment(List<TextView> chips) {
        StringBuilder sb = new StringBuilder();
        for (TextView t : chips) if (TUi.isOn(t)) sb.append(sb.length() == 0 ? "" : ", ").append(TUi.chipText(t));
        String extra = note.getText().toString().trim();
        if (extra.length() > 300) extra = extra.substring(0, 300);
        if (!extra.isEmpty()) sb.append(sb.length() == 0 ? "" : " – ").append(extra);
        return sb.toString();
    }

    private void submit(MaterialButton button) {
        button.setEnabled(false);
        rate("FARMER", stars[0], comment(farmChips), () ->
                rate("BUYER", stars[1], comment(buyerChips), () -> {
                    Toast.makeText(this, "Thanks for your feedback!", Toast.LENGTH_SHORT).show();
                    home();
                }, button), button);
    }

    private void rate(String role, int stars, String comment, Runnable next, MaterialButton button) {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", Math.max(1, stars));
        body.put("toRole", role);
        body.put("comment", comment);
        api.submitRating(orderId, body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> r) {
                String err = r.isSuccessful() ? null : AuthInputUtils.errorMessage(r, "Rating failed.");
                if (err != null && !err.contains("already rated")) {
                    button.setEnabled(true);
                    Toast.makeText(RateExperienceActivity.this, err, Toast.LENGTH_LONG).show();
                    return;
                }
                next.run();
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                button.setEnabled(true);
                Toast.makeText(RateExperienceActivity.this, "Can't reach the server.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void home() {
        startActivity(new Intent(this, DeliveryDashboardActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }
}
