package com.harvesthub.mobile.ui.farmer;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;

import java.util.ArrayList;
import java.util.List;

public class ListingDetailsActivity extends AppCompatActivity {

    private RecyclerView rvPhotos;
    private PhotoAdapter photoAdapter;
    private List<Uri> photoUris = new ArrayList<>();
    
    private AutoCompleteTextView actvCrop;
    private TextInputLayout tilCropDropdown, tilYield, tilPrice;
    private TextInputEditText etYield, etPrice;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private TextView tvAddNewCrop;

    private List<String> cropList = new ArrayList<>();
    private ArrayAdapter<String> cropAdapter;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    photoUris.add(uri);
                    photoAdapter.notifyDataSetChanged();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        initViews();
        setupPhotoList();
        setupCropDropdown();
        setupClickListeners();
    }

    private void initViews() {
        rvPhotos = findViewById(R.id.rvPhotos);
        actvCrop = findViewById(R.id.actvCrop);
        tilCropDropdown = findViewById(R.id.tilCropDropdown);
        tilYield = findViewById(R.id.tilYield);
        tilPrice = findViewById(R.id.tilPrice);
        etYield = findViewById(R.id.etYield);
        etPrice = findViewById(R.id.etPrice);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        tvAddNewCrop = findViewById(R.id.tvAddNewCrop);
    }

    private void setupPhotoList() {
        photoAdapter = new PhotoAdapter();
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPhotos.setAdapter(photoAdapter);
    }

    private void setupCropDropdown() {
        cropList.add("Big Onions");
        cropList.add("Tomatoes");
        cropList.add("Carrots");
        cropList.add("Potatoes");
        cropList.add("Leeks");

        cropAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cropList);
        actvCrop.setAdapter(cropAdapter);
    }

    private void setupClickListeners() {
        tvAddNewCrop.setOnClickListener(v -> showAddNewCropDialog());

        btnSubmit.setOnClickListener(v -> {
            if (validateForm()) {
                submitListing();
            }
        });
    }

    private void showAddNewCropDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Crop");

        final EditText input = new EditText(this);
        input.setHint("Enter crop name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCrop = input.getText().toString().trim();
            if (!newCrop.isEmpty()) {
                cropList.add(newCrop);
                cropAdapter.notifyDataSetChanged();
                actvCrop.setText(newCrop, false);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (actvCrop.getText().toString().isEmpty()) {
            tilCropDropdown.setError("Please select a crop");
            isValid = false;
        } else {
            tilCropDropdown.setError(null);
        }

        String yieldStr = etYield.getText().toString().trim();
        if (yieldStr.isEmpty() || Double.parseDouble(yieldStr) <= 0) {
            tilYield.setError("Enter a valid yield");
            isValid = false;
        } else {
            tilYield.setError(null);
        }

        String priceStr = etPrice.getText().toString().trim();
        if (priceStr.isEmpty() || Double.parseDouble(priceStr) <= 0) {
            tilPrice.setError("Enter a valid price");
            isValid = false;
        } else {
            tilPrice.setError(null);
        }

        return isValid;
    }

    private void submitListing() {
        btnSubmit.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        // Simulate network delay
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Listing Submitted Successfully!", Toast.LENGTH_SHORT).show();
            // Navigate to success screen or back
            finish();
        }, 2000);
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_thumbnail, parent, false);
            if (viewType == TYPE_ADD_BUTTON) {
                return new AddViewHolder(view);
            }
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ImageViewHolder) {
                ((ImageViewHolder) holder).ivThumbnail.setImageURI(photoUris.get(position));
                ((ImageViewHolder) holder).ivThumbnail.setPadding(0, 0, 0, 0);
                ((ImageViewHolder) holder).ivThumbnail.setBackground(null);
            } else if (holder instanceof AddViewHolder) {
                holder.itemView.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
            }
        }

        @Override
        public int getItemCount() {
            // Photos + the Add Button
            return photoUris.size() + 1;
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            ImageViewHolder(View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            }
        }

        class AddViewHolder extends RecyclerView.ViewHolder {
            AddViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
