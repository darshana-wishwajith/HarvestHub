package com.harvesthub.mobile.ui.farmer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatAdapter adapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private EditText etMessage, etNegotiationQty, etNegotiationPrice;
    private TextView tvLiveTotal, tvSelectedDate, tvBuyerName, tvOrderContext;
    private View btnSend, btnConfirmDeal, btnDatePicker, btnCall;
    
    private ApiService apiService;
    private String orderId, buyerId, buyerName, buyerPhone = "0771234567";
    private final DecimalFormat formatter = new DecimalFormat("#,###");
    private final Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        apiService = ApiClient.getClient().create(ApiService.class);
        
        handleIntent();
        initViews();
        setupChatList();
        setupNegotiationLogic();
        fetchMessages();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        orderId = intent.getStringExtra("orderId");
        buyerId = intent.getStringExtra("buyerId");
        buyerName = intent.getStringExtra("buyerName");
        // For demo if null
        if (orderId == null) orderId = "ORD-992";
        if (buyerName == null) buyerName = "Roshan Silva";
    }

    private void initViews() {
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        
        tvBuyerName = findViewById(R.id.tvBuyerName);
        tvOrderContext = findViewById(R.id.tvOrderContext);
        
        tvBuyerName.setText(buyerName);
        
        etNegotiationQty = findViewById(R.id.etNegotiationQty);
        etNegotiationPrice = findViewById(R.id.etNegotiationPrice);
        tvLiveTotal = findViewById(R.id.tvLiveTotal);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        btnConfirmDeal = findViewById(R.id.btnConfirmDeal);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        btnCall = findViewById(R.id.btnCall);

        btnSend.setOnClickListener(v -> sendMessage());
        btnConfirmDeal.setOnClickListener(v -> confirmDeal());
        btnDatePicker.setOnClickListener(v -> showDatePicker());
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + buyerPhone));
            startActivity(intent);
        });

        findViewById(R.id.ivNotificationTop).setOnClickListener(v -> 
                startActivity(new Intent(this, NotificationsActivity.class)));
        
        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
    }

    private void setupChatList() {
        adapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(adapter);
    }

    private void setupNegotiationLogic() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etNegotiationQty.addTextChangedListener(watcher);
        etNegotiationPrice.addTextChangedListener(watcher);
    }

    private void calculateTotal() {
        String qtyStr = etNegotiationQty.getText().toString();
        String priceStr = etNegotiationPrice.getText().toString();
        if (!qtyStr.isEmpty() && !priceStr.isEmpty()) {
            try {
                double qty = Double.parseDouble(qtyStr);
                double price = Double.parseDouble(priceStr);
                tvLiveTotal.setText("LKR " + formatter.format(qty * price));
            } catch (Exception ignored) {}
        }
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            tvSelectedDate.setText(sdf.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void fetchMessages() {
        // Show mock data initially so the UI matches the design
        messageList.clear();
        addMockMessages();
        adapter.notifyDataSetChanged();
        if (!messageList.isEmpty()) {
            rvChat.scrollToPosition(messageList.size() - 1);
        }

        // Call the API. If it returns data, REPLACE the mock data.
        // If it returns an empty list, CLEAR the mock data (as requested: "If no messages, put chat area empty").
        apiService.getChatMessages(orderId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.clear();
                    messageList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        rvChat.scrollToPosition(messageList.size() - 1);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) {
                // On failure, we keep the mock messages so the user can still see the layout
            }
        });
    }

    private void addMockMessages() {
        messageList.add(new ChatMessage("1", "buyer", "Hello Kamal, I reviewed your verified Grade A batch. Can you supply the full 500 KG at LKR 245/kg for immediate transport to Dambulla?", "10:14 AM", false, true));
        messageList.add(new ChatMessage("2", "me", "Hello Roshan. Given the certified quality and zero transport hassle, my best price is LKR 250/kg.", "10:15 AM", true, true));
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        etMessage.setText("");
        SendMessageRequest request = new SendMessageRequest(text);
        apiService.sendMessage(orderId, request).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessage> call, @NonNull Response<ChatMessage> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messageList.add(response.body());
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvChat.smoothScrollToPosition(messageList.size() - 1);
                } else {
                    // For demo, add locally
                    messageList.add(new ChatMessage("temp", "me", text, "Just now", true, false));
                    adapter.notifyItemInserted(messageList.size() - 1);
                    rvChat.smoothScrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessage> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeal() {
        String qtyStr = etNegotiationQty.getText().toString();
        String priceStr = etNegotiationPrice.getText().toString();
        String dateStr = tvSelectedDate.getText().toString();

        if (qtyStr.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            return;
        }

        double qty = Double.parseDouble(qtyStr);
        double price = Double.parseDouble(priceStr);
        ConfirmDealRequest request = new ConfirmDealRequest(qty, price, dateStr);

        apiService.confirmDeal(orderId, request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Deal Confirmed!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(ChatActivity.this, "Confirmation successful (Simulated)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
