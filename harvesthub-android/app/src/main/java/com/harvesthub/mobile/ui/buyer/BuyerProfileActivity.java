package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.BuyerDashboard;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.farmer.AccountDialogs;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer "Profile & Settings" (567:1553): identity, trust score and rating, delivery location (required,
 * editable — decision 15 Nov), wallet, notification preferences, change password, sign out.
 */
public class BuyerProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);
        session = new SessionManager(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.rowDeliveryLocation).setOnClickListener(v -> startActivity(new Intent(this, SetDeliveryAddressActivity.class)));
        findViewById(R.id.rowWallet).setOnClickListener(v -> startActivity(new Intent(this, BuyerWalletActivity.class)));
        findViewById(R.id.rowNotifications).setOnClickListener(v -> AccountDialogs.notificationPreferences(this));
        findViewById(R.id.rowPassword).setOnClickListener(v -> AccountDialogs.changePassword(this));
        findViewById(R.id.rowHelp).setOnClickListener(v -> AccountDialogs.help(this));
        findViewById(R.id.btnSignOut).setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Sign out?")
                .setPositiveButton("Sign out", (d, w) -> {
                    session.logoutUser();
                    Intent i = new Intent(this, com.harvesthub.mobile.ui.common.RoleSelectionActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Cancel", null).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        User u = session.getUser();
        if (u != null) {
            text(R.id.tvProfileName, u.getFullName());
            text(R.id.tvProfileInitials, initials(u.getFullName()));
            text(R.id.tvProfileContact, (u.getEmail() == null ? "" : u.getEmail())
                    + (u.getMobileNumber() == null ? "" : "\n" + u.getMobileNumber()));
        }
        String me = session.getUserId();
        if (me == null) return;
        ApiClient.getApiService().getBuyerDashboard(me).enqueue(new Callback<BuyerDashboard>() {
            @Override
            public void onResponse(@NonNull Call<BuyerDashboard> call, @NonNull Response<BuyerDashboard> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                BuyerDashboard d = r.body();
                text(R.id.tvProfileBadge, "✓ Verified Buyer • Trust " + d.trustPercent + "%"
                        + (d.ratingAverage == null ? "" : String.format(Locale.US, " • %.1f★ (%d)", d.ratingAverage, d.ratingCount)));
                text(R.id.rowDeliveryLocation, "📍 Delivery location\n" + (d.hasDeliveryLocation
                        ? (d.deliveryAddress == null ? "Set" : d.deliveryAddress) : "Not set – required for orders"));
                text(R.id.rowWallet, "💳 Wallet & escrow\nBalance " + BaseBuyerOrderActivity.rs(d.walletBalance)
                        + " • " + BaseBuyerOrderActivity.rs(d.escrowHeld) + " in escrow");
            }

            @Override
            public void onFailure(@NonNull Call<BuyerDashboard> call, @NonNull Throwable t) { }
        });
    }

    private static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "B";
        String[] p = name.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase(Locale.US);
    }

    private void text(int id, String s) {
        TextView t = findViewById(id);
        if (t != null) t.setText(s);
    }
}
