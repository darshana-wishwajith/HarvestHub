package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ChatMessage;
import com.harvesthub.mobile.model.ConfirmDealRequest;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private String orderId;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_negotiation_chat);

        initViews();
        loadIntentData();
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        loadMessages();
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
        orderId = getIntent().getStringExtra("order_id");
        if (orderId == null || orderId.isEmpty()) {
            orderId = getIntent().getStringExtra("ORDER_ID");
        }
        if (orderId == null || orderId.isEmpty()) {
            orderId = "order_001";
        }

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
                        .setPositiveButton("Confirm & Lock Escrow", (dialog, which) -> confirmDeal())
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
                sendChatMessage(text);
            }
        });
    }

    private void loadMessages() {
        if (orderId == null || orderId.isEmpty()) return;
        apiService.getChatMessages(orderId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (ChatMessage message : response.body()) {
                        if (message.getText() != null) appendUserMessage(message.getText());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable throwable) { }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    private void sendChatMessage(String text) {
        String senderId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                ? sessionManager.getUser().getId() : "user_buyer_perera";
        SendMessageRequest request = new SendMessageRequest(senderId, text);
        apiService.sendMessage(orderId, request).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(Call<ChatMessage> call, Response<ChatMessage> response) {
                if (response.isSuccessful()) {
                    appendUserMessage(text);
                    etMessageInput.setText("");
                }
            }

            @Override
            public void onFailure(Call<ChatMessage> call, Throwable throwable) {
                Toast.makeText(NegotiationChatActivity.this, "Message could not be sent.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeal() {
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "This conversation has no order reference.", Toast.LENGTH_LONG).show();
            return;
        }
        ConfirmDealRequest request = new ConfirmDealRequest(
                parseNumber(tvCardQuantity.getText().toString()),
                parseNumber(tvCardPrice.getText().toString()),
                tvCardPickupDate.getText().toString());
        apiService.confirmDeal(orderId, request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(NegotiationChatActivity.this, "Deal could not be confirmed.", Toast.LENGTH_LONG).show();
                    return;
                }
                isDealAccepted = true;
                tvStatusBadge.setText("Status: Deal Accepted");
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_accepted);
                btnAcceptDeal.setText("Proceed to Order Summary");
                btnAcceptDeal.setBackgroundColor(ContextCompat.getColor(NegotiationChatActivity.this, R.color.brand_dark_green));
                btnAcceptDeal.setOnClickListener(v -> openOrderSummary());
                Toast.makeText(NegotiationChatActivity.this, "Deal confirmed and recorded.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable throwable) {
                Toast.makeText(NegotiationChatActivity.this, "Network error while confirming the deal.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openOrderSummary() {
        Intent intentSummary = new Intent(this, OrderSummaryActivity.class);
        intentSummary.putExtra("order_id", orderId);
        intentSummary.putExtra("crop_name", tvNegotiatingCrop.getText().toString().replace("Negotiating: ", ""));
        intentSummary.putExtra("farmer_name", tvCardFarmerName.getText().toString());
        intentSummary.putExtra("order_ref", orderId);
        intentSummary.putExtra("crop_cost", tvCardPrice.getText().toString());
        intentSummary.putExtra("transit_cost", "0");
        intentSummary.putExtra("total_advance", tvCardPrice.getText().toString());
        startActivity(intentSummary);
    }

    private double parseNumber(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9.]", "");
        try {
            return normalized.isEmpty() ? 0 : Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
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
        tvTime.setText("Just now Â· Sent");
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

