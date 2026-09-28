package com.example.project_part_2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.button.MaterialButton;

public class NegotiationChatActivity extends AppCompatActivity {

    private TextView tvNegotiatingCrop;
    private TextView tvTargetPickup;
    private TextView tvOrderRef;
    private TextView tvCardFarmerName;
    private TextView tvCardQuantity;
    private TextView tvCardPrice;
    private TextView tvCardPickupDate;
    private TextView tvStatusBadge;
    private MaterialButton btnAcceptDeal;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;
    private LinearLayout layoutDynamicMessages;
    private NestedScrollView scrollChatArea;

    private boolean isDealAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_negotiation_chat);

        initViews();
        loadIntentData();
        setupListeners();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvNegotiatingCrop = findViewById(R.id.tvNegotiatingCrop);
        tvTargetPickup = findViewById(R.id.tvTargetPickup);
        tvOrderRef = findViewById(R.id.tvOrderRef);
        tvCardFarmerName = findViewById(R.id.tvCardFarmerName);
        tvCardQuantity = findViewById(R.id.tvCardQuantity);
        tvCardPrice = findViewById(R.id.tvCardPrice);
        tvCardPickupDate = findViewById(R.id.tvCardPickupDate);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        btnAcceptDeal = findViewById(R.id.btnAcceptDeal);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        layoutDynamicMessages = findViewById(R.id.layoutDynamicMessages);
        scrollChatArea = findViewById(R.id.scrollChatArea);
    }

    private void loadIntentData() {
        String cropName = getIntent().getStringExtra("crop_name");
        String farmerName = getIntent().getStringExtra("farmer_name");
        String offeredPrice = getIntent().getStringExtra("offered_price");
        String quantity = getIntent().getStringExtra("quantity");
        String pickupDate = getIntent().getStringExtra("pickup_date");
        String orderRef = getIntent().getStringExtra("order_ref");

        if (cropName != null && !cropName.isEmpty()) {
            tvNegotiatingCrop.setText("Negotiating: " + cropName);
        }
        if (farmerName != null && !farmerName.isEmpty()) {
            tvCardFarmerName.setText(farmerName);
        }
        if (offeredPrice != null && !offeredPrice.isEmpty()) {
            tvCardPrice.setText(offeredPrice);
        }
        if (quantity != null && !quantity.isEmpty()) {
            tvCardQuantity.setText(quantity);
        }
        if (pickupDate != null && !pickupDate.isEmpty()) {
            tvTargetPickup.setText("Target Pickup: " + pickupDate);
            tvCardPickupDate.setText(pickupDate);
        }
        if (orderRef != null && !orderRef.isEmpty()) {
            tvOrderRef.setText("Order Ref " + orderRef);
        }
    }

    private void setupListeners() {
        // Accept Deal Button
        btnAcceptDeal.setOnClickListener(v -> {
            if (!isDealAccepted) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Formal Contract")
                        .setMessage("Do you agree to accept this supply offer at " + tvCardPrice.getText().toString() + " for " + tvCardQuantity.getText().toString() + "? Funds will be safely secured in HarvestHub Escrow.")
                        .setPositiveButton("Confirm & Lock Escrow", (dialog, which) -> {
                            isDealAccepted = true;
                            tvStatusBadge.setText("Status: Deal Accepted ✓");
                            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_accepted);
                            btnAcceptDeal.setText("Proceed to Order Summary →");
                            btnAcceptDeal.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_dark_green));
                            btnAcceptDeal.setOnClickListener(v2 -> {
                                Intent intentSummary = new Intent(this, OrderSummaryActivity.class);
                                intentSummary.putExtra("crop_name", "200kg Tomatoes (Grade A)");
                                intentSummary.putExtra("farmer_name", tvCardFarmerName.getText().toString());
                                intentSummary.putExtra("order_ref", tvOrderRef.getText().toString().replace("Order Ref ", ""));
                                intentSummary.putExtra("crop_cost", "Rs. 7,000");
                                intentSummary.putExtra("transit_cost", "Rs. 4,500");
                                intentSummary.putExtra("total_advance", "Rs. 11,845");
                                startActivity(intentSummary);
                            });
                            Toast.makeText(this, "Contract Confirmed! Escrow protection activated.", Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        // Edit Price Counter-Offer
        findViewById(R.id.btnEditPrice).setOnClickListener(v -> showEditDialog("Counter-Offer Price", tvCardPrice.getText().toString(), newPrice -> {
            tvCardPrice.setText(newPrice);
            tvStatusBadge.setText("Status: Counter-Offered");
            appendUserMessage("I would like to propose a counter-offer price of " + newPrice + ".");
        }));

        // Edit Pickup Date Counter-Offer
        findViewById(R.id.btnEditPickupDate).setOnClickListener(v -> showEditDialog("Change Pickup Date", tvCardPickupDate.getText().toString(), newDate -> {
            tvCardPickupDate.setText(newDate);
            tvTargetPickup.setText("Target Pickup: " + newDate);
            appendUserMessage("Can we adjust the pickup date to " + newDate + "?");
        }));

        // Send Message
        btnSendMessage.setOnClickListener(v -> {
            String text = etMessageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                appendUserMessage(text);
                etMessageInput.setText("");
            }
        });
    }

    private void showEditDialog(String title, String currentValue, ValueCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        final EditText input = new EditText(this);
        input.setText(currentValue);
        input.setSelection(input.getText().length());
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String updated = input.getText().toString().trim();
            if (!updated.isEmpty()) {
                callback.onValueSet(updated);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void appendUserMessage(String message) {
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.gravity = Gravity.END;
        containerParams.topMargin = dpToPx(14);
        containerParams.leftMargin = dpToPx(48);
        messageContainer.setLayoutParams(containerParams);

        TextView tvMsg = new TextView(this);
        tvMsg.setBackgroundResource(R.drawable.bg_chat_buyer);
        tvMsg.setText(message);
        tvMsg.setTextColor(ContextCompat.getColor(this, R.color.white));
        tvMsg.setTextSize(14f);
        tvMsg.setLineSpacing(dpToPx(3), 1f);
        tvMsg.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        messageContainer.addView(tvMsg);

        TextView tvTime = new TextView(this);
        tvTime.setText("Just now · Sent");
        tvTime.setTextColor(0xFF9CA3AF);
        tvTime.setTextSize(11f);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.gravity = Gravity.END;
        timeParams.topMargin = dpToPx(4);
        timeParams.rightMargin = dpToPx(4);
        tvTime.setLayoutParams(timeParams);
        messageContainer.addView(tvTime);

        layoutDynamicMessages.addView(messageContainer);

        scrollChatArea.post(() -> scrollChatArea.fullScroll(NestedScrollView.FOCUS_DOWN));
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    private interface ValueCallback {
        void onValueSet(String value);
    }
}
