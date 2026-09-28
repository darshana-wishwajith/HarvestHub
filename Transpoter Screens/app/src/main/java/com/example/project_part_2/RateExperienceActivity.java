package com.example.project_part_2;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * RateExperienceActivity
 * Transporter verified rating and feedback submission for Farmer (Pickup) and Buyer (Drop-off).
 */
public class RateExperienceActivity extends AppCompatActivity {

    private ImageView btnClose;
    private TextView chipPunctualLoading;
    private TextView chipAccurateWeight;
    private TextView chipFriendly;
    private TextView chipCratesReady;

    private TextView chipQuickUnloading;
    private TextView chipFairInspection;
    private TextView chipPromptSignOff;
    private TextView chipEasyBayAccess;

    private EditText etPickupNotes;
    private EditText etDeliveryNotes;
    private LinearLayout btnSubmitFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_experience);

        // Configure light status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#F8F9FA"));
        }
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        initViews();
        setupListeners();

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setFocusableInTouchMode(true);
            scrollView.requestFocus();
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);

        // Pickup chips
        chipPunctualLoading = findViewById(R.id.chipPunctualLoading);
        chipAccurateWeight = findViewById(R.id.chipAccurateWeight);
        chipFriendly = findViewById(R.id.chipFriendly);
        chipCratesReady = findViewById(R.id.chipCratesReady);

        // Delivery chips
        chipQuickUnloading = findViewById(R.id.chipQuickUnloading);
        chipFairInspection = findViewById(R.id.chipFairInspection);
        chipPromptSignOff = findViewById(R.id.chipPromptSignOff);
        chipEasyBayAccess = findViewById(R.id.chipEasyBayAccess);

        etPickupNotes = findViewById(R.id.etPickupNotes);
        etDeliveryNotes = findViewById(R.id.etDeliveryNotes);
        btnSubmitFeedback = findViewById(R.id.btnSubmitFeedback);
    }

    private void setupListeners() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        // Setup interactive tag chip toggling
        setupChipToggle(chipPunctualLoading, true);
        setupChipToggle(chipAccurateWeight, true);
        setupChipToggle(chipFriendly, false);
        setupChipToggle(chipCratesReady, true);

        setupChipToggle(chipQuickUnloading, true);
        setupChipToggle(chipFairInspection, true);
        setupChipToggle(chipPromptSignOff, false);
        setupChipToggle(chipEasyBayAccess, false);

        if (btnSubmitFeedback != null) {
            btnSubmitFeedback.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Feedback Submitted")
                        .setMessage("Your verified ratings for Bandara Farms and Colombo Manning Market have been securely recorded.")
                        .setPositiveButton("Done", (dialog, which) -> finish())
                        .show();
            });
        }
    }

    private void setupChipToggle(TextView chip, boolean initiallySelected) {
        if (chip == null) return;
        chip.setTag(initiallySelected);

        chip.setOnClickListener(v -> {
            boolean isSelected = (boolean) chip.getTag();
            boolean newState = !isSelected;
            chip.setTag(newState);

            if (newState) {
                chip.setBackgroundResource(R.drawable.bg_tag_chip_selected);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_tag_chip_unselected);
                chip.setTextColor(Color.parseColor("#475569"));
            }
        });
    }
}
