package com.harvesthub.mobile.ui.farmer;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Settings dialogs: change password, notification preferences, help. */
public final class AccountDialogs {

    private AccountDialogs() {}

    private static EditText passwordField(Context ctx, String hint) {
        EditText e = new EditText(ctx);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    public static void changePassword(@NonNull Context ctx) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * ctx.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        EditText current = passwordField(ctx, "Current password");
        EditText fresh = passwordField(ctx, "New password");
        EditText confirm = passwordField(ctx, "Confirm new password");
        box.addView(current);
        box.addView(fresh);
        box.addView(confirm);
        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setTitle("Change Password")
                .setMessage("At least 8 characters with letters and numbers.")
                .setView(box)
                .setPositiveButton("Update", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = fresh.getText().toString();
            if (!AuthInputUtils.isStrongEnough(n)) {
                fresh.setError(ctx.getString(R.string.fp_err_password_rule));
                return;
            }
            if (!n.equals(confirm.getText().toString())) {
                confirm.setError(ctx.getString(R.string.fp_err_password_mismatch));
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("currentPassword", current.getText().toString());
            body.put("newPassword", n);
            ApiClient.getApiService().changePassword(body).enqueue(new Callback<GenericResponse>() {
                @Override
                public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ctx, "Password updated.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        current.setError(AuthInputUtils.errorMessage(response, "Could not update the password."));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                    Toast.makeText(ctx, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                }
            });
        }));
        dialog.show();
    }

    private static final String[] KEYS = {"ORDERS", "PAYMENTS", "LISTINGS", "MARKET"};
    private static final String[] LABELS = {"Orders & deliveries", "Payments & penalties", "Listing verification", "Market updates"};

    public static void notificationPreferences(@NonNull Context ctx) {
        ApiClient.getApiService().getPreferences().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Map<String, Object> prefs = response.body() == null ? new HashMap<>() : response.body();
                boolean[] checked = new boolean[KEYS.length];
                for (int i = 0; i < KEYS.length; i++) checked[i] = !Boolean.FALSE.equals(prefs.get(KEYS[i]));
                new AlertDialog.Builder(ctx)
                        .setTitle("Notification Preferences")
                        .setMultiChoiceItems(LABELS, checked, (d, which, isChecked) -> checked[which] = isChecked)
                        .setPositiveButton("Save", (d, w) -> {
                            Map<String, Object> body = new HashMap<>();
                            for (int i = 0; i < KEYS.length; i++) body.put(KEYS[i], checked[i]);
                            ApiClient.getApiService().updatePreferences(body).enqueue(new Callback<Map<String, Object>>() {
                                @Override
                                public void onResponse(@NonNull Call<Map<String, Object>> c, @NonNull Response<Map<String, Object>> r) {
                                    Toast.makeText(ctx, r.isSuccessful() ? "Preferences saved." : "Could not save.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(@NonNull Call<Map<String, Object>> c, @NonNull Throwable t) {
                                    Toast.makeText(ctx, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(ctx, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void help(@NonNull Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("Help & Support")
                .setMessage("• Create a listing: tap Create New Listing at your farm – the app checks your GPS, then guides "
                        + "you through 2 photos and a 10-second video.\n\n"
                        + "• Payments: the buyer pays an advance into escrow; you get 20% when the goods leave your farm "
                        + "and the rest after the driver and buyer both confirm delivery. Farmers pay 0% platform fee.\n\n"
                        + "• Do not harvest before the 'Vehicle Assigned – Start Harvesting' notification.\n\n"
                        + "• Need help? Contact your Agrarian Service Centre or the HarvestHub team (ICT 2212, Group 12).")
                .setPositiveButton("OK", null)
                .show();
    }
}
