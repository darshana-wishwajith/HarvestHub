package com.harvesthub.mobile.ui.farmer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ChatMessage;
import com.harvesthub.mobile.model.ConfirmDealRequest;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * In-app negotiation chat. The farmer agrees quantity, price and pickup date
 * with the buyer and taps "Confirm Final Deal". Changing a term is a counter-offer the buyer must confirm.
 */
public class ChatActivity extends AppCompatActivity {

    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;
    private Order order;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private RecyclerView rvMessages;
    private EditText etQuantity, etPrice;
    private TextView tvPickupDate, tvTotalPrice;
    private String pickupDate; // yyyy-MM-dd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupNavigation();

        orderId = getIntent().getStringExtra(OrderFlowRouter.EXTRA_ORDER_ID);
        if (orderId == null || orderId.isEmpty()) {
            Toast.makeText(this, "No order selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getApiService();

        rvMessages = findViewById(R.id.rvMessages);
        chatAdapter = new ChatAdapter(messages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(chatAdapter);

        etQuantity = findViewById(R.id.etQuantity);
        etPrice = findViewById(R.id.etPrice);
        tvPickupDate = findViewById(R.id.tvPickupDate);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        TextWatcher recalc = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateTotal(); }
        };
        etQuantity.addTextChangedListener(recalc);
        etPrice.addTextChangedListener(recalc);
        tvPickupDate.setOnClickListener(v -> pickDate());

        EditText etMessage = findViewById(R.id.etMessage);
        ImageView btnSend = findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(v -> sendMessage(etMessage));
        Button btnConfirmDeal = findViewById(R.id.btnConfirmDeal);
        btnConfirmDeal.setOnClickListener(v -> confirmDeal(btnConfirmDeal));
        findViewById(R.id.btnPlus).setVisibility(View.GONE);
        findViewById(R.id.btnAttachment).setVisibility(View.GONE);

        loadOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (apiService != null) loadMessages();
    }

    private void loadOrder() {
        apiService.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatActivity.this, "Could not load the order.", Toast.LENGTH_SHORT).show();
                    return;
                }
                order = response.body();
                if (!"NEGOTIATING".equals(order.getStatus())) {
                    // the deal moved on: show the right step instead
                    startActivity(OrderFlowRouter.intentFor(ChatActivity.this, order, sessionManager.getUserId()));
                    finish();
                    return;
                }
                bindOrder();
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOrder() {
        ((TextView) findViewById(R.id.tvBuyerName)).setText(order.getBuyerName() != null ? order.getBuyerName() : "Buyer");
        ((TextView) findViewById(R.id.ivAvatar)).setText(initials(order.getBuyerName()));
        ((TextView) findViewById(R.id.tvBuyerRating)).setText(order.isBuyerConfirmed() ? "Confirmed ✓" : "");
        ((TextView) findViewById(R.id.tvProductSummary)).setText(String.format(Locale.US, "%s • %,.0f KG • LKR %,.0f/kg",
                order.getCropName(), order.getQuantity(), order.getPricePerKg()));
        etQuantity.setText(String.format(Locale.US, "%.0f", order.getQuantity()));
        etPrice.setText(String.format(Locale.US, "%.0f", order.getPricePerKg()));
        pickupDate = order.getPickupDate();
        tvPickupDate.setText(pickupDate != null ? pickupDate : "Pick date");
        updateTotal();
        loadMessages();
    }

    private void updateTotal() {
        double q = parse(etQuantity), p = parse(etPrice);
        tvTotalPrice.setText(String.format(Locale.US, "LKR %,.0f", q * p));
    }

    private static double parse(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this, (view, y, m, day) -> {
            pickupDate = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, day);
            tvPickupDate.setText(pickupDate);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.getDatePicker().setMinDate(System.currentTimeMillis());
        d.show();
    }

    private void confirmDeal(Button btn) {
        double q = parse(etQuantity), p = parse(etPrice);
        if (q <= 0 || p <= 0) {
            Toast.makeText(this, "Enter the quantity and price per kg.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pickupDate == null) {
            Toast.makeText(this, "Pick a pickup date.", Toast.LENGTH_SHORT).show();
            pickDate();
            return;
        }
        btn.setEnabled(false);
        apiService.confirmDeal(orderId, new ConfirmDealRequest(q, p, pickupDate)).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                btn.setEnabled(true);
                if (!response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, AuthInputUtils.errorMessage(response, "Could not confirm the deal."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                if (response.body() != null) {
                    Toast.makeText(ChatActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                }
                OrderFlowRouter.open(ChatActivity.this, orderId, true); // waiting for buyer or payment
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btn.setEnabled(true);
                Toast.makeText(ChatActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(EditText etMessage) {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        apiService.sendMessage(orderId, new SendMessageRequest(sessionManager.getUserId(), text)).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessage> call, @NonNull Response<ChatMessage> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.add(response.body());
                    chatAdapter.notifyItemInserted(messages.size() - 1);
                    rvMessages.scrollToPosition(messages.size() - 1);
                    etMessage.setText("");
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessage> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        apiService.getChatMessages(orderId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    chatAdapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        View topNav = findViewById(R.id.included_top_nav);
        topNav.findViewById(R.id.btnNotification).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));
        BottomNavHelper.setup(this, findViewById(R.id.included_nav), BottomNavHelper.Tab.LISTINGS);
    }

    /** "Lahiru Sahan" → "LS", "Kamal" → "K". */
    static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] p = name.trim().split("\\s+");
        String first = p[0].substring(0, 1);
        String last = p.length > 1 ? p[p.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(java.util.Locale.ROOT);
    }
}
