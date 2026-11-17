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
    private String farmerId = "";
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
        farmerId = new com.harvesthub.mobile.utils.SessionManager(this).getUserId();
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
                AccountDialogs.changePassword(this));

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
        // NIC is a verified identity attribute: the server never changes it, so don't let it look editable
        binding.etNic.setEnabled(false);
        binding.tilNic.setHelperText("NIC is verified at registration and can't be changed here.");
        if (selectedImageUri == null && profile.getProfilePhotoUrl() != null && !profile.getProfilePhotoUrl().isEmpty()) {
            com.harvesthub.mobile.utils.RemoteImage.load(binding.ivProfileLarge, profile.getProfilePhotoUrl());
        }
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
        if (!phone.matches("^\\d{10}$")) {
            binding.tilPhone.setError("Enter a 10-digit phone number, e.g. 0771234567");
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
            body = photoPart(selectedImageUri);
            if (body == null) {
                Toast.makeText(this, "Could not read that photo. Choose a JPG or PNG under 5 MB.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        binding.btnSaveChanges.setEnabled(false);
        apiService.updateFarmerProfile(farmerId, rbName, rbPhone, rbNic, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                binding.btnSaveChanges.setEnabled(true);
                if (response.isSuccessful()) {
                    // keep the signed-in copy in step, so greetings and headers show the new name at once
                    com.harvesthub.mobile.utils.SessionManager sm = new com.harvesthub.mobile.utils.SessionManager(EditProfileActivity.this);
                    com.harvesthub.mobile.model.User u = sm.getUser();
                    if (u != null) {
                        u.setFullName(name);
                        u.setMobileNumber(phone);
                        sm.saveUser(u);
                    }
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(response,
                            "Failed to update profile"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                binding.btnSaveChanges.setEnabled(true);
                Toast.makeText(EditProfileActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Reads the picked image into a "photo" multipart part (the server checks the type and size). */
    private MultipartBody.Part photoPart(Uri uri) {
        try (java.io.InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 5 * 1024 * 1024) return null;
            }
            String type = getContentResolver().getType(uri);
            if (type == null || !type.startsWith("image/")) type = "image/jpeg";
            RequestBody rb = RequestBody.create(MediaType.parse(type), out.toByteArray());
            return MultipartBody.Part.createFormData("photo", type.endsWith("png") ? "profile.png" : "profile.jpg", rb);
        } catch (Exception e) {
            return null;
        }
    }
}
