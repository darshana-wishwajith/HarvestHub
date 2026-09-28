package com.example.project_part_2;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MyDemandRequestsActivity extends AppCompatActivity {

    private RecyclerView rvDemandRequests;
    private DemandRequestAdapter adapter;
    private TextView tabActiveAds;
    private TextView tabClosed;

    private List<DemandRequestItem> allDemandList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_demand_requests);

        initData();
        initViews();
        setupRecyclerView();
        setupTabs();
    }

    private void initData() {
        allDemandList = new ArrayList<>();
        allDemandList.add(new DemandRequestItem(
                "1",
                DemandStatus.ACTIVE,
                "Posted 1d ago",
                "Need 200kg of Tomatoes",
                "Deadline: Friday",
                "Target: Grade A"
        ));
        allDemandList.add(new DemandRequestItem(
                "2",
                DemandStatus.ACTIVE,
                "Posted 30 minutes ago",
                "Need 200kg of Tomatoes",
                "Deadline: Friday",
                "Target: Grade B"
        ));
        allDemandList.add(new DemandRequestItem(
                "3",
                DemandStatus.CLOSED,
                "Fulfilled 4d ago",
                "500kg Big Onions",
                null,
                "Target: Grade B"
        ));
    }

    private void initViews() {
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tabActiveAds = findViewById(R.id.tabActiveAds);
        tabClosed = findViewById(R.id.tabClosed);
        rvDemandRequests = findViewById(R.id.rvDemandRequests);
    }

    private void setupRecyclerView() {
        rvDemandRequests.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DemandRequestAdapter(allDemandList, (item, action) -> {
            switch (action) {
                case "view_offers":
                    Intent intentOffers = new Intent(this, SupplyOffersActivity.class);
                    intentOffers.putExtra("ad_title", "Responding to your ad: " + item.getTitle());
                    startActivity(intentOffers);
                    break;
                case "edit_ad":
                    Intent intentEdit = new Intent(this, PostDemandRequestActivity.class);
                    startActivity(intentEdit);
                    break;
                case "republish":
                    Toast.makeText(this, "Republishing demand request for: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });

        rvDemandRequests.setAdapter(adapter);
    }

    private void setupTabs() {
        tabActiveAds.setOnClickListener(v -> selectTab(true));
        tabClosed.setOnClickListener(v -> selectTab(false));
    }

    private void selectTab(boolean isActiveTab) {
        if (isActiveTab) {
            tabActiveAds.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
            tabActiveAds.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabActiveAds.setTypeface(Typeface.DEFAULT_BOLD);

            tabClosed.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
            tabClosed.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tabClosed.setTypeface(Typeface.DEFAULT);

            List<DemandRequestItem> activeItems = new ArrayList<>();
            for (DemandRequestItem item : allDemandList) {
                if (item.getStatus() == DemandStatus.ACTIVE) {
                    activeItems.add(item);
                }
            }
            adapter.updateList(activeItems);
        } else {
            tabClosed.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_selected));
            tabClosed.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabClosed.setTypeface(Typeface.DEFAULT_BOLD);

            tabActiveAds.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_chip_unselected));
            tabActiveAds.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            tabActiveAds.setTypeface(Typeface.DEFAULT);

            List<DemandRequestItem> closedItems = new ArrayList<>();
            for (DemandRequestItem item : allDemandList) {
                if (item.getStatus() == DemandStatus.CLOSED) {
                    closedItems.add(item);
                }
            }
            adapter.updateList(closedItems);
        }
    }
}
