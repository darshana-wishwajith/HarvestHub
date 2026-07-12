package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerProfileResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvInitial, tvProfileName, tvProfilePhone, tvProfileNic, tvCurrentLanguage;
    private ApiService apiService;
    private final String farmerId = "FARMER_123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_settings);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        fetchProfile();
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvInitial = findViewById(R.id.tvInitial);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfilePhone = findViewById(R.id.tvProfilePhone);
        tvProfileNic = findViewById(R.id.tvProfileNic);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);

        findViewById(R.id.btnEditPhoto).setOnClickListener(v -> 
            startActivity(new Intent(this, EditProfileActivity.class)));

        findViewById(R.id.rowChangePassword).setOnClickListener(v -> 
            Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowNotifications).setOnClickListener(v -> 
            Toast.makeText(this, "Notification Preferences clicked", Toast.LENGTH_SHORT).show());

        findViewById(R.id.rowLanguage).setOnClickListener(v -> showLanguageDialog());

        findViewById(R.id.rowHelp).setOnClickListener(v -> 
            Toast.makeText(this, "Help & Support clicked", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutConfirmation());

        findViewById(R.id.ivNotificationTop).setOnClickListener(v -> 
            startActivity(new Intent(this, NotificationsActivity.class)));

        findViewById(R.id.navHomeItem).setOnClickListener(v -> finish());
        findViewById(R.id.navListingsItem).setOnClickListener(v -> {
            startActivity(new Intent(this, ListingsActivity.class));
            finish();
        });
        findViewById(R.id.navWalletItem).setOnClickListener(v -> {
            startActivity(new Intent(this, WalletActivity.class));
            finish();
        });
    }

    private void fetchProfile() {
        apiService.getFarmerProfile(farmerId).enqueue(new Callback<FarmerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<FarmerProfileResponse> call, @NonNull Response<FarmerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    // Load mock if failed
                    updateUI(new FarmerProfileResponse("Kamal Perera", "kamal@example.com", "077 123 4567", "852145659V", null, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FarmerProfileResponse> call, @NonNull Throwable t) {
                updateUI(new FarmerProfileResponse("Kamal Perera", "kamal@example.com", "077 123 4567", "852145659V", null, null));
            }
        });
    }

    private void updateUI(FarmerProfileResponse profile) {
        tvProfileName.setText(profile.getFullName());
        tvProfilePhone.setText(profile.getPhoneNumber());
        tvProfileNic.setText("NIC: " + profile.getNic());

        if (profile.getProfilePhotoUrl() == null || profile.getProfilePhotoUrl().isEmpty()) {
            ivProfile.setVisibility(View.INVISIBLE);
            tvInitial.setVisibility(View.VISIBLE);
            if (profile.getFullName() != null && !profile.getFullName().isEmpty()) {
                tvInitial.setText(String.valueOf(profile.getFullName().charAt(0)).toUpperCase());
            }
        } else {
            ivProfile.setVisibility(View.VISIBLE);
            tvInitial.setVisibility(View.GONE);
            // Load image with Glide/Picasso here
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Sinhala", "Tamil"};
        new AlertDialog.Builder(this)
                .setTitle("Select App Language")
                .setItems(languages, (dialog, which) -> {
                    tvCurrentLanguage.setText(languages[which]);
                    Toast.makeText(this, "Language set to " + languages[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    // Clear session/prefs here
                    Intent intent = new Intent(this, FarmerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
