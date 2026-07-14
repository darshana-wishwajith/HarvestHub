package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerProfileResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private ImageView ivProfile;
    private TextView tvInitial, tvProfileName, tvProfilePhone, tvProfileNic, tvCurrentLanguage;
    private ApiService apiService;
    private SessionManager sessionManager;
    private String farmerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sessionManager = new SessionManager(requireContext());
        if (sessionManager.getUser() != null) {
            farmerId = sessionManager.getUser().getId();
        } else {
            farmerId = "FARMER_123";
        }

        apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        initViews(view);
        fetchProfile();

        return view;
    }

    private void initViews(View v) {
        ivProfile = v.findViewById(R.id.ivProfile);
        tvInitial = v.findViewById(R.id.tvInitial);
        tvProfileName = v.findViewById(R.id.tvProfileName);
        tvProfilePhone = v.findViewById(R.id.tvProfilePhone);
        tvProfileNic = v.findViewById(R.id.tvProfileNic);
        tvCurrentLanguage = v.findViewById(R.id.tvCurrentLanguage);

        v.findViewById(R.id.btnEditPhoto).setOnClickListener(view -> 
            startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        v.findViewById(R.id.rowChangePassword).setOnClickListener(view -> 
            startActivity(new Intent(requireContext(), ResetPasswordActivity.class)));

        v.findViewById(R.id.rowNotifications).setOnClickListener(view -> 
            Toast.makeText(requireContext(), "Notification Preferences clicked", Toast.LENGTH_SHORT).show());

        v.findViewById(R.id.rowLanguage).setOnClickListener(view -> showLanguageDialog());

        v.findViewById(R.id.rowHelp).setOnClickListener(view -> 
            Toast.makeText(requireContext(), "Help & Support clicked", Toast.LENGTH_SHORT).show());

        v.findViewById(R.id.btnLogout).setOnClickListener(view -> showLogoutConfirmation());
    }

    private void fetchProfile() {
        apiService.getFarmerProfile(farmerId).enqueue(new Callback<FarmerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<FarmerProfileResponse> call, @NonNull Response<FarmerProfileResponse> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else if (isAdded()) {
                    updateUI(new FarmerProfileResponse("Kamal Perera", "kamal@example.com", "077 123 4567", "852145659V", null, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<FarmerProfileResponse> call, @NonNull Throwable t) {
                if (isAdded()) {
                    updateUI(new FarmerProfileResponse("Kamal Perera", "kamal@example.com", "077 123 4567", "852145659V", null, null));
                }
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
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Sinhala", "Tamil"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select App Language")
                .setItems(languages, (dialog, which) -> {
                    tvCurrentLanguage.setText(languages[which]);
                    Toast.makeText(requireContext(), "Language set to " + languages[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    // TODO: Final logout logic in a later step
                    android.util.Log.d("SettingsFragment", "Logout requested");
                    sessionManager.logoutUser();
                    Intent intent = new Intent(requireContext(), com.harvesthub.mobile.ui.common.RoleSelectionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}