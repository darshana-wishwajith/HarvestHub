package com.harvesthub.mobile.ui.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.ui.buyer.BuyerDashboardActivity;
import com.harvesthub.mobile.ui.farmer.FarmerDashboardActivity;
import com.harvesthub.mobile.ui.transporter.DeliveryDashboardActivity;
import com.harvesthub.mobile.utils.SessionManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        
        android.util.Log.d("LauncherActivity", "isLoggedIn: " + sessionManager.isLoggedIn() + ", role: " + sessionManager.getUserRole());

        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            if (role != null) role = role.trim().toUpperCase();
            
            android.util.Log.d("LauncherActivity", "isLoggedIn: true, role: [" + role + "]");

            Intent intent;
            if ("FARMER".equals(role)) {
                intent = new Intent(this, FarmerDashboardActivity.class);
            } else if ("BUYER".equals(role)) {
                intent = new Intent(this, BuyerDashboardActivity.class);
            } else if ("TRANSPORTER".equals(role)) {
                intent = new Intent(this, DeliveryDashboardActivity.class);
            } else {
                android.util.Log.w("LauncherActivity", "Unknown role: " + role + ". Going to role selection.");
                intent = new Intent(this, MainActivity.class);
            }
            startActivity(intent);
        } else {
            // Not logged in, show the role selection screen
            startActivity(new Intent(this, MainActivity.class));
        }
        
        finish();
    }
}
