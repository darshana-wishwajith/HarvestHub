package com.harvesthub.mobile.ui.buyer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Figma buyer 2.2 "Advanced Filter & Search Panel". Category (crop catalogue), officer-verified grade, district,
 * max price per kg and min quantity — applied server-side by GET /buyer/listings.
 */
public class FilterMarketplaceBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "FilterMarketplaceBottomSheet";

    /** Current filter values; null / empty = not set. */
    public static class Filters {
        public final Set<String> categories = new LinkedHashSet<>();
        public final Set<String> grades = new LinkedHashSet<>();
        public String district;
        public Double maxPrice;
        public Double minQty;

        public boolean isEmpty() {
            return categories.isEmpty() && grades.isEmpty() && (district == null || district.isEmpty())
                    && maxPrice == null && minQty == null;
        }

        static String join(Set<String> s) {
            return s.isEmpty() ? null : android.text.TextUtils.join(",", s);
        }
    }

    public interface Listener {
        void onFiltersApplied(Filters filters);
    }

    private Filters filters = new Filters();

    public static FilterMarketplaceBottomSheet with(Filters current) {
        FilterMarketplaceBottomSheet s = new FilterMarketplaceBottomSheet();
        s.filters = copy(current);
        return s;
    }

    private static Filters copy(Filters f) {
        Filters c = new Filters();
        if (f == null) return c;
        c.categories.addAll(f.categories);
        c.grades.addAll(f.grades);
        c.district = f.district;
        c.maxPrice = f.maxPrice;
        c.minQty = f.minQty;
        return c;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter_marketplace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindCategory(view, R.id.chipCategoryVeg, "Vegetables");
        bindCategory(view, R.id.chipCategoryFruits, "Fruits");
        bindCategory(view, R.id.chipCategoryGrains, "Grains");
        bindGrade(view, R.id.chipGradeA, "A");
        bindGrade(view, R.id.chipGradeB, "B");
        bindGrade(view, R.id.chipGradeC, "C");

        EditText district = view.findViewById(R.id.etProximity);
        EditText maxPrice = view.findViewById(R.id.etMaxPrice);
        EditText minQty = view.findViewById(R.id.etMinQty);
        if (filters.district != null) district.setText(filters.district);
        if (filters.maxPrice != null) maxPrice.setText(String.valueOf(filters.maxPrice.intValue()));
        if (filters.minQty != null) minQty.setText(String.valueOf(filters.minQty.intValue()));

        view.findViewById(R.id.tvClearAll).setOnClickListener(v -> {
            filters = new Filters();
            district.setText("");
            maxPrice.setText("");
            minQty.setText("");
            refreshAll(view);
        });

        MaterialButton show = view.findViewById(R.id.btnShowResults);
        show.setOnClickListener(v -> {
            filters.district = district.getText().toString().trim();
            filters.maxPrice = number(maxPrice);
            filters.minQty = number(minQty);
            if (getParentFragment() instanceof Listener) ((Listener) getParentFragment()).onFiltersApplied(filters);
            dismiss();
        });
        refreshAll(view);
    }

    private static Double number(EditText e) {
        String s = e.getText().toString().replaceAll("[^0-9.]", "");
        try {
            double d = Double.parseDouble(s);
            return d > 0 ? d : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void bindCategory(View root, int id, String value) {
        View chip = root.findViewById(id);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            if (!filters.categories.remove(value)) filters.categories.add(value);
            refreshAll(root);
        });
    }

    private void bindGrade(View root, int id, String value) {
        View chip = root.findViewById(id);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            if (!filters.grades.remove(value)) filters.grades.add(value);
            refreshAll(root);
        });
    }

    private void refreshAll(View root) {
        paintCategory(root.findViewById(R.id.chipCategoryVeg), filters.categories.contains("Vegetables"));
        paintCategory(root.findViewById(R.id.chipCategoryFruits), filters.categories.contains("Fruits"));
        paintCategory(root.findViewById(R.id.chipCategoryGrains), filters.categories.contains("Grains"));
        paintGrade(root.findViewById(R.id.chipGradeA), filters.grades.contains("A"));
        paintGrade(root.findViewById(R.id.chipGradeB), filters.grades.contains("B"));
        paintGrade(root.findViewById(R.id.chipGradeC), filters.grades.contains("C"));
    }

    private void paintCategory(View chip, boolean on) {
        if (chip == null) return;
        chip.setBackground(ContextCompat.getDrawable(requireContext(), on ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected));
        if (chip instanceof TextView) {
            ((TextView) chip).setTextColor(ContextCompat.getColor(requireContext(), on ? R.color.white : R.color.text_primary));
        }
    }

    private void paintGrade(View chip, boolean on) {
        if (chip == null) return;
        chip.setBackground(ContextCompat.getDrawable(requireContext(), on ? R.drawable.bg_grade_a_selected : R.drawable.bg_grade_unselected));
    }
}
