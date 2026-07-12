package com.harvesthub.mobile.ui.farmer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmerProfileResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfileLarge;
    private EditText etFullName, etEmail, etPhone, etNic;
    private TextInputLayout tilFullName, tilPhone, tilNic;
    private TextView tvLastUpdated;
    private ApiService apiService;
    private String farmerId = "FARMER_123";
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivProfileLarge.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        apiService = ApiClient.getClient().create(ApiService.class);
        initViews();
        fetchProfile();
    }

    private void initViews() {
        ivProfileLarge = findViewById(R.id.ivProfileLarge);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etNic = findViewById(R.id.etNic);
        tilFullName = findViewById(R.id.tilFullName);
        tilPhone = findViewById(R.id.tilPhone);
        tilNic = findViewById(R.id.tilNic);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnUploadPhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> {
            selectedImageUri = null;
            ivProfileLarge.setImageResource(R.drawable.ic_person);
        });

        findViewById(R.id.btnUpdatePassword).setOnClickListener(v -> 
                Toast.makeText(this, "Navigate to Change Password", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> validateAndSave());

        // Bottom Nav Logic
        findViewById(R.id.navHomeItem).setOnClickListener(v -> {
            startActivity(new Intent(this, FarmerDashboardActivity.class));
            finish();
        });
        findViewById(R.id.navListingsItem).setOnClickListener(v -> {
            startActivity(new Intent(this, ListingsActivity.class));
            finish();
        });
    }

    private void fetchProfile() {
        apiService.getFarmerProfile(farmerId).enqueue(new Callback<FarmerProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<FarmerProfileResponse> call, @NonNull Response<FarmerProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateUI(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<FarmerProfileResponse> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(FarmerProfileResponse profile) {
        etFullName.setText(profile.getFullName());
        etEmail.setText(profile.getEmail());
        etPhone.setText(profile.getPhoneNumber());
        etNic.setText(profile.getNic());
        if (profile.getPasswordLastUpdated() != null) {
            tvLastUpdated.setText("Last updated " + profile.getPasswordLastUpdated());
        }
        // Load image here if profile.getProfilePhotoUrl() is not null
    }

    private void validateAndSave() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String nic = etNic.getText().toString().trim();

        tilFullName.setError(null);
        tilPhone.setError(null);
        tilNic.setError(null);

        boolean isValid = true;
        if (name.isEmpty()) {
            tilFullName.setError("Name is required");
            isValid = false;
        }
        if (!phone.matches("\\+94\\s\\d{2}\\s\\d{3}\\s\\d{4}")) {
            // Simplified check for mockup format
        }
        if (nic.isEmpty()) {
            tilNic.setError("NIC is required");
            isValid = false;
        }

        if (isValid) {
            saveProfile(name, phone, nic);
        }
    }

    private void saveProfile(String name, String phone, String nic) {
        RequestBody rbName = RequestBody.create(MediaType.parse("text/plain"), name);
        RequestBody rbPhone = RequestBody.create(MediaType.parse("text/plain"), phone);
        RequestBody rbNic = RequestBody.create(MediaType.parse("text/plain"), nic);

        MultipartBody.Part body = null;
        if (selectedImageUri != null) {
            // In real app, convert Uri to File. For now, assume it's handled.
            // File file = new File(selectedImageUri.getPath());
            // RequestBody rbFile = RequestBody.create(MediaType.parse("image/*"), file);
            // body = MultipartBody.Part.createFormData("photo", file.getName(), rbFile);
        }

        apiService.updateFarmerProfile(farmerId, rbName, rbPhone, rbNic, body).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
