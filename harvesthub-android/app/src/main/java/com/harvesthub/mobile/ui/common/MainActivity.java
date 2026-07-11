package com.harvesthub.mobile.ui.common;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.buyer.BuyerLoginActivity;
import com.harvesthub.mobile.ui.farmer.FarmerLoginActivity;
import com.harvesthub.mobile.ui.farmer.RegisterActivity;
import com.harvesthub.mobile.ui.transporter.TransporterLoginActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupCardListeners();

    }

    private void setupCardListeners() {

        MaterialCardView cardFarmer = findViewById(R.id.cardFarmer);
        MaterialCardView cardBuyer = findViewById(R.id.cardBuyer);
        MaterialCardView cardTransporter = findViewById(R.id.cardTransporter);

        cardFarmer.setOnClickListener(v -> navigateToFarmerLogin());
        cardBuyer.setOnClickListener(v -> navigateToBuyerLogin());
        cardTransporter.setOnClickListener(v -> navigateToTransporterLogin());
    }

    private void navigateToFarmerLogin() {
        // According to instructions, pass role to next activity
        Intent intent = new Intent(this, FarmerLoginActivity.class);
        startActivity(intent);
    }

    private void navigateToBuyerLogin() {
        // According to instructions, pass role to next activity
        Intent intent = new Intent(this, BuyerLoginActivity.class);
        startActivity(intent);
    }

    private void navigateToTransporterLogin() {
        // According to instructions, pass role to next activity
        Intent intent = new Intent(this, TransporterLoginActivity.class);
        startActivity(intent);
    }
}
