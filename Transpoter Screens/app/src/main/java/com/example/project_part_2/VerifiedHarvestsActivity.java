package com.example.project_part_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class VerifiedHarvestsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verified_harvests);

        RecyclerView rvHarvestList = findViewById(R.id.rvHarvestList);
        rvHarvestList.setLayoutManager(new LinearLayoutManager(this));

        ImageButton btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> {
            FilterMarketplaceBottomSheet filterBottomSheet = new FilterMarketplaceBottomSheet();
            filterBottomSheet.show(getSupportFragmentManager(), FilterMarketplaceBottomSheet.TAG);
        });

        ExtendedFloatingActionButton fabPostDemand = findViewById(R.id.fabPostDemand);
        if (fabPostDemand != null) {
            fabPostDemand.setOnClickListener(v -> {
                Intent intent = new Intent(this, PostDemandRequestActivity.class);
                startActivity(intent);
            });
        }

        ImageButton btnNavMyDemands = findViewById(R.id.btnNavMyDemands);
        if (btnNavMyDemands != null) {
            btnNavMyDemands.setOnClickListener(v -> {
                Intent intent = new Intent(this, MyDemandRequestsActivity.class);
                startActivity(intent);
            });
        }

        ImageButton btnNavOffers = findViewById(R.id.btnNavOffers);
        if (btnNavOffers != null) {
            btnNavOffers.setOnClickListener(v -> {
                Intent intent = new Intent(this, SupplyOffersActivity.class);
                startActivity(intent);
            });
        }

        List<HarvestItem> harvestList = new ArrayList<>();
        harvestList.add(new HarvestItem(
                "Grade A Verified",
                R.drawable.bg_badge_grade_a,
                R.drawable.ic_check,
                "Listed 2h ago",
                "Big Onions",
                "500 KG",
                "Dambulla",
                "Rs. 180 /KG"
        ));
        harvestList.add(new HarvestItem(
                "Grade B Standard",
                R.drawable.bg_badge_grade_b,
                R.drawable.ic_check,
                "Listed 4h ago",
                "Red Potatoes",
                "800 KG",
                "Nuwara Eliya",
                "Rs. 140 /KG"
        ));
        harvestList.add(new HarvestItem(
                "Grade C Commercial",
                R.drawable.bg_badge_grade_c,
                R.drawable.ic_tag,
                "Listed 7h ago",
                "Green Chillies",
                "1,500 KG",
                "Jaffna",
                "Rs. 95 /KG"
        ));

        HarvestAdapter adapter = new HarvestAdapter(harvestList, item ->
                Toast.makeText(this, "Purchase requested for: " + item.getCropName(), Toast.LENGTH_SHORT).show()
        );
        rvHarvestList.setAdapter(adapter);
    }
}
