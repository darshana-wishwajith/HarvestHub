package com.example.project_part_2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SupplyOffersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supply_offers);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvRespondingToAd = findViewById(R.id.tvRespondingToAd);
        String adTitle = getIntent().getStringExtra("ad_title");
        if (adTitle == null || adTitle.isEmpty()) {
            adTitle = "Responding to your ad: Need 200kg of Tomatoes";
        }
        tvRespondingToAd.setText(adTitle);

        RecyclerView rvSupplyOffers = findViewById(R.id.rvSupplyOffers);
        rvSupplyOffers.setLayoutManager(new LinearLayoutManager(this));

        List<SupplyOfferItem> offerList = new ArrayList<>();
        offerList.add(new SupplyOfferItem(
                "1",
                "Grade A Verified",
                R.drawable.bg_badge_grade_a,
                true,
                "Offer sent 10m ago",
                "Farmer: W.B.D.W. Bandara",
                "Location: Dambulla",
                "Estimated: 210 KG",
                "Rs. 175 / KG"
        ));
        offerList.add(new SupplyOfferItem(
                "2",
                "Grade B Standard",
                R.drawable.bg_badge_grade_b,
                false,
                "Offer sent 1h ago",
                "Farmer: Sunil Perera",
                "Location: Kandy",
                "Estimated: 200 KG",
                "Rs. 160 / KG"
        ));

        SupplyOfferAdapter adapter = new SupplyOfferAdapter(offerList, selectedOffer -> {
            Intent intentChat = new Intent(this, NegotiationChatActivity.class);
            intentChat.putExtra("crop_name", "200kg Tomatoes");
            intentChat.putExtra("farmer_name", selectedOffer.getFarmerName().replace("Farmer: ", ""));
            intentChat.putExtra("offered_price", selectedOffer.getOfferedPrice());
            intentChat.putExtra("quantity", "200kg");
            intentChat.putExtra("pickup_date", "Friday");
            intentChat.putExtra("order_ref", "#HH-842");
            startActivity(intentChat);
        });
        rvSupplyOffers.setAdapter(adapter);
    }
}
