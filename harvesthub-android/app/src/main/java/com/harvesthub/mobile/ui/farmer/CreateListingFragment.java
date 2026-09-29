package com.harvesthub.mobile.ui.farmer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.FragmentCreateListingBinding;
import com.harvesthub.mobile.databinding.ItemPhotoThumbnailBinding;
import com.harvesthub.mobile.model.Listing;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class CreateListingFragment extends Fragment {

    private FragmentCreateListingBinding binding;
    private PhotoAdapter photoAdapter;
    private final List<Uri> photoUris = new ArrayList<>();
    private final List<String> cropList = new ArrayList<>();
    private ArrayAdapter<String> cropAdapter;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    photoUris.add(uri);
                    photoAdapter.notifyItemInserted(photoUris.size() - 1);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateListingBinding.inflate(inflater, container, false);
        
        setupPhotoList();
        setupCropDropdown();
        setupClickListeners();

        return binding.getRoot();
    }

    private void setupPhotoList() {
        photoAdapter = new PhotoAdapter();
        binding.rvPhotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotos.setAdapter(photoAdapter);
    }

    private void setupCropDropdown() {
        cropList.add("Big Onions");
        cropList.add("Tomatoes");
        cropList.add("Carrots");
        cropList.add("Potatoes");
        cropList.add("Leeks");

        cropAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, cropList);
        binding.actvCrop.setAdapter(cropAdapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnAddNewCrop.setOnClickListener(v -> showAddNewCropDialog());

        binding.btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitListing();
            }
        });
    }

    private void showAddNewCropDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Crop");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter crop name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCrop = input.getText().toString().trim();
            if (!newCrop.isEmpty()) {
                cropList.add(newCrop);
                cropAdapter.notifyDataSetChanged();
                binding.actvCrop.setText(newCrop, false);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (binding.actvCrop.getText().toString().isEmpty()) {
            binding.tilCropDropdown.setError("Please select a crop");
            isValid = false;
        } else {
            binding.tilCropDropdown.setError(null);
        }

        String yieldStr = binding.etYield.getText().toString().trim();
        if (yieldStr.isEmpty() || Double.parseDouble(yieldStr) <= 0) {
            binding.tilYield.setError("Enter a valid yield");
            isValid = false;
        } else {
            binding.tilYield.setError(null);
        }

        String priceStr = binding.etPrice.getText().toString().trim();
        if (priceStr.isEmpty() || Double.parseDouble(priceStr) <= 0) {
            binding.tilPrice.setError("Enter a valid price");
            isValid = false;
        } else {
            binding.tilPrice.setError(null);
        }

        return isValid;
    }

    private void submitListing() {
        binding.btnSubmit.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        String cropName = binding.actvCrop.getText().toString().trim();
        String yieldStr = binding.etYield.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();

        SessionManager sessionManager = new SessionManager(requireContext());
        String farmerId = (sessionManager.getUser() != null && sessionManager.getUser().getId() != null && !sessionManager.getUser().getId().isEmpty()) 
                ? sessionManager.getUser().getId() 
                : "user_farmer_kamal";
        String location = (sessionManager.getUser() != null && sessionManager.getUser().getLocation() != null && !sessionManager.getUser().getLocation().isEmpty()) 
                ? sessionManager.getUser().getLocation() 
                : "Dambulla Sector";

        Listing newListing = new Listing(farmerId, cropName, location, yieldStr, priceStr, "PENDING");

        ApiService apiService = ApiClient.getClient(sessionManager).create(ApiService.class);
        apiService.createListing(newListing).enqueue(new Callback<Listing>() {
            @Override
            public void onResponse(@NonNull Call<Listing> call, @NonNull Response<Listing> response) {
                if (isAdded()) {
                    binding.progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "Listing Submitted for Verification!", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    } else {
                        binding.btnSubmit.setEnabled(true);
                        Toast.makeText(requireContext(), "Failed to submit listing", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Listing> call, @NonNull Throwable t) {
                if (isAdded()) {
                    binding.btnSubmit.setEnabled(true);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_IMAGE = 0;
        private static final int TYPE_ADD_BUTTON = 1;

        @Override
        public int getItemViewType(int position) {
            return (position == photoUris.size()) ? TYPE_ADD_BUTTON : TYPE_IMAGE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPhotoThumbnailBinding b = ItemPhotoThumbnailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            if (viewType == TYPE_ADD_BUTTON) {
                return new AddViewHolder(b);
            }
            return new ImageViewHolder(b);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ImageViewHolder) {
                ((ImageViewHolder) holder).binding.ivThumbnail.setImageURI(photoUris.get(position));
                ((ImageViewHolder) holder).binding.ivThumbnail.setPadding(0, 0, 0, 0);
                ((ImageViewHolder) holder).binding.ivThumbnail.setBackground(null);
                ((ImageViewHolder) holder).binding.getRoot().setStrokeWidth(0);
            } else if (holder instanceof AddViewHolder) {
                ((AddViewHolder) holder).binding.ivThumbnail.setImageResource(R.drawable.ic_plus);
                ((AddViewHolder) holder).binding.ivThumbnail.setPadding(35, 35, 35, 35);
                ((AddViewHolder) holder).binding.ivThumbnail.setBackgroundResource(R.drawable.bg_dashed_border);
                ((AddViewHolder) holder).binding.ivThumbnail.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#718096")));
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), CameraCaptureActivity.class);
                    v.getContext().startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return photoUris.size() + 1;
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ItemPhotoThumbnailBinding binding;
            ImageViewHolder(ItemPhotoThumbnailBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        class AddViewHolder extends RecyclerView.ViewHolder {
            ItemPhotoThumbnailBinding binding;
            AddViewHolder(ItemPhotoThumbnailBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
