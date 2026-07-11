package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class AccountVerifiedActivity extends AppCompatActivity {

    private String resetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_verified);

        resetToken = getIntent().getStringExtra("resetToken");

        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        btnResetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(AccountVerifiedActivity.this, CreateNewPasswordActivity.class);
            intent.putExtra("resetToken", resetToken);
            startActivity(intent);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Stay on this screen
            }
        });
    }
}
