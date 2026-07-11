package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class ActiveOrderTrackerActivity extends AppCompatActivity {

    private LinearLayout navHomeItem;
    private LinearLayout navMarketItem;
    private LinearLayout navListingsItem;
    private LinearLayout navWalletItem;
    private LinearLayout navSettingsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_order_tracker);

        initViews();
        setupBottomNavigation();
    }

    private void initViews() {
        navHomeItem = findViewById(R.id.navHomeItem);
        navMarketItem = findViewById(R.id.navMarketItem);
        navListingsItem = findViewById(R.id.navListingsItem);
        navWalletItem = findViewById(R.id.navWalletItem);
        navSettingsItem = findViewById(R.id.navSettingsItem);
    }

    private void setupBottomNavigation() {
        navHomeItem.setOnClickListener(v -> showNavigationToast("Home"));
        navMarketItem.setOnClickListener(v -> showNavigationToast("Market"));
        navListingsItem.setOnClickListener(v -> showNavigationToast("Listings"));
        navWalletItem.setOnClickListener(v -> showNavigationToast("Wallet"));
        navSettingsItem.setOnClickListener(v -> showNavigationToast("Settings"));
    }

    private void showNavigationToast(String destination) {
        Toast.makeText(this, destination, Toast.LENGTH_SHORT).show();
    }
}
