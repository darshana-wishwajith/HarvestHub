package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;

public class RatingFeedbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_feedback);

        MaterialButton btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
        btnSubmitFeedback.setOnClickListener(v -> {
            Toast.makeText(this, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });

        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.llNavListings).setOnClickListener(v -> {
            // Already on listings/rating context
        });
        
        // Add other navigation listeners as needed
    }
}
