package com.harvesthub.mobile.ui.farmer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.HistoryListing;

import java.util.List;

public class HistoryListingAdapter extends RecyclerView.Adapter<HistoryListingAdapter.ViewHolder> {

    private final List<HistoryListing> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryListing item);
    }

    public HistoryListingAdapter(List<HistoryListing> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryListing item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCropName, tvStatusBadge, tvSecondRowText, tvLabelLeft, tvValueLeft, tvLabelRight, tvValueRight;
        ImageView ivSecondRowIcon;
        MaterialButton btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCropName = itemView.findViewById(R.id.tvCropName);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvSecondRowText = itemView.findViewById(R.id.tvSecondRowText);
            tvLabelLeft = itemView.findViewById(R.id.tvLabelLeft);
            tvValueLeft = itemView.findViewById(R.id.tvValueLeft);
            tvLabelRight = itemView.findViewById(R.id.tvLabelRight);
            tvValueRight = itemView.findViewById(R.id.tvValueRight);
            ivSecondRowIcon = itemView.findViewById(R.id.ivSecondRowIcon);
            btnAction = itemView.findViewById(R.id.btnAction);
        }

        public void bind(HistoryListing item, OnItemClickListener listener) {
            tvCropName.setText(item.getCropName());
            tvStatusBadge.setText(item.getStatus());

            if ("DELIVERED".equalsIgnoreCase(item.getStatus())) {
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2D6A4F")));
                ivSecondRowIcon.setImageResource(R.drawable.ic_calendar);
                tvSecondRowText.setText(item.getDate());
                tvLabelLeft.setText("Quantity");
                tvValueLeft.setText(item.getQuantityKg() + " KG");
                tvLabelRight.setText("Total Value");
                tvValueRight.setText(item.getTotalValue());
                btnAction.setText("View Receipt");
            } else if ("CLOSED".equalsIgnoreCase(item.getStatus())) {
                tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                ivSecondRowIcon.setImageResource(R.drawable.ic_person);
                tvSecondRowText.setText("Buyer: " + item.getBuyerName());
                tvLabelLeft.setText("Completed");
                tvValueLeft.setText(item.getDate());
                tvLabelRight.setText("Final Price");
                tvValueRight.setText(item.getFinalPricePerKg() + "/kg");
                btnAction.setText("View Details");
            }

            btnAction.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
