package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

/**
 * Transporter Portal - Settings & Carrier Profile Activity
 */
public class TransporterSettingsActivity extends AppCompatActivity {

    private FrameLayout btnHeaderProfile;
    private TextView tvUserName;
    private TextView tvUserCarrierId;
    private TextView tvDutySubtitle;
    private SwitchCompat switchDutyStatus;
    private SwitchCompat switchCabAlerts;

    private LinearLayout rowVehicleDocuments;
    private LinearLayout rowPayoutBank;
    private LinearLayout rowNavigationRouting;
    private LinearLayout rowSupportDisputes;

    private TextView tvLanguageSubtitle;
    private TextView tabLangEn;
    private TextView tabLangSi;
    private TextView tabLangTa;

    private LinearLayout btnSignOut;
    private LinearLayout navHome;
    private LinearLayout navHistory;
    private LinearLayout navSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_transporter_settings);

        // Light status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
        }

        initViews();
        setupListeners();

        final android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(0, 0);
                }
            });
        }
    }

    private void initViews() {
        btnHeaderProfile = findViewById(R.id.btnHeaderProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserCarrierId = findViewById(R.id.tvUserCarrierId);
        tvDutySubtitle = findViewById(R.id.tvDutySubtitle);
        switchDutyStatus = findViewById(R.id.switchDutyStatus);
        switchCabAlerts = findViewById(R.id.switchCabAlerts);

        rowVehicleDocuments = findViewById(R.id.rowVehicleDocuments);
        rowPayoutBank = findViewById(R.id.rowPayoutBank);
        rowNavigationRouting = findViewById(R.id.rowNavigationRouting);
        rowSupportDisputes = findViewById(R.id.rowSupportDisputes);

        tvLanguageSubtitle = findViewById(R.id.tvLanguageSubtitle);
        tabLangEn = findViewById(R.id.tabLangEn);
        tabLangSi = findViewById(R.id.tabLangSi);
        tabLangTa = findViewById(R.id.tabLangTa);

        btnSignOut = findViewById(R.id.btnSignOut);
        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navSettings = findViewById(R.id.navSettings);
    }

    private void setupListeners() {
        // Top profile button
        btnHeaderProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Carrier Profile: LK-4091", Toast.LENGTH_SHORT).show();
            }
        });

        // Duty Status Toggle
        switchDutyStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    tvDutySubtitle.setText("Ready for Reefer Dispatches");
                    tvDutySubtitle.setTextColor(Color.parseColor("#15803D"));
                    Toast.makeText(TransporterSettingsActivity.this, "Transporter status: Active & Available", Toast.LENGTH_SHORT).show();
                } else {
                    tvDutySubtitle.setText("Off-Duty / Unavailable");
                    tvDutySubtitle.setTextColor(Color.parseColor("#64748B"));
                    Toast.makeText(TransporterSettingsActivity.this, "Transporter status: Off-Duty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cab Alerts Toggle
        switchCabAlerts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(TransporterSettingsActivity.this, "High-volume audible dispatch chime enabled", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(TransporterSettingsActivity.this, "Cab alerts silenced", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Vehicle Documents row
        rowVehicleDocuments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Opening Vehicle Documents & Licenses (Exp Oct 2025)", Toast.LENGTH_SHORT).show();
            }
        });

        // Payout Bank row
        rowPayoutBank.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Bank Payout: Sampath Bank â€¢â€¢â€¢â€¢ 4012", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigation & Routing row
        rowNavigationRouting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Navigation: Google Maps offline cache active", Toast.LENGTH_SHORT).show();
            }
        });

        // Support & Disputes row
        rowSupportDisputes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Calling 24/7 Transporter Dispatch Hotline...", Toast.LENGTH_SHORT).show();
            }
        });

        // Language Tabs
        tabLangEn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLanguage("EN");
            }
        });

        tabLangSi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLanguage("SI");
            }
        });

        tabLangTa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectLanguage("TA");
            }
        });

        // Sign Out Button
        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Signing out from HarvestHub Transporter Portal...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(TransporterSettingsActivity.this, TransporterLoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });

        // Navigation bar tabs
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Navigating to Transporter Home", Toast.LENGTH_SHORT).show();
            }
        });

        navHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TransporterSettingsActivity.this, "Navigating to Transporter History", Toast.LENGTH_SHORT).show();
            }
        });

        navSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Already on Settings
                Toast.makeText(TransporterSettingsActivity.this, "Settings (Current)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectLanguage(String lang) {
        // Reset all tabs
        tabLangEn.setBackground(null);
        tabLangEn.setTextColor(Color.parseColor("#64748B"));
        tabLangEn.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabLangSi.setBackground(null);
        tabLangSi.setTextColor(Color.parseColor("#64748B"));
        tabLangSi.setTypeface(null, android.graphics.Typeface.NORMAL);

        tabLangTa.setBackground(null);
        tabLangTa.setTextColor(Color.parseColor("#64748B"));
        tabLangTa.setTypeface(null, android.graphics.Typeface.NORMAL);

        switch (lang) {
            case "EN":
                tabLangEn.setBackgroundResource(R.drawable.transporter_bg_language_selected);
                tabLangEn.setTextColor(Color.parseColor("#111827"));
                tabLangEn.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLanguageSubtitle.setText("English (Sri Lanka)");
                break;
            case "SI":
                tabLangSi.setBackgroundResource(R.drawable.transporter_bg_language_selected);
                tabLangSi.setTextColor(Color.parseColor("#111827"));
                tabLangSi.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLanguageSubtitle.setText("à·ƒà·’à¶‚à·„à¶½ (à·à·Šâ€à¶»à·“ à¶½à¶‚à¶šà·)");
                break;
            case "TA":
                tabLangTa.setBackgroundResource(R.drawable.transporter_bg_language_selected);
                tabLangTa.setTextColor(Color.parseColor("#111827"));
                tabLangTa.setTypeface(null, android.graphics.Typeface.BOLD);
                tvLanguageSubtitle.setText("à®¤à®®à®¿à®´à¯ (à®‡à®²à®™à¯à®•à¯ˆ)");
                break;
        }
    }
}

