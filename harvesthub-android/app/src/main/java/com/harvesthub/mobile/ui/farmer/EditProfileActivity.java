package com.harvesthub.mobile.ui.farmer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ActivityEditProfileBinding;
import com.harvesthub.mobile.model.FarmerProfileResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ActivityEditProfileBinding binding;
    private ApiService apiService;
    private final String farmerId = "FARMER_123";
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    binding.ivProfileLarge.setImageURI(selectedImageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getClient().create(ApiService.class);
        initListeners();
        fetchProfile();
    }

    private void initListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnUploadPhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        binding.btnRemovePhoto.setOnClickListener(v -> {
            selectedImageUri = null;
            binding.ivProfileLarge.setImageResource(R.drawable.ic_person);
        });

        binding.btnUpdatePassword.setOnClickListener(v -> 
                startActivity(new Intent(this, ResetPasswordActivity.class)));

        binding.btnSaveChanges.setOnClickListener(v -> validateAndSave());
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
        binding.etFullName.setText(profile.getFullName());
        binding.etEmail.setText(profile.getEmail());
        binding.etPhone.setText(profile.getPhoneNumber());
        binding.etNic.setText(profile.getNic());
        if (profile.getPasswordLastUpdated() != null) {
            binding.tvLastUpdated.setText("Last updated " + profile.getPasswordLastUpdated());
        }
    }

    private void validateAndSave() {
        String name = binding.etFullName.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String nic = binding.etNic.getText().toString().trim();

        binding.tilFullName.setError(null);
        binding.tilPhone.setError(null);
        binding.tilNic.setError(null);

        boolean isValid = true;
        if (name.isEmpty()) {
            binding.tilFullName.setError("Name is required");
            isValid = false;
        }
        if (nic.isEmpty()) {
            binding.tilNic.setError("NIC is required");
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
            // Real app would handle file conversion
        }

        apiService.updateFarmerProfile(farmerId, rbName, rbPhone, rbNic, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
