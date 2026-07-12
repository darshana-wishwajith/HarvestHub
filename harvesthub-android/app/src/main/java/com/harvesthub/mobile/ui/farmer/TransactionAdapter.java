package com.harvesthub.mobile.ui.farmer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Transaction;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction item = transactions.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvTime.setText(item.getTimestamp());
        
        String prefix = item.isCredit() ? "+ " : "- ";
        holder.tvAmount.setText(prefix + item.getAmount());
        holder.tvAmount.setTextColor(item.isCredit() ? Color.parseColor("#13A34A") : Color.parseColor("#D32F2F"));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvAmount;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTransactionTitle);
            tvTime = itemView.findViewById(R.id.tvTransactionTime);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
        }
    }
}
