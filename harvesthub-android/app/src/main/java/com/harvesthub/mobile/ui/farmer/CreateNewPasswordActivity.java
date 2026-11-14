package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.model.ResetPasswordRequest;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Forgot password, step 4 (Figma "7.Forgot password - Create New Password").
 * "Save &amp; Login" sets the new password with the verified code, then signs the farmer in with it.
 */
public class CreateNewPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etConfirmPassword;
    private TextView tvNewPasswordError, tvConfirmPasswordError, tvError;
    private View llErrorContainer;
    private MaterialButton btnSave;
    private String identifier, code;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_password);

        identifier = getIntent().getStringExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER);
        code = getIntent().getStringExtra(VerifyOtpActivity.EXTRA_CODE);
        if (identifier == null || code == null) { // only reachable through the forgot-password flow
            finish();
            return;
        }

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tvNewPasswordError = findViewById(R.id.tvNewPasswordError);
        tvConfirmPasswordError = findViewById(R.id.tvConfirmPasswordError);
        tvError = findViewById(R.id.tvError);
        llErrorContainer = findViewById(R.id.llErrorContainer);
        btnSave = findViewById(R.id.btnSave);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupToggle(findViewById(R.id.ivToggleNew), etNewPassword);
        setupToggle(findViewById(R.id.ivToggleConfirm), etConfirmPassword);
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    private void setupToggle(ImageView toggle, EditText field) {
        toggle.setOnClickListener(v -> {
            boolean hidden = field.getTransformationMethod() instanceof PasswordTransformationMethod;
            field.setTransformationMethod(hidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            toggle.setImageResource(hidden ? R.drawable.ic_tl_eye : R.drawable.ic_fp_eye_off);
            field.setSelection(field.getText().length());
        });
    }

    private void validateAndSave() {
        String password = etNewPassword.getText().toString();
        String confirm = etConfirmPassword.getText().toString();
        tvNewPasswordError.setVisibility(View.GONE);
        tvConfirmPasswordError.setVisibility(View.GONE);
        llErrorContainer.setVisibility(View.GONE);

        if (!AuthInputUtils.isStrongEnough(password)) {
            tvNewPasswordError.setText(R.string.fp_err_password_rule);
            tvNewPasswordError.setVisibility(View.VISIBLE);
            etNewPassword.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            tvConfirmPasswordError.setText(R.string.fp_err_password_mismatch);
            tvConfirmPasswordError.setVisibility(View.VISIBLE);
            etConfirmPassword.requestFocus();
            return;
        }

        setBusy(true);
        ApiClient.getApiService().resetPassword(new ResetPasswordRequest(identifier, code, password))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            signIn(password);
                        } else {
                            setBusy(false);
                            showError(AuthInputUtils.errorMessage(response, getString(R.string.fp_err_code_invalid)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        setBusy(false);
                        showError(getString(R.string.fp_network_error));
                    }
                });
    }

    /** The "Login" half of "Save & Login". If it fails, the password is still changed: go to sign-in. */
    private void signIn(String password) {
        ApiClient.getApiService().login(new LoginRequest(identifier, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                        LoginResponse body = response.body();
                        User user = body == null ? null : body.getUser();
                        if (response.isSuccessful() && user != null && "FARMER".equalsIgnoreCase(user.getRole())) {
                            SessionManager session = new SessionManager(CreateNewPasswordActivity.this);
                            session.saveAuthToken(body.getToken());
                            session.saveUser(user);
                            session.setLogin(true, "FARMER");
                            openAndClearTask(MainActivity.class);
                        } else {
                            goToSignIn();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                        goToSignIn();
                    }
                });
    }

    private void goToSignIn() {
        Toast.makeText(this, R.string.fp_password_updated, Toast.LENGTH_LONG).show();
        openAndClearTask(FarmerLoginActivity.class);
    }

    private void openAndClearTask(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        llErrorContainer.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? R.string.fp_saving : R.string.fp_save_login);
    }
}
