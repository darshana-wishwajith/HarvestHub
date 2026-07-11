package com.harvesthub.mobile.ui.farmer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.CropBatch;

import java.util.List;

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.BatchViewHolder> {

    private final List<CropBatch> batches;
    private final OnBatchSelectedListener listener;

    public interface OnBatchSelectedListener {
        void onBatchSelected(CropBatch batch);
    }

    public BatchAdapter(List<CropBatch> batches, OnBatchSelectedListener listener) {
        this.batches = batches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        CropBatch batch = batches.get(position);
        holder.tvBatchNumber.setText("Batch: " + batch.getBatchNumber());
        holder.tvBatchInfo.setText((batch.isVerified() ? "Verified" : "Unverified") + " - " + (int)batch.getAvailableQuantity() + " KG available");
        holder.ivVerified.setVisibility(batch.isVerified() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onBatchSelected(batch));
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvBatchNumber, tvBatchInfo;
        ImageView ivVerified;

        BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBatchNumber = itemView.findViewById(R.id.tvBatchNumber);
            tvBatchInfo = itemView.findViewById(R.id.tvBatchInfo);
            ivVerified = itemView.findViewById(R.id.ivVerified);
        }
    }
}
