package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** Daily wholesale price of a crop at an economic centre (Market Insights, workflow 1.1). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "market_prices")
@CompoundIndex(name = "crop_center_date", def = "{'cropName': 1, 'center': 1, 'date': 1}", unique = true)
public class MarketPrice {
    @Id
    private String id;
    private String cropName;
    private String center;       // e.g. "Dambulla Market"
    private String province;
    private String date;         // yyyy-MM-dd
    private double pricePerKg;
    private String supplyLevel;  // LOW, MEDIUM, HIGH
    private String demandLevel;  // LOW, MEDIUM, HIGH
    private String source;       // SEED_TEST, OFFICER, ORDER
}
