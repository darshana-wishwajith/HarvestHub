package com.harvesthub.backend.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletSummaryResponse {
    private String availableBalance;
    private String trustScore;
    private String emptyTripPenalties;
}
