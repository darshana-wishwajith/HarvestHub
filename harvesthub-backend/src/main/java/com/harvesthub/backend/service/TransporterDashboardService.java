package com.harvesthub.backend.service;

import com.harvesthub.backend.config.MarketplaceRules;
import com.harvesthub.backend.dto.TransporterDashboardResponse;
import com.harvesthub.backend.model.LogisticsRecord;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.Transaction;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.LogisticsRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.TransactionRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Figma transporter Home / Jobs / History data, built from real orders, logistics plans and wallet transactions. */
@Service
public class TransporterDashboardService {

    /** Statuses where the driver still has work to do on the order. */
    public static final Set<String> ACTIVE = Set.of("TRANSPORTER_ACCEPTED", "HARVESTING", "HARVEST_READY", "ARRIVED_AT_PICKUP",
            "AWAITING_BUYER_LOADING_APPROVAL", "LOADING_APPROVED", "IN_TRANSIT", "ARRIVED_AT_DESTINATION",
            "BALANCE_PAID", "DELIVERED", "RETURN_JOURNEY");
    private static final Set<String> EARNING_TYPES = Set.of("TRANSPORT_PAYOUT", "EMPTY_TRIP_FEE");

    private final UserRepository users;
    private final OrderRepository orders;
    private final LogisticsRepository logistics;
    private final TransactionRepository transactions;
    private final LedgerService ledger;
    private final MarketplaceRules rules;

    public TransporterDashboardService(UserRepository users, OrderRepository orders, LogisticsRepository logistics,
                                       TransactionRepository transactions, LedgerService ledger, MarketplaceRules rules) {
        this.users = users;
        this.orders = orders;
        this.logistics = logistics;
        this.transactions = transactions;
        this.ledger = ledger;
        this.rules = rules;
    }

