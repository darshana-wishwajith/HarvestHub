package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.widget.TextView;
import android.graphics.Color;
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
        int green = Color.parseColor("#15803D");
        LinearLayout at = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
        LinearLayout ar = TUi.hrow(at, 0);
        TUi.icon(ar, R.drawable.ic_check_circle_green, 18, green);
        TextView ok = TUi.fill(ar, "GPS Position Recorded \u2022 At Farm", 13, green, true);
        ((LinearLayout.LayoutParams) ok.getLayoutParams()).leftMargin = TUi.dp(this, 6);
        String arrived = o.reachedAt("ARRIVED_AT_PICKUP");
        if (arrived != null) TUi.label(ar, time(arrived), 11, TUi.MUTED, false);
        TUi.text(at, o.getFarmerName() == null ? "Farm" : o.getFarmerName(), 20, TUi.INK, true).setPadding(0, TUi.dp(this, 8), 0, 0);
        if (o.getPickupAddress() != null) {
            LinearLayout ad = TUi.hrow(at, 2);
            TUi.icon(ad, R.drawable.ic_location_pin, 14, TUi.MUTED);
            TUi.fill(ad, " " + o.getPickupAddress(), 12, TUi.MUTED, false);
        }

        LinearLayout man = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
        TUi.iconHeader(man, R.drawable.ic_archive_box, "Manifest " + ref(), null, 0, 0);
        LinearLayout mb = TUi.box(man, Color.WHITE, 12);
        LinearLayout mr = TUi.hrow(mb, 0);
        TUi.iconTile(mr, R.drawable.ic_leaf, 38, TUi.GREY_BG, TUi.DARK, false);
        LinearLayout mt = TUi.col(mr, 1f);
        mt.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(mt, o.getCropName() == null ? "Produce" : o.getCropName(), 16, TUi.INK, true);
        TUi.text(mt, "Buyer: " + (o.getBuyerName() == null ? "\u2014" : o.getBuyerName()), 12, TUi.MUTED, false);
        LinearLayout tl = TUi.tiles(mb);
        TUi.tile(tl, 0, "ORDER", ref(), o.getDeliveryDate() == null ? null : "Deliver by " + o.getDeliveryDate());
        TUi.tile(tl, 0, "NET WEIGHT", String.format(java.util.Locale.US, "%,.0f kg", o.getQuantity()),
                o.getVehicleLabel() == null ? null : "Fits " + o.getVehicleLabel());

        int n = photoFiles.size();
        boolean enough = n >= MIN_PHOTOS;
        LinearLayout seq = TUi.card(c, TUi.GREY_BG, TUi.GREY_BG);
        TUi.iconHeader(seq, R.drawable.ic_list, "Pre-Loading Sequence", "Step 3 of 4", Color.parseColor("#E2DFDC"), TUi.INK);
        TUi.checkRow(seq, "1. Farm Arrival Recorded", 2, "Verified");
        TUi.checkRow(seq, "2. Farmer Handover Confirmed", 2, "Verified");
        LinearLayout shots = TUi.box(seq, Color.WHITE, 8);
        shots.setElevation(TUi.dp(this, 2));
        LinearLayout sr = TUi.hrow(shots, 0);
        if (enough) TUi.icon(sr, R.drawable.ic_check_circle_green, 20, green);
        else TUi.iconTile(sr, R.drawable.ic_camera_add, 36, TUi.DARK, Color.WHITE, false);
        TextView st = TUi.fill(sr, "3. Pre-Loading Photos", 15, TUi.DARK, true);
        ((LinearLayout.LayoutParams) st.getLayoutParams()).leftMargin = TUi.dp(this, 10);
        TUi.tag(sr, enough ? "Ready" : "Required", enough ? TUi.MINT : TUi.RED_BG, enough ? TUi.MINT_INK : TUi.RED);
        LinearLayout gr = TUi.hrow(shots, 10);
        TUi.icon(gr, R.drawable.ic_gps_crosshair, 14, green);
        TUi.fill(gr, " GPS Auto-Stamping Active", 12, TUi.MUTED, true);
        TUi.label(gr, n + " / " + MIN_PHOTOS + " Shots", 12, TUi.INK, true);
        if (n > 0) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            LinearLayout strip = new LinearLayout(this);
            hs.addView(strip);
            for (String p : photoFiles) {
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(TUi.round(TUi.GREY_BG, TUi.dp(this, 10)));
                iv.setClipToOutline(true);
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 8;
                iv.setImageBitmap(BitmapFactory.decodeFile(p, opt));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(TUi.dp(this, 110), TUi.dp(this, 90));
                lp.rightMargin = TUi.dp(this, 8);
                lp.topMargin = TUi.dp(this, 10);
                iv.setOnLongClickListener(v -> {
                    photoFiles.remove(p);
                    render();
                    return true;
                });
                strip.addView(iv, lp);
            }
            shots.addView(hs);
            TUi.text(shots, "Long-press a photo to remove it.", 11, TUi.MUTED, false);
        }
        TUi.checkRow(seq, "4. Buyer Approval", 0, "Next");
        TUi.text(seq, "Photograph the crates and the produce up close before loading. Take " + MIN_PHOTOS + " to " + MAX_PHOTOS + " photos.",
                12, TUi.MUTED, false).setPadding(0, TUi.dp(this, 10), 0, 0);

        if (n < MAX_PHOTOS) {
            TUi.iconButton(actions, enough ? "Add Another Photo" : "Launch Smart Evidence Camera", R.drawable.ic_viewfinder_bracket,
                    enough ? Color.parseColor("#E9E7E5") : TUi.DARK, enough ? TUi.INK : Color.WHITE,
                    v -> camera.launch(new Intent(this, EvidenceCameraActivity.class)
                            .putExtra(EvidenceCameraActivity.EXTRA_LABEL, "Order " + ref() + " \u2022 photo " + (n + 1))));
        }
        if (enough || uploading) {
            MaterialButton submit = TUi.iconButton(actions, uploading ? "Uploading\u2026" : "Submit for Buyer Approval",
                    R.drawable.ic_check_circle_outline_white, TUi.DARK, Color.WHITE, v -> submit((MaterialButton) v));
            submit.setEnabled(!uploading);
        }
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
