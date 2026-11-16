package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma "Appeal Dispute". Decision 15 Nov: an appeal is a dispute ticket (type APPEAL) for the agricultural officers
 * with the driver's statement and optional photos. The system evidence (approved pre-loading photos, GPS track,
 * arrival time) is already on the order. The damage charge is not changed by submitting.
 */
public class AppealDisputeActivity extends BaseTransporterOrderActivity {

    private static final String[] REASONS = {"Unloading handling damage", "Pre-existing crop defect", "Incorrect penalty amount", "Other"};

    private List<TextView> reasonChips;
    private EditText statement;
    private final ArrayList<String> extraPhotos = new ArrayList<>();
    private ActivityResultLauncher<Intent> camera;
    private LinearLayout photoStrip;

    @Override
    protected String title() {
        return "Appeal Dispute";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        camera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                String p = r.getData().getStringExtra(EvidenceCameraActivity.EXTRA_PHOTO_PATH);
                if (p != null && extraPhotos.size() < 4) {
                    extraPhotos.add(p);
                    showThumbs();
                }
            }
        });
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean pollWhileOpen() {
        return false;
    }

    @Override
    protected boolean handOver(Order o) {
        return false; // stays open on RETURN_JOURNEY and RETURNED
    }

    private boolean built;

    @Override
    protected void render() {
        if (built) return; // keep the typed statement when coming back from the camera
        built = true;
        super.render();
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout head = TUi.card(c, TUi.DARK, TUi.DARK);
        TUi.text(head, (o.getFarmerName() == null ? "" : o.getFarmerName() + " • ") + ref(), 12, TUi.SURFACE, true);
        TUi.text(head, cargo(o), 17, android.graphics.Color.WHITE, true);

        LinearLayout sys = TUi.card(c);
        TUi.text(sys, "System evidence attached automatically", 15, TUi.INK, true);
        int photos = o.getLoadingEvidenceUrls() == null ? 0 : o.getLoadingEvidenceUrls().size();
        TUi.step(sys, "Geotagged pre-loading photos (" + photos + ") – approved by the buyer", 2, null);
        TUi.step(sys, "GPS track of the journey", 2, null);
        TUi.step(sys, "Arrival time at the buyer: " + time(o.getActualDeliveryDate()), 2, null);

        LinearLayout f = TUi.card(c);
        TUi.caption(f, "Dispute reason category");
        reasonChips = TUi.chips(f, REASONS, false, null);
        TUi.setChip(reasonChips.get(0), true);
        TUi.text(f, "Transporter statement", 14, TUi.INK, true).setPadding(0, TUi.dp(this, 12), 0, 0);
        statement = TUi.input(f, "What happened? (max 500 characters)", true);
        TUi.text(f, "Supplementary photos (optional, up to 4)", 14, TUi.INK, true).setPadding(0, TUi.dp(this, 12), 0, 0);
        photoStrip = new LinearLayout(this);
        f.addView(photoStrip);
        TUi.secondary(f, "+ Add supplementary photo", v -> {
            if (extraPhotos.size() >= 4) Toast.makeText(this, "Up to 4 photos.", Toast.LENGTH_SHORT).show();
            else camera.launch(new Intent(this, EvidenceCameraActivity.class).putExtra(EvidenceCameraActivity.EXTRA_LABEL, "Appeal " + ref()));
        });
        showThumbs();

        LinearLayout note = TUi.card(c, TUi.SURFACE, TUi.SURFACE);
        TUi.text(note, "How appeals work", 14, TUi.DARK, true);
        TUi.body(note, "Your appeal goes to the agricultural officers for review. The damage charge stays on your wallet "
                + "unless an officer decides otherwise.");

        TUi.primary(actions, "Submit Official Appeal →", v -> submit((MaterialButton) v));
    }

    private void showThumbs() {
        if (photoStrip == null) return;
        photoStrip.removeAllViews();
        for (String p : extraPhotos) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 8;
            iv.setImageBitmap(BitmapFactory.decodeFile(p, opt));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(TUi.dp(this, 80), TUi.dp(this, 64));
            lp.rightMargin = TUi.dp(this, 6);
            lp.topMargin = TUi.dp(this, 8);
            photoStrip.addView(iv, lp);
        }
    }

    private void submit(MaterialButton button) {
        String text = statement.getText().toString().trim();
        if (text.length() < 10) {
            statement.setError("Write a short statement (at least 10 characters)");
            return;
        }
        if (text.length() > 500) {
            statement.setError("Max 500 characters");
            return;
        }
        button.setEnabled(false);
        upload(0, new ArrayList<>(), urls -> {
            String reason = "Appeal: ";
            for (TextView t : reasonChips) if (TUi.isOn(t)) reason += TUi.chipText(t);
            Map<String, Object> body = new HashMap<>();
            body.put("reason", reason);
            body.put("details", text);
            body.put("photoUrls", urls);
            ApiClient.getApiService().openDisputeWithPhotos(orderId, body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> r) {
                    button.setEnabled(true);
                    if (!r.isSuccessful()) {
                        Toast.makeText(AppealDisputeActivity.this, AuthInputUtils.errorMessage(r, "Could not submit the appeal."), Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(AppealDisputeActivity.this, "Appeal sent to the officers.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(AppealDisputeActivity.this, DisputeCaseActivity.class)
                            .putExtra(TransporterOrderRouter.EXTRA_ORDER_ID, orderId));
                    finish();
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                    button.setEnabled(true);
                    Toast.makeText(AppealDisputeActivity.this, "Can't reach the server.", Toast.LENGTH_SHORT).show();
                }
            });
        }, () -> button.setEnabled(true));
    }

    interface Done {
        void urls(List<String> urls);
    }

    private void upload(int i, List<String> urls, Done done, Runnable failed) {
        if (i >= extraPhotos.size()) {
            done.urls(urls);
            return;
        }
        File f = new File(extraPhotos.get(i));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", f.getName(), RequestBody.create(MediaType.parse("image/jpeg"), f));
        ApiClient.getApiService().uploadMedia(part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().get("url") == null) {
                    Toast.makeText(AppealDisputeActivity.this, AuthInputUtils.errorMessage(r, "Photo upload failed."), Toast.LENGTH_LONG).show();
                    failed.run();
                    return;
                }
                urls.add(r.body().get("url"));
                upload(i + 1, urls, done, failed);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                Toast.makeText(AppealDisputeActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
                failed.run();
            }
        });
    }
}
