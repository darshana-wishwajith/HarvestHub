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

public class HarvestAdapter extends RecyclerView.Adapter<HarvestAdapter.HarvestViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(HarvestItem item);
    }

    private List<HarvestItem> harvestList;
    private OnItemClickListener listener;

    public HarvestAdapter(List<HarvestItem> harvestList, OnItemClickListener listener) {
        this.harvestList = harvestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HarvestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_harvest_card, parent, false);
        return new HarvestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HarvestViewHolder holder, int position) {
        HarvestItem item = harvestList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCropName.setText(item.getCropName());
        holder.tvEstimatedQty.setText("Estimated: " + item.getQuantity());
        holder.tvLocation.setText("Location: " + item.getLocation());
        holder.tvPrice.setText(item.getUnitPrice());
        holder.tvListedTime.setText(item.getListedTime());

        holder.tvBadge.setText(item.getGradeText());
        holder.tvBadge.setBackground(ContextCompat.getDrawable(context, item.getBadgeBgRes()));

        Drawable icon = ContextCompat.getDrawable(context, item.getBadgeIconRes());
        holder.tvBadge.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        holder.btnPurchaseRequest.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return harvestList.size();
    }

    static class HarvestViewHolder extends RecyclerView.ViewHolder {
        TextView tvBadge;
        TextView tvListedTime;
        TextView tvCropName;
        TextView tvEstimatedQty;
        TextView tvLocation;
        TextView tvPrice;
        MaterialButton btnPurchaseRequest;

        HarvestViewHolder(View view) {
            super(view);
            tvBadge = view.findViewById(R.id.tvBadge);
            tvListedTime = view.findViewById(R.id.tvListedTime);
            tvCropName = view.findViewById(R.id.tvCropName);
            tvEstimatedQty = view.findViewById(R.id.tvEstimatedQty);
            tvLocation = view.findViewById(R.id.tvLocation);
            tvPrice = view.findViewById(R.id.tvPrice);
            btnPurchaseRequest = view.findViewById(R.id.btnPurchaseRequest);
        }
    }
}

