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
        String idText = item.getId();
        if (idText != null && idText.length() > 8 && !idText.contains("-") && !idText.contains(" ")) {
            idText = idText.substring(0, 8).toUpperCase();
        }
        if (idText != null && !idText.startsWith("#") && !idText.startsWith("Listing #")) {
            b.tvListingId.setText("Listing #" + idText);
        } else if (idText != null && idText.startsWith("#")) {
            b.tvListingId.setText("Listing " + idText);
        } else {
            b.tvListingId.setText(idText != null ? idText : "Listing #");
        }

        String qty = item.getQuantity() != null ? item.getQuantity() : "0 KG";
        if (!qty.toUpperCase().contains("KG")) {
            qty = qty + " KG";
        }
        b.tvYieldValue.setText(qty);

        String pr = item.getPrice() != null ? item.getPrice() : "LKR 0/kg";
        if (!pr.toUpperCase().contains("LKR") && !pr.contains("Rs") && !pr.contains("rs")) {
            pr = "LKR " + pr + "/kg";
        }
        b.tvExpectedValue.setText(pr);
        b.tvLocation.setText(item.getLocation() != null ? item.getLocation() : "Sri Lanka");

        String cDate = formatRelativeDate(item.getCreatedDate());
        b.tvCreatedDate.setText(cDate.startsWith("Created:") ? cDate : "Created: " + cDate);

        String uTime = formatRelativeTime(item.getUpdatedTime());
        b.tvUpdatedTime.setText(uTime.startsWith("Updated:") ? uTime : "Updated: " + uTime);

        // Crop Thumbnail
        int imageRes = item.getImageResource();
        if (imageRes == 0) {
            imageRes = getCropDrawable(item.getCropName());
        }

        if (imageRes == R.drawable.ic_leaf_outline) {
            b.ivThumbnail.setBackgroundColor(Color.parseColor("#F1F5F9"));
            int pad = (int) (10 * holder.itemView.getResources().getDisplayMetrics().density);
            b.ivThumbnail.setPadding(pad, pad, pad, pad);
            b.ivThumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
            b.ivThumbnail.setImageResource(imageRes);
        } else {
            b.ivThumbnail.setBackgroundColor(Color.TRANSPARENT);
            b.ivThumbnail.setPadding(0, 0, 0, 0);
            b.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            b.ivThumbnail.setImageResource(imageRes);
        }

        // Reset Styles to Default (White card, light gray stroke)
        b.listingCard.setCardBackgroundColor(Color.WHITE);
        b.listingCard.setStrokeColor(Color.parseColor("#E2E8F0"));
        b.ivThumbnail.setColorFilter(null);
        b.tvReviewNote.setVisibility(View.GONE);
        b.llActions.setVisibility(View.VISIBLE);
        b.btnSecondary.setVisibility(View.VISIBLE);

        // Default Edit button style
        b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_edit_chat));
        b.btnPrimary.setText("Edit");
        b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.btnPrimary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
        b.btnPrimary.setStrokeWidth((int) (1 * holder.itemView.getResources().getDisplayMetrics().density));
        b.btnPrimary.setTextColor(Color.parseColor("#1E293B"));
        b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.parseColor("#1E293B")));

        // Default Deactivate button style
        b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_circle_slash));
        b.btnSecondary.setText("Deactivate");
        b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
        b.btnSecondary.setStrokeWidth((int) (1 * holder.itemView.getResources().getDisplayMetrics().density));
        b.btnSecondary.setTextColor(Color.parseColor("#64748B"));
        b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.parseColor("#64748B")));

        ListingItem.Status status = item.getStatus() != null ? item.getStatus() : ListingItem.Status.LISTED;

        switch (status) {
            case DRAFT:
                setBadge(b.tvStatusBadge, "DRAFT", Color.parseColor("#50667F"), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                break;

            case PENDING:
                setBadge(b.tvStatusBadge, "PENDING", Color.parseColor("#D97706"), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case UNDER_REVIEW:
                setBadge(b.tvStatusBadge, "UNDER REVIEW", Color.parseColor("#E2E8F0"), Color.parseColor("#475569"));
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.llActions.setVisibility(View.GONE);
                break;

            case ACTIVE:
            case LISTED:
            case APPROVED:
                setBadge(b.tvStatusBadge, "LISTED", Color.parseColor("#16A34A"), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case RETAKE_REQUESTED:
                setBadge(b.tvStatusBadge, "RETAKE", Color.parseColor("#E11D48"), Color.WHITE);
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.tvReviewNote.setText("Media retake requested by officer");
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setVisibility(View.GONE);
                break;

            case REJECTED:
                setBadge(b.tvStatusBadge, "REJECTED", Color.parseColor("#EF4444"), Color.WHITE);
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.tvReviewNote.setText("Listing was rejected by officer");
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setVisibility(View.GONE);
                break;

            case ONGOING:
                setBadge(b.tvStatusBadge, "ONGOING", Color.parseColor("#2563EB"), Color.WHITE);
                b.listingCard.setCardBackgroundColor(Color.parseColor("#F0F7FF"));
                b.listingCard.setStrokeColor(Color.parseColor("#BFDBFE"));

                // Chat Button (Solid Blue)
                b.btnPrimary.setText("Chat");
                b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_chat_bubble_outline));
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnPrimary.setStrokeWidth(0);
                b.btnPrimary.setOnClickListener(v -> listener.onChat(item));

                // Track Button (Light Blue with Stroke)
                b.btnSecondary.setText("Track");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_truck));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
                b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#93C5FD")));
                b.btnSecondary.setTextColor(Color.parseColor("#2563EB"));
                b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.parseColor("#2563EB")));
                b.btnSecondary.setStrokeWidth((int) (1 * holder.itemView.getResources().getDisplayMetrics().density));
                b.btnSecondary.setOnClickListener(v -> listener.onTrack(item));
                break;

            case DEACTIVATED:
                setBadge(b.tvStatusBadge, "DEACTIVATED", Color.parseColor("#5A6B82"), Color.WHITE);
                applyGrayscale(b.ivThumbnail);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));

                // Re-activate Button (Solid Dark Forest Green)
                b.btnSecondary.setText("Re-activate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_rotate_refresh));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#082F1E")));
                b.btnSecondary.setTextColor(Color.WHITE);
                b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnSecondary.setStrokeWidth(0);
                b.btnSecondary.setOnClickListener(v -> listener.onReactivate(item));
                break;

            case WANTED:
                setBadge(b.tvStatusBadge, "WANTED", Color.parseColor("#D97706"), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setText("Supply Offer");
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnPrimary.setStrokeWidth(0);
                b.btnPrimary.setOnClickListener(v -> listener.onSupply(item));
                break;

            default:
                setBadge(b.tvStatusBadge, status.name(), Color.parseColor("#50667F"), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;
        }
    }

    private String formatRelativeDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Oct 24";
        if (dateStr.contains("T")) {
            try {
                String[] parts = dateStr.split("T")[0].split("-");
                if (parts.length == 3) {
                    int month = Integer.parseInt(parts[1]);
                    String[] months = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    return (month >= 1 && month <= 12 ? months[month] : "Oct") + " " + Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {}
        }
        return dateStr;
    }

    private String formatRelativeTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return "Yesterday";
        if (timeStr.contains("T")) {
            return "2h ago";
        }
        return timeStr;
    }

    public static int getCropDrawable(String cropName) {
        if (cropName == null) return R.drawable.ic_leaf_outline;
        String lower = cropName.toLowerCase();
        if (lower.contains("tomato")) return R.drawable.img_crop_tomatoes;
        if (lower.contains("onion")) return R.drawable.img_crop_onions;
        if (lower.contains("carrot")) return R.drawable.img_crop_carrots;
        if (lower.contains("potato")) return R.drawable.img_crop_potatoes;
        if (lower.contains("leek")) return R.drawable.img_crop_leeks;
        return R.drawable.ic_leaf_outline;
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