package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class StartHarvestingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_harvesting);

        setupNavigation();

        Button btnStartHarvesting = findViewById(R.id.btnStartHarvesting);

        btnStartHarvesting.setOnClickListener(v -> {
            Toast.makeText(this, "Harvesting started. Good luck!", Toast.LENGTH_SHORT).show();
            // Navigate to Harvest Ready screen or Dashboard
            Intent intent = new Intent(this, HarvestReadyActivity.class);
            startActivity(intent);
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