    public TransporterDashboardResponse dashboard(String transporterId) {
        User u = users.findById(transporterId).orElseThrow(() -> new IllegalStateException("User not found"));
        TransporterDashboardResponse r = new TransporterDashboardResponse();
        r.setFullName(u.getFullName());
        r.setOnline(!Boolean.FALSE.equals(u.getOnline()));
        r.setVehicleType(u.getVehicleType());
        MarketplaceRules.Vehicle v = rules.getVehicles().stream()
                .filter(x -> x.getType().equals(u.getVehicleType())).findFirst().orElse(null);
        r.setVehicleLabel(v == null ? u.getVehicleType() : v.getLabel());
        r.setCapacityKg(u.getVehicleCapacityKg() != null ? u.getVehicleCapacityKg() : v == null ? 0 : v.getCapacityKg());
        r.setVehiclePlate(u.getVehiclePlate());
        r.setVehicleBodyType(u.getVehicleBodyType());
        r.setLicenceUploaded(u.getLicenceUrl() != null);
        r.setBaseAddress(u.getFarmLocation() == null ? null : u.getFarmLocation().getAddress());
        r.setRatingAverage(u.getRatingAverage());
        r.setRatingCount(u.getRatingCount());

        var w = ledger.wallet(transporterId);
        r.setBalance(FeeCalculator.round2(w.getBalance()));
        r.setSuspended(w.isSuspended());
        r.setAmountOwed(w.isSuspended() ? FeeCalculator.round2(-w.getBalance()) : 0);

        // earnings from the ledger
        String today = LocalDate.now().toString();
        String weekStart = LocalDate.now().minusDays(6).toString();
        List<Transaction> txns = transactions.findByFarmerId(transporterId);
        for (Transaction t : txns) {
            String day = t.getTimestamp() == null ? "" : t.getTimestamp();
            if (EARNING_TYPES.contains(t.getType()) && t.isCredit()) {
                r.setEarningsTotal(r.getEarningsTotal() + t.getAmountValue());
                if (day.startsWith(today)) r.setEarningsToday(r.getEarningsToday() + t.getAmountValue());
                if (day.compareTo(weekStart) >= 0) r.setEarningsWeek(r.getEarningsWeek() + t.getAmountValue());
                if ("EMPTY_TRIP_FEE".equals(t.getType())) r.setEmptyTripTotal(r.getEmptyTripTotal() + t.getAmountValue());
            }
            if ("DAMAGE_CHARGE".equals(t.getType()) && !t.isCredit()) {
                r.setDamageChargesTotal(r.getDamageChargesTotal() + t.getAmountValue());
            }
        }
        txns.stream()
                .filter(t -> t.getType() != null && Set.of("TRANSPORT_PAYOUT", "EMPTY_TRIP_FEE", "DAMAGE_CHARGE", "DEPOSIT", "WITHDRAWAL").contains(t.getType()))
                .sorted(Comparator.comparing((Transaction t) -> t.getTimestamp() == null ? "" : t.getTimestamp()).reversed())
                .limit(10)
                .forEach(t -> {
                    TransporterDashboardResponse.Payout p = new TransporterDashboardResponse.Payout();
                    p.setTitle(t.getTitle());
                    p.setType(t.getType());
                    p.setAmount(t.isCredit() ? t.getAmountValue() : -t.getAmountValue());
                    p.setTimestamp(t.getTimestamp());
                    p.setOrderId(t.getOrderId());
                    r.getRecentPayouts().add(p);
                });

        // jobs
        List<Order> mine = orders.findByTransporterId(transporterId);
        for (Order o : mine) {
            String s = o.getStatus() == null ? "" : o.getStatus();
            switch (s) {
                case "COMPLETED" -> {
                    r.setTripsCompleted(r.getTripsCompleted() + 1);
                    if (o.getPayment() != null) {
                        r.setGrossFreightTotal(r.getGrossFreightTotal() + o.getPayment().getTransportFee());
                        r.setCommissionTotal(r.getCommissionTotal() + FeeCalculator.platformCommission(o.getPayment(), rules));
                    }
                    String end = o.getActualDeliveryDate() == null ? "" : o.getActualDeliveryDate();
                    if (end.compareTo(weekStart) >= 0) r.setTripsThisWeek(r.getTripsThisWeek() + 1);
                }
                case "PRELOAD_REJECTED" -> r.setEmptyTrips(r.getEmptyTrips() + 1);
                case "RETURN_JOURNEY", "RETURNED" -> r.setRejectedDeliveries(r.getRejectedDeliveries() + 1);
                case "TRANSPORTER_ASSIGNED" -> {
                    if (o.getTransportRequestExpiresAt() == null || o.getTransportRequestExpiresAt() > System.currentTimeMillis()) {
                        r.getPendingRequests().add(job(o));
                    }
                }
                default -> { }
            }
            if (ACTIVE.contains(s) && r.getActiveJob() == null) r.setActiveJob(job(o));
        }
        r.setBusy(r.getActiveJob() != null);
        r.setEarningsTotal(FeeCalculator.round2(r.getEarningsTotal()));
        r.setEarningsToday(FeeCalculator.round2(r.getEarningsToday()));
        r.setEarningsWeek(FeeCalculator.round2(r.getEarningsWeek()));
        r.setGrossFreightTotal(FeeCalculator.round2(r.getGrossFreightTotal()));
        r.setCommissionTotal(FeeCalculator.round2(r.getCommissionTotal()));
        r.setEmptyTripTotal(FeeCalculator.round2(r.getEmptyTripTotal()));
        r.setDamageChargesTotal(FeeCalculator.round2(r.getDamageChargesTotal()));
        return r;
    }

    public TransporterDashboardResponse.Job job(Order o) {
        TransporterDashboardResponse.Job j = new TransporterDashboardResponse.Job();
        j.setOrderId(o.getOrderId());
        j.setStatus(o.getStatus());
        j.setCropName(o.getCropName());
        j.setQuantityKg(o.getQuantity());
        j.setPickupAddress(o.getPickupAddress());
        j.setDropoffAddress(o.getDropAddress());
        j.setFarmerName(o.getFarmerName());
        j.setBuyerName(o.getBuyerName());
        j.setVehicleLabel(o.getVehicleLabel());
        j.setExpiresAt("TRANSPORTER_ASSIGNED".equals(o.getStatus()) ? o.getTransportRequestExpiresAt() : null);
        LogisticsRecord lr = logistics.findByOrderId(o.getOrderId()).orElse(null);
        if (lr != null) j.setDistanceKm(lr.getDistanceKm());
        if (o.getPayment() != null) {
            j.setTransportFee(o.getPayment().getTransportFee());
            j.setPayout(FeeCalculator.transporterPayout(o.getPayment(), rules));
        } else if (lr != null) {
            j.setTransportFee(lr.getTransportFee());
            j.setPayout(FeeCalculator.round2(lr.getTransportFee() * (1 - rules.getTransporterCommissionRate())));
        }
        return j;
    }

    public boolean hasActiveJob(String transporterId) {
        return orders.findByTransporterIdAndStatusIn(transporterId, List.copyOf(ACTIVE)).stream().findAny().isPresent();
    }
}
