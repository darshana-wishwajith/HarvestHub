package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma "Arrived at Farm" – pre-loading sequence (workflow 4.2, decision 15 Nov: 2–4 photos). Photos are taken
 * with the in-app evidence camera (GPS + time stamped), uploaded, and submitted for the buyer's approval.
 */
public class LiabilityCheckpointActivity extends BaseTransporterOrderActivity {

    static final int MIN_PHOTOS = 2;
    static final int MAX_PHOTOS = 4;

    private final ArrayList<String> photoFiles = new ArrayList<>();
    private ActivityResultLauncher<Intent> camera;
    private boolean uploading;

    @Override
    protected String title() {
        return "Arrived at Farm";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        camera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK && r.getData() != null) {
                String path = r.getData().getStringExtra(EvidenceCameraActivity.EXTRA_PHOTO_PATH);
                if (path != null && photoFiles.size() < MAX_PHOTOS) photoFiles.add(path);
                if (order != null) render();
            }
        });
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<String> saved = savedInstanceState.getStringArrayList("photos");
            if (saved != null) photoFiles.addAll(saved);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putStringArrayList("photos", photoFiles);
    }

    @Override
    protected boolean pollWhileOpen() {
        return false; // nothing changes on the server until the driver submits
    }

    @Override
    protected void bind(@NonNull Order o, @NonNull LinearLayout c, @NonNull LinearLayout actions) {
        LinearLayout at = TUi.card(c);
        TUi.pill(at, "✓ Arrival recorded • At farm", TUi.SURFACE, TUi.DARK);
        TUi.text(at, o.getFarmerName() == null ? "Farm" : o.getFarmerName(), 20, TUi.INK, true);
        TUi.body(at, o.getPickupAddress() == null ? "" : o.getPickupAddress());

        LinearLayout man = TUi.card(c);
        TUi.title(man, "Manifest " + ref());
        TUi.text(man, o.getCropName() == null ? "Produce" : o.getCropName(), 16, TUi.INK, true);
        TUi.row(man, "Net weight", String.format(java.util.Locale.US, "%,.0f kg", o.getQuantity()));
        if (o.getVehicleLabel() != null) TUi.row(man, "Vehicle class", o.getVehicleLabel());
        TUi.row(man, "Buyer", o.getBuyerName() == null ? "—" : o.getBuyerName());

        LinearLayout seq = TUi.card(c);
        TUi.title(seq, "Pre-Loading Sequence");
        TUi.step(seq, "1. Farm arrival recorded", 2, "Done");
        TUi.step(seq, "2. Harvest marked ready by the farmer", 2, "Done");
        int n = photoFiles.size();
        TUi.step(seq, "3. Pre-loading photos (" + n + " / min " + MIN_PHOTOS + ")", n >= MIN_PHOTOS ? 2 : 1, n >= MIN_PHOTOS ? "Ready" : "Required");
        TUi.step(seq, "4. Buyer approval", 0, null);
        TUi.body(seq, "Photograph the crates and the produce up close before loading. Each photo is stamped with time and GPS.");
        if (n > 0) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            LinearLayout strip = new LinearLayout(this);
            hs.addView(strip);
            for (String p : photoFiles) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 8;
                iv.setImageBitmap(BitmapFactory.decodeFile(p, opt));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(TUi.dp(this, 110), TUi.dp(this, 90));
                lp.rightMargin = TUi.dp(this, 8);
                iv.setOnLongClickListener(v -> {
                    photoFiles.remove(p);
                    render();
                    return true;
                });
                strip.addView(iv, lp);
            }
            seq.addView(hs);
            TUi.body(seq, "Long-press a photo to remove it.");
        }

        if (n < MAX_PHOTOS) {
            TUi.secondary(actions, "📷  Launch Evidence Camera", v -> camera.launch(new Intent(this, EvidenceCameraActivity.class)
                    .putExtra(EvidenceCameraActivity.EXTRA_LABEL, "Order " + ref() + " • photo " + (n + 1))));
        }
        MaterialButton submit = TUi.primary(actions, uploading ? "Uploading…" : "Submit for Buyer Approval", v -> submit((MaterialButton) v));
        submit.setEnabled(n >= MIN_PHOTOS && !uploading);
    }

    private void submit(MaterialButton button) {
        uploading = true;
        button.setEnabled(false);
        button.setText("Uploading 1 / " + photoFiles.size() + "…");
        uploadNext(0, new ArrayList<>(), button);
    }

    private void uploadNext(int i, List<String> urls, MaterialButton button) {
        if (i >= photoFiles.size()) {
            uploading = false;
            sendEvent("LOADING_EVIDENCE_SUBMITTED", "Pre-loading photos", urls, button);
            return;
        }
        File f = new File(photoFiles.get(i));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", f.getName(),
                RequestBody.create(MediaType.parse("image/jpeg"), f));
        ApiClient.getApiService().uploadMedia(part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().get("url") == null) {
                    fail(AuthInputUtils.errorMessage(r, "Photo upload failed."));
                    return;
                }
                urls.add(r.body().get("url"));
                button.setText("Uploading " + Math.min(i + 2, photoFiles.size()) + " / " + photoFiles.size() + "…");
                uploadNext(i + 1, urls, button);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                fail("Upload failed – check your connection and try again.");
            }
        });
    }

    private void fail(String msg) {
        uploading = false;
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        render();
    }

    @Override
    protected void onEventDone(String event, Order updated) {
        for (String p : photoFiles) //noinspection ResultOfMethodCallIgnored
            new File(p).delete();
        photoFiles.clear();
        super.onEventDone(event, updated);
    }
}
