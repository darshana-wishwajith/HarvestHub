package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.BuyerDashboardResponse;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.SupplyOffer;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.SupplyOfferRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

/** Buyer home + escrow figures for the buyer wallet. */
@Service
public class BuyerDashboardService {

    /** Statuses where the next step is the buyer's. */
    static final Set<String> BUYER_ACTION = Set.of("NEGOTIATING", "AWAITING_PAYMENT", "AWAITING_BUYER_LOADING_APPROVAL",
            "ARRIVED_AT_DESTINATION", "BALANCE_PAID");

    private final UserRepository users;
    private final OrderRepository orders;
    private final SupplyOfferRepository offers;
    private final MarketService market;
    private final ListingService listings;
    private final LedgerService ledger;
    private final TrustService trust;

    public BuyerDashboardService(UserRepository users, OrderRepository orders, SupplyOfferRepository offers,
                                 MarketService market, ListingService listings, LedgerService ledger, TrustService trust) {
        this.users = users;
        this.orders = orders;
        this.offers = offers;
        this.market = market;
        this.listings = listings;
        this.ledger = ledger;
        this.trust = trust;
    }

    /** Buyer money currently in escrow for one order (advance + balance paid, minus what was released or refunded). */
    public static double escrowHeld(Order o) {
        Order.Payment p = o.getPayment();
        if (p == null || p.isSettled() || OrderService.FINAL.contains(o.getStatus())) return 0;
        double held = (p.isAdvancePaid() ? p.getAdvanceDue() : 0) + (p.isBalancePaid() ? p.getBalanceDue() : 0);
        if (p.isFarmerAdvanceReleased()) held -= p.getAdvanceCropPortion();
        return FeeCalculator.round2(Math.max(0, held));
    }

    public double escrowHeld(String buyerId) {
        return FeeCalculator.round2(orders.findByBuyerId(buyerId).stream().mapToDouble(BuyerDashboardService::escrowHeld).sum());
    }

    /** Total the buyer paid for orders that completed (released to farmer, transporter and platform). */
    public double escrowReleased(String buyerId) {
        return FeeCalculator.round2(orders.findByBuyerId(buyerId).stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()) && o.getPayment() != null)
                .mapToDouble(o -> o.getPayment().getAdvanceDue() + o.getPayment().getBalanceDue()).sum());
    }

    public BuyerDashboardResponse dashboard(String buyerId) {
        User u = users.findById(buyerId).orElseThrow(() -> new IllegalStateException("User not found"));
        BuyerDashboardResponse r = new BuyerDashboardResponse();
        r.setBuyerName(u.getFullName());
        int hour = LocalTime.now(ZoneId.of("Asia/Colombo")).getHour();
        String first = u.getFullName() == null ? "" : u.getFullName().trim().split("\\s+")[0];
        r.setGreeting((hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening") + ", " + first + "!");
        r.setHasDeliveryLocation(u.getFarmLocation() != null && u.getFarmLocation().getLatitude() != null
                && u.getFarmLocation().getLongitude() != null);
        r.setDeliveryAddress(u.getFarmLocation() == null ? null : u.getFarmLocation().getAddress());

        List<Order> mine = orders.findByBuyerId(buyerId);
        r.setActiveOrders((int) mine.stream().filter(o -> !OrderService.FINAL.contains(o.getStatus())).count());
        r.setActionRequired((int) mine.stream().filter(o -> BUYER_ACTION.contains(o.getStatus())
                && !("BALANCE_PAID".equals(o.getStatus()) && o.isBuyerConfirmedReceipt())).count());
        double held = 0;
        for (Order o : mine) {
            double h = escrowHeld(o);
            if (h > 0) {
                held += h;
                r.getEscrowOrders().add(new BuyerDashboardResponse.EscrowOrder(o.getOrderId(), o.getCropName(),
                        o.getFarmerName(), o.getStatus(), h));
            }
        }
        r.setEscrowHeld(FeeCalculator.round2(held));

        List<WantedAd> ads = market.getWantedAdsByBuyer(buyerId).stream()
                .filter(a -> "OPEN".equals(a.getStatus()) || "ACTIVE".equals(a.getStatus())).toList();
        r.setOpenWantedAds(ads.size());
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Colombo"));
        int pending = 0, expiring = 0;
        if (!ads.isEmpty()) {
            for (SupplyOffer of : offers.findByRequestIdIn(ads.stream().map(WantedAd::getRequestId).toList())) {
                if (!"PENDING".equals(of.getStatus())) continue;
                pending++;
                WantedAd ad = ads.stream().filter(a -> a.getRequestId().equals(of.getRequestId())).findFirst().orElse(null);
                if (ad != null && today.equals(MarketService.parseDeadline(ad.getDeadline()))) expiring++;
            }
        }
        r.setPendingOffers(pending);
        r.setOffersExpiringToday(expiring);

        r.setWalletBalance(ledger.wallet(buyerId).getBalance());
        r.setTrustPercent(trust.trustOf(u));
        r.setRatingAverage(u.getRatingAverage());
        r.setRatingCount(u.getRatingCount() == null ? 0 : u.getRatingCount());

        for (Listing l : listings.getMarketplaceListings(null, null).stream().limit(3).toList()) {
            String img = l.getMediaUrls() == null ? null : l.getMediaUrls().stream()
                    .filter(m -> m != null && !m.toLowerCase().matches(".*\\.(mp4|3gp|webm)$")).findFirst().orElse(null);
            r.getHighlights().add(new BuyerDashboardResponse.Highlight(l.getId(), l.getCropName(), l.getGrade(),
                    l.pricePerKgValue(), l.quantityKgValue(), l.getLocation(), l.getFarmerName(), img,
                    l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate()));
        }
        return r;
    }
}
