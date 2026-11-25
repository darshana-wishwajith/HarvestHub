package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.model.TransporterDashboard;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.buyer.SetDeliveryAddressActivity;
import com.harvesthub.mobile.ui.farmer.AccountDialogs;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Settings" screen: profile + rating, vehicle & capacity profile, driving licence,
 * vehicle base, wallet, notifications, password, log out. No bank accounts (mock wallet only).
 */
public class TransporterSettingsActivity extends AppCompatActivity {

    private LinearLayout content;
    private SessionManager session;
    private ActivityResultLauncher<String> pickLicence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickLicence = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) LicenceUpload.upload(this, uri, this::load);
        });
        content = TUi.scaffold(this, "Settings", TUi.Tab.SETTINGS);
        session = new SessionManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.getApiService().getTransporterDashboard(session.getUserId()).enqueue(new Callback<TransporterDashboard>() {
            @Override
            public void onResponse(@NonNull Call<TransporterDashboard> call, @NonNull Response<TransporterDashboard> r) {
                if (r.isSuccessful() && r.body() != null) render(r.body());
            }

            @Override
            public void onFailure(@NonNull Call<TransporterDashboard> call, @NonNull Throwable t) {
                content.removeAllViews();
                TUi.body(content, "Can't reach the server.");
            }
        });
    }

    private void render(TransporterDashboard d) {
        content.removeAllViews();
        String name = d.fullName == null || d.fullName.isEmpty() ? "Driver" : d.fullName;

        LinearLayout p = TUi.card(content);
        LinearLayout pr = TUi.hrow(p, 0);
        pr.setGravity(android.view.Gravity.TOP);
        TextView av = new TextView(this);
        av.setText(name.substring(0, 1).toUpperCase(Locale.US));
        av.setGravity(android.view.Gravity.CENTER);
        av.setTextSize(24);
        av.setTypeface(TUi.bold(this));
        av.setTextColor(Color.WHITE);
        av.setBackground(TUi.round(TUi.DARK, TUi.dp(this, 40)));
        pr.addView(av, new LinearLayout.LayoutParams(TUi.dp(this, 64), TUi.dp(this, 64)));
        LinearLayout pt = TUi.col(pr, 1f);
        pt.setPadding(TUi.dp(this, 14), 0, 0, 0);
        LinearLayout nr = TUi.hrow(pt, 0);
        TextView nm = TUi.fill(nr, name, 20, TUi.INK, true);
        nm.setMaxLines(1);
        nm.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.tag(nr, d.suspended ? "Suspended" : "Active", d.suspended ? TUi.RED_BG : TUi.MINT, d.suspended ? TUi.RED : TUi.MINT_INK);
        TUi.pill(pt, d.ratingAverage == null ? "\u2605 No ratings yet"
                : String.format(Locale.US, "\u2605 %.1f (%d rated)", d.ratingAverage, d.ratingCount == null ? 0 : d.ratingCount), TUi.GREY_BG, TUi.INK).setTextSize(12);
        if (session.getUser() != null) {
            TUi.text(pt, session.getUser().getMobileNumber() + (session.getUser().getEmail() == null ? "" : " \u2022 " + session.getUser().getEmail()),
                    12, TUi.MUTED, false).setPadding(0, TUi.dp(this, 6), 0, 0);
        }
        LinearLayout lic = TUi.hrow(pt, 4);
        TUi.icon(lic, d.licenceUploaded ? R.drawable.ic_verified_badge : R.drawable.ic_warning_amber, 14, 0);
        TUi.fill(lic, d.licenceUploaded ? " Driving licence on file" : " Driving licence photo missing", 12, d.licenceUploaded ? TUi.DARK : TUi.AMBER, true);
        LinearLayout stats = TUi.box(p, TUi.GREY_BG, 14);
        LinearLayout sr = TUi.hrow(stats, 0);
        stat(sr, "Trips", String.valueOf(d.tripsCompleted));
        stat(sr, "Empty Trips", String.valueOf(d.emptyTrips));
        stat(sr, "Status", d.suspended ? "Locked" : d.online ? "Online" : "Offline");

        section("Vehicle & capacity profile", R.drawable.ic_truck);
        LinearLayout v = TUi.card(content);
        setting(v, R.drawable.ic_truck, "Assigned Unit", (d.vehicleLabel == null ? "Not set" : d.vehicleLabel) + (d.vehiclePlate == null ? "" : " (" + d.vehiclePlate + ")"),
                null, x -> startActivity(new Intent(this, TransporterVehicleProfileActivity.class)));
        TUi.divider(v, 10);
        setting(v, R.drawable.ic_scale_manifest, "Payload Capacity", String.format(Locale.US, "%,.0f kg", d.capacityKg)
                + (d.vehicleBodyType == null ? "" : " \u2022 " + DeliveryDashboardActivity.body(d.vehicleBodyType)),
                null, x -> startActivity(new Intent(this, TransporterVehicleProfileActivity.class)));
        TUi.divider(v, 10);
        setting(v, R.drawable.ic_badge_license, "Driving Licence", d.licenceUploaded ? "Tap to replace the photo" : "Tap to upload a photo",
                d.licenceUploaded ? "\u2713 Uploaded" : "Missing", x -> pickLicence.launch("image/*"));

        section("Wallet & vehicle base", R.drawable.ic_wallet);
        LinearLayout w = TUi.card(content);
        setting(w, R.drawable.ic_wallet, "HarvestHub Wallet", TUi.rs(d.balance), d.balance < 0 ? "Below zero" : null,
                x -> startActivity(new Intent(this, TransporterWalletActivity.class)));
        TUi.divider(w, 10);
        setting(w, R.drawable.ic_location_pin, "Vehicle Base (nearest-driver dispatch)", d.baseAddress == null ? "Not set" : d.baseAddress, null,
                x -> startActivity(new Intent(this, SetDeliveryAddressActivity.class).putExtra(SetDeliveryAddressActivity.EXTRA_TRANSPORTER, true)));

        section("Dispatch & account preferences", R.drawable.ic_settings);
        LinearLayout pf = TUi.card(content);
        setting(pf, R.drawable.ic_bell, "Notification Preferences", "Job requests, approvals and payouts", null,
                x -> AccountDialogs.notificationPreferences(this));
        TUi.divider(pf, 10);
        setting(pf, R.drawable.ic_gps_crosshair, "Auto-GPS While In Transit", "Shared with the buyer only during a trip", "Enabled", null);
        TUi.divider(pf, 10);
        setting(pf, R.drawable.ic_lock_outline, "Password", "Change your password", null, x -> AccountDialogs.changePassword(this));

        com.google.android.material.button.MaterialButton out = TUi.iconButton(content, "Log Out of Transporter Account", R.drawable.ic_logout,
                TUi.RED_BG, TUi.RED, x -> new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setPositiveButton("Log out", (dd, ww) -> {
                    TransporterTrackingService.stop(this);
                    session.logoutUser();
                    Intent i = new Intent(this, com.harvesthub.mobile.ui.common.RoleSelectionActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }).setNegativeButton("Cancel", null).show());
        ((LinearLayout.LayoutParams) out.getLayoutParams()).topMargin = TUi.dp(this, 28);
        TextView ver = TUi.text(content, "HarvestHub Mobile v" + com.harvesthub.mobile.BuildConfig.VERSION_NAME, 11, TUi.MUTED, false);
        ver.setGravity(android.view.Gravity.CENTER);
        ver.setPadding(0, TUi.dp(this, 10), 0, 0);
    }

    private void stat(LinearLayout row, String label, String value) {
        LinearLayout c = TUi.col(row, 1f);
        TUi.text(c, label, 11, TUi.MUTED, true).setGravity(android.view.Gravity.CENTER);
        TUi.text(c, value, 15, TUi.INK, true).setGravity(android.view.Gravity.CENTER);
    }

    private void section(String title, int iconRes) {
        LinearLayout r = TUi.hrow(content, 20);
        TextView t = TUi.fill(r, title.toUpperCase(Locale.US), 11, TUi.INK, true);
        t.setLetterSpacing(0.05f);
        TUi.icon(r, iconRes, 16, TUi.MUTED);
    }

    /** Settings row: icon tile, small label, bold value, then a status pill or a chevron. */
    private void setting(LinearLayout card, int iconRes, String label, String value, String tagText, android.view.View.OnClickListener click) {
        LinearLayout r = TUi.hrow(card, 0);
        TUi.iconTile(r, iconRes, 40, TUi.GREY_BG, TUi.INK, false);
        LinearLayout t = TUi.col(r, 1f);
        t.setPadding(TUi.dp(this, 12), 0, TUi.dp(this, 8), 0);
        TUi.text(t, label, 12, TUi.MUTED, true);
        TextView v = TUi.text(t, value, 15, TUi.INK, true);
        v.setMaxLines(2);
        v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (tagText != null) {
            boolean bad = tagText.startsWith("Missing") || tagText.startsWith("Below");
            TUi.tag(r, tagText, bad ? TUi.RED_BG : TUi.MINT, bad ? TUi.RED : TUi.MINT_INK);
        } else if (click != null) {
            TUi.icon(r, R.drawable.ic_chevron_right, 18, TUi.INK);
        }
        if (click != null) r.setOnClickListener(click);
    }

    /** Uploads the driving-licence photo and stores its URL on the profile. */
    public static final class LicenceUpload {
        public static void upload(AppCompatActivity a, Uri uri, Runnable done) {
            try {
                java.io.File f = new java.io.File(a.getCacheDir(), "licence_" + System.currentTimeMillis() + ".jpg");
                try (java.io.InputStream in = a.getContentResolver().openInputStream(uri);
                     java.io.OutputStream out = new java.io.FileOutputStream(f)) {
                    if (in == null) throw new java.io.IOException("no data");
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                String type = a.getContentResolver().getType(uri);
                okhttp3.MultipartBody.Part part = okhttp3.MultipartBody.Part.createFormData("file", f.getName(),
                        okhttp3.RequestBody.create(okhttp3.MediaType.parse(type == null ? "image/jpeg" : type), f));
                ApiClient.getApiService().uploadMedia(part).enqueue(new Callback<java.util.Map<String, String>>() {
                    @Override
                    public void onResponse(@NonNull Call<java.util.Map<String, String>> call, @NonNull Response<java.util.Map<String, String>> r) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                        if (!r.isSuccessful() || r.body() == null || r.body().get("url") == null) {
                            Toast.makeText(a, com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(r, "Licence upload failed.")
                                    + " You can add it later in Settings.", Toast.LENGTH_LONG).show();
                            if (done != null) done.run();   // always continue (never strand a new driver)
                            return;
                        }
                        java.util.Map<String, Object> body = new java.util.HashMap<>();
                        body.put("licenceUrl", r.body().get("url"));
                        ApiClient.getApiService().updateVehicle(body).enqueue(new Callback<com.harvesthub.mobile.model.User>() {
                            @Override
                            public void onResponse(@NonNull Call<com.harvesthub.mobile.model.User> c, @NonNull Response<com.harvesthub.mobile.model.User> r2) {
                                Toast.makeText(a, r2.isSuccessful() ? "Driving licence saved." : "Could not save the licence.", Toast.LENGTH_SHORT).show();
                                if (done != null) done.run();
                            }

                            @Override
                            public void onFailure(@NonNull Call<com.harvesthub.mobile.model.User> c, @NonNull Throwable t) {
                                Toast.makeText(a, "Can't reach the server.", Toast.LENGTH_SHORT).show();
                                if (done != null) done.run();
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<java.util.Map<String, String>> call, @NonNull Throwable t) {
                        Toast.makeText(a, "Licence upload failed. You can add it later in Settings.", Toast.LENGTH_LONG).show();
                        if (done != null) done.run();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(a, "Could not read that image.", Toast.LENGTH_SHORT).show();
                if (done != null) done.run();
            }
        }
    }
}
