package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ItemListingVariantBinding;
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
        default void onDeactivate(ListingItem item) {}
        default void onReactivate(ListingItem item) {}
        default void onSupply(ListingItem item) {}
    }

    public ListingAdapter(List<ListingItem> items, OnListingActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListingVariantBinding binding = ItemListingVariantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ListingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        ListingItem item = items.get(position);
        ItemListingVariantBinding b = holder.binding;
        
        b.tvTitle.setText(item.getCropName());
        b.tvListingId.setText("Listing #" + item.getId());
        b.tvYieldValue.setText(item.getQuantity());
        b.tvExpectedValue.setText(item.getPrice());
        b.tvLocation.setText(item.getLocation());
        b.tvCreatedDate.setText("Created: " + item.getCreatedDate());
        b.tvUpdatedTime.setText("Updated: " + item.getUpdatedTime());

        if (item.getImageResource() != 0) {
            b.ivThumbnail.setImageResource(item.getImageResource());
        } else {
            b.ivThumbnail.setImageResource(R.drawable.ic_leaf);
        }

        // Reset Styles
        b.listingCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stat_card_bg));
        b.listingCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stat_card_stroke));
        b.ivThumbnail.setColorFilter(null);
        b.tvReviewNote.setVisibility(View.GONE);
        b.llActions.setVisibility(View.VISIBLE);
        b.btnSecondary.setVisibility(View.VISIBLE);
        
        // Default Button Styles (Edit style)
        b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
        b.btnPrimary.setText("Edit");
        b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.btnPrimary.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.border_gray)));
        b.btnPrimary.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.stat_text_value));
        b.btnPrimary.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.stat_text_value)));

        // Default Secondary Button (Deactivate style)
        b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_visibility_off));
        b.btnSecondary.setText("Deactivate");
        b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.btnSecondary.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_deactivate_stroke)));
        b.btnSecondary.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_deactivate_text));
        b.btnSecondary.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_deactivate_text)));

        switch (item.getStatus()) {
            case DRAFT:
                setBadge(b.tvStatusBadge, "DRAFT", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_draft_pill), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                break;

            case PENDING:
                setBadge(b.tvStatusBadge, "PENDING", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_pending_pill), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case UNDER_REVIEW:
                setBadge(b.tvStatusBadge, "UNDER REVIEW", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_review_pill), ContextCompat.getColor(holder.itemView.getContext(), R.color.status_review_text));
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.llActions.setVisibility(View.GONE);
                break;

            case LISTED:
                setBadge(b.tvStatusBadge, "LISTED", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_listed_pill), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case ONGOING:
                setBadge(b.tvStatusBadge, "ONGOING", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_ongoing_pill), Color.WHITE);
                b.listingCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_ongoing_bg));
                b.listingCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_ongoing_stroke));
                
                // Chat Button
                b.btnPrimary.setText("Chat");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_chat_bubble_outline));
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_chat_blue)));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnPrimary.setStrokeWidth(0);
                b.btnPrimary.setOnClickListener(v -> listener.onChat(item));

                // Track Button
                b.btnSecondary.setText("Track");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_truck));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_track_bg)));
                b.btnSecondary.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_track_text)));
                b.btnSecondary.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_track_text));
                b.btnSecondary.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.listing_track_text)));
                b.btnSecondary.setOnClickListener(v -> listener.onTrack(item));
                break;

            case DEACTIVATED:
                setBadge(b.tvStatusBadge, "DEACTIVATED", ContextCompat.getColor(holder.itemView.getContext(), R.color.status_deactivated_pill), Color.WHITE);
                applyGrayscale(b.ivThumbnail);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                
                b.btnSecondary.setText("Re-activate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_clock));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.btn_reactivate_bg)));
                b.btnSecondary.setTextColor(Color.WHITE);
                b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnSecondary.setStrokeWidth(0);
                b.btnSecondary.setOnClickListener(v -> listener.onReactivate(item));
                break;
        }
    }

    private void setBadge(TextView view, String text, int bgColor, int textColor) {
        view.setText(text);
        view.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        view.setTextColor(textColor);
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
        ItemListingVariantBinding binding;

        ListingViewHolder(@NonNull ItemListingVariantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
