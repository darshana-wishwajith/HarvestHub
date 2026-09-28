package com.harvesthub.mobile.ui.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.ui.buyer.BuyerDashboardActivity;
import com.harvesthub.mobile.utils.SessionManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn() && sessionManager.getUser() != null) {
            if ("FARMER".equals(sessionManager.getUser().getRole())) {
                startActivity(new Intent(this, MainActivity.class));
            } else if ("BUYER".equals(sessionManager.getUser().getRole())) {
                startActivity(new Intent(this, BuyerDashboardActivity.class));
            } else {
                startActivity(new Intent(this, RoleSelectionActivity.class));
            }
        } else {
            startActivity(new Intent(this, RoleSelectionActivity.class));
        }
        finish();
    }
}
