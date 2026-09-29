package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Redirects to the unified fragment-based BuyerDashboardActivity DEMANDS tab.
 */
public class MyDemandRequestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, BuyerDashboardActivity.class);
        intent.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, BuyerDashboardActivity.Tab.DEMANDS.name());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
