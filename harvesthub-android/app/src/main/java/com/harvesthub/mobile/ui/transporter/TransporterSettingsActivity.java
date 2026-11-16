package com.harvesthub.mobile.ui.transporter;

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
 * Figma transporter "Settings": profile + rating, vehicle & capacity profile, driving licence (decision 15 Nov),
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
        LinearLayout p = TUi.card(content);
        TUi.text(p, d.fullName == null ? "Driver" : d.fullName, 20, TUi.INK, true);
        TUi.pill(p, d.suspended ? "Suspended" : d.online ? "Active • Online" : "Active • Offline",
                d.suspended ? TUi.RED_BG : TUi.SURFACE, d.suspended ? TUi.RED : TUi.DARK);
        TUi.row(p, "Rating", d.ratingAverage == null ? "No ratings yet"
                : String.format(Locale.US, "★ %.1f (%d trips rated)", d.ratingAverage, d.ratingCount == null ? 0 : d.ratingCount));
        TUi.row(p, "Completed trips", String.valueOf(d.tripsCompleted));
        if (session.getUser() != null) {
            TUi.row(p, "Mobile", String.valueOf(session.getUser().getMobileNumber()));
            TUi.row(p, "Email", String.valueOf(session.getUser().getEmail()));
        }

        TUi.caption(content, "Vehicle & capacity profile");
        LinearLayout v = TUi.card(content);
        TUi.row(v, "Assigned unit", (d.vehicleLabel == null ? "—" : d.vehicleLabel) + (d.vehiclePlate == null ? "" : " (" + d.vehiclePlate + ")"));
        TUi.row(v, "Payload capacity", String.format(Locale.US, "%,.0f kg", d.capacityKg));
        TUi.row(v, "Configuration", d.vehicleBodyType == null ? "—" : DeliveryDashboardActivity.body(d.vehicleBodyType));
        TUi.row(v, "Driving licence", d.licenceUploaded ? "✓ Uploaded" : "Missing", d.licenceUploaded ? TUi.ACCENT : TUi.RED, false);
        TUi.secondary(v, "Edit vehicle", x -> startActivity(new Intent(this, TransporterVehicleProfileActivity.class)));
        TUi.secondary(v, d.licenceUploaded ? "Replace licence photo" : "Upload licence photo", x -> pickLicence.launch("image/*"));

        TUi.caption(content, "Vehicle base (used for nearest-driver dispatch)");
        LinearLayout b = TUi.card(content);
        TUi.body(b, d.baseAddress == null ? "Not set" : d.baseAddress);
        TUi.secondary(b, "Change vehicle address", x -> startActivity(new Intent(this, SetDeliveryAddressActivity.class)
                .putExtra(SetDeliveryAddressActivity.EXTRA_TRANSPORTER, true)));

        TUi.caption(content, "Wallet");
        LinearLayout w = TUi.card(content);
        TUi.row(w, "Balance", TUi.rs(d.balance), d.balance < 0 ? TUi.RED : TUi.DARK, true);
        TUi.secondary(w, "Open wallet", x -> startActivity(new Intent(this, TransporterWalletActivity.class)));

        TUi.caption(content, "Dispatch preferences");
        LinearLayout pr = TUi.card(content);
        TUi.body(pr, "Live GPS is shared with the buyer only while a trip is in transit (notification shown).");
        TUi.secondary(pr, "Notification preferences", x -> AccountDialogs.notificationPreferences(this));
        TUi.secondary(pr, "Change password", x -> AccountDialogs.changePassword(this));

        TUi.danger(content, "Log out of transporter account", x -> new AlertDialog.Builder(this)
                .setTitle("Log out?")
                .setPositiveButton("Log out", (dd, ww) -> {
                    TransporterTrackingService.stop(this);
                    session.logoutUser();
                    Intent i = new Intent(this, com.harvesthub.mobile.ui.common.RoleSelectionActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                }).setNegativeButton("Cancel", null).show());
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
                            Toast.makeText(a, com.harvesthub.mobile.utils.AuthInputUtils.errorMessage(r, "Licence upload failed."), Toast.LENGTH_LONG).show();
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
                            }
                        });
                    }

                    @Override
                    public void onFailure(@NonNull Call<java.util.Map<String, String>> call, @NonNull Throwable t) {
                        Toast.makeText(a, "Upload failed.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(a, "Could not read that image.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
