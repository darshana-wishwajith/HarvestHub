package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.buyer.SetDeliveryAddressActivity;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Transporter "Vehicle Profile" (design 16 Nov): sign-up step 2 of 3 and "Edit vehicle" in Settings.
 * Vehicle classes come from GET /api/vehicle-types (decision 15 Nov: backend classes in the design's card grid);
 * the configuration (body type) is required; the driving-licence photo is required at sign-up and uploaded right
 * after the account is created. Every field is checked at once and errors show under the field (like farmer
 * registration).
 */
public class TransporterVehicleProfileActivity extends AppCompatActivity {

    private static final String[] BODY_CODES = {"REEFER", "FLATBED", "BOX"};
    private static final int[] BODY_CARD_IDS = {R.id.cfgReefer, R.id.cfgFlatbed, R.id.cfgBox};

    private RegisterRequest register;   // null = editing the signed-in driver's vehicle
    private final List<Map<String, Object>> types = new ArrayList<>();
    private final List<View> typeCards = new ArrayList<>();
    private String selectedType;
    private String selectedBody;
    private Uri licence;
    private boolean hasSavedLicence;

    private EditText etPayload, etPlate;
    private TextView tvProvince, tvLicenceStatus, tvTypeStatus;
    private TextView errType, errConfig, errPayload, errPlate, errLicence;
    private ActivityResultLauncher<String> pickLicence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_vehicle_profile);
        register = getIntent().getParcelableExtra(SetDeliveryAddressActivity.EXTRA_REGISTER);

        etPayload = findViewById(R.id.etPayload);
        etPlate = findViewById(R.id.etPlate);
        tvProvince = findViewById(R.id.tvProvince);
        tvLicenceStatus = findViewById(R.id.tvLicenceStatus);
        tvTypeStatus = findViewById(R.id.tvTypeStatus);
        errType = findViewById(R.id.errType);
        errConfig = findViewById(R.id.errConfig);
        errPayload = findViewById(R.id.errPayload);
        errPlate = findViewById(R.id.errPlate);
        errLicence = findViewById(R.id.errLicence);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        if (register == null) {   // editing from Settings
            findViewById(R.id.cardProgress).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.tvSaveLabel)).setText("Save Vehicle");
            ((TextView) findViewById(R.id.tvDocsSubtitle)).setText("Upload a new photo only if your licence changed");
        }

        setUpConfigCard(0, R.drawable.ic_snowflake, "Refrigerated Truck (Reefer)", "Temp controlled (+2°C to +8°C)", "Dairy & Berries Safe");
        setUpConfigCard(1, R.drawable.ic_lorry, "Flatbed Farm Truck", "Heavy produce & field crates", "Open Cargo Deck");
        setUpConfigCard(2, R.drawable.ic_archive_box, "Sprinter Box Truck", "Dry organic bulk & grains", "Weather Protected");

        pickLicence = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            licence = copyToCache(uri);   // keep it readable after leaving this screen
            if (licence == null) return;
            showLicenceState();
            hide(errLicence);
        });
        findViewById(R.id.btnUploadLicence).setOnClickListener(v -> pickLicence.launch("image/*"));

        etPlate.addTextChangedListener(new SimpleWatcher(() -> {
            hide(errPlate);
            showProvince();
        }));
        etPayload.addTextChangedListener(new SimpleWatcher(() -> hide(errPayload)));

        User me = new SessionManager(this).getUser();
        if (register == null && me != null) {
            if (me.getVehiclePlate() != null) etPlate.setText(me.getVehiclePlate());
            if (me.getVehicleCapacityKg() != null) etPayload.setText(String.valueOf(me.getVehicleCapacityKg().intValue()));
            selectedType = me.getVehicleType();
            selectBody(me.getVehicleBodyType());
            hasSavedLicence = me.isLicenceUploaded();
        }
        showLicenceState();
        loadTypes();
    }

    // ---------------------------------------------------------------- vehicle type grid

    private void loadTypes() {
        ApiClient.getApiService().getVehicleTypes().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().isEmpty()) {
                    tvTypeStatus.setText("Could not load vehicle types. Go back and try again.");
                    return;
                }
                tvTypeStatus.setVisibility(View.GONE);
                types.clear();
                types.addAll(r.body());
                buildGrid();
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                tvTypeStatus.setText("Can't reach the server. Check your internet and try again.");
            }
        });
    }

    private void buildGrid() {
        LinearLayout grid = findViewById(R.id.typeGrid);
        grid.removeAllViews();
        typeCards.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        int gap = dp(10);
        LinearLayout row = null;
        for (int i = 0; i < types.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) rp.topMargin = gap;
                grid.addView(row, rp);
            }
            Map<String, Object> t = types.get(i);
            String type = String.valueOf(t.get("type"));
            double cap = t.get("capacityKg") instanceof Number ? ((Number) t.get("capacityKg")).doubleValue() : 0;
            View card = inflater.inflate(R.layout.item_vehicle_type_card, row, false);
            ((TextView) card.findViewById(R.id.vtName)).setText(String.valueOf(t.get("label")));
            ((TextView) card.findViewById(R.id.vtCapacity)).setText(String.format(Locale.US, "Up to %,.0f kg", cap));
            ((ImageView) card.findViewById(R.id.vtIcon)).setImageResource(cap <= 1_000 ? R.drawable.ic_truck_mini : R.drawable.ic_truck);
            card.setTag(type);
            card.setOnClickListener(v -> selectType(type));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i % 2 == 1) cp.setMarginStart(gap);
            row.addView(card, cp);
            typeCards.add(card);
        }
        if (types.size() % 2 == 1 && row != null) {   // keep the last card half width
            row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        }
        selectType(selectedType);
    }

    private void selectType(String type) {
        selectedType = type;
        if (type != null) hide(errType);
        for (View card : typeCards) {
            boolean on = card.getTag().equals(type);
            card.setBackgroundResource(on ? R.drawable.vp_card_selected_bg : R.drawable.vp_card_bg);
            card.findViewById(R.id.vtCheck).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            View box = card.findViewById(R.id.vtIconBox);
            box.setBackgroundResource(on ? R.drawable.bg_btn_sign_in : R.drawable.vp_soft_bg);
            ((ImageView) card.findViewById(R.id.vtIcon)).setColorFilter(on ? Color.WHITE : Color.parseColor("#64748B"));
        }
    }

    // ---------------------------------------------------------------- configuration cards

    private void setUpConfigCard(int index, int icon, String title, String desc, String tag) {
        View card = findViewById(BODY_CARD_IDS[index]);
        ((ImageView) card.findViewById(R.id.cfgIcon)).setImageResource(icon);
        ((TextView) card.findViewById(R.id.cfgTitle)).setText(title);
        ((TextView) card.findViewById(R.id.cfgDesc)).setText(desc);
        ((TextView) card.findViewById(R.id.cfgTag)).setText(tag);
        card.setOnClickListener(v -> selectBody(BODY_CODES[index]));
        selectBody(selectedBody);
    }

    private void selectBody(String code) {
        selectedBody = code;
        if (code != null) hide(errConfig);
        for (int i = 0; i < BODY_CODES.length; i++) {
            View card = findViewById(BODY_CARD_IDS[i]);
            boolean on = BODY_CODES[i].equals(code);
            card.setBackgroundResource(on ? R.drawable.vp_card_selected_bg : R.drawable.vp_card_bg);
            ((ImageView) card.findViewById(R.id.cfgCheck)).setImageResource(on ? R.drawable.ic_check_circle_green : R.drawable.vp_radio_off);
            card.findViewById(R.id.cfgIconBox).setBackgroundResource(on ? R.drawable.bg_btn_sign_in : R.drawable.vp_soft_bg);
            ((ImageView) card.findViewById(R.id.cfgIcon)).setColorFilter(on ? Color.WHITE : Color.parseColor("#475569"));
            TextView tag = card.findViewById(R.id.cfgTag);
            tag.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(on ? "#BBF7D0" : "#E5E7EB")));
            tag.setTextColor(Color.parseColor(on ? "#166534" : "#475569"));
        }
    }

    // ---------------------------------------------------------------- plate & licence

    /** "WP ND-4892" -> "Western Province (LK)" badge, like the design. */
    private void showProvince() {
        String p = etPlate.getText().toString().trim().toUpperCase(Locale.US);
        String province = p.length() >= 2 ? provinceFor(p.substring(0, 2)) : null;
        tvProvince.setVisibility(province != null ? View.VISIBLE : View.GONE);
        if (province != null) tvProvince.setText(province + "\n(LK)");
    }

    static String provinceFor(String code) {
        switch (code) {
            case "WP": return "Western Province";
            case "CP": return "Central Province";
            case "SP": return "Southern Province";
            case "NP": return "Northern Province";
            case "EP": return "Eastern Province";
            case "NW": return "North Western Province";
            case "NC": return "North Central Province";
            case "UP": return "Uva Province";
            case "SG": return "Sabaragamuwa Province";
            default: return null;
        }
    }

    private void showLicenceState() {
        ImageView icon = findViewById(R.id.ivLicenceState);
        if (licence != null) {
            tvLicenceStatus.setText("Photo selected • uploads when you save");
            icon.setImageResource(R.drawable.ic_check_circle_dark_green);
        } else if (hasSavedLicence) {
            tvLicenceStatus.setText("Uploaded • tap Upload to replace");
            icon.setImageResource(R.drawable.ic_check_circle_dark_green);
        } else {
            tvLicenceStatus.setText("Not uploaded yet");
            icon.setImageResource(R.drawable.ic_badge_license);
        }
    }

    private Uri copyToCache(Uri uri) {
        try {
            java.io.File f = new java.io.File(getCacheDir(), "licence_" + System.currentTimeMillis() + ".jpg");
            try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                 java.io.OutputStream out = new java.io.FileOutputStream(f)) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            return Uri.fromFile(f);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read that image.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private double capacityOf(String type) {
        for (Map<String, Object> t : types) {
            if (type.equals(t.get("type")) && t.get("capacityKg") instanceof Number) return ((Number) t.get("capacityKg")).doubleValue();
        }
        return Double.MAX_VALUE;
    }

    // ---------------------------------------------------------------- save

    private void save() {
        View firstError = null;
        if (selectedType == null) firstError = show(errType, "Choose your vehicle type", null);
        if (selectedBody == null) firstError = show(errConfig, "Choose your vehicle configuration", firstError);

        String plate = etPlate.getText().toString().trim().toUpperCase(Locale.US).replaceAll("\\s+", " ");
        if (plate.isEmpty()) {
            firstError = show(errPlate, "License plate number is required", firstError);
        } else if (!plate.matches("^([A-Z]{2} )?[A-Z0-9]{2,3}[- ]?\\d{4}$")) {
            firstError = show(errPlate, "License plate must look like WP ND-4892", firstError);
        }

        Double cap = null;
        String capText = etPayload.getText().toString().trim().replace(",", "");
        if (!capText.isEmpty()) {
            try {
                cap = Double.parseDouble(capText);
                if (selectedType != null && (cap <= 0 || cap > capacityOf(selectedType))) {
                    firstError = show(errPayload, "Must be between 1 and " + (int) capacityOf(selectedType) + " kg for this vehicle type", firstError);
                }
            } catch (NumberFormatException e) {
                firstError = show(errPayload, "Enter the payload in kilograms", firstError);
            }
        }
        if (register != null && licence == null) {
            firstError = show(errLicence, "Upload a photo of your driving licence", firstError);
        }
        if (firstError != null) {
            View target = firstError;
            ScrollView scroll = findViewById(R.id.scroll);
            scroll.post(() -> scroll.smoothScrollTo(0, Math.max(0, topIn(scroll, target) - dp(120))));
            return;
        }

        if (register != null) {
            register.setVehicle(selectedType, plate, cap, selectedBody);
            startActivity(new Intent(this, SetDeliveryAddressActivity.class)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_REGISTER, register)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_TRANSPORTER, true)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_LICENCE_URI, licence));
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("vehicleType", selectedType);
        body.put("vehiclePlate", plate);
        body.put("vehicleCapacityKg", cap);
        body.put("vehicleBodyType", selectedBody);
        View btn = findViewById(R.id.btnSave);
        btn.setEnabled(false);
        ApiClient.getApiService().updateVehicle(body).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> r) {
                btn.setEnabled(true);
                if (!r.isSuccessful() || r.body() == null) {
                    Toast.makeText(TransporterVehicleProfileActivity.this, AuthInputUtils.errorMessage(r, "Could not save the vehicle."), Toast.LENGTH_LONG).show();
                    return;
                }
                new SessionManager(TransporterVehicleProfileActivity.this).saveUser(r.body());
                if (licence != null) {
                    TransporterSettingsActivity.LicenceUpload.upload(TransporterVehicleProfileActivity.this, licence, () -> finish());
                } else {
                    Toast.makeText(TransporterVehicleProfileActivity.this, "Vehicle saved.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                btn.setEnabled(true);
                Toast.makeText(TransporterVehicleProfileActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------- helpers

    /** Shows {@code message} in the red label; returns the first error view (for scrolling). */
    private static View show(TextView label, String message, View firstError) {
        label.setText(message);
        label.setVisibility(View.VISIBLE);
        return firstError != null ? firstError : label;
    }

    private static void hide(TextView label) {
        label.setVisibility(View.GONE);
    }

    private static int topIn(View ancestor, View v) {
        int y = 0;
        View cur = v;
        while (cur != null && cur != ancestor) {
            y += cur.getTop();
            cur = (View) cur.getParent();
        }
        return y;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static final class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { onChange.run(); }
        @Override public void afterTextChanged(Editable e) { }
    }
}
