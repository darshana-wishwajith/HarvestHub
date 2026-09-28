package com.harvesthub.mobile.ui.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.buyer.BuyerDashboardActivity;
import com.harvesthub.mobile.ui.buyer.BuyerLoginActivity;
import com.harvesthub.mobile.ui.farmer.FarmerLoginActivity;
import com.harvesthub.mobile.ui.transporter.TransporterLoginActivity;
import com.harvesthub.mobile.utils.SessionManager;

public class RoleSelectionActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);
        
        sessionManager = new SessionManager(this);
        setupCardListeners();
    }

    private void setupCardListeners() {
        MaterialCardView cardFarmer = findViewById(R.id.cardFarmer);
        MaterialCardView cardBuyer = findViewById(R.id.cardBuyer);
        MaterialCardView cardTransporter = findViewById(R.id.cardTransporter);

        cardFarmer.setOnClickListener(v -> {
            updateSelection(cardFarmer, cardBuyer, cardTransporter);
            handleFarmerSelection();
        });
        cardBuyer.setOnClickListener(v -> {
            updateSelection(cardBuyer, cardFarmer, cardTransporter);
            navigateToBuyerLogin();
        });
        cardTransporter.setOnClickListener(v -> {
            updateSelection(cardTransporter, cardFarmer, cardBuyer);
            navigateToTransporterLogin();
        });
    }

    private void updateSelection(MaterialCardView selected, MaterialCardView... others) {
        selected.setChecked(true);
        selected.setActivated(true);
        selected.setSelected(true);
        for (MaterialCardView other : others) {
            other.setChecked(false);
            other.setActivated(false);
            other.setSelected(false);
        }
    }

    private void handleFarmerSelection() {
        // If already logged in as Farmer, go to dashboard
        if (sessionManager.isLoggedIn() && "FARMER".equals(sessionManager.getUserRole())) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            // Otherwise, go to login
            Intent intent = new Intent(this, FarmerLoginActivity.class);
            startActivity(intent);
        }
    }

    private void navigateToBuyerLogin() {
        if (sessionManager.isLoggedIn() && "BUYER".equals(sessionManager.getUserRole())) {
            Intent intent = new Intent(this, BuyerDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        Intent intent = new Intent(this, BuyerLoginActivity.class);
        startActivity(intent);
    }

    private void navigateToTransporterLogin() {
        Intent intent = new Intent(this, TransporterLoginActivity.class);
        startActivity(intent);
    }
}
