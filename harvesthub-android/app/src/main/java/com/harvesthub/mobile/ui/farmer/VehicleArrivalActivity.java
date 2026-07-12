package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class VehicleArrivalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_arrival);

        Button btnCallDriver = findViewById(R.id.btnCallDriver);

        btnCallDriver.setOnClickListener(v -> {
            // Sample phone number for demonstration
            String phoneNumber = "0112345678";
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        });

        // Live ETA polling could be initiated here
        // Note: For actual live updates, consider using Firebase Realtime Database or a similar socket-based solution.
    }
}