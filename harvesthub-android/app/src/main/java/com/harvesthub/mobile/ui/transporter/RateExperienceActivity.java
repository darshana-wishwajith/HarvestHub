package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
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
 * Figma "2-Way Handshake Complete" + trip wrap-up rating (workflow 5.4). The driver rates the farmer and the buyer
 * 1–5 stars; highlight chips and the optional note are saved in the rating comment (decision 15 Nov).
 */
public class RateExperienceActivity extends BaseTransporterOrderActivity {

    private static final String[] FARM_CHIPS = {"Fresh Harvest", "On-Time Loading", "Accurate Weight", "Well Packed"};
    private static final String[] BUYER_CHIPS = {"Fast Unloading", "Smooth Handshake", "Helpful Team", "Easy Access"};

    private RatingBar farmStars, buyerStars;
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
        LinearLayout ok = TUi.banner(c, TUi.SURFACE, TUi.DARK, "✓ 2-Way Handshake Complete", "Wallet credited");
        TUi.row(ok, "Amount deposited", "+" + rs(o.driverPayout()), TUi.DARK, true);
        TUi.row(ok, "Order", ref());

        boolean rated = o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId());
        if (rated) {
            TUi.body(c, "You already rated this trip. Thank you!");
            TUi.primary(actions, "Back to Home", v -> home());
            return;
        }
        TUi.caption(c, "Step 2 of 2: trip wrap-up");

        LinearLayout f = TUi.card(c);
        TUi.pill(f, "Origin – farmer", TUi.SURFACE, TUi.DARK);
        TUi.text(f, o.getFarmerName() == null ? "Farmer" : o.getFarmerName(), 18, TUi.INK, true);
        TUi.body(f, o.getPickupAddress() == null ? "" : o.getPickupAddress());
        farmStars = stars(f);
        TUi.caption(f, "Highlights from pickup");
        farmChips = TUi.chips(f, FARM_CHIPS, true, null);

        LinearLayout b = TUi.card(c);
        TUi.pill(b, "Consignee – buyer", TUi.GREY_BG, TUi.INK);
        TUi.text(b, o.getBuyerName() == null ? "Buyer" : o.getBuyerName(), 18, TUi.INK, true);
        TUi.body(b, o.getDropAddress() == null ? "" : o.getDropAddress());
        buyerStars = stars(b);
        TUi.caption(b, "Highlights from delivery");
        buyerChips = TUi.chips(b, BUYER_CHIPS, true, null);

        LinearLayout n = TUi.card(c);
        TUi.text(n, "Optional notes", 15, TUi.INK, true);
        note = TUi.input(n, "Road conditions, loading or unloading notes… (max 300 characters)", true);

        TUi.primary(actions, "✓  Submit Feedback", v -> submit((MaterialButton) v));
        TUi.secondary(actions, "Skip to Home", v -> home());
    }

    private RatingBar stars(LinearLayout parent) {
        RatingBar r = new RatingBar(this);
        r.setNumStars(5);
        r.setStepSize(1f);
        r.setRating(5f);
        r.setProgressTintList(android.content.res.ColorStateList.valueOf(TUi.DARK));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = TUi.dp(this, 8);
        parent.addView(r, lp);
        return r;
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
        rate("FARMER", (int) farmStars.getRating(), comment(farmChips), () ->
                rate("BUYER", (int) buyerStars.getRating(), comment(buyerChips), () -> {
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
