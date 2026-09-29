package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Redirects to the unified fragment-based BuyerDashboardActivity OFFERS tab.
 */
public class SupplyOffersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, BuyerDashboardActivity.class);
        intent.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.OFFERS.name());
        if (getIntent().hasExtra("ad_title")) {
            intent.putExtra(BuyerDashboardActivity.EXTRA_AD_TITLE, getIntent().getStringExtra("ad_title"));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
