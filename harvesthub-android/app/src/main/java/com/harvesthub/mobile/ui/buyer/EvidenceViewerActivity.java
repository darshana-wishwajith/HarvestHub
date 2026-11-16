package com.harvesthub.mobile.ui.buyer;

import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.utils.RemoteImage;

import java.util.Locale;

/** All pre-loading photos the driver uploaded for an order, with time and GPS. Read-only. */
public class EvidenceViewerActivity extends BaseBuyerOrderActivity {

    private int shown = -1;

    @Override
    protected int layout() {
        return R.layout.activity_buyer_evidence_viewer;
    }

    @Override
    protected boolean followStatus() {
        return false;
    }

    @Override
    protected void bind(@NonNull Order o) {
        Order.TransportEvent e = o.lastEvent("LOADING_EVIDENCE_SUBMITTED");
        String gps = e != null && e.latitude != null && e.longitude != null
                ? String.format(Locale.US, " • GPS %.4f, %.4f", e.latitude, e.longitude) : "";
        setText(R.id.tvEvidenceMeta, "Order " + ref(o) + (o.getLoadingEvidenceAt() == null ? "" : " • " + time(o.getLoadingEvidenceAt())) + gps);
        int n = o.getLoadingEvidenceUrls() == null ? 0 : o.getLoadingEvidenceUrls().size();
        if (n == shown) return;
        shown = n;
        LinearLayout list = findViewById(R.id.layoutEvidencePhotos);
        list.removeAllViews();
        int h = Math.round(260 * getResources().getDisplayMetrics().density);
        int gap = Math.round(12 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < n; i++) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(0xFFE2E8F0);
            iv.setContentDescription("Loading photo " + (i + 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h);
            lp.topMargin = gap;
            list.addView(iv, lp);
            RemoteImage.load(iv, mediaUrl(o.getLoadingEvidenceUrls().get(i)));
        }
    }
}
