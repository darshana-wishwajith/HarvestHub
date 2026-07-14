package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.BottomNavHelper;

import java.util.ArrayList;
import java.util.List;

public class CreateListingActivity extends AppCompatActivity {

    private final List<String> photoPaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        setupNavigation();
        setupCropDropdown();
        setupPhotoRecyclerView();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            Toast.makeText(this, "Listing submitted for verification!", Toast.LENGTH_LONG).show();
            finish();
        });

        // Also handle adding a new crop
        findViewById(R.id.tvAddNewCrop).setOnClickListener(v -> {
            Toast.makeText(this, "Add new category clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupPhotoRecyclerView() {
        RecyclerView rvPhotos = findViewById(R.id.rvPhotos);
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        PhotoAdapter photoAdapter = new PhotoAdapter(photoPaths, () -> {
            startActivity(new Intent(this, CameraCaptureActivity.class));
        });
        rvPhotos.setAdapter(photoAdapter);
    }

    private void setupCropDropdown() {
        String[] crops = {"Tomatoes", "Carrots", "Potatoes", "Onions", "Leeks", "Cabbage"};
        AutoCompleteTextView actvCrop = findViewById(R.id.actvCrop);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, crops);
        actvCrop.setAdapter(adapter);
    }

    private void setupNavigation() {
        // Setup Top Navigation
        View topNav = findViewById(R.id.top_nav_bar);
        View btnNotification = topNav.findViewById(R.id.btnNotification);
        btnNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Setup Bottom Navigation
        View navBar = findViewById(R.id.bottom_nav_bar);
        BottomNavHelper.setup(this, navBar, BottomNavHelper.Tab.LISTINGS);
    }

    // Inner Adapter Class
    private static class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
        private final List<String> photos;
        private final Runnable onAddPhotoClick;

        public PhotoAdapter(List<String> photos, Runnable onAddPhotoClick) {
            this.photos = photos;
            this.onAddPhotoClick = onAddPhotoClick;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_thumbnail, parent, false);
            return new PhotoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
            if (position == photos.size()) {
                // Last item is always the PLUS button
                holder.ivThumbnail.setImageResource(R.drawable.ic_plus);
                holder.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER);
                holder.ivThumbnail.setPadding(0, 0, 0, 0);
                holder.itemView.setOnClickListener(v -> onAddPhotoClick.run());
            } else {
                // Show actual photo
                holder.ivThumbnail.setImageResource(R.drawable.ic_leaf); // Placeholder
                holder.ivThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.ivThumbnail.setPadding(0, 0, 0, 0);
                holder.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return photos.size() + 1; // +1 for the plus icon
        }

        static class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;

            public PhotoViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            }
        }
    }
}
