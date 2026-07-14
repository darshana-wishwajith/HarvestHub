package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class HarvestReadyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_harvest_ready);

        setupNavigation();

        ImageView btnCallDriver = findViewById(R.id.btnCallDriver);
        Button btnConfirmReady = findViewById(R.id.btnConfirmReady);

        btnCallDriver.setOnClickListener(v -> {
            String phoneNumber = "0771234567";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        });

        btnConfirmReady.setOnClickListener(v -> {
            Toast.makeText(this, "Harvest confirmed as ready!", Toast.LENGTH_SHORT).show();
            // Proceed to Vehicle Arrival tracking
            Intent intent = new Intent(this, VehicleArrivalActivity.class);
            startActivity(intent);
            finish();
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
