package com.harvesthub.mobile.ui.transporter;

import com.harvesthub.mobile.R;
import android.graphics.Color;
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
 * "Appeal Dispute" screen. An appeal is a dispute ticket (type APPEAL) for the agricultural officers
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
        int green = Color.parseColor("#15803D");
        String photo = o.getLoadingEvidenceUrls() == null || o.getLoadingEvidenceUrls().isEmpty() ? null : o.getLoadingEvidenceUrls().get(0);
        int photos = o.getLoadingEvidenceUrls() == null ? 0 : o.getLoadingEvidenceUrls().size();

        LinearLayout head = TUi.card(c, TUi.DARK, TUi.DARK);
        LinearLayout hr = TUi.hrow(head, 0);
        TUi.thumb(hr, photo, 60, 60);
        LinearLayout ht = TUi.col(hr, 1f);
        ht.setPadding(TUi.dp(this, 12), 0, 0, 0);
        TUi.text(ht, "\u2713 " + (o.getFarmerName() == null ? "" : o.getFarmerName().toUpperCase(java.util.Locale.US) + " \u2022 ") + ref(), 11,
                Color.parseColor("#86EFAC"), true);
        TextView cg = TUi.text(ht, cargo(o), 16, Color.WHITE, true);
        cg.setMaxLines(1);
        cg.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TUi.text(ht, (o.getPickupAddress() == null ? "Farm" : o.getPickupAddress()) + " \u2192 " + (o.getDropAddress() == null ? "Buyer" : o.getDropAddress()),
                12, Color.parseColor("#D1FAE5"), false);

        LinearLayout sys = TUi.card(c);
        LinearLayout sh = TUi.hrow(sys, 0);
        TUi.iconTile(sh, R.drawable.ic_shield_check, 36, TUi.MINT, TUi.DARK, true);
        LinearLayout st = TUi.col(sh, 1f);
        st.setPadding(TUi.dp(this, 10), 0, 0, 0);
        TUi.text(st, "System Evidence Attached", 16, TUi.INK, true);
        TUi.text(st, "Automatically linked from the trip record", 12, TUi.MUTED, false);
        proof(sys, "Geotagged Pre-Loading Photos", photos + (photos == 1 ? " file" : " files") + " \u2022 Approved by the buyer at origin", "Attached");
        proof(sys, "Continuous GPS Track", o.getDistanceKm() == null ? "Journey positions every 30 s" : TUi.km(o.getDistanceKm()) + " route \u2022 positions every 30 s", "Verified");
        proof(sys, "Arrival Timestamp", "Arrived " + time(o.getActualDeliveryDate()) + (o.getDropAddress() == null ? "" : " \u2022 " + o.getDropAddress()), "Recorded");

        LinearLayout f = TUi.card(c);
        TUi.text(f, "DISPUTE REASON CATEGORY", 11, TUi.MUTED, true).setLetterSpacing(0.05f);
        reasonChips = TUi.chips(f, REASONS, false, null);
        TUi.setChip(reasonChips.get(0), true);
        LinearLayout sl = TUi.hrow(f, 14);
        TUi.fill(sl, "Transporter Statement", 14, TUi.INK, true);
        final TextView counter = TUi.label(sl, "0 / 500", 12, TUi.MUTED, true);
        statement = TUi.input(f, "What happened? Describe the handover and the condition of the goods.", true);
        statement.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        statement.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int n) { }
            public void onTextChanged(CharSequence s, int a, int b, int n) { counter.setText(s.length() + " / 500"); }
            public void afterTextChanged(android.text.Editable s) { }
        });
        LinearLayout el = TUi.hrow(f, 14);
        TUi.fill(el, "Supplementary Evidence", 14, TUi.INK, true);
        TUi.label(el, "Optional", 12, green, true);
        LinearLayout add = TUi.box(f, TUi.GREY_BG, 8);
        add.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        add.setPadding(TUi.dp(this, 12), TUi.dp(this, 16), TUi.dp(this, 12), TUi.dp(this, 16));
        TUi.iconTile(add, R.drawable.ic_camera_add, 44, TUi.MINT, TUi.DARK, true);
        TextView at = TUi.text(add, "+ Add Supplementary Photo", 13, TUi.DARK, true);
        at.setGravity(android.view.Gravity.CENTER);
        at.setPadding(0, TUi.dp(this, 8), 0, 0);
        TextView ah = TUi.text(add, "Up to 4 photos, e.g. unloading area, damaged crates", 12, TUi.MUTED, false);
        ah.setGravity(android.view.Gravity.CENTER);
        add.setOnClickListener(v -> {
            if (extraPhotos.size() >= 4) Toast.makeText(this, "Up to 4 photos.", Toast.LENGTH_SHORT).show();
            else camera.launch(new Intent(this, EvidenceCameraActivity.class).putExtra(EvidenceCameraActivity.EXTRA_LABEL, "Appeal " + ref()));
        });
        photoStrip = new LinearLayout(this);
        f.addView(photoStrip);
        showThumbs();

        TUi.notice(c, R.drawable.ic_escrow_recoil, Color.parseColor("#D7F5DD"), green, "How Appeals Work",
                "Your appeal goes to the agricultural officers for review. The damage charge stays on your wallet unless an officer decides otherwise.");

        TUi.primary(actions, "Submit Official Appeal \u2192", v -> submit((MaterialButton) v));
        TextView cancel = TUi.link(actions, "Cancel & Go Back", TUi.MUTED, v -> finish());
        cancel.setPaintFlags(cancel.getPaintFlags() & ~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }

    private void proof(LinearLayout parent, String title, String sub, String tagText) {
        LinearLayout b = TUi.box(parent, TUi.GREY_BG, 8);
        LinearLayout r = TUi.hrow(b, 0);
        r.setGravity(android.view.Gravity.TOP);
        TUi.icon(r, R.drawable.ic_check_circle_green, 18, Color.parseColor("#15803D"));
        LinearLayout t = TUi.col(r, 1f);
        t.setPadding(TUi.dp(this, 8), 0, TUi.dp(this, 8), 0);
        TUi.text(t, title, 14, TUi.INK, true);
        TUi.text(t, sub, 12, TUi.MUTED, false);
        TUi.tag(r, tagText, TUi.MINT, TUi.MINT_INK);
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
