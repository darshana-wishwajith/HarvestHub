package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        setupNavigation();

        ImageView btnSendMessage = findViewById(R.id.btnSendMessage);
        EditText etMessage = findViewById(R.id.etMessage);
        Button btnConfirmDeal = findViewById(R.id.btnConfirmDeal);
        ImageView btnPlus = findViewById(R.id.btnPlus);
        ImageView btnAttachment = findViewById(R.id.btnAttachment);

        btnSendMessage.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                Toast.makeText(this, "Message sent: " + message, Toast.LENGTH_SHORT).show();
                etMessage.setText("");
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });

        btnConfirmDeal.setOnClickListener(v -> {
            Toast.makeText(this, "Deal confirmed!", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, WaitForBuyerApprovalActivity.class));
            finish();
        });

        btnPlus.setOnClickListener(v -> {
            Toast.makeText(this, "Plus button clicked", Toast.LENGTH_SHORT).show();
        });

        btnAttachment.setOnClickListener(v -> {
            Toast.makeText(this, "Attachment button clicked", Toast.LENGTH_SHORT).show();
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
