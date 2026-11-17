package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
 * Figma buyer 6.4 "Rate Your Experience": the buyer rates the farmer (crop quality) and the driver
 * (communication, punctuality), each with its own stars and comment. Ratings feed the trust scores.
 */
public class BuyerRatingActivity extends BaseBuyerOrderActivity {

    private final List<List<ImageView>> rows = new ArrayList<>();
    private final int[] stars = {5, 5};
    private final int[] scoreIds = {R.id.tvFarmerScore, R.id.tvDriverScore};

    @Override
    protected int layout() {
        return R.layout.activity_buyer_rating;
    }

    @Override
    protected boolean followStatus() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (api == null) return;
        rows.add(buildStars(findViewById(R.id.starsFarmer), 0));
        rows.add(buildStars(findViewById(R.id.starsDriver), 1));
        paint(0);
        paint(1);
        findViewById(R.id.btnSubmitFeedback).setOnClickListener(this::submit);
    }

    private List<ImageView> buildStars(LinearLayout holder, int row) {
        List<ImageView> list = new ArrayList<>();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < 5; i++) {
            final int value = i + 1;
            ImageView star = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(36 * d), Math.round(36 * d));
            lp.setMarginEnd(Math.round(8 * d));
            star.setLayoutParams(lp);
            star.setPadding(Math.round(3 * d), Math.round(3 * d), Math.round(3 * d), Math.round(3 * d));
            star.setContentDescription(value + " star");
            star.setOnClickListener(v -> {
                stars[row] = value;
                paint(row);
            });
            holder.addView(star);
            list.add(star);
        }
        return list;
    }

    private void paint(int row) {
        List<ImageView> r = rows.get(row);
        for (int i = 0; i < r.size(); i++) {
            r.get(i).setImageResource(i < stars[row] ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }
        setText(scoreIds[row], stars[row] + ".0");
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split(" +");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase(java.util.Locale.US);
    }

    @Override
    protected void bind(@NonNull Order o) {
        setText(R.id.tvRatingOrderRef, ref(o));
        String farmer = o.getFarmerName() == null ? "" : o.getFarmerName();
        setText(R.id.tvRateFarmerName, "Farmer: " + farmer);
        setText(R.id.tvFarmerInitials, initials(farmer));
        String place = o.getPickupAddress() == null ? "" : o.getPickupAddress() + " • ";
        setText(R.id.tvRateFarmerMeta, place + kg(o.getQuantity()) + " " + o.getCropName());
        boolean hasDriver = o.getTransporterName() != null;
        show(R.id.cardRateDriver, hasDriver);
        if (hasDriver) {
            setText(R.id.tvRateDriverName, "Driver: " + o.getTransporterName());
            setText(R.id.tvDriverInitials, initials(o.getTransporterName()));
            setText(R.id.tvRateDriverMeta, vehicleLine(o));
        }
        if (o.getRatedBy() != null && o.getRatedBy().contains(session.getUserId())) {
            findViewById(R.id.btnSubmitFeedback).setEnabled(false);
            setText(R.id.btnSubmitFeedback, "Feedback sent – thank you");
        }
    }

    private String text(int id) {
        return ((EditText) findViewById(id)).getText().toString().trim();
    }

    private void submit(View button) {
        button.setEnabled(false);
        boolean rateDriver = order != null && order.getTransporterName() != null;
        rate("FARMER", stars[0], text(R.id.etFarmerComment), () -> {
            if (rateDriver) rate("TRANSPORTER", stars[1], text(R.id.etDriverComment), this::done, button);
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
