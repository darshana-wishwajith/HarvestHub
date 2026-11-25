package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.WalletSummaryResponse;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.model.Wallet;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/** Wallet screen: balance, trust %, star rating, penalties, transactions, mock withdrawal. */
@Service
public class WalletService {

    private final LedgerService ledger;
    private final TransactionRepository transactionRepository;
    private final UserRepository users;
    private final TrustService trust;
    private final BuyerDashboardService buyerFigures;

    public WalletService(LedgerService ledger, TransactionRepository transactionRepository, UserRepository users,
                         TrustService trust, BuyerDashboardService buyerFigures) {
        this.buyerFigures = buyerFigures;
        this.ledger = ledger;
        this.transactionRepository = transactionRepository;
        this.users = users;
        this.trust = trust;
    }

    public WalletSummaryResponse getWalletSummary(String userId) {
        Wallet w = ledger.wallet(userId);
        User u = users.findById(userId).orElse(null);
        int trustPct = u == null ? 0 : trust.trustOf(u);
        WalletSummaryResponse r = new WalletSummaryResponse();
        r.setAvailableBalance(LedgerService.format(w.getBalance()));
        r.setTrustScore(String.valueOf(trustPct));
        r.setEmptyTripPenalties(LedgerService.format(w.getTotalPenalties()));
        r.setBalance(w.getBalance());
        r.setTrustPercent(trustPct);
        r.setRatingAverage(u == null ? null : u.getRatingAverage());
        r.setRatingCount(u == null || u.getRatingCount() == null ? 0 : u.getRatingCount());
        r.setSuspended(w.isSuspended());
        r.setTotalPenalties(w.getTotalPenalties());
        if (u != null && "BUYER".equals(u.getRole())) {
            r.setEscrowHeld(buyerFigures.escrowHeld(userId));
            r.setEscrowReleased(buyerFigures.escrowReleased(userId));
        }
        String today = java.time.LocalDate.now().toString();
        java.util.List<Transaction> todays = transactionRepository.findByFarmerId(userId).stream()
                .filter(t -> t.isCredit() && t.getTimestamp() != null && t.getTimestamp().startsWith(today)).toList();
        r.setTodayInflow(FeeCalculator.round2(todays.stream().mapToDouble(Transaction::getAmountValue).sum()));
        r.setTodayInflowCount(todays.size());
        return r;
    }

    public List<Transaction> getRecentTransactions(String userId, int limit) {
        return transactionRepository.findByFarmerIdOrderByTimestampDesc(userId, PageRequest.of(0, Math.max(1, Math.min(limit, 100))));
    }

    public Transaction topUp(String userId, double amount) {
        return ledger.topUp(userId, amount);
    }

    public Transaction withdraw(String userId, double amount) {
        return ledger.withdraw(userId, amount);
    }
}
