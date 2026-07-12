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
    private com.google.android.material.card.MaterialCardView cvError;
    private android.widget.TextView tvErrorMessage;
    private View ivCloseError;

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
        cvError = findViewById(R.id.cvError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        ivCloseError = findViewById(R.id.ivCloseError);

        ivCloseError.setOnClickListener(v -> cvError.setVisibility(View.GONE));
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
        cvError.setVisibility(View.GONE);
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
                    startActivity(intent);
                } else {
                    String detailedErrorMessage = "No account found with this email or mobile number";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String bodyString = errorBody.string();
                            if (bodyString != null && !bodyString.trim().isEmpty()) {
                                detailedErrorMessage = bodyString;
                            } else {
                                detailedErrorMessage = "Error: " + response.code() + " " + response.message();
                            }
                        } else if (response.message() != null && !response.message().isEmpty()) {
                            detailedErrorMessage = response.message();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ForgotPasswordError", "Error parsing error body", e);
                        detailedErrorMessage = "Error: " + response.code();
                    }
                    tvErrorMessage.setText(detailedErrorMessage);
                    cvError.setVisibility(View.VISIBLE);
                    android.util.Log.e("ForgotPasswordError", "HTTP Code: " + response.code() + ", Error: " + detailedErrorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("Send Verification Code");
                String errorMessage = "Network error: " + t.getMessage();
                tvErrorMessage.setText(errorMessage);
                cvError.setVisibility(View.VISIBLE);
                android.util.Log.e("ForgotPasswordError", errorMessage, t);
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
