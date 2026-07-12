package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class StartHarvestingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_harvesting);

        Button btnStartHarvesting = findViewById(R.id.btnStartHarvesting);

        btnStartHarvesting.setOnClickListener(v -> {
            Toast.makeText(this, "Harvesting started. Good luck!", Toast.LENGTH_SHORT).show();
            // Navigate to Harvest Ready screen or Dashboard
            Intent intent = new Intent(this, HarvestReadyActivity.class);
            startActivity(intent);
        });
    }
}