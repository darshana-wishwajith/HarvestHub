package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ForgotPasswordRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Forgot password, step 1 (Figma "3.Forgot Password - VC sending Screen").
 * Sends a 6-digit code to the account's email, then opens {@link VerifyOtpActivity}.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_IDENTIFIER = "identifier";

    private EditText etIdentifier;
    private TextView tvIdentifierError;
    private MaterialButton btnSendCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etIdentifier = findViewById(R.id.etIdentifier);
        tvIdentifierError = findViewById(R.id.tvIdentifierError);
        btnSendCode = findViewById(R.id.btnSendCode);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnSendCode.setOnClickListener(v -> submit());
        etIdentifier.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });
        etIdentifier.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tvIdentifierError.setVisibility(View.GONE);
        });
    }

    private void submit() {
        String identifier = AuthInputUtils.normaliseIdentifier(etIdentifier.getText().toString());
        if (identifier == null) {
            tvIdentifierError.setVisibility(View.VISIBLE);
            etIdentifier.requestFocus();
            return;
        }
        tvIdentifierError.setVisibility(View.GONE);
        setLoading(true);

        ApiClient.getApiService().forgotPassword(new ForgotPasswordRequest(identifier))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        setLoading(false);
                        if (!response.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    AuthInputUtils.errorMessage(response, getString(R.string.fp_err_identifier)),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        // The server answers the same way whether or not the account exists (privacy).
                        Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                        intent.putExtra(EXTRA_IDENTIFIER, identifier);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        setLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this, R.string.fp_network_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        btnSendCode.setEnabled(!loading);
        btnSendCode.setText(loading ? R.string.fp_sending : R.string.fp_send_code);
        etIdentifier.setEnabled(!loading);
    }
}
