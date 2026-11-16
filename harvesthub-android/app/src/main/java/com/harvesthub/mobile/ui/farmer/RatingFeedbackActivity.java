package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.MainActivity;
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

/** Figma 32: rate the buyer and the transporter after completion (workflow 5.4). Sent to the backend → trust score. */
public class RatingFeedbackActivity extends BaseOrderActivity {

    private final int[] stars = {5, 5};
    private final List<List<ImageView>> rows = new ArrayList<>();

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
        if (orderId == null) return;
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
        findViewById(R.id.btnSubmitFeedback).setOnClickListener(this::submit);
    }

    /** The 5 star ImageViews inside a card (the layout has no ids on them). */
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

    private void paint(int row) {
        List<ImageView> r = rows.get(row);
        for (int i = 0; i < r.size(); i++) {
            r.get(i).setImageResource(i < stars[row] ? R.drawable.ic_rating_star_filled : R.drawable.ic_rating_star_unfilled);
        }
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvBatch, (o.getCropName() != null ? o.getCropName() : "") + " | " + kg(o.getQuantity()));
        Order.Payment p = o.getPayment();
        setText(R.id.tvTotalPayout, "Total Payout: " + lkr(p != null ? p.cropValue : o.cropValue()));
        setText(R.id.tvRateBuyer, "Rate the Buyer: " + (o.getBuyerName() != null ? o.getBuyerName() : ""));
        boolean hasDriver = o.getTransporterName() != null;
        setText(R.id.tvRateTransporter, hasDriver ? "Rate the Transporter: " + o.getTransporterName() : "");
        findViewById(R.id.transporterRatingCard).setVisibility(hasDriver ? View.VISIBLE : View.GONE);
        if (o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId())) {
            findViewById(R.id.btnSubmitFeedback).setEnabled(false);
            setText(R.id.btnSubmitFeedback, "Feedback sent – thank you");
        }
    }

    private void submit(View button) {
        button.setEnabled(false);
        String comment = ((EditText) findViewById(R.id.etFeedback)).getText().toString().trim();
        boolean rateDriver = order != null && order.getTransporterName() != null;
        rate("BUYER", stars[0], comment, () -> {
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
                    Toast.makeText(RatingFeedbackActivity.this, AuthInputUtils.errorMessage(response, "Could not send the rating."),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                button.setEnabled(true);
                Toast.makeText(RatingFeedbackActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void done() {
        Toast.makeText(this, "Thank you for your feedback", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.Tab.LISTINGS.name());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
