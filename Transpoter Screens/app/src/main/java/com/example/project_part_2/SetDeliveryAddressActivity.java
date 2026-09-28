package com.example.project_part_2;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 1.3: Set Delivery Address / Confirm Drop-off Location
 * Step 2 of 2: Location Setup for Automated Transport Routing
 */
public class SetDeliveryAddressActivity extends AppCompatActivity {

    private ImageView btnBack;
    private FrameLayout btnRecenter;
    private LinearLayout layoutAddressBox;
    private TextView tvDeliveryAddress;
    private ImageView btnEditAddress;
    private LinearLayout btnSaveAddress;

    private String currentAddress = "No. 45, Main Street, Dambulla";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_delivery_address);

        // White status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRecenter = findViewById(R.id.btnRecenter);
        layoutAddressBox = findViewById(R.id.layoutAddressBox);
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        btnEditAddress = findViewById(R.id.btnEditAddress);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);
    }

    private void setupListeners() {
        // Back Button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // GPS Recenter Button
        btnRecenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SetDeliveryAddressActivity.this, "GPS Re-centered: Dambulla Hub (±3m)", Toast.LENGTH_SHORT).show();
            }
        });

        // Edit Address Dialog
        View.OnClickListener editAddressListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditAddressDialog();
            }
        };
        btnEditAddress.setOnClickListener(editAddressListener);
        layoutAddressBox.setOnClickListener(editAddressListener);

        // Save Delivery Address Button
        btnSaveAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSaveAddress();
            }
        });
    }

    private void showEditAddressDialog() {
        final EditText input = new EditText(this);
        input.setText(currentAddress);
        input.setSelection(currentAddress.length());

        new AlertDialog.Builder(this)
                .setTitle("Edit Delivery Address")
                .setMessage("Enter your preferred warehouse or drop-off location:")
                .setView(input)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String updated = input.getText().toString().trim();
                        if (!updated.isEmpty()) {
                            currentAddress = updated;
                            tvDeliveryAddress.setText(currentAddress);
                            Toast.makeText(SetDeliveryAddressActivity.this, "Delivery location updated!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleSaveAddress() {
        Toast.makeText(this, "✓ Delivery Address Saved: " + currentAddress, Toast.LENGTH_LONG).show();

        // Navigate forward to Verified Harvests Marketplace (Page 2.1)
        Intent intent = new Intent(SetDeliveryAddressActivity.this, VerifiedHarvestsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
