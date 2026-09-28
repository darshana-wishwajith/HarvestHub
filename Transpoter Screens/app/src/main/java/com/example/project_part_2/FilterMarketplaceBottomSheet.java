package com.example.project_part_2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class FilterMarketplaceBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "FilterMarketplaceBottomSheet";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnShowResults = view.findViewById(R.id.btnShowResults);
        TextView tvClearAll = view.findViewById(R.id.tvClearAll);

        if (tvClearAll != null) {
            tvClearAll.setOnClickListener(v -> 
                Toast.makeText(getContext(), "Filters Cleared", Toast.LENGTH_SHORT).show()
            );
        }

        if (btnShowResults != null) {
            btnShowResults.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Applying Filters...", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }
    }
}
