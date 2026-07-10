package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;

public class CreateNewPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private Button btnSave;
    private String resetToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_password);

        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        View btnBack = findViewById(R.id.btnBack);

        resetToken = getIntent().getStringExtra("resetToken");

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> validateAndReset());
    }

    private void validateAndReset() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Password cannot be empty");
            isValid = false;
        } else if (newPassword.length() < 8) {
            tilNewPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (isValid) {
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, FarmerLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
