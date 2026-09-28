package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class SupplyOfferAdapter extends RecyclerView.Adapter<SupplyOfferAdapter.OfferViewHolder> {

    public interface OnOfferClickListener {
        void onOfferClick(SupplyOfferItem item);
    }

    private List<SupplyOfferItem> items;
    private OnOfferClickListener onOfferClick;

    public SupplyOfferAdapter(List<SupplyOfferItem> items, OnOfferClickListener onOfferClick) {
        this.items = items;
        this.onOfferClick = onOfferClick;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_supply_offer_card, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        SupplyOfferItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvGradeBadge.setText(item.getGradeText());
        holder.tvGradeBadge.setBackground(ContextCompat.getDrawable(context, item.getGradeBadgeBgRes()));

        if (item.isHasCheckmark()) {
            Drawable checkDrawable = ContextCompat.getDrawable(context, R.drawable.ic_check);
            holder.tvGradeBadge.setCompoundDrawablesWithIntrinsicBounds(checkDrawable, null, null, null);
        } else {
            holder.tvGradeBadge.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        holder.tvOfferTime.setText(item.getOfferTime());
        holder.tvFarmerName.setText(item.getFarmerName());
        holder.tvLocation.setText(item.getLocation());
        holder.tvEstimatedQty.setText(item.getEstimatedQty());
        holder.tvOfferedPrice.setText(item.getOfferedPrice());

        holder.btnViewChat.setOnClickListener(v -> {
            if (onOfferClick != null) onOfferClick.onOfferClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        TextView tvGradeBadge;
        TextView tvOfferTime;
        TextView tvFarmerName;
        TextView tvLocation;
        TextView tvEstimatedQty;
        TextView tvOfferedPrice;
        MaterialButton btnViewChat;

        OfferViewHolder(View view) {
            super(view);
            tvGradeBadge = view.findViewById(R.id.tvGradeBadge);
            tvOfferTime = view.findViewById(R.id.tvOfferTime);
            tvFarmerName = view.findViewById(R.id.tvFarmerName);
            tvLocation = view.findViewById(R.id.tvLocation);
            tvEstimatedQty = view.findViewById(R.id.tvEstimatedQty);
            tvOfferedPrice = view.findViewById(R.id.tvOfferedPrice);
            btnViewChat = view.findViewById(R.id.btnViewChat);
        }
    }
}

