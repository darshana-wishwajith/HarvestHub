package com.example.project_part_2;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PostDemandRequestActivity extends AppCompatActivity {

    private AutoCompleteTextView actvTargetCrop;
    private EditText etRequiredQuantity;
    private EditText etDeadline;
    private EditText etTargetPrice;
    private EditText etAdditionalDetails;

    private LinearLayout btnGradeA;
    private LinearLayout btnGradeB;
    private LinearLayout btnGradeC;

    private ImageView ivCheckGradeA;
    private ImageView ivCheckGradeB;
    private ImageView ivCheckGradeC;

    private TextView tvGradeA;
    private TextView tvGradeB;
    private TextView tvGradeC;

    private String selectedGrade = "Grade A";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_demand_request);

        initViews();
        setupCropDropdown();
        setupDatePicker();
        setupGradeSelectors();
        setupPublishAction();
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        actvTargetCrop = findViewById(R.id.actvTargetCrop);
        etRequiredQuantity = findViewById(R.id.etRequiredQuantity);
        etDeadline = findViewById(R.id.etDeadline);
        etTargetPrice = findViewById(R.id.etTargetPrice);
        etAdditionalDetails = findViewById(R.id.etAdditionalDetails);

        btnGradeA = findViewById(R.id.btnGradeA);
        btnGradeB = findViewById(R.id.btnGradeB);
        btnGradeC = findViewById(R.id.btnGradeC);

        ivCheckGradeA = findViewById(R.id.ivCheckGradeA);
        ivCheckGradeB = findViewById(R.id.ivCheckGradeB);
        ivCheckGradeC = findViewById(R.id.ivCheckGradeC);

        tvGradeA = findViewById(R.id.tvGradeA);
        tvGradeB = findViewById(R.id.tvGradeB);
        tvGradeC = findViewById(R.id.tvGradeC);
    }

    private void setupCropDropdown() {
        String[] crops = {
                "Big Onions",
                "Red Potatoes",
                "Green Chillies",
                "Carrots",
                "Tomatoes",
                "Cabbage",
                "Pumpkin",
                "Beans",
                "Leeks",
                "Beetroot"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, crops);
        actvTargetCrop.setAdapter(adapter);

        LinearLayout layoutTargetCrop = findViewById(R.id.layoutTargetCrop);
        ImageView ivDropdownCrop = findViewById(R.id.ivDropdownCrop);

        View.OnClickListener openDropdown = v -> {
            actvTargetCrop.requestFocus();
            actvTargetCrop.showDropDown();
        };

        if (layoutTargetCrop != null) layoutTargetCrop.setOnClickListener(openDropdown);
        if (ivDropdownCrop != null) ivDropdownCrop.setOnClickListener(openDropdown);
    }

    private void setupDatePicker() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        DatePickerDialog.OnDateSetListener datePickerListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etDeadline.setText(dateFormat.format(calendar.getTime()));
        };

        View.OnClickListener showDatePicker = v -> new DatePickerDialog(
                this,
                datePickerListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();

        LinearLayout layoutDeadline = findViewById(R.id.layoutDeadline);
        ImageView ivCalendar = findViewById(R.id.ivCalendar);

        if (layoutDeadline != null) layoutDeadline.setOnClickListener(showDatePicker);
        if (etDeadline != null) etDeadline.setOnClickListener(showDatePicker);
        if (ivCalendar != null) ivCalendar.setOnClickListener(showDatePicker);
    }

    private void setupGradeSelectors() {
        btnGradeA.setOnClickListener(v -> selectGrade("Grade A"));
        btnGradeB.setOnClickListener(v -> selectGrade("Grade B"));
        btnGradeC.setOnClickListener(v -> selectGrade("Grade C"));
    }

    private void selectGrade(String grade) {
        selectedGrade = grade;

        btnGradeA.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));
        btnGradeB.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));
        btnGradeC.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_unselected));

        ivCheckGradeA.setVisibility(View.GONE);
        ivCheckGradeB.setVisibility(View.GONE);
        ivCheckGradeC.setVisibility(View.GONE);

        tvGradeA.setTypeface(Typeface.DEFAULT);
        tvGradeB.setTypeface(Typeface.DEFAULT);
        tvGradeC.setTypeface(Typeface.DEFAULT);

        switch (grade) {
            case "Grade A":
                btnGradeA.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeA.setVisibility(View.VISIBLE);
                tvGradeA.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case "Grade B":
                btnGradeB.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeB.setVisibility(View.VISIBLE);
                tvGradeB.setTypeface(Typeface.DEFAULT_BOLD);
                break;
            case "Grade C":
                btnGradeC.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_grade_a_selected));
                ivCheckGradeC.setVisibility(View.VISIBLE);
                tvGradeC.setTypeface(Typeface.DEFAULT_BOLD);
                break;
        }
    }

    private void setupPublishAction() {
        MaterialButton btnPublishDemand = findViewById(R.id.btnPublishDemand);
        btnPublishDemand.setOnClickListener(v -> {
            String crop = actvTargetCrop.getText().toString().trim();
            String qty = etRequiredQuantity.getText().toString().trim();

            if (crop.isEmpty()) {
                Toast.makeText(this, "Please enter or select a target crop", Toast.LENGTH_SHORT).show();
                actvTargetCrop.requestFocus();
                return;
            }

            if (qty.isEmpty()) {
                Toast.makeText(this, "Please enter required quantity", Toast.LENGTH_SHORT).show();
                etRequiredQuantity.requestFocus();
                return;
            }

            String priceText = etTargetPrice.getText().toString().trim();
            String priceMsg = !priceText.isEmpty() ? " at Rs. " + priceText + "/KG" : "";
            Toast.makeText(
                    this,
                    "Demand for " + qty + " KG of " + crop + " (" + selectedGrade + ")" + priceMsg + " published to Global Feed!",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent = new Intent(this, MyDemandRequestsActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
