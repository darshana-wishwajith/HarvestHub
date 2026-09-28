package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ChatMessage;
import com.harvesthub.mobile.model.ConfirmDealRequest;
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private ApiService apiService;
    private SessionManager sessionManager;
    private String orderId;
    private RecyclerView rvMessages;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupNavigation();

        orderId = getIntent().getStringExtra("ORDER_ID");
        if (orderId == null || orderId.isEmpty()) {
            orderId = getIntent().getStringExtra("order_id");
        }
        if (orderId == null || orderId.isEmpty()) {
            orderId = "order_001";
        }
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);

        rvMessages = findViewById(R.id.rvMessages); // Ensure this ID matches layout
        if (rvMessages != null) {
            chatAdapter = new ChatAdapter(messages);
            rvMessages.setLayoutManager(new LinearLayoutManager(this));
            rvMessages.setAdapter(chatAdapter);
        }

        loadMessages();

        ImageView btnSendMessage = findViewById(R.id.btnSendMessage);
        EditText etMessage = findViewById(R.id.etMessage);
        Button btnConfirmDeal = findViewById(R.id.btnConfirmDeal);
        ImageView btnPlus = findViewById(R.id.btnPlus);
        ImageView btnAttachment = findViewById(R.id.btnAttachment);

        btnSendMessage.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                String senderId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null)
                        ? sessionManager.getUser().getId() : "user_farmer_kamal";
                SendMessageRequest request = new SendMessageRequest(senderId, message);
                apiService.sendMessage(orderId, request).enqueue(new Callback<ChatMessage>() {
                    @Override
                    public void onResponse(Call<ChatMessage> call, Response<ChatMessage> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            messages.add(response.body());
                            if (chatAdapter != null) chatAdapter.notifyItemInserted(messages.size() - 1);
                            if (rvMessages != null) rvMessages.scrollToPosition(messages.size() - 1);
                            etMessage.setText("");
                        } else {
                            Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatMessage> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmDeal.setOnClickListener(v -> {
            ConfirmDealRequest request = new ConfirmDealRequest("CONFIRMED", "Confirmed by farmer");
            apiService.confirmDeal(orderId, request).enqueue(new retrofit2.Callback<com.harvesthub.mobile.model.GenericResponse>() {
                @Override
                public void onResponse(Call<com.harvesthub.mobile.model.GenericResponse> call, Response<com.harvesthub.mobile.model.GenericResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, "Deal confirmed!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ChatActivity.this, WaitForBuyerApprovalActivity.class);
                        intent.putExtra("ORDER_ID", orderId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ChatActivity.this, "Failed to confirm deal", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<com.harvesthub.mobile.model.GenericResponse> call, Throwable t) {
                    Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnPlus.setOnClickListener(v -> {
            Toast.makeText(this, "Plus button clicked", Toast.LENGTH_SHORT).show();
        });

        btnAttachment.setOnClickListener(v -> {
            Toast.makeText(this, "Attachment button clicked", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    private void loadMessages() {
        apiService.getChatMessages(orderId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(Call<List<ChatMessage>> call, Response<List<ChatMessage>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    messages.clear();
                    messages.addAll(response.body());
                    if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
                    if (rvMessages != null && !messages.isEmpty()) rvMessages.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onFailure(Call<List<ChatMessage>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavigation() {
        // Setup Top Navigation
        View topNav = findViewById(R.id.included_top_nav);
        View btnNotification = topNav.findViewById(R.id.btnNotification);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Setup Bottom Navigation
        View navBar = findViewById(R.id.included_nav);
        BottomNavHelper.setup(this, navBar, BottomNavHelper.Tab.LISTINGS);
    }
}
