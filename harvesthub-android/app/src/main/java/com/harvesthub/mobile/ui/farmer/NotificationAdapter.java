package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.NotificationItem;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> notifications;
    private final OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onMarkAsRead(NotificationItem notification, int position);
    }

    public NotificationAdapter(List<NotificationItem> notifications, OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);

        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTimestamp.setText(item.getTimestamp());

        // Read/Unread styling
        if (item.isRead()) {
            holder.cardNotification.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.notif_item_bg));
            holder.tvMarkAsRead.setVisibility(View.GONE);
            holder.cardNotification.setAlpha(0.6f);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.notif_item_stroke));
        } else {
            holder.cardNotification.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.notif_item_unread_bg));
            holder.tvMarkAsRead.setVisibility(View.VISIBLE);
            holder.cardNotification.setAlpha(1.0f);
            holder.cardNotification.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_green));
        }

        // Type-based icon styling
        setupIcon(holder, item.getType());

        holder.tvMarkAsRead.setOnClickListener(v -> listener.onMarkAsRead(item, position));
    }

    private void setupIcon(NotificationViewHolder holder, String type) {
        int iconRes;
        int bgColor;
        int iconTint;

        switch (type) {
            case "DELIVERY_SUCCESS":
                iconRes = R.drawable.ic_check_circle;
                bgColor = Color.parseColor("#E8F5E9");
                iconTint = Color.parseColor("#13A34A");
                break;
            case "PENALTY":
                iconRes = R.drawable.ic_warning;
                bgColor = Color.parseColor("#FFEBEE");
                iconTint = Color.parseColor("#D32F2F");
                break;
            case "LISTING_APPROVED":
                iconRes = R.drawable.ic_description;
                bgColor = Color.parseColor("#E3F2FD");
                iconTint = Color.parseColor("#1976D2");
                break;
            default:
                iconRes = R.drawable.ic_bell;
                bgColor = Color.parseColor("#F5F5F5");
                iconTint = Color.parseColor("#757575");
                break;
        }

        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(bgColor));
        holder.ivIcon.setImageTintList(ColorStateList.valueOf(iconTint));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardNotification;
        ShapeableImageView ivIcon;
        TextView tvTitle, tvBody, tvTimestamp, tvMarkAsRead;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMarkAsRead = itemView.findViewById(R.id.tvMarkAsRead);
        }
    }
}
