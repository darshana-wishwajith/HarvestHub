package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallet_summaries")
public class WalletSummary {
    @Id
    private String id;
    private String farmerId;
    private String availableBalance;
    private String trustScore;
    private String emptyTripPenalties;
}
