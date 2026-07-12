package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ActiveListing;

import java.util.List;

public class ActiveListingAdapter extends RecyclerView.Adapter<ActiveListingAdapter.ViewHolder> {

    private final List<ActiveListing> listings;
    private final OnListingActionListener listener;

    public interface OnListingActionListener {
        void onEdit(ActiveListing listing);
        void onDeactivate(ActiveListing listing);
        void onReactivate(ActiveListing listing);
        void onChat(ActiveListing listing);
        void onTrack(ActiveListing listing);
    }

    public ActiveListingAdapter(List<ActiveListing> listings, OnListingActionListener listener) {
        this.listings = listings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(listings.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardContainer;
        ImageView ivCropThumbnail;
        TextView tvCropName, tvListingCode, tvStatusBadge, tvEstYield, tvExpectedPrice;
        TextView tvCreatedDate, tvUpdatedTime, tvLocation, tvHelperText;
        LinearLayout llActions;
        MaterialButton btnPrimary, btnSecondary;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            ivCropThumbnail = itemView.findViewById(R.id.ivCropThumbnail);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvListingCode = itemView.findViewById(R.id.tvListingCode);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvEstYield = itemView.findViewById(R.id.tvEstYield);
            tvExpectedPrice = itemView.findViewById(R.id.tvExpectedPrice);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);
            tvUpdatedTime = itemView.findViewById(R.id.tvUpdatedTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvHelperText = itemView.findViewById(R.id.tvHelperText);
            llActions = itemView.findViewById(R.id.llActions);
            btnPrimary = itemView.findViewById(R.id.btnPrimary);
            btnSecondary = itemView.findViewById(R.id.btnSecondary);
        }

        public void bind(ActiveListing listing, OnListingActionListener listener) {
            tvCropName.setText(listing.getCropName());
            tvListingCode.setText(listing.getListingCode());
            tvEstYield.setText(listing.getEstYieldKg());
            tvExpectedPrice.setText(listing.getExpectedPricePerKg());
            tvCreatedDate.setText("Created: " + listing.getCreatedDate());
            tvUpdatedTime.setText("Updated: " + listing.getUpdatedTime());
            tvLocation.setText(listing.getLocation());

            // Reset defaults
            cardContainer.setCardBackgroundColor(Color.WHITE);
            tvHelperText.setVisibility(View.GONE);
            llActions.setVisibility(View.VISIBLE);
            btnPrimary.setVisibility(View.VISIBLE);
            btnSecondary.setVisibility(View.VISIBLE);
            btnPrimary.setStrokeColorResource(R.color.text_gray_light);
            btnSecondary.setStrokeColorResource(R.color.text_gray_light);
            btnPrimary.setBackgroundTintList(null);
            btnSecondary.setBackgroundTintList(null);
            btnPrimary.setTextColor(Color.BLACK);
            btnSecondary.setTextColor(Color.BLACK);
            btnPrimary.setIcon(null);
            btnSecondary.setIcon(null);

            String status = listing.getStatus().toUpperCase();
            tvStatusBadge.setText(status);

            switch (status) {
                case "DRAFT":
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    btnPrimary.setText("Edit");
                    btnSecondary.setVisibility(View.GONE);
                    btnPrimary.setOnClickListener(v -> listener.onEdit(listing));
                    break;

                case "PENDING":
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB300")));
                    btnPrimary.setText("Edit");
                    btnSecondary.setText("Deactivate");
                    btnPrimary.setOnClickListener(v -> listener.onEdit(listing));
                    btnSecondary.setOnClickListener(v -> listener.onDeactivate(listing));
                    break;

                case "UNDER REVIEW":
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    tvStatusBadge.setTextColor(Color.parseColor("#757575"));
                    llActions.setVisibility(View.GONE);
                    tvHelperText.setVisibility(View.VISIBLE);
                    break;

                case "LISTED":
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D6A4F")));
                    btnPrimary.setText("Edit");
                    btnSecondary.setText("Deactivate");
                    btnPrimary.setOnClickListener(v -> listener.onEdit(listing));
                    btnSecondary.setOnClickListener(v -> listener.onDeactivate(listing));
                    break;

                case "ONGOING":
                    cardContainer.setCardBackgroundColor(Color.parseColor("#F0F7FF"));
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
                    btnPrimary.setText("Chat");
                    btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2196F3")));
                    btnPrimary.setTextColor(Color.WHITE);
                    btnSecondary.setText("Track");
                    btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#2196F3")));
                    btnSecondary.setTextColor(Color.parseColor("#2196F3"));
                    btnPrimary.setOnClickListener(v -> listener.onChat(listing));
                    btnSecondary.setOnClickListener(v -> listener.onTrack(listing));
                    break;

                case "DEACTIVATED":
                    tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#616161")));
                    ivCropThumbnail.setAlpha(0.5f);
                    btnPrimary.setText("Edit");
                    btnSecondary.setText("Re-activate");
                    btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1B4332")));
                    btnSecondary.setTextColor(Color.WHITE);
                    btnPrimary.setOnClickListener(v -> listener.onEdit(listing));
                    btnSecondary.setOnClickListener(v -> listener.onReactivate(listing));
                    break;
            }
        }
    }
}
