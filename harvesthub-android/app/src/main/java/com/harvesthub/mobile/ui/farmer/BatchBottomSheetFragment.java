package com.harvesthub.mobile.ui.farmer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.CropBatch;

import java.util.List;

public class BatchBottomSheetFragment extends BottomSheetDialogFragment {

    private final List<CropBatch> batches;
    private final BatchAdapter.OnBatchSelectedListener listener;

    public BatchBottomSheetFragment(List<CropBatch> batches, BatchAdapter.OnBatchSelectedListener listener) {
        this.batches = batches;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_batches, container, false);
        RecyclerView rvBatches = view.findViewById(R.id.rvBatches);
        rvBatches.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBatches.setAdapter(new BatchAdapter(batches, batch -> {
            listener.onBatchSelected(batch);
            dismiss();
        }));
        return view;
    }
}
