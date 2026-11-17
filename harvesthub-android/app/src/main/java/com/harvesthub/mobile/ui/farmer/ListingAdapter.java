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
        String idText = item.getReferenceCode() != null ? "#" + item.getReferenceCode() : item.getId();
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
        b.tvLocation.setText(shortPlace(item.getLocation()));
        b.tvCreatedDate.setText("Created: " + formatDate(item.getCreatedDate()));
        b.tvUpdatedTime.setText("Updated: " + formatAgo(item.getUpdatedTime() != null ? item.getUpdatedTime() : item.getCreatedDate()));

        // Thumbnail: the listing's own wide-angle photo from the server; crop picture only if there is none.
        // Tap it to see all captured media (2 photos + video).
        String photo = item.getCoverPhotoUrl();
        b.ivThumbnail.setOnClickListener(item.getMediaUrls() == null || item.getMediaUrls().isEmpty() ? null : v ->
                com.harvesthub.mobile.ui.common.MediaViewerActivity.open(v.getContext(), item.getMediaUrls(), item.getCropName(), 0));
        b.ivThumbnail.setColorFilter(null);
        if (photo != null) {
            b.ivThumbnail.setBackgroundColor(Color.parseColor("#F1F5F9"));
            b.ivThumbnail.setPadding(0, 0, 0, 0);
            b.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            com.bumptech.glide.Glide.with(b.ivThumbnail)
                    .load(mediaUrl(ListingItem.thumbUrl(photo, 160)))   // few-KB thumbnail, not the 1 MB photo
                    .placeholder(getCropDrawable(item.getCropName()))
                    .error(getCropDrawable(item.getCropName()))
                    .centerCrop()
                    .into(b.ivThumbnail);
        } else {
            com.bumptech.glide.Glide.with(b.ivThumbnail).clear(b.ivThumbnail);
            int imageRes = item.getImageResource() != 0 ? item.getImageResource() : getCropDrawable(item.getCropName());
            boolean icon = imageRes == R.drawable.ic_leaf_outline;
            int pad = icon ? (int) (10 * holder.itemView.getResources().getDisplayMetrics().density) : 0;
            b.ivThumbnail.setBackgroundColor(icon ? Color.parseColor("#F1F5F9") : Color.TRANSPARENT);
            b.ivThumbnail.setPadding(pad, pad, pad, pad);
            b.ivThumbnail.setScaleType(icon ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
            b.ivThumbnail.setImageResource(imageRes);
        }

        // Reset Styles to Default (White card, light gray stroke)
        b.listingCard.setCardBackgroundColor(Color.WHITE);
        b.listingCard.setStrokeColor(Color.parseColor("#E2E8F0"));
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

        // A buyer is negotiating on this lot (before the deal is accepted): show it like Ongoing, with Chat.
        if (item.getNegotiationOrderId() != null && status != ListingItem.Status.ONGOING) {
            setBadge(b.tvStatusBadge, item.getNegotiationCount() > 1 ? "IN NEGOTIATION · " + item.getNegotiationCount() : "IN NEGOTIATION",
                    Color.parseColor("#2563EB"), Color.WHITE);
            b.listingCard.setCardBackgroundColor(Color.parseColor("#F0F7FF"));
            b.listingCard.setStrokeColor(Color.parseColor("#BFDBFE"));
            b.tvReviewNote.setVisibility(View.VISIBLE);
            b.tvReviewNote.setText((item.getNegotiationBuyerName() != null ? item.getNegotiationBuyerName() : "A buyer")
                    + " wants this lot. Reply in the chat to agree on the price and quantity."
                    + (item.getNegotiationCount() > 1 ? " (" + item.getNegotiationCount() + " buyers are negotiating; the newest is shown.)" : ""));
            styleChatTrack(holder, b, item);
            return;
        }

        switch (status) {
            case DRAFT:
                setBadge(b.tvStatusBadge, "DRAFT", Color.parseColor("#50667F"), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                break;

            case PENDING:   // design: Edit + Deactivate
                setBadge(b.tvStatusBadge, "PENDING", Color.parseColor("#D97706"), Color.WHITE);
                if (item.isCropPending()) {
                    b.tvReviewNote.setVisibility(View.VISIBLE);
                    b.tvReviewNote.setText("The new crop type will be checked by the officer with this listing.");
                }
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case UNDER_REVIEW:
                setBadge(b.tvStatusBadge, "UNDER REVIEW", Color.parseColor("#E2E8F0"), Color.parseColor("#475569"));
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.tvReviewNote.setText("Our team is verifying your listing details.");
                b.llActions.setVisibility(View.GONE);
                break;

            case ACTIVE:
            case LISTED:
            case APPROVED:   // design: Edit + Deactivate
                setBadge(b.tvStatusBadge, item.getGrade() != null ? "LISTED · GRADE " + item.getGrade() : "LISTED",
                        Color.parseColor("#16A34A"), Color.WHITE);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setOnClickListener(v -> listener.onDeactivate(item));
                break;

            case DEACTIVATED:   // design: grey photo, Edit + filled Re-activate
                setBadge(b.tvStatusBadge, "DEACTIVATED", Color.parseColor("#50667F"), Color.WHITE);
                applyGrayscale(b.ivThumbnail);
                b.btnPrimary.setOnClickListener(v -> listener.onEdit(item));
                b.btnSecondary.setText("Re-activate");
                b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_refresh));
                b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1B4332")));
                b.btnSecondary.setTextColor(Color.WHITE);
                b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnSecondary.setStrokeWidth(0);
                b.btnSecondary.setOnClickListener(v -> listener.onReactivate(item));
                break;

            case RETAKE_REQUESTED:
                setBadge(b.tvStatusBadge, "ACTION REQUIRED", Color.parseColor("#E11D48"), Color.WHITE);
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.tvReviewNote.setText("Retake requested: " + (item.getReviewNotes() != null && !item.getReviewNotes().isEmpty()
                        ? item.getReviewNotes() : "please capture clearer photos."));
                b.btnPrimary.setText("Retake Photos");
                b.btnPrimary.setOnClickListener(v -> listener.onAction(item));
                b.btnSecondary.setVisibility(View.GONE);
                break;

            case REJECTED:
                setBadge(b.tvStatusBadge, "REJECTED", Color.parseColor("#EF4444"), Color.WHITE);
                b.tvReviewNote.setVisibility(View.VISIBLE);
                b.tvReviewNote.setText("Rejected: " + (item.getReviewNotes() != null && !item.getReviewNotes().isEmpty()
                        ? item.getReviewNotes() : "does not meet marketplace standards."));
                b.llActions.setVisibility(View.GONE);
                break;

            case ONGOING:
                setBadge(b.tvStatusBadge, "ONGOING", Color.parseColor("#2563EB"), Color.WHITE);
                b.listingCard.setCardBackgroundColor(Color.parseColor("#F0F7FF"));
                b.listingCard.setStrokeColor(Color.parseColor("#BFDBFE"));
                styleChatTrack(holder, b, item);
                break;

            case WANTED:
                setBadge(b.tvStatusBadge, item.isUrgent() ? "URGENT" : "WANTED", Color.parseColor("#D97706"), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setText("Supply Offer");
                b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#16A34A")));
                b.btnPrimary.setTextColor(Color.WHITE);
                b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.WHITE));
                b.btnPrimary.setStrokeWidth(0);
                b.btnPrimary.setOnClickListener(v -> listener.onSupply(item));
                break;

            case COMPLETED:
                setBadge(b.tvStatusBadge, "COMPLETED", Color.parseColor("#16A34A"), Color.WHITE);
                b.btnSecondary.setVisibility(View.GONE);
                b.btnPrimary.setText("View Details");
                b.btnPrimary.setOnClickListener(v -> listener.onTrack(item));
                break;

            default:
                setBadge(b.tvStatusBadge, status.name().replace('_', ' '), Color.parseColor("#50667F"), Color.WHITE);
                b.llActions.setVisibility(View.GONE);
                break;
        }
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /** Server times are ISO local date-times in Sri Lanka time, e.g. "2026-11-16T22:27:43". */
    private static java.util.Calendar parse(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try {
            String[] d = iso.substring(0, 10).split("-");
            java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Colombo"));
            c.clear();
            c.set(Integer.parseInt(d[0]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[2]));
            if (iso.length() >= 16 && iso.charAt(10) == 'T') {
                c.set(java.util.Calendar.HOUR_OF_DAY, Integer.parseInt(iso.substring(11, 13)));
                c.set(java.util.Calendar.MINUTE, Integer.parseInt(iso.substring(14, 16)));
            }
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    /** "Nov 16" (adds the year when it is not this year). */
    static String formatDate(String iso) {
        java.util.Calendar c = parse(iso);
        if (c == null) return "\u2014";
        String s = MONTHS[c.get(java.util.Calendar.MONTH)] + " " + c.get(java.util.Calendar.DAY_OF_MONTH);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        return c.get(java.util.Calendar.YEAR) == year ? s : s + ", " + c.get(java.util.Calendar.YEAR);
    }

    /** "Just now", "15m ago", "3h ago", "Yesterday", "4 days ago", then the date. */
    static String formatAgo(String iso) {
        java.util.Calendar c = parse(iso);
        if (c == null) return "\u2014";
        long mins = (System.currentTimeMillis() - c.getTimeInMillis()) / 60_000L;
        if (mins < 1) return "Just now";
        if (mins < 60) return mins + "m ago";
        if (mins < 24 * 60) return (mins / 60) + "h ago";
        long days = mins / (24 * 60);
        if (days == 1) return "Yesterday";
        if (days < 7) return days + " days ago";
        return formatDate(iso);
    }

    /** "Senate Building, Mihintale 50300, Sri Lanka" -> "Mihintale" (the town, like the sector name in the design). */
    static String shortPlace(String address) {
        if (address == null || address.trim().isEmpty()) return "Sri Lanka";
        if (address.trim().startsWith("Lat ")) return "GPS pinned";
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String p : address.split(",")) {
            String t = p.trim().replaceAll("\\s*\\d{5}$", "").trim();   // drop the postcode
            if (!t.isEmpty() && !t.equalsIgnoreCase("Sri Lanka") && !t.matches("^[0-9A-Z]{4,8}\\+[0-9A-Z]{2,3}$")) parts.add(t);
        }
        return parts.isEmpty() ? "Sri Lanka" : parts.get(parts.size() - 1);
    }

    /** "/uploads/x.jpg" -> full URL on the API server. */
    static String mediaUrl(String path) {
        if (path.startsWith("http")) return path;
        String base = com.harvesthub.mobile.BuildConfig.API_BASE_URL;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + (path.startsWith("/") ? path : "/" + path);
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

    /** Design "Ongoing": solid blue Chat + outlined blue Track. */
    private void styleChatTrack(ListingViewHolder holder, ItemListingVariantBinding b, ListingItem item) {
        b.llActions.setVisibility(View.VISIBLE);
        b.btnSecondary.setVisibility(View.VISIBLE);
        b.btnPrimary.setText("Chat");
        b.btnPrimary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_chat_bubble_outline));
        b.btnPrimary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        b.btnPrimary.setTextColor(Color.WHITE);
        b.btnPrimary.setIconTint(ColorStateList.valueOf(Color.WHITE));
        b.btnPrimary.setStrokeWidth(0);
        b.btnPrimary.setOnClickListener(v -> listener.onChat(item));
        b.btnSecondary.setText("Track");
        b.btnSecondary.setIcon(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_truck));
        b.btnSecondary.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EFF6FF")));
        b.btnSecondary.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#93C5FD")));
        b.btnSecondary.setTextColor(Color.parseColor("#2563EB"));
        b.btnSecondary.setIconTint(ColorStateList.valueOf(Color.parseColor("#2563EB")));
        b.btnSecondary.setStrokeWidth((int) (1 * holder.itemView.getResources().getDisplayMetrics().density));
        b.btnSecondary.setOnClickListener(v -> listener.onTrack(item));
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