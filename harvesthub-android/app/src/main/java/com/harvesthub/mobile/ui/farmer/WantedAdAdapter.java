package com.harvesthub.mobile.ui.farmer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.WantedAd;

import java.text.DecimalFormat;
import java.util.List;

public class WantedAdAdapter extends RecyclerView.Adapter<WantedAdAdapter.WantedAdViewHolder> {

    private final List<WantedAd> wantedAds;
    private final OnWantedAdActionListener listener;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public interface OnWantedAdActionListener {
        void onSendOffer(WantedAd ad);
    }

    public WantedAdAdapter(List<WantedAd> wantedAds, OnWantedAdActionListener listener) {
        this.wantedAds = wantedAds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WantedAdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wanted_ad, parent, false);
        return new WantedAdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WantedAdViewHolder holder, int position) {
        WantedAd ad = wantedAds.get(position);
        holder.tvCropName.setText(ad.getCropName());
        holder.tvLocation.setText(ad.getLocation());
        holder.tvRequiredQty.setText(formatter.format(ad.getRequiredQuantityKg()) + " KG");
        holder.tvTargetPrice.setText("LKR " + formatter.format(ad.getTargetPricePerKg()) + "/kg");
        holder.tvUrgentBadge.setVisibility(ad.isUrgent() ? View.VISIBLE : View.GONE);

        holder.btnSendOffer.setOnClickListener(v -> listener.onSendOffer(ad));
    }

    @Override
    public int getItemCount() {
        return wantedAds.size();
    }

    static class WantedAdViewHolder extends RecyclerView.ViewHolder {
        TextView tvCropName, tvLocation, tvRequiredQty, tvTargetPrice, tvUrgentBadge;
        MaterialButton btnSendOffer;

        WantedAdViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvRequiredQty = itemView.findViewById(R.id.tvRequiredQty);
            tvTargetPrice = itemView.findViewById(R.id.tvTargetPrice);
            tvUrgentBadge = itemView.findViewById(R.id.tvUrgentBadge);
            btnSendOffer = itemView.findViewById(R.id.btnSendOffer);
        }
    }
}
