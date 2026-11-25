package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.DashboardResponse;
import com.harvesthub.backend.dto.FarmerSummaryResponse;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/** Farmer Home and the My Listings header — computed only from stored data. */
@Service
public class DashboardService {

    /** Ledger entries that are farmer income (crop money). */
    static final Set<String> INCOME_TYPES = Set.of("CROP_ADVANCE", "CROP_BALANCE");

    private final UserRepository users;
    private final ListingRepository listings;
    private final OrderRepository orders;
    private final TransactionRepository transactions;
    private final TrustService trust;

    public DashboardService(UserRepository users, ListingRepository listings, OrderRepository orders,
                            TransactionRepository transactions, TrustService trust) {
        this.users = users;
        this.listings = listings;
        this.orders = orders;
        this.transactions = transactions;
        this.trust = trust;
    }

    /** Income in a month: crop payments from the ledger, plus older seed data without a type. */
    double income(String farmerId, YearMonth month) {
        String prefix = month.toString(); // yyyy-MM
        return transactions.findByFarmerId(farmerId).stream()
                .filter(Transaction::isCredit)
                .filter(t -> t.getType() == null || INCOME_TYPES.contains(t.getType()))
                .filter(t -> t.getTimestamp() != null && t.getTimestamp().startsWith(prefix))
                .mapToDouble(t -> t.getAmountValue() > 0 ? t.getAmountValue()
                        : com.harvesthub.backend.model.Listing.parseNumber(t.getAmount()))
                .sum();
    }

    public DashboardResponse getFarmerDashboard(String farmerId) {
        User u = users.findById(farmerId).orElseThrow(() -> new IllegalStateException("User not found"));
        YearMonth now = YearMonth.now();
        double thisMonth = income(farmerId, now);
        double lastMonth = income(farmerId, now.minusMonths(1));
        String change = lastMonth > 0 ? String.format("%+.0f%%", (thisMonth - lastMonth) / lastMonth * 100) : null;
        long active = listings.findByFarmerIdAndStatusIn(farmerId, List.of("ACTIVE", "ONGOING")).size();
        long completed = orders.findByFarmerId(farmerId).stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();
        int trustPct = trust.trustOf(u);

        DashboardResponse r = new DashboardResponse();
        r.setFarmerName(u.getFullName());
        int hour = LocalTime.now(java.time.ZoneId.of("Asia/Colombo")).getHour();
        String first = u.getFullName() == null ? "" : u.getFullName().trim().split("\\s+")[0];
        r.setGreeting((hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening") + ", " + first + "!");
        r.setTotalRevenue(LedgerService.format(thisMonth));
        r.setPreviousRevenue(LedgerService.format(lastMonth));
        r.setRevenueChange(change);
        r.setActiveListings(String.format("%02d", active));
        r.setCompletedOrders(String.format("%02d", completed));
        r.setTrustScore(String.valueOf(trustPct));
        r.setTrustPercent(trustPct);
        r.setRatingAverage(u.getRatingAverage());
        r.setRatingCount(u.getRatingCount() == null ? 0 : u.getRatingCount());
        if (u.getFarmLocation() != null) {
            r.setLocation(u.getFarmLocation().getAddress());
            r.setLatitude(u.getFarmLocation().getLatitude());
            r.setLongitude(u.getFarmLocation().getLongitude());
        }
        return r;
    }

    public FarmerSummaryResponse getFarmerSummary(String farmerId) {
        User u = users.findById(farmerId).orElseThrow(() -> new IllegalStateException("User not found"));
        int trustPct = trust.trustOf(u);
        return new FarmerSummaryResponse("LKR " + LedgerService.format(income(farmerId, YearMonth.now())),
                String.valueOf(trustPct), trustPct, u.getRatingAverage());
    }
}
