package com.harvesthub.mobile.ui.common;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.farmer.RegisterActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvAppName = findViewById(R.id.tvAppName);
        Button btnFarmer = findViewById(R.id.btnFarmer);

        String appName = "HarvestHub";
        SpannableString ss = new SpannableString(appName);
        ss.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#1B4332")), 7, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvAppName.setText(ss);

        btnFarmer.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });
    }
}
