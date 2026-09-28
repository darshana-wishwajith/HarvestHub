package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ActivityFarmerLoginBinding;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.PasswordUtils;
import com.harvesthub.mobile.utils.SessionManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerLoginActivity extends AppCompatActivity {

    private ActivityFarmerLoginBinding binding;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFarmerLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        binding.btnSignIn.setOnClickListener(v -> performLogin());

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        binding.ivCloseError.setOnClickListener(v -> binding.cvError.setVisibility(View.GONE));
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility);
        } else {
            binding.etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            binding.ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
        }
        isPasswordVisible = !isPasswordVisible;
        binding.etPassword.setSelection(binding.etPassword.getText().length());
    }

    private void performLogin() {
        String emailOrMobile = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailOrMobile)) {
            binding.etEmail.setError("Please enter your email or mobile number");
            binding.etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Please enter your password");
            binding.etPassword.requestFocus();
            return;
        }

        String hashedPassword = password; // Backend handles BCrypt hashing

        ApiService apiService = ApiClient.getApiService();
        LoginRequest loginRequest = new LoginRequest(emailOrMobile, hashedPassword);

        binding.cvError.setVisibility(View.GONE);
        binding.btnSignIn.setEnabled(false);
        binding.btnSignIn.setText("Signing In...");

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                binding.btnSignIn.setEnabled(true);
                binding.btnSignIn.setText(R.string.sign_in_as_farmer);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    SessionManager sessionManager = new SessionManager(FarmerLoginActivity.this);
                    
                    User user = loginResponse.getUser();
                    if (user != null && (user.getRole() == null || user.getRole().isEmpty())) {
                        user.setRole("FARMER");
                    }
                    
                    sessionManager.saveAuthToken(loginResponse.getToken());
                    sessionManager.saveUser(user);
                    sessionManager.setLogin(true, "FARMER");

                    Toast.makeText(FarmerLoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(FarmerLoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String detailedErrorMessage = "Login Failed";
                    try (ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String bodyString = errorBody.string();
                            if (bodyString != null && !bodyString.trim().isEmpty()) {
                                detailedErrorMessage = formatError(bodyString);
                            } else {
                                detailedErrorMessage = "Login Failed: " + response.code() + " " + response.message();
                            }
                        } else {
                            detailedErrorMessage = "Login Failed: " + response.code() + " " + response.message();
                        }
                    } catch (Exception e) {
                        detailedErrorMessage = "Login Failed: " + response.code();
                    }
                    binding.tvErrorMessage.setText(detailedErrorMessage);
                    binding.cvError.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                binding.btnSignIn.setEnabled(true);
                binding.btnSignIn.setText(R.string.sign_in_as_farmer);
                String errorMessage = "Network error: " + t.getMessage();
                binding.tvErrorMessage.setText(errorMessage);
                binding.cvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private String formatError(String errorBody) {
        try {
            if (errorBody.trim().startsWith("{")) {
                org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                StringBuilder sb = new StringBuilder();
                java.util.Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String message = jsonObject.optString(key);
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("• ").append(message);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorBody;
    }
}
