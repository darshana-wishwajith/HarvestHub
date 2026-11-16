package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.FragmentCreateListingBinding;
import com.harvesthub.mobile.databinding.ItemPhotoThumbnailBinding;
import com.harvesthub.mobile.model.CropInfo;
import com.harvesthub.mobile.model.Listing;
import com.harvesthub.mobile.model.MarketTrendResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma 14 "Listing Details &amp; Submission" (workflow 1.4). Reached only after the location lock
 * and the guided capture; sends crop, Estimated Total Yield and expected price together with the
 * captured media and GPS. Without a capture it starts the capture flow first.
 */
public class CreateListingFragment extends Fragment {

    public static final String ARG_CAPTURED_AT = "capturedAt";
    public static final String ARG_DEVICE = "device";

    private FragmentCreateListingBinding binding;
    private ApiService api;
    private final List<String> cropList = new ArrayList<>();
    private ArrayAdapter<String> cropAdapter;
    private ArrayList<String> media;
    private double lat, lng;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        api = ApiClient.getApiService();
        Bundle args = getArguments();
        media = args == null ? null : args.getStringArrayList(MainActivity.EXTRA_CAPTURED_MEDIA);
        if (media == null || media.size() < 3) {
            // workflow 1.2: every listing starts with the location lock + guided camera
            startActivity(new Intent(requireContext(), LocationVerificationActivity.class));
            getParentFragmentManager().popBackStack();
            return binding.getRoot();
        }
        lat = args.getDouble(LocationVerificationActivity.EXTRA_LAT);
        lng = args.getDouble(LocationVerificationActivity.EXTRA_LNG);

        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(new CapturedAdapter());
        cropAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cropList);
        binding.actvCrop.setAdapter(cropAdapter);
        binding.actvCrop.setOnItemClickListener((p, v, pos, id) -> loadPriceInsight(cropList.get(pos)));
        String preset = args.getString(LocationVerificationActivity.EXTRA_CROP);
        if (preset != null) binding.actvCrop.setText(preset, false);
        binding.tvPricingInsight.setText("Choose a crop to see the current market price.");
        binding.etYield.addTextChangedListener(clearError(binding.tilYield));
        binding.etPrice.addTextChangedListener(clearError(binding.tilPrice));

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnAddNewCrop.setOnClickListener(v -> showAddNewCropDialog());
        binding.btnSubmit.setOnClickListener(v -> {
            if (validateForm()) submitListing();
        });
        loadCrops();
        return binding.getRoot();
    }

    private TextWatcher clearError(com.google.android.material.textfield.TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { til.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void loadCrops() {
        api.getCrops().enqueue(new Callback<List<CropInfo>>() {
            @Override
            public void onResponse(@NonNull Call<List<CropInfo>> call, @NonNull Response<List<CropInfo>> response) {
                if (!isAdded() || response.body() == null) return;
                cropList.clear();
                for (CropInfo c : response.body()) cropList.add(c.getName() + (c.isPending() ? " (pending)" : ""));
                cropAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<List<CropInfo>> call, @NonNull Throwable t) {
                if (isAdded()) Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String selectedCrop() {
        return binding.actvCrop.getText().toString().replace(" (pending)", "").trim();
    }

    private void loadPriceInsight(String crop) {
        String name = crop.replace(" (pending)", "");
        api.getMarketTrend(name, 7).enqueue(new Callback<MarketTrendResponse>() {
            @Override
            public void onResponse(@NonNull Call<MarketTrendResponse> call, @NonNull Response<MarketTrendResponse> response) {
                if (binding == null || response.body() == null) return;
                MarketTrendResponse t = response.body();
                binding.tvPricingInsight.setText(t.getCurrentAverage() == null
                        ? "No market price data for " + name + " yet."
                        : String.format(Locale.US, "Pricing insights: current average market rate for %s is LKR %,.0f/kg.",
                        name, t.getCurrentAverage()));
            }

            @Override
            public void onFailure(@NonNull Call<MarketTrendResponse> call, @NonNull Throwable t) { }
        });
    }

    /** "Can't find your harvest? Add New" — saved as a pending crop, reviewed with the listing (user decision 14 Nov). */
    private void showAddNewCropDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Crop name, e.g. Red Onions");
        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Crop")
                .setMessage("New crop types are checked by the officer together with your listing.")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newCrop = input.getText().toString().trim();
                    if (newCrop.length() >= 2) {
                        cropList.add(newCrop + " (pending)");
                        cropAdapter.notifyDataSetChanged();
                        binding.actvCrop.setText(newCrop, false);
                        binding.tvPricingInsight.setText("No market price data for a new crop yet.");
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .show();
    }

    private boolean validateForm() {
        boolean ok = true;
        if (selectedCrop().isEmpty()) {
            binding.tilCropDropdown.setError("Please select a crop");
            ok = false;
        } else {
            binding.tilCropDropdown.setError(null);
        }
        if (number(binding.etYield.getText().toString()) <= 0) {
            binding.tilYield.setError("Enter the estimated total yield in kg");
            ok = false;
        }
        if (number(binding.etPrice.getText().toString()) <= 0) {
            binding.tilPrice.setError("Enter the expected price per kg");
            ok = false;
        }
        return ok;
    }

    private static double number(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void submitListing() {
        binding.btnSubmit.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        Listing l = new Listing(null, selectedCrop(), null, binding.etYield.getText().toString().trim(),
                binding.etPrice.getText().toString().trim(), null);
        l.setGpsLatitude(lat);
        l.setGpsLongitude(lng);
        l.setMediaUrls(media);
        Bundle args = getArguments();
        if (args != null) {
            l.setCapturedAt(args.getString(ARG_CAPTURED_AT));
            l.setDeviceModel(args.getString(ARG_DEVICE));
        }
        api.createListing(l).enqueue(new Callback<Listing>() {
            @Override
            public void onResponse(@NonNull Call<Listing> call, @NonNull Response<Listing> response) {
                if (!isAdded()) return;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Listing submitted – waiting for officer verification.", Toast.LENGTH_LONG).show();
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new ListingsFragment())
                            .commit();
                } else {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(requireContext(), AuthInputUtils.errorMessage(response, "Failed to submit listing"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Listing> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                binding.btnSubmit.setEnabled(true);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** Shows the three captured items (already uploaded). */
    private class CapturedAdapter extends RecyclerView.Adapter<CapturedAdapter.Holder> {
        private final int[] icons = {R.drawable.ic_check_circle, R.drawable.ic_check_circle, R.drawable.ic_check_circle};

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemPhotoThumbnailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            h.b.ivThumbnail.setImageResource(icons[position]);
            h.b.ivThumbnail.setPadding(30, 30, 30, 30);
            h.b.ivThumbnail.setContentDescription(position == 0 ? "Wide angle photo uploaded"
                    : position == 1 ? "Close-up photo uploaded" : "Video uploaded");
        }

        @Override
        public int getItemCount() {
            return 3;
        }

        class Holder extends RecyclerView.ViewHolder {
            final ItemPhotoThumbnailBinding b;

            Holder(ItemPhotoThumbnailBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
