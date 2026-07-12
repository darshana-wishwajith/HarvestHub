package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ListingItem;

import java.util.List;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {

    private final List<ListingItem> items;
    private final OnListingActionListener listener;

    public interface OnListingActionListener {
        void onAction(ListingItem item);
    }

    public ListingAdapter(List<ListingItem> items, OnListingActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_listing, parent, false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        ListingItem item = items.get(position);
        holder.tvCropName.setText(item.getCropName());
        holder.tvLocation.setText(item.getLocation());
        holder.tvValueLeft.setText(item.getQuantity());
        holder.tvValueRight.setText(item.getPrice());

        if (item.getType().equals("WANTED")) {
            holder.tvLabelLeft.setText("Required");
            holder.tvLabelRight.setText("Target");
            holder.btnAction.setText("Send Offer");
            holder.btnAction.setVisibility(View.VISIBLE);
            if (item.isUrgent()) {
                holder.tvStatusBadge.setText("URGENT");
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E53935")));
                holder.tvStatusBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvStatusBadge.setVisibility(View.GONE);
            }
        } else if (item.getType().equals("ACTIVE")) {
            holder.tvLabelLeft.setText("Listed");
            holder.tvLabelRight.setText("Price");
            holder.btnAction.setText("View Details");
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText("ACTIVE");
            holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#13A34A")));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
        } else { // HISTORY
            holder.tvLabelLeft.setText("Sold");
            holder.tvLabelRight.setText("Final Price");
            holder.btnAction.setText("View Summary");
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText(item.getStatus().toUpperCase());
            if (item.getStatus().equalsIgnoreCase("Completed")) {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#78909C")));
            } else {
                holder.tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#828282")));
            }
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
        }

        holder.btnAction.setOnClickListener(v -> listener.onAction(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        TextView tvCropName, tvLocation, tvLabelLeft, tvValueLeft, tvLabelRight, tvValueRight, tvStatusBadge;
        MaterialButton btnAction;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvLabelLeft = itemView.findViewById(R.id.tvLabelLeft);
            tvValueLeft = itemView.findViewById(R.id.tvValueLeft);
            tvLabelRight = itemView.findViewById(R.id.tvLabelRight);
            tvValueRight = itemView.findViewById(R.id.tvValueRight);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
