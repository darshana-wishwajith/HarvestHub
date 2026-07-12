package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.CropBatch;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.SupplyOfferRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSupplyOfferActivity extends AppCompatActivity {

    private TextView tvRespondingTo, tvSelectedBatch, tvPayout;
    private ImageView ivSelectedBatchVerified;
    private TextInputLayout tilQuantity, tilPrice;
    private TextInputEditText etQuantity, etPrice;
    private MaterialButton btnSendOffer;
    private View cardSelectBatch;

    private String requestId, cropName, location;
    private double requestedQuantity, targetPrice;
    private List<CropBatch> verifiedBatches = new ArrayList<>();
    private CropBatch selectedBatch;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_supply_offer);

        initViews();
        handleIntent();
        setupListeners();
        fetchVerifiedBatches();
    }

    private void initViews() {
        tvRespondingTo = findViewById(R.id.tvRespondingTo);
        tvSelectedBatch = findViewById(R.id.tvSelectedBatch);
        tvPayout = findViewById(R.id.tvPayout);
        ivSelectedBatchVerified = findViewById(R.id.ivSelectedBatchVerified);
        tilQuantity = findViewById(R.id.tilQuantity);
        tilPrice = findViewById(R.id.tilPrice);
        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);
        btnSendOffer = findViewById(R.id.btnSendOffer);
        cardSelectBatch = findViewById(R.id.cardSelectBatch);

        findViewById(R.id.ivNotification).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Bottom Nav setup
        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
        // Add other nav listeners if needed
    }

    private void handleIntent() {
        Intent intent = getIntent();
        requestId = intent.getStringExtra("requestId");
        cropName = intent.getStringExtra("cropName");
        requestedQuantity = intent.getDoubleExtra("requestedQuantity", 0);
        targetPrice = intent.getDoubleExtra("targetPrice", 0);
        location = intent.getStringExtra("location");

        String respondingText = String.format("Responding to: %s Req (%s KG | Target: LKR %s/kg - %s)",
                cropName, formatter.format(requestedQuantity), formatter.format(targetPrice), location);
        tvRespondingTo.setText(respondingText);
    }

    private void setupListeners() {
        cardSelectBatch.setOnClickListener(v -> showBatchPicker());

        TextWatcher calculationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculatePayout();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        etQuantity.addTextChangedListener(calculationWatcher);
        etPrice.addTextChangedListener(calculationWatcher);

        btnSendOffer.setOnClickListener(v -> validateAndSubmit());
    }

    private void calculatePayout() {
        String qtyStr = etQuantity.getText().toString();
        String priceStr = etPrice.getText().toString();

        if (qtyStr.isEmpty() || priceStr.isEmpty()) {
            tvPayout.setText("Estimated Total Payout: LKR 0 (Zero farmer commission)");
            return;
        }

        try {
            double qty = Double.parseDouble(qtyStr);
            double price = Double.parseDouble(priceStr);
            double total = qty * price;
            tvPayout.setText(String.format("Estimated Total Payout: LKR %s (Zero farmer commission)", formatter.format(total)));
        } catch (NumberFormatException e) {
            tvPayout.setText("Estimated Total Payout: LKR 0 (Zero farmer commission)");
        }
    }

    private void fetchVerifiedBatches() {
        String farmerId = "FARMER_123"; // Should be from session/auth
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.getFarmerVerifiedBatches(farmerId, cropName).enqueue(new Callback<List<CropBatch>>() {
            @Override
            public void onResponse(@NonNull Call<List<CropBatch>> call, @NonNull Response<List<CropBatch>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    verifiedBatches = response.body();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CropBatch>> call, @NonNull Throwable t) {
                Toast.makeText(CreateSupplyOfferActivity.this, "Failed to load batches", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBatchPicker() {
        if (verifiedBatches.isEmpty()) {
            Toast.makeText(this, "No verified batches available", Toast.LENGTH_SHORT).show();
            return;
        }

        BatchBottomSheetFragment bottomSheet = new BatchBottomSheetFragment(verifiedBatches, batch -> {
            selectedBatch = batch;
            tvSelectedBatch.setText(String.format("Attach Batch: #%s (Verified - %s KG available)",
                    batch.getBatchNumber(), (int)batch.getAvailableQuantity()));
            ivSelectedBatchVerified.setVisibility(View.VISIBLE);
        });
        bottomSheet.show(getSupportFragmentManager(), "BatchPicker");
    }

    private void validateAndSubmit() {
        if (selectedBatch == null) {
            Toast.makeText(this, "Please select a verified batch", Toast.LENGTH_SHORT).show();
            return;
        }

        String qtyStr = etQuantity.getText().toString();
        String priceStr = etPrice.getText().toString();

        if (qtyStr.isEmpty()) {
            tilQuantity.setError("Enter quantity");
            return;
        }
        tilQuantity.setError(null);

        double qty = Double.parseDouble(qtyStr);
        if (qty <= 0 || qty > selectedBatch.getAvailableQuantity()) {
            tilQuantity.setError("Invalid quantity. Max: " + (int)selectedBatch.getAvailableQuantity() + " KG");
            return;
        }

        if (priceStr.isEmpty()) {
            tilPrice.setError("Enter proposed price");
            return;
        }
        tilPrice.setError(null);

        double price = Double.parseDouble(priceStr);
        if (price <= 0) {
            tilPrice.setError("Enter valid price");
            return;
        }

        submitOffer(qty, price);
    }

    private void submitOffer(double qty, double price) {
        btnSendOffer.setEnabled(false);
        btnSendOffer.setText("Sending...");

        SupplyOfferRequest request = new SupplyOfferRequest(requestId, selectedBatch.getId(), qty, price);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.createSupplyOffer(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                btnSendOffer.setEnabled(true);
                btnSendOffer.setText("Send Offer to Buyer");

                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CreateSupplyOfferActivity.this, "Offer sent to buyer", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String detailedErrorMessage = "Failed to send offer";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            detailedErrorMessage = errorBody.string();
                        } else if (response.message() != null && !response.message().isEmpty()) {
                            detailedErrorMessage = response.message();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CreateOfferError", "Error parsing error body", e);
                    }
                    Toast.makeText(CreateSupplyOfferActivity.this, detailedErrorMessage, Toast.LENGTH_SHORT).show();
                    android.util.Log.e("CreateOfferError", "Error: " + detailedErrorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btnSendOffer.setEnabled(true);
                btnSendOffer.setText("Send Offer to Buyer");
                String errorMessage = "Network error: " + t.getMessage();
                Toast.makeText(CreateSupplyOfferActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                android.util.Log.e("CreateOfferError", errorMessage, t);
            }
        });
    }
}
