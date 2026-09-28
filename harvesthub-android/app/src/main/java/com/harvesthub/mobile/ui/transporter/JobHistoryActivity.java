package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class JobHistoryActivity extends AppCompatActivity {

    private TextView chipAllTrips;
    private TextView chipCompleted;
    private TextView chipDisputed;
    private TextView chipThisMonth;

    private CardView cardJob1;
    private CardView cardJob2;
    private CardView cardJob3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_job_history);

        // Configure light status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#F8FAFC"));
        }

        initViews();
        setupFilterChips();
        setupCardClicks();
        setupBottomNav();

        ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setFocusableInTouchMode(true);
            scrollView.requestFocus();
            String filter = getIntent().getStringExtra("filter");
            if ("disputed".equalsIgnoreCase(filter)) {
                selectFilter(2);
            } else if ("completed".equalsIgnoreCase(filter)) {
                selectFilter(1);
            } else if (getIntent().getBooleanExtra("scroll_to_bottom", false)) {
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            } else {
                scrollView.post(() -> scrollView.scrollTo(0, 0));
            }
        }
    }

    private void initViews() {
        chipAllTrips = findViewById(R.id.chipAllTrips);
        chipCompleted = findViewById(R.id.chipCompleted);
        chipDisputed = findViewById(R.id.chipDisputed);
        chipThisMonth = findViewById(R.id.chipThisMonth);

        cardJob1 = findViewById(R.id.cardJob1);
        cardJob2 = findViewById(R.id.cardJob2);
        cardJob3 = findViewById(R.id.cardJob3);

        View btnHeaderReceipt = findViewById(R.id.btnHeaderReceipt);
        if (btnHeaderReceipt != null) {
            btnHeaderReceipt.setOnClickListener(v -> 
                Toast.makeText(JobHistoryActivity.this, "Opening Verified Dispatch Ledger", Toast.LENGTH_SHORT).show()
            );
        }

        View btnHeaderProfile = findViewById(R.id.btnHeaderProfile);
        if (btnHeaderProfile != null) {
            btnHeaderProfile.setOnClickListener(v -> {
                Intent intent = new Intent(JobHistoryActivity.this, TransporterSettingsActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupFilterChips() {
        chipAllTrips.setOnClickListener(v -> selectFilter(0));
        chipCompleted.setOnClickListener(v -> selectFilter(1));
        chipDisputed.setOnClickListener(v -> selectFilter(2));
        chipThisMonth.setOnClickListener(v -> selectFilter(3));
    }

    private void selectFilter(int index) {
        // Reset all chips
        resetChipStyle(chipAllTrips);
        resetChipStyle(chipCompleted);
        resetChipStyle(chipDisputed);
        resetChipStyle(chipThisMonth);

        switch (index) {
            case 0: // All Trips
                highlightChipStyle(chipAllTrips);
                cardJob1.setVisibility(View.VISIBLE);
                cardJob2.setVisibility(View.VISIBLE);
                cardJob3.setVisibility(View.VISIBLE);
                break;
            case 1: // Completed
                highlightChipStyle(chipCompleted);
                cardJob1.setVisibility(View.VISIBLE);
                cardJob2.setVisibility(View.GONE);
                cardJob3.setVisibility(View.VISIBLE);
                break;
            case 2: // Disputed
                highlightChipStyle(chipDisputed);
                cardJob1.setVisibility(View.GONE);
                cardJob2.setVisibility(View.VISIBLE);
                cardJob3.setVisibility(View.GONE);
                break;
            case 3: // This Month
                highlightChipStyle(chipThisMonth);
                cardJob1.setVisibility(View.VISIBLE);
                cardJob2.setVisibility(View.VISIBLE);
                cardJob3.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void resetChipStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.transporter_bg_chip_history_unselected);
        chip.setTextColor(Color.parseColor("#475569"));
    }

    private void highlightChipStyle(TextView chip) {
        chip.setBackgroundResource(R.drawable.transporter_bg_chip_history_selected);
        chip.setTextColor(Color.parseColor("#FFFFFF"));
    }

    private void setupCardClicks() {
        // Card 1
        View btnReceiptCard1 = findViewById(R.id.btnReceiptCard1);
        if (btnReceiptCard1 != null) {
            btnReceiptCard1.setOnClickListener(v ->
                Toast.makeText(JobHistoryActivity.this, "Order #842 Consignment Receipt & Ratings", Toast.LENGTH_SHORT).show()
            );
        }
        cardJob1.setOnClickListener(v ->
            Toast.makeText(JobHistoryActivity.this, "Dambulla to Colombo - Order #842 (Delivered)", Toast.LENGTH_SHORT).show()
        );

        // Card 2
        View btnIncidentReport = findViewById(R.id.btnIncidentReport);
        if (btnIncidentReport != null) {
            btnIncidentReport.setOnClickListener(v ->
                Toast.makeText(JobHistoryActivity.this, "Incident Report #810: Bay #14 Inspection Evidence", Toast.LENGTH_LONG).show()
            );
        }
        cardJob2.setOnClickListener(v ->
            Toast.makeText(JobHistoryActivity.this, "Nuwara Eliya to Colombo - Order #810 (Disputed)", Toast.LENGTH_SHORT).show()
        );

        // Card 3
        View btnReceiptCard3 = findViewById(R.id.btnReceiptCard3);
        if (btnReceiptCard3 != null) {
            btnReceiptCard3.setOnClickListener(v ->
                Toast.makeText(JobHistoryActivity.this, "Order #798 Consignment Receipt & Ratings", Toast.LENGTH_SHORT).show()
            );
        }
        cardJob3.setOnClickListener(v ->
            Toast.makeText(JobHistoryActivity.this, "Kandy Hub to Colombo - Order #798 (Delivered)", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupBottomNav() {
        View navHome = findViewById(R.id.navHome);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(JobHistoryActivity.this, DeliveryDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        }

        View navHistory = findViewById(R.id.navHistory);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                // Already on History tab
                Toast.makeText(JobHistoryActivity.this, "Trip History (All 24 Records)", Toast.LENGTH_SHORT).show();
            });
        }

        View navSettings = findViewById(R.id.navSettings);
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> {
                Intent intent = new Intent(JobHistoryActivity.this, TransporterSettingsActivity.class);
                startActivity(intent);
            });
        }
    }
}

