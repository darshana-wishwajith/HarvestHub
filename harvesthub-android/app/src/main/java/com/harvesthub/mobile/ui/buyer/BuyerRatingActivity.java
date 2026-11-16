package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer 6.4 "Post-Transaction Rating Sheet": the buyer rates the farmer (crop quality) and the driver
 * (communication, punctuality). Ratings feed the trust scores. Reuses the farmer rating layout without its
 * farmer navigation bars.
 */
public class BuyerRatingActivity extends BaseBuyerOrderActivity {

    private final List<List<ImageView>> rows = new ArrayList<>();
    private final int[] stars = {5, 5};

    @Override
    protected int layout() {
        return R.layout.activity_rating_feedback;
    }

    @Override
    protected boolean followStatus() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (api == null) return;
        show(R.id.included_top_nav, false);
        show(R.id.included_nav, false);
        rows.add(starRow(findViewById(R.id.buyerRatingCard)));
        rows.add(starRow(findViewById(R.id.transporterRatingCard)));
        for (int r = 0; r < rows.size(); r++) {
            final int row = r;
            for (int i = 0; i < rows.get(r).size(); i++) {
                final int value = i + 1;
                rows.get(r).get(i).setOnClickListener(v -> {
                    stars[row] = value;
                    paint(row);
                });
            }
            paint(r);
        }
        relabel(findViewById(R.id.buyerRatingCard), getString(R.string.communication_fairness), "How was the crop quality?");
        relabel(findViewById(R.id.transporterRatingCard), getString(R.string.punctuality_handling), "How was the communication and punctuality?");
        ((EditText) findViewById(R.id.etFeedback)).setHint("Anything else? (optional)");
        findViewById(R.id.btnSubmitFeedback).setOnClickListener(this::submit);
    }

    private static List<ImageView> starRow(View card) {
        List<ImageView> best = new ArrayList<>();
        collect(card, best);
        return best;
    }

    private static void collect(View v, List<ImageView> best) {
        if (!(v instanceof ViewGroup)) return;
        ViewGroup g = (ViewGroup) v;
        List<ImageView> direct = new ArrayList<>();
        for (int i = 0; i < g.getChildCount(); i++) {
            if (g.getChildAt(i) instanceof ImageView) direct.add((ImageView) g.getChildAt(i));
        }
        if (direct.size() == 5 && best.isEmpty()) {
            best.addAll(direct);
            return;
        }
        for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), best);
    }

    private static void relabel(View v, String from, String to) {
        if (v instanceof TextView && from.contentEquals(((TextView) v).getText())) {
            ((TextView) v).setText(to);
        } else if (v instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) relabel(((ViewGroup) v).getChildAt(i), from, to);
        }
    }

    private void paint(int row) {
        List<ImageView> r = rows.get(row);
        for (int i = 0; i < r.size(); i++) {
            r.get(i).setImageResource(i < stars[row] ? R.drawable.ic_rating_star_filled : R.drawable.ic_rating_star_unfilled);
        }
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvBatch, "Order " + ref(o) + " | " + o.getCropName() + " | " + kg(o.getQuantity()));
        Order.Payment p = o.getPayment();
        setText(R.id.tvTotalPayout, "Total paid: " + rs(p == null ? o.cropValue() : p.advanceDue + p.balanceDue));
        setText(R.id.tvRateBuyer, "Farmer: " + (o.getFarmerName() == null ? "" : o.getFarmerName()));
        boolean hasDriver = o.getTransporterName() != null;
        setText(R.id.tvRateTransporter, hasDriver ? "Driver: " + o.getTransporterName() + " • " + vehicleLine(o) : "");
        show(R.id.transporterRatingCard, hasDriver);
        if (o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId())) {
            findViewById(R.id.btnSubmitFeedback).setEnabled(false);
            setText(R.id.btnSubmitFeedback, "Feedback sent – thank you");
        }
    }

    private void submit(View button) {
        button.setEnabled(false);
        String comment = ((EditText) findViewById(R.id.etFeedback)).getText().toString().trim();
        boolean rateDriver = order != null && order.getTransporterName() != null;
        rate("FARMER", stars[0], comment, () -> {
            if (rateDriver) rate("TRANSPORTER", stars[1], comment, this::done, button);
            else done();
        }, button);
    }

    private void rate(String toRole, int value, String comment, Runnable next, View button) {
        Map<String, Object> body = new HashMap<>();
        body.put("rating", value);
        body.put("toRole", toRole);
        body.put("comment", comment);
        api.submitRating(orderId, body).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    next.run();
                } else {
                    button.setEnabled(true);
                    Toast.makeText(BuyerRatingActivity.this, AuthInputUtils.errorMessage(response, "Could not send the rating."),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                button.setEnabled(true);
                Toast.makeText(BuyerRatingActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void done() {
        Toast.makeText(this, "Thank you for your feedback", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, BuyerDashboardActivity.class);
        i.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.ORDERS.name());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
