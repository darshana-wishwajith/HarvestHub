package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.SendOtpRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilIdentifier;
    private TextInputEditText etIdentifier;
    private MaterialButton btnSendCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> validateAndSend());
    }

    private void initViews() {
        tilIdentifier = findViewById(R.id.tilIdentifier);
        etIdentifier = findViewById(R.id.etIdentifier);
        btnSendCode = findViewById(R.id.btnSendCode);
    }

    private void validateAndSend() {
        String identifier = etIdentifier.getText().toString().trim();
        tilIdentifier.setError(null);

        if (identifier.isEmpty()) {
            tilIdentifier.setError("Email or mobile number is required");
            return;
        }

        boolean isEmail = Patterns.EMAIL_ADDRESS.matcher(identifier).matches();
        boolean isPhone = identifier.matches("07[0-9]{8}");

        if (!isEmail && !isPhone) {
            tilIdentifier.setError("Enter a valid email or 10-digit mobile number");
            return;
        }

        performSendOtp(identifier);
    }

    private void performSendOtp(String identifier) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("Sending...");

        SendOtpRequest request = new SendOtpRequest(identifier);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.sendVerificationCode(request).enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("Send Verification Code");

                if (response.isSuccessful() && response.body() != null) {
                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                    intent.putExtra("identifier", identifier);
                    intent.putExtra("maskedIdentifier", maskIdentifier(identifier));
                    // If API returns userId or token, pass it here
                    // intent.putExtra("userId", response.body().getUserId()); 
                    startActivity(intent);
                } else {
                    tilIdentifier.setError("No account found with this email or mobile number");
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("Send Verification Code");
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String maskIdentifier(String identifier) {
        if (Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            int atIndex = identifier.indexOf("@");
            if (atIndex <= 3) return identifier;
            return identifier.substring(0, 3) + "***" + identifier.substring(atIndex);
        } else if (identifier.matches("07[0-9]{8}")) {
            return identifier.substring(0, 3) + "****" + identifier.substring(7);
        }
        return identifier;
    }
}
