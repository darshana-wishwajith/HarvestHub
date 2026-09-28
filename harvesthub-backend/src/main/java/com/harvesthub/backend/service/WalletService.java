package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.WalletSummaryResponse;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.WalletSummary;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.WalletSummaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletSummaryRepository walletSummaryRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    public WalletSummaryResponse getWalletSummary(String farmerId) {
        WalletSummary wallet = walletSummaryRepository.findByFarmerId(farmerId)
                .orElse(new WalletSummary(null, farmerId, "Rs. 0.00", "5.0", "0"));
        return new WalletSummaryResponse(
                wallet.getAvailableBalance(),
                wallet.getTrustScore(),
                wallet.getEmptyTripPenalties()
        );
    }

    public List<Transaction> getRecentTransactions(String farmerId, int limit) {
        return transactionRepository.findByFarmerIdOrderByTimestampDesc(
                farmerId, PageRequest.of(0, limit));
    }
}
