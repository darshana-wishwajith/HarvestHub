package com.example.project_part_2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class DemandRequestAdapter extends RecyclerView.Adapter<DemandRequestAdapter.DemandViewHolder> {

    public interface OnActionClickListener {
        void onActionClick(DemandRequestItem item, String action);
    }

    private List<DemandRequestItem> items;
    private OnActionClickListener onActionClick;

    public DemandRequestAdapter(List<DemandRequestItem> items, OnActionClickListener onActionClick) {
        this.items = items;
        this.onActionClick = onActionClick;
    }

    @NonNull
    @Override
    public DemandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_demand_request_card, parent, false);
        return new DemandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DemandViewHolder holder, int position) {
        DemandRequestItem item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvDemandTitle.setText(item.getTitle());
        holder.tvTimeAgo.setText(item.getTimeAgo());
        holder.tvTargetGrade.setText(item.getTargetGrade());

        if (item.getDeadline() != null) {
            holder.layoutDeadline.setVisibility(View.VISIBLE);
            holder.tvDeadline.setText(item.getDeadline());
        } else {
            holder.layoutDeadline.setVisibility(View.GONE);
        }

        if (item.getStatus() == DemandStatus.ACTIVE) {
            holder.tvStatusBadge.setText("Status: Active");
            holder.tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_status_active));
            holder.layoutActiveActions.setVisibility(View.VISIBLE);
            holder.layoutClosedActions.setVisibility(View.GONE);
        } else {
            holder.tvStatusBadge.setText("Status: Closed");
            holder.tvStatusBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_badge_status_closed));
            holder.layoutActiveActions.setVisibility(View.GONE);
            holder.layoutClosedActions.setVisibility(View.VISIBLE);
        }

        holder.btnViewOffers.setOnClickListener(v -> {
            if (onActionClick != null) onActionClick.onActionClick(item, "view_offers");
        });

        holder.btnEditAd.setOnClickListener(v -> {
            if (onActionClick != null) onActionClick.onActionClick(item, "edit_ad");
        });

        holder.btnRepublishRequest.setOnClickListener(v -> {
            if (onActionClick != null) onActionClick.onActionClick(item, "republish");
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<DemandRequestItem> newList) {
        this.items = newList;
        notifyDataSetChanged();
    }

    static class DemandViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatusBadge;
        TextView tvTimeAgo;
        TextView tvDemandTitle;
        LinearLayout layoutDeadline;
        TextView tvDeadline;
        TextView tvTargetGrade;
        LinearLayout layoutActiveActions;
        LinearLayout layoutClosedActions;
        MaterialButton btnViewOffers;
        MaterialButton btnEditAd;
        MaterialButton btnRepublishRequest;

        DemandViewHolder(View view) {
            super(view);
            tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
            tvTimeAgo = view.findViewById(R.id.tvTimeAgo);
            tvDemandTitle = view.findViewById(R.id.tvDemandTitle);
            layoutDeadline = view.findViewById(R.id.layoutDeadline);
            tvDeadline = view.findViewById(R.id.tvDeadline);
            tvTargetGrade = view.findViewById(R.id.tvTargetGrade);
            layoutActiveActions = view.findViewById(R.id.layoutActiveActions);
            layoutClosedActions = view.findViewById(R.id.layoutClosedActions);
            btnViewOffers = view.findViewById(R.id.btnViewOffers);
            btnEditAd = view.findViewById(R.id.btnEditAd);
            btnRepublishRequest = view.findViewById(R.id.btnRepublishRequest);
        }
    }
}
