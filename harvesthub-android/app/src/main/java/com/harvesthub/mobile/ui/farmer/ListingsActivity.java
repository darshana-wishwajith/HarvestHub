package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

public class ListingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard); // Reusing the dashboard activity layout which has top and bottom nav

        // Set Title for the activity (optional, if your top bar supports it)
        
        // Load the ListingsFragment into the container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.farmerDashboardContainer, new ListingsFragment())
                    .commit();
        }

        setupNavigation();
    }

    private void setupNavigation() {
        // Setup Top Navigation
        View topNav = findViewById(R.id.included_top_nav);
        View btnNotification = topNav.findViewById(R.id.btnNotification);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Setup Bottom Navigation using the helper
        View navBar = findViewById(R.id.included_nav);
        BottomNavHelper.setup(this, navBar, BottomNavHelper.Tab.LISTINGS);
    }
}
