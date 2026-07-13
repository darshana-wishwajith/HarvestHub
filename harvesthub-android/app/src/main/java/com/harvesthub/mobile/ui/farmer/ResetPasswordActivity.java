package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ResetPasswordRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.PasswordUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etResetToken, etNewPassword;
    private Button btnResetPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etResetToken = findViewById(R.id.et_reset_token);
        etNewPassword = findViewById(R.id.et_new_password);
        btnResetPassword = findViewById(R.id.btn_reset_password);
        progressBar = findViewById(R.id.progress_bar);

        btnResetPassword.setOnClickListener(v -> {
            String token = etResetToken.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();

            if (token.isEmpty()) {
                Toast.makeText(this, "Please enter the reset token", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
                return;
            }

            resetPassword(token, newPassword);
        });
    }

    private void resetPassword(String token, String newPassword) {
        setLoading(true);
        
        // Hash the new password before sending (as per requirement)
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        ResetPasswordRequest request = new ResetPasswordRequest(token, hashedPassword);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.resetPassword(request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ResetPasswordActivity.this, "Password reset successfully", Toast.LENGTH_LONG).show();
                    // Navigate back to Login
                    Intent intent = new Intent(ResetPasswordActivity.this, FarmerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Failed to reset password";
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String bodyString = errorBody.string();
                            if (bodyString.contains("error")) {
                                org.json.JSONObject jsonObject = new org.json.JSONObject(bodyString);
                                errorMessage = jsonObject.optString("error", errorMessage);
                            } else if (bodyString.contains("message")) {
                                org.json.JSONObject jsonObject = new org.json.JSONObject(bodyString);
                                errorMessage = jsonObject.optString("message", errorMessage);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ResetPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ResetPasswordActivity.this, "Request failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!isLoading);
        etResetToken.setEnabled(!isLoading);
        etNewPassword.setEnabled(!isLoading);
    }
}
