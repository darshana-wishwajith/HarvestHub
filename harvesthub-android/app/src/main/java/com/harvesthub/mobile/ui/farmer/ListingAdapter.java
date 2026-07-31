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
        b.tvUpdatedTime.setText(item.getUpdatedTime());

        if (item.getImageResource() != 0) {
            b.ivThumbnail.setImageResource(item.getImageResource());
        } else {
            b.ivThumbnail.setImageResource(R.drawable.ic_leaf); // Default
        }

        // Reset Styles to Theme Defaults
        b.listingCard.setCardBackgroundColor(Color.WHITE);
        b.ivThumbnail.setColorFilter(null);
        b.tvReviewNote.setVisibility(View.GONE);
        b.llActions.setVisibility(View.VISIBLE);
        b.btnSecondary.setVisibility(View.GONE);
        
        // Default Primary Button: Outlined Light Green
        b.btnPrimary.setIcon(null);
        b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F1FFF1")));
        b.btnPrimary.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green));
        b.btnPrimary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#C8E6C9")));
        b.btnPrimary.setStrokeWidth(1);

        // Default Secondary Button: Subtle Gray or Red depending on status
        b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        b.btnSecondary.setTextColor(Color.parseColor("#616161"));
        b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        b.btnSecondary.setStrokeWidth(1);

        switch (item.getStatus()) {
            case DRAFT:
                setBadge(b.tvStatusBadge, "DRAFT", R.color.status_draft_bg, R.color.status_draft_text);
                b.btnPrimary.setText("Edit Details");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                break;

            case PENDING:
                setBadge(b.tvStatusBadge, "PENDING", R.color.status_pending_bg, R.color.status_pending_text);
                b.btnPrimary.setText("Edit Details");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                
                b.btnSecondary.setVisibility(View.VISIBLE);
                b.btnSecondary.setText("Deactivate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_lock));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
                b.btnSecondary.setTextColor(Color.parseColor("#D32F2F"));
                b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                break;

            case UNDER_REVIEW:
                setBadge(b.tvStatusBadge, "UNDER REVIEW", R.color.status_review_bg, R.color.status_review_text);
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.llActions.setVisibility(View.GONE);
                break;

            case LISTED:
                setBadge(b.tvStatusBadge, "LISTED", R.color.status_listed_bg, R.color.status_listed_text);
                b.btnPrimary.setText("Edit Details");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));

                b.btnSecondary.setVisibility(View.VISIBLE);
                b.btnSecondary.setText("Deactivate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_eye));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF5F5")));
                b.btnSecondary.setTextColor(Color.parseColor("#D32F2F"));
                b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#FFEBEE")));
                break;

            case ONGOING:
                setBadge(b.tvStatusBadge, "ONGOING", R.color.status_ongoing_bg, R.color.status_ongoing_text);
                b.listingCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_ongoing_card_tint));
                
                b.btnPrimary.setText("Open Chat");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_send_chat));
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E3F2FD")));
                b.btnPrimary.setTextColor(Color.parseColor("#1976D2"));
                b.btnPrimary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#BBDEFB")));
                b.btnPrimary.setOnClickListener(v -> listener.onChat(item));

                b.btnSecondary.setVisibility(View.VISIBLE);
                b.btnSecondary.setText("Track Order");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_lorry));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                b.btnSecondary.setTextColor(Color.WHITE);
                b.btnSecondary.setStrokeColor(null);
                b.btnSecondary.setOnClickListener(v -> listener.onTrack(item));
                break;

            case DEACTIVATED:
                setBadge(b.tvStatusBadge, "DEACTIVATED", R.color.status_deactivated_bg, R.color.status_deactivated_text);
                applyGrayscale(b.ivThumbnail);
                b.btnPrimary.setText("Edit Details");
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));

                b.btnSecondary.setVisibility(View.VISIBLE);
                b.btnSecondary.setText("Re-activate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_clock));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                b.btnSecondary.setTextColor(Color.WHITE);
                b.btnSecondary.setStrokeColor(null);
                break;

            case WANTED:
                setBadge(b.tvStatusBadge, "WANTED", R.color.status_pending_bg, R.color.status_pending_text);
                b.btnPrimary.setText("Send Supply Offer");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_supply));
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setStrokeColor(null);
                b.btnPrimary.setOnClickListener(v -> listener.onSupply(item));
                break;

            case COMPLETED:
                setBadge(b.tvStatusBadge, "COMPLETED", R.color.status_listed_bg, R.color.status_listed_text);
                b.btnPrimary.setText("View Details");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_info));
                break;

            case CANCELLED:
                setBadge(b.tvStatusBadge, "CANCELLED", R.color.status_deactivated_bg, R.color.status_deactivated_text);
                applyGrayscale(b.ivThumbnail);
                b.btnPrimary.setText("Re-list Item");
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_green)));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setStrokeColor(null);
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
        ItemListingVariantBinding binding;

        ListingViewHolder(@NonNull ItemListingVariantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
