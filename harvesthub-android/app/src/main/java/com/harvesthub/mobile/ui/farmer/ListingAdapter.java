package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ListingItem;

import java.util.List;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private final List<ListingItem> items;
    private final OnListingActionListener listener;

    public interface OnListingActionListener {
        void onAction(ListingItem item);
        default void onChat(ListingItem item) {}
        default void onTrack(ListingItem item) {}
        default void onEdit(ListingItem item) {}
        default void onSupply(ListingItem item) {}
    }

    public ListingAdapter(List<ListingItem> items, OnListingActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing_variant, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        ListingItem item = items.get(position);
        holder.tvTitle.setText(item.getCropName());
        holder.tvListingId.setText("Listing #" + item.getId());
        holder.tvYieldValue.setText(item.getQuantity());
        holder.tvExpectedValue.setText(item.getPrice());
        holder.tvLocation.setText(item.getLocation());
        holder.tvUpdatedTime.setText(item.getUpdatedTime());

        if (item.getImageResource() != 0) {
            holder.ivThumbnail.setImageResource(item.getImageResource());
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_leaf); // Default
        }

        // Reset Styles to Theme Defaults
        holder.card.setCardBackgroundColor(Color.WHITE);
        holder.ivThumbnail.setColorFilter(null);
        holder.tvReviewNote.setVisibility(View.GONE);
        holder.llActions.setVisibility(View.VISIBLE);
        holder.btnSecondary.setVisibility(View.GONE);
        
        // Default Primary Button: Outlined Light Green
        holder.btnPrimary.setIcon(null);
        holder.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1FFF1")));
        holder.btnPrimary.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        holder.btnPrimary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C8E6C9")));
        holder.btnPrimary.setStrokeWidth(1);

        // Default Secondary Button: Subtle Gray or Red depending on status
        holder.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        holder.btnSecondary.setTextColor(Color.parseColor("#616161"));
        holder.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        holder.btnSecondary.setStrokeWidth(1);

        switch (item.getStatus()) {
            case DRAFT:
                setBadge(holder.tvStatusBadge, "DRAFT", R.color.status_draft_bg, R.color.status_draft_text);
                holder.btnPrimary.setText("Edit Details");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                holder.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                break;

            case PENDING:
                setBadge(holder.tvStatusBadge, "PENDING", R.color.status_pending_bg, R.color.status_pending_text);
                holder.btnPrimary.setText("Edit Details");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                holder.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                
                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Deactivate");
                holder.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_lock));
                holder.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
                holder.btnSecondary.setTextColor(Color.parseColor("#D32F2F"));
                holder.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                break;

            case UNDER_REVIEW:
                setBadge(holder.tvStatusBadge, "UNDER REVIEW", R.color.status_review_bg, R.color.status_review_text);
                holder.tvReviewNote.setVisibility(View.VISIBLE);
                holder.llActions.setVisibility(View.GONE);
                break;

            case LISTED:
                setBadge(holder.tvStatusBadge, "LISTED", R.color.status_listed_bg, R.color.status_listed_text);
                holder.btnPrimary.setText("Edit Details");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                holder.btnPrimary.setOnClickListener(v -> listener.onEdit(item));

                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Deactivate");
                holder.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_eye));
                holder.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
                holder.btnSecondary.setTextColor(Color.parseColor("#D32F2F"));
                holder.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                break;

            case ONGOING:
                setBadge(holder.tvStatusBadge, "ONGOING", R.color.status_ongoing_bg, R.color.status_ongoing_text);
                holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_ongoing_card_tint));
                
                holder.btnPrimary.setText("Open Chat");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_send_chat));
                holder.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                holder.btnPrimary.setTextColor(Color.parseColor("#1976D2"));
                holder.btnPrimary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
                holder.btnPrimary.setOnClickListener(v -> listener.onChat(item));

                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Track Order");
                holder.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_lorry));
                holder.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                holder.btnSecondary.setTextColor(Color.WHITE);
                holder.btnSecondary.setStrokeColor(null);
                holder.btnSecondary.setOnClickListener(v -> listener.onTrack(item));
                break;

            case DEACTIVATED:
                setBadge(holder.tvStatusBadge, "DEACTIVATED", R.color.status_deactivated_bg, R.color.status_deactivated_text);
                applyGrayscale(holder.ivThumbnail);
                holder.btnPrimary.setText("Edit Details");
                holder.btnPrimary.setOnClickListener(v -> listener.onEdit(item));

                holder.btnSecondary.setVisibility(View.VISIBLE);
                holder.btnSecondary.setText("Re-activate");
                holder.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_clock));
                holder.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                holder.btnSecondary.setTextColor(Color.WHITE);
                holder.btnSecondary.setStrokeColor(null);
                break;

            case WANTED:
                setBadge(holder.tvStatusBadge, "WANTED", R.color.status_pending_bg, R.color.status_pending_text);
                holder.btnPrimary.setText("Send Supply Offer");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_supply));
                holder.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                holder.btnPrimary.setTextColor(Color.WHITE);
                holder.btnPrimary.setStrokeColor(null);
                holder.btnPrimary.setOnClickListener(v -> listener.onSupply(item));
                break;

            case COMPLETED:
                setBadge(holder.tvStatusBadge, "COMPLETED", R.color.status_listed_bg, R.color.status_listed_text);
                holder.btnPrimary.setText("View Details");
                holder.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_info));
                break;

            case CANCELLED:
                setBadge(holder.tvStatusBadge, "CANCELLED", R.color.status_deactivated_bg, R.color.status_deactivated_text);
                applyGrayscale(holder.ivThumbnail);
                holder.btnPrimary.setText("Re-list Item");
                holder.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                holder.btnPrimary.setTextColor(Color.WHITE);
                holder.btnPrimary.setStrokeColor(null);
                break;
        }
    }

    private void setBadge(TextView view, String text, int bgRes, int textRes) {
        view.setText(text);
        view.setBackgroundTintList(ContextCompat.getColorStateList(view.getContext(), bgRes));
        view.setTextColor(ContextCompat.getColor(view.getContext(), textRes));
    }

    private void applyGrayscale(ImageView iv) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        iv.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivThumbnail;
        TextView tvTitle, tvListingId, tvStatusBadge, tvYieldValue, tvExpectedValue;
        TextView tvLocation, tvUpdatedTime, tvReviewNote;
        LinearLayout llActions;
        MaterialButton btnPrimary, btnSecondary;

        ListingViewHolder(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.listingCard);
            ivThumbnail = v.findViewById(R.id.ivThumbnail);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvListingId = v.findViewById(R.id.tvListingId);
            tvStatusBadge = v.findViewById(R.id.tvStatusBadge);
            tvYieldValue = v.findViewById(R.id.tvYieldValue);
            tvExpectedValue = v.findViewById(R.id.tvExpectedValue);
            tvLocation = v.findViewById(R.id.tvLocation);
            tvUpdatedTime = v.findViewById(R.id.tvUpdatedTime);
            tvReviewNote = v.findViewById(R.id.tvReviewNote);
            llActions = v.findViewById(R.id.llActions);
            btnPrimary = v.findViewById(R.id.btnPrimary);
            btnSecondary = v.findViewById(R.id.btnSecondary);
        }
    }
}
