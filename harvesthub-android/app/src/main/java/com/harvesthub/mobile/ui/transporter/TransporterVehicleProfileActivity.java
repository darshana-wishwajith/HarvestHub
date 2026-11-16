package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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
 * Figma transporter "Vehicle Profile" (sign-up step 2 of 3, and "Edit vehicle" in Settings). Vehicle classes come
 * from GET /api/vehicle-types (decision 15 Nov: backend classes, not the Figma 500/1,000/3,000/5,000 kg cards);
 * body type is a label only; the driving-licence photo is uploaded right after the account is created.
 */
public class TransporterVehicleProfileActivity extends AppCompatActivity {

    private static final String[] BODY_LABELS = {"Refrigerated (reefer)", "Flatbed farm truck", "Box truck"};
    private static final String[] BODY_CODES = {"REEFER", "FLATBED", "BOX"};

    private RegisterRequest register;   // null = editing the signed-in driver's vehicle
    private final List<Map<String, Object>> types = new ArrayList<>();
    private final List<TextView> typeCards = new ArrayList<>();
    private String selectedType;
    private List<TextView> bodyChips;
    private EditText payload, plate;
    private Uri licence;
    private TextView licenceLabel;
    private ActivityResultLauncher<String> pickLicence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickLicence = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            licence = copyToCache(uri); // keep it readable after leaving this screen
            if (licence == null) return;
            licenceLabel.setText("✓ Licence photo selected");
            licenceLabel.setTextColor(TUi.DARK);
        });
        register = getIntent().getParcelableExtra(SetDeliveryAddressActivity.EXTRA_REGISTER);
        LinearLayout c = TUi.scaffold(this, "Vehicle Profile", null);
        if (register != null) ((View) findViewById(R.id.navTpHome).getParent()).setVisibility(View.GONE);
        findViewById(R.id.btnTpBell).setVisibility(register != null ? View.GONE : View.VISIBLE);

        if (register != null) {
            LinearLayout s = TUi.card(c);
            TUi.caption(s, "Step 2 of 3");
            TUi.text(s, "Vehicle & capacity verification", 15, TUi.DARK, true);
        }
        TUi.title(c, "Register your transport vehicle").setPadding(0, TUi.dp(this, 16), 0, 0);
        TUi.body(c, "Used to match you with loads your vehicle can carry.");

        TUi.caption(c, "Select vehicle type • required");
        LinearLayout typeBox = new LinearLayout(this);
        typeBox.setOrientation(LinearLayout.VERTICAL);
        c.addView(typeBox);

        TUi.caption(c, "Vehicle configuration");
        bodyChips = TUi.chips(c, BODY_LABELS, false, null);

        LinearLayout specs = TUi.card(c);
        TUi.text(specs, "Capacity & legal specs", 16, TUi.DARK, true);
        TUi.caption(specs, "Max payload (kg, optional – defaults to the class limit)");
        payload = TUi.input(specs, "e.g. 900", false);
        payload.setInputType(InputType.TYPE_CLASS_NUMBER);
        TUi.caption(specs, "Licence plate number • required");
        plate = TUi.input(specs, "e.g. WP ND-4892", false);
        plate.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        LinearLayout docs = TUi.card(c);
        TUi.text(docs, "Compliance document", 16, TUi.DARK, true);
        TUi.body(docs, "Photo of your driving licence (stored for the agricultural officers).");
        licenceLabel = TUi.text(docs, "No photo selected", 14, TUi.MUTED, true);
        TUi.secondary(docs, "Upload driving licence photo", v -> pickLicence.launch("image/*"));

        TUi.primary(TUi.actions(this), register != null ? "Save & continue to vehicle address →" : "Save vehicle", v -> save());

        User me = new SessionManager(this).getUser();
        if (register == null && me != null) {
            if (me.getVehiclePlate() != null) plate.setText(me.getVehiclePlate());
            if (me.getVehicleCapacityKg() != null) payload.setText(String.valueOf(me.getVehicleCapacityKg().intValue()));
            selectedType = me.getVehicleType();
            for (int i = 0; i < BODY_CODES.length; i++) if (BODY_CODES[i].equals(me.getVehicleBodyType())) TUi.setChip(bodyChips.get(i), true);
        }
        loadTypes(typeBox);
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

    private void loadTypes(LinearLayout box) {
        ApiClient.getApiService().getVehicleTypes().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> r) {
                if (!r.isSuccessful() || r.body() == null) {
                    TUi.body(box, "Could not load vehicle types. Go back and try again.");
                    return;
                }
                types.clear();
                types.addAll(r.body());
                for (Map<String, Object> t : types) {
                    String type = String.valueOf(t.get("type"));
                    double cap = t.get("capacityKg") instanceof Number ? ((Number) t.get("capacityKg")).doubleValue() : 0;
                    TextView card = TUi.text(box, t.get("label") + "\nUp to " + String.format(Locale.US, "%,.0f kg", cap), 15, TUi.INK, true);
                    int p = TUi.dp(TransporterVehicleProfileActivity.this, 14);
                    card.setPadding(p, p, p, p);
                    ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = TUi.dp(TransporterVehicleProfileActivity.this, 8);
                    card.setTag(type);
                    card.setOnClickListener(v -> select(type));
                    typeCards.add(card);
                }
                select(selectedType);
            }

            @Override
            public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                TUi.body(box, "Can't reach the server.");
            }
        });
    }

    private void select(String type) {
        selectedType = type;
        for (TextView t : typeCards) {
            boolean on = t.getTag().equals(type);
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setColor(on ? TUi.SURFACE : android.graphics.Color.WHITE);
            d.setStroke(TUi.dp(this, on ? 2 : 1), on ? TUi.DARK : TUi.LINE);
            d.setCornerRadius(TUi.dp(this, 14));
            t.setBackground(d);
        }
    }

    private double capacityOf(String type) {
        for (Map<String, Object> t : types) {
            if (type.equals(t.get("type")) && t.get("capacityKg") instanceof Number) return ((Number) t.get("capacityKg")).doubleValue();
        }
        return Double.MAX_VALUE;
    }

    private void save() {
        if (selectedType == null) {
            Toast.makeText(this, "Choose a vehicle type.", Toast.LENGTH_SHORT).show();
            return;
        }
        String p = plate.getText().toString().trim().toUpperCase(Locale.US).replaceAll("\\s+", " ");
        if (!p.matches("^([A-Z]{2} )?[A-Z0-9]{2,3}[- ]?\\d{4}$")) {
            plate.setError("Licence plate must look like WP ND-4892");
            return;
        }
        Double cap = null;
        String capText = payload.getText().toString().trim();
        if (!capText.isEmpty()) {
            try {
                cap = Double.parseDouble(capText);
            } catch (NumberFormatException e) {
                payload.setError("Enter kilograms");
                return;
            }
            if (cap <= 0 || cap > capacityOf(selectedType)) {
                payload.setError("Must be between 1 and " + (int) capacityOf(selectedType) + " kg for this class");
                return;
            }
        }
        String bodyType = null;
        for (int i = 0; i < bodyChips.size(); i++) if (TUi.isOn(bodyChips.get(i))) bodyType = BODY_CODES[i];

        if (register != null) {
            if (licence == null) {
                Toast.makeText(this, "Add a photo of your driving licence.", Toast.LENGTH_LONG).show();
                return;
            }
            register.setVehicle(selectedType, p, cap, bodyType);
            startActivity(new Intent(this, SetDeliveryAddressActivity.class)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_REGISTER, register)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_TRANSPORTER, true)
                    .putExtra(SetDeliveryAddressActivity.EXTRA_LICENCE_URI, licence));
            return;
        }
        Map<String, Object> body = new HashMap<>();
        body.put("vehicleType", selectedType);
        body.put("vehiclePlate", p);
        body.put("vehicleCapacityKg", cap);
        if (bodyType != null) body.put("vehicleBodyType", bodyType);
        ApiClient.getApiService().updateVehicle(body).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> r) {
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
                Toast.makeText(TransporterVehicleProfileActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
