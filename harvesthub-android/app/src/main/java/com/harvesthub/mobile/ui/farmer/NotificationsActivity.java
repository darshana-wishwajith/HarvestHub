package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class NotificationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        TextView tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        View btnMarkRead = findViewById(R.id.btnMarkRead);

        tvMarkAllRead.setOnClickListener(v -> {
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });

        btnMarkRead.setOnClickListener(v -> {
            Toast.makeText(this, "Notification marked as read", Toast.LENGTH_SHORT).show();
        });
    }
}