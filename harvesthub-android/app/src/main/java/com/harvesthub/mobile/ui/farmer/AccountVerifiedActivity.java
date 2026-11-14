package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

/**
 * Forgot password, step 3 (Figma "5.Forgot Password - Account Verified Page").
 * Shown after the code was accepted; passes the identifier and code on to {@link CreateNewPasswordActivity}.
 */
public class AccountVerifiedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_verified);

        String identifier = getIntent().getStringExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER);
        String code = getIntent().getStringExtra(VerifyOtpActivity.EXTRA_CODE);
        if (identifier == null || code == null) { // only reachable through the forgot-password flow
            finish();
            return;
        }

        findViewById(R.id.btnResetPassword).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateNewPasswordActivity.class);
            intent.putExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER, identifier);
            intent.putExtra(VerifyOtpActivity.EXTRA_CODE, code);
            startActivity(intent);
            finish();
        });

        // Back leaves the whole forgot-password flow and returns to the farmer sign-in screen.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(AccountVerifiedActivity.this, FarmerLoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }
}
