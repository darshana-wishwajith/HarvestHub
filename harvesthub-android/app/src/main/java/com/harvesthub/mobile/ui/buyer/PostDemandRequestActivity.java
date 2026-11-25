package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * "Post Demand Request" screen (wanted ad). Crops come from the catalogue (GET /api/crops).
 * With EXTRA_EDIT_AD (JSON of the ad) the same form edits an open ad (PUT /wanted-ads/{id}).
 */
public class PostDemandRequestActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_AD = "edit_ad_json";
    private com.harvesthub.mobile.model.WantedAd editing;
    private String deadlineIso;

    private AutoCompleteTextView actvTargetCrop;
    private EditText etRequiredQuantity;
    private EditText etDeadline;
    private EditText etTargetPrice;
    private EditText etAdditionalDetails;

    private LinearLayout btnGradeA;
    private LinearLayout btnGradeB;
    private LinearLayout btnGradeC;

    private ImageView ivCheckGradeA;
    private ImageView ivCheckGradeB;
    private ImageView ivCheckGradeC;

    private TextView tvGradeA;
    private TextView tvGradeB;
    private TextView tvGradeC;

    private String selectedGrade = "Grade A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_demand_request);

        initViews();
        setupCropDropdown();
        setupDatePicker();
        setupGradeSelectors();
        setupPublishAction();
        String json = getIntent().getStringExtra(EXTRA_EDIT_AD);
        if (json != null) fillForEdit(new com.google.gson.Gson().fromJson(json, com.harvesthub.mobile.model.WantedAd.class));
    }

    private void fillForEdit(com.harvesthub.mobile.model.WantedAd ad) {
        editing = ad;
        TextView title = findViewById(R.id.tvPostDemandTitle);
        if (title != null) title.setText("Edit Demand Request");
        actvTargetCrop.setText(ad.getCropName());
        etRequiredQuantity.setText(String.format(Locale.US, "%.0f", ad.getRequiredQuantityKg()));
        if (ad.getTargetPricePerKg() > 0) etTargetPrice.setText(String.format(Locale.US, "%.0f", ad.getTargetPricePerKg()));
        deadlineIso = ad.getDeadline();
        etDeadline.setText(ad.getDeadline());
        if (ad.getNotes() != null) etAdditionalDetails.setText(ad.getNotes());
        if (ad.getTargetGrade() != null) selectGrade(ad.getTargetGrade().startsWith("Grade") ? ad.getTargetGrade() : "Grade " + ad.getTargetGrade());
        ((MaterialButton) findViewById(R.id.btnPublishDemand)).setText("Save Changes");
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        actvTargetCrop = findViewById(R.id.actvTargetCrop);
        etRequiredQuantity = findViewById(R.id.etRequiredQuantity);
        etDeadline = findViewById(R.id.etDeadline);
        etTargetPrice = findViewById(R.id.etTargetPrice);
        etAdditionalDetails = findViewById(R.id.etAdditionalDetails);

        btnGradeA = findViewById(R.id.btnGradeA);
        btnGradeB = findViewById(R.id.btnGradeB);
        btnGradeC = findViewById(R.id.btnGradeC);

        ivCheckGradeA = findViewById(R.id.ivCheckGradeA);
        ivCheckGradeB = findViewById(R.id.ivCheckGradeB);
        ivCheckGradeC = findViewById(R.id.ivCheckGradeC);

        tvGradeA = findViewById(R.id.tvGradeA);
        tvGradeB = findViewById(R.id.tvGradeB);
        tvGradeC = findViewById(R.id.tvGradeC);
    }

    private void setupCropDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new java.util.ArrayList<>());
        actvTargetCrop.setAdapter(adapter);
        com.harvesthub.mobile.network.ApiClient.getApiService().getCrops().enqueue(new retrofit2.Callback<java.util.List<com.harvesthub.mobile.model.CropInfo>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.harvesthub.mobile.model.CropInfo>> call,
                                   retrofit2.Response<java.util.List<com.harvesthub.mobile.model.CropInfo>> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                java.util.List<String> names = new java.util.ArrayList<>();
                for (com.harvesthub.mobile.model.CropInfo c : r.body()) if (c.getName() != null) names.add(c.getName());
                adapter.clear();
                adapter.addAll(names);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.harvesthub.mobile.model.CropInfo>> call, Throwable t) { }
        });

        LinearLayout layoutTargetCrop = findViewById(R.id.layoutTargetCrop);
        ImageView ivDropdownCrop = findViewById(R.id.ivDropdownCrop);

        View.OnClickListener openDropdown = v -> {
            actvTargetCrop.requestFocus();
            actvTargetCrop.showDropDown();
        };

        if (layoutTargetCrop != null) layoutTargetCrop.setOnClickListener(openDropdown);
        if (ivDropdownCrop != null) ivDropdownCrop.setOnClickListener(openDropdown);
    }

    private void setupDatePicker() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        DatePickerDialog.OnDateSetListener datePickerListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            deadlineIso = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etDeadline.setText(dateFormat.format(calendar.getTime()));
        };

        View.OnClickListener showDatePicker = v -> {
            DatePickerDialog d = new DatePickerDialog(this, datePickerListener,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            d.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            d.show();
        };

        LinearLayout layoutDeadline = findViewById(R.id.layoutDeadline);
        ImageView ivCalendar = findViewById(R.id.ivCalendar);

        if (layoutDeadline != null) layoutDeadline.setOnClickListener(showDatePicker);
        if (etDeadline != null) etDeadline.setOnClickListener(showDatePicker);
        if (ivCalendar != null) ivCalendar.setOnClickListener(showDatePicker);
    }

    private void setupGradeSelectors() {
        btnGradeA.setOnClickListener(v -> selectGrade("Grade A"));
        btnGradeB.setOnClickListener(v -> selectGrade("Grade B"));
        btnGradeC.setOnClickListener(v -> selectGrade("Grade C"));
    }

    private void selectGrade(String grade) {
        selectedGrade = grade;

        btnGradeA.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));
        btnGradeB.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));
        btnGradeC.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));

        ivCheckGradeA.setVisibility(View.GONE);
        ivCheckGradeB.setVisibility(View.GONE);
        ivCheckGradeC.setVisibility(View.GONE);

        tvGradeA.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.regular(tvGradeA.getContext()));
        tvGradeB.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.regular(tvGradeB.getContext()));
        tvGradeC.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.regular(tvGradeC.getContext()));

        switch (grade) {
            case "Grade A":
                btnGradeA.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeA.setVisibility(View.VISIBLE);
                tvGradeA.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(tvGradeA.getContext()));
                break;
            case "Grade B":
                btnGradeB.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeB.setVisibility(View.VISIBLE);
                tvGradeB.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(tvGradeB.getContext()));
                break;
            case "Grade C":
                btnGradeC.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeC.setVisibility(View.VISIBLE);
                tvGradeC.setTypeface(com.harvesthub.mobile.ui.transporter.TUi.bold(tvGradeC.getContext()));
                break;
        }
    }

    private void setupPublishAction() {
        MaterialButton btnPublishDemand = findViewById(R.id.btnPublishDemand);
        btnPublishDemand.setOnClickListener(v -> {
            String crop = actvTargetCrop.getText().toString().trim();
            String qty = etRequiredQuantity.getText().toString().trim();

            if (crop.isEmpty()) {
                Toast.makeText(this, "Please enter or select a target crop", Toast.LENGTH_SHORT).show();
                actvTargetCrop.requestFocus();
                return;
            }

            if (qty.isEmpty()) {
                Toast.makeText(this, "Please enter required quantity", Toast.LENGTH_SHORT).show();
                etRequiredQuantity.requestFocus();
                return;
            }

            if (deadlineIso == null || deadlineIso.isEmpty()) {
                Toast.makeText(this, "Choose the date you need it by", Toast.LENGTH_SHORT).show();
                return;
            }
            double quantity = 0, price = 0;
            try { quantity = Double.parseDouble(qty.replaceAll("[^0-9.]", "")); } catch (Exception ignored) { }
            try { price = Double.parseDouble(etTargetPrice.getText().toString().replaceAll("[^0-9.]", "")); } catch (Exception ignored) { }
            if (quantity <= 0) {
                etRequiredQuantity.setError("Enter the quantity in kg");
                return;
            }
            com.harvesthub.mobile.utils.SessionManager session = new com.harvesthub.mobile.utils.SessionManager(this);
            com.harvesthub.mobile.model.WantedAd ad = new com.harvesthub.mobile.model.WantedAd();
            ad.setCropName(crop);
            if (session.getUser() != null && session.getUser().getFarmLocation() != null) {
                ad.setLocation(session.getUser().getFarmLocation().getAddress());
            }
            ad.setRequiredQuantityKg(quantity);
            ad.setTargetPricePerKg(price);
            ad.setUrgent(isWithinDays(deadlineIso, 3));
            ad.setTargetGrade(selectedGrade.replace("Grade ", ""));
            ad.setDeadline(deadlineIso);
            ad.setNotes(etAdditionalDetails.getText().toString().trim());

            btnPublishDemand.setEnabled(false);
            com.harvesthub.mobile.network.ApiService api = com.harvesthub.mobile.network.ApiClient.getApiService();
            retrofit2.Call<com.harvesthub.mobile.model.WantedAd> call = editing == null ? api.createWantedAd(ad)
                    : api.updateWantedAd(editing.getRequestId(), ad);
            call.enqueue(new retrofit2.Callback<com.harvesthub.mobile.model.WantedAd>() {
                @Override
                public void onResponse(retrofit2.Call<com.harvesthub.mobile.model.WantedAd> c,
                                       retrofit2.Response<com.harvesthub.mobile.model.WantedAd> response) {
                    btnPublishDemand.setEnabled(true);
                    if (!response.isSuccessful()) {
                        Toast.makeText(PostDemandRequestActivity.this,
                                com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(response, "Could not save the request."),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(PostDemandRequestActivity.this, editing == null
                            ? "Published. Farmers can now send you offers." : "Request updated.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(PostDemandRequestActivity.this, BuyerDashboardActivity.class);
                    intent.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.DEMANDS.name());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(retrofit2.Call<com.harvesthub.mobile.model.WantedAd> c, Throwable t) {
                    btnPublishDemand.setEnabled(true);
                    Toast.makeText(PostDemandRequestActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /** Urgent when needed within a few days (shown first on the farmers' Wanted Ads feed). */
    private static boolean isWithinDays(String iso, int days) {
        try {
            java.util.Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso);
            return d != null && d.getTime() - System.currentTimeMillis() < days * 86_400_000L;
        } catch (Exception e) {
            return false;
        }
    }
}
