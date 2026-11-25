package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.MarketCenterResponse;
import com.harvesthub.backend.dto.MarketTrendResponse;
import com.harvesthub.backend.model.Crop;
import com.harvesthub.backend.model.MarketPrice;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.repository.CropRepository;
import com.harvesthub.backend.repository.MarketPriceRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.WantedAdRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Market Insights from the market_prices collection plus prices of completed orders.
 * No random or fixed numbers: with no data the response says so (samples = 0).
 */
@Service
public class MarketService {

    private final WantedAdRepository wantedAdRepository;
    private final MarketPriceRepository prices;
    private final OrderRepository orders;
    private final CropRepository crops;
    private final com.harvesthub.backend.repository.SupplyOfferRepository offerRepository;

    public MarketService(WantedAdRepository wantedAdRepository, MarketPriceRepository prices,
                         OrderRepository orders, CropRepository crops,
                         com.harvesthub.backend.repository.SupplyOfferRepository offerRepository) {
        this.offerRepository = offerRepository;
        this.wantedAdRepository = wantedAdRepository;
        this.prices = prices;
        this.orders = orders;
        this.crops = crops;
    }

    public MarketTrendResponse getMarketTrend(String crop, int days) {
        int window = Math.max(1, Math.min(days, 90));
        LocalDate from = LocalDate.now().minusDays(window - 1L);
        TreeMap<String, List<Double>> byDay = new TreeMap<>();
        List<MarketPrice> records = prices.findByCropNameIgnoreCaseAndDateGreaterThanEqualOrderByDateAsc(crop, from.toString());
        records.forEach(p -> byDay.computeIfAbsent(p.getDate(), d -> new ArrayList<>()).add(p.getPricePerKg()));
        int samples = records.size();
        for (Order o : orders.findByStatus("COMPLETED")) {
            String day = o.getActualDeliveryDate() != null && o.getActualDeliveryDate().length() >= 10
                    ? o.getActualDeliveryDate().substring(0, 10) : null;
            if (day != null && crop.equalsIgnoreCase(o.getCropName()) && day.compareTo(from.toString()) >= 0) {
                byDay.computeIfAbsent(day, d -> new ArrayList<>()).add(o.getPricePerKg());
                samples++;
            }
        }
        List<String> labels = new ArrayList<>(byDay.keySet());
        List<Double> trend = byDay.values().stream()
                .map(v -> FeeCalculator.round2(v.stream().mapToDouble(Double::doubleValue).average().orElse(0)))
                .collect(Collectors.toList());
        Double current = trend.isEmpty() ? null : trend.get(trend.size() - 1);
        Double change = trend.size() < 2 || trend.get(0) == 0 ? null
                : FeeCalculator.round2((current - trend.get(0)) / trend.get(0) * 100);
        return new MarketTrendResponse(trend, crop, labels, current, change, insight(crop, change, window), samples);
    }

    private String insight(String crop, Double change, int window) {
        if (change == null) return "Not enough price data for " + crop + " yet.";
        List<MarketCenterResponse> centers = getMarketCenters(crop);
        Optional<MarketCenterResponse> best = centers.stream().filter(c -> "HARVEST NOW".equals(c.getRecommendation())).findFirst();
        Optional<MarketCenterResponse> glut = centers.stream().filter(c -> "High".equals(c.getSupply())).findFirst();
        if (change >= 3) {
            return String.format("%s prices rose %.1f%% in %d days.%s", crop, change, window,
                    best.map(c -> " Demand is high at " + c.getName() + " – a good time to list.").orElse(" Consider listing soon."));
        }
        if (change <= -3) {
            return String.format("%s prices fell %.1f%% in %d days.%s", crop, -change, window,
                    glut.map(c -> " Supply is high at " + c.getName() + " – consider delaying your harvest.").orElse(""));
        }
        return String.format("%s prices are stable (%+.1f%% in %d days).", crop, change, window);
    }

    /** Latest situation per economic centre for a crop (or all crops when crop is blank). */
    public List<MarketCenterResponse> getMarketCenters(String crop) {
        LocalDate today = LocalDate.now();
        List<MarketPrice> recent = prices.findByDateGreaterThanEqual(today.minusDays(14).toString()).stream()
                .filter(p -> crop == null || crop.isBlank() || crop.equalsIgnoreCase(p.getCropName()))
                .toList();
        Map<String, List<MarketPrice>> byCenter = recent.stream().collect(Collectors.groupingBy(MarketPrice::getCenter));
        List<MarketCenterResponse> out = new ArrayList<>();
        byCenter.forEach((center, list) -> {
            list = new ArrayList<>(list);
            list.sort(Comparator.comparing(MarketPrice::getDate));
            MarketPrice latest = list.get(list.size() - 1);
            double first = list.get(0).getPricePerKg();
            double rise = first == 0 ? 0 : (latest.getPricePerKg() - first) / first;
            String supply = cap(latest.getSupplyLevel()), demand = cap(latest.getDemandLevel());
            String rec;
            if ("High".equals(demand) && !"High".equals(supply) || rise > 0.05) rec = "HARVEST NOW";
            else if ("High".equals(supply) && !"High".equals(demand) || rise < -0.05) rec = "DELAY HARVEST";
            else rec = "HOLD";
            long age = ChronoUnit.DAYS.between(LocalDate.parse(latest.getDate()), today);
            out.add(new MarketCenterResponse(center, latest.getProvince(), rec, supply, percent(supply), demand, percent(demand),
                    age <= 0 ? "Today" : age == 1 ? "Yesterday" : age + " days ago", false, latest.getPricePerKg()));
        });
        out.sort(Comparator.comparing((MarketCenterResponse c) -> !"HARVEST NOW".equals(c.getRecommendation()))
                .thenComparing(c -> -c.getDemandPercent()));
        if (!out.isEmpty()) out.get(0).setHighlighted(true);
        return out;
    }

    private static String cap(String level) {
        if (level == null) return "Medium";
        String l = level.toLowerCase();
        return Character.toUpperCase(l.charAt(0)) + l.substring(1);
    }

    private static int percent(String level) {
        return switch (level) {
            case "Low" -> 30;
            case "High" -> 85;
            default -> 60;
        };
    }

    /** Verified catalogue plus crops this farmer added that are still pending. */
    public List<Crop> getCrops(String userId) {
        return crops.findAll().stream()
                .filter(c -> !c.isPending() || Objects.equals(userId, c.getAddedByUserId()))
                .sorted(Comparator.comparing(Crop::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<WantedAd> getWantedAds(String cropFilter, String searchQuery) {
        List<WantedAd> ads;
        if (cropFilter != null && !cropFilter.isEmpty() && !"All".equalsIgnoreCase(cropFilter)) {
            ads = wantedAdRepository.findByCropNameContainingIgnoreCase(cropFilter);
        } else {
            ads = wantedAdRepository.findAll();
        }
        ads.forEach(this::closeIfExpired);
        ads = ads.stream().filter(MarketService::isOpen).collect(Collectors.toList());
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.toLowerCase();
            ads = ads.stream()
                    .filter(ad -> (ad.getCropName() != null && ad.getCropName().toLowerCase().contains(query))
                            || (ad.getLocation() != null && ad.getLocation().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }
        ads.sort(Comparator.comparing((WantedAd a) -> !a.isUrgent())
                .thenComparing(a -> String.valueOf(a.getCreatedDate()), Comparator.reverseOrder()));
        return ads;
    }

    public WantedAd createWantedAd(WantedAd ad) {
        if (ad.getRequestId() == null || ad.getRequestId().trim().isEmpty()) {
            ad.setRequestId("REQ-" + System.currentTimeMillis());
        }
        if (ad.getCreatedDate() == null || ad.getCreatedDate().trim().isEmpty()) {
            ad.setCreatedDate(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (ad.getStatus() == null || ad.getStatus().trim().isEmpty()) ad.setStatus("OPEN");
        return wantedAdRepository.save(ad);
    }

    public List<WantedAd> getWantedAdsByBuyer(String buyerId) {
        List<WantedAd> ads = new java.util.ArrayList<>(wantedAdRepository.findByBuyerId(buyerId));
        ads.forEach(this::closeIfExpired);
        java.util.Map<String, List<com.harvesthub.backend.model.SupplyOffer>> byAd = ads.isEmpty() ? java.util.Map.of()
                : offerRepository.findByRequestIdIn(ads.stream().map(WantedAd::getRequestId).toList()).stream()
                .collect(Collectors.groupingBy(com.harvesthub.backend.model.SupplyOffer::getRequestId));
        for (WantedAd ad : ads) {
            List<com.harvesthub.backend.model.SupplyOffer> list = byAd.getOrDefault(ad.getRequestId(), List.of());
            ad.setOfferCount(list.size());
            ad.setPendingOfferCount((int) list.stream().filter(o -> "PENDING".equals(o.getStatus())).count());
        }
        ads.sort(Comparator.comparing((WantedAd a) -> String.valueOf(a.getCreatedDate()), Comparator.reverseOrder()));
        return ads;
    }

    private WantedAd ownAd(String adId, String buyerId) {
        WantedAd ad = wantedAdRepository.findById(adId).orElseThrow(() -> new IllegalStateException("Wanted ad not found"));
        if (!buyerId.equals(ad.getBuyerId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not your wanted ad");
        }
        return ad;
    }

    private static boolean isOpen(WantedAd ad) {
        return ad.getStatus() == null || "OPEN".equals(ad.getStatus()) || "ACTIVE".equals(ad.getStatus());
    }

    /** Edit is allowed only while the ad is open and no offer was accepted. */
    public WantedAd updateWantedAd(String adId, String buyerId, WantedAd changes) {
        WantedAd ad = ownAd(adId, buyerId);
        if (!isOpen(ad)) throw new IllegalStateException("Only open requests can be edited");
        boolean accepted = offerRepository.findByRequestId(adId).stream().anyMatch(o -> "ACCEPTED".equals(o.getStatus()));
        if (accepted) throw new IllegalStateException("An offer was already accepted for this request");
        if (changes.getCropName() != null && !changes.getCropName().isBlank()) ad.setCropName(changes.getCropName().trim());
        if (changes.getRequiredQuantityKg() > 0) ad.setRequiredQuantityKg(changes.getRequiredQuantityKg());
        if (changes.getTargetPricePerKg() >= 0) ad.setTargetPricePerKg(changes.getTargetPricePerKg());
        if (changes.getDeadline() != null && !changes.getDeadline().isBlank()) ad.setDeadline(changes.getDeadline());
        if (changes.getTargetGrade() != null) ad.setTargetGrade(changes.getTargetGrade());
        if (changes.getNotes() != null) ad.setNotes(changes.getNotes());
        if (changes.getLocation() != null && !changes.getLocation().isBlank()) ad.setLocation(changes.getLocation());
        ad.setUrgent(changes.isUrgent());
        return wantedAdRepository.save(ad);
    }

    public WantedAd closeWantedAd(String adId, String buyerId) {
        WantedAd ad = ownAd(adId, buyerId);
        if (!isOpen(ad)) return ad;
        close(ad, "MANUAL");
        return ad;
    }

    /** Republish = a new open copy with a new deadline. */
    public WantedAd republishWantedAd(String adId, String buyerId, String deadline) {
        WantedAd old = ownAd(adId, buyerId);
        if (isOpen(old)) throw new IllegalStateException("This request is still open");
        if (deadline == null || deadline.isBlank()) throw new IllegalStateException("Choose a new deadline");
        if (parseDeadline(deadline) != null && parseDeadline(deadline).isBefore(java.time.LocalDate.now())) {
            throw new IllegalStateException("The deadline must be today or later");
        }
        WantedAd copy = new WantedAd();
        copy.setCropName(old.getCropName());
        copy.setLocation(old.getLocation());
        copy.setRequiredQuantityKg(old.getRequiredQuantityKg());
        copy.setTargetPricePerKg(old.getTargetPricePerKg());
        copy.setUrgent(old.isUrgent());
        copy.setBuyerId(old.getBuyerId());
        copy.setBuyerName(old.getBuyerName());
        copy.setTargetGrade(old.getTargetGrade());
        copy.setNotes(old.getNotes());
        copy.setDeadline(deadline);
        copy.setRepublishedFrom(old.getRequestId());
        return createWantedAd(copy);
    }

    private void close(WantedAd ad, String reason) {
        ad.setStatus("CLOSED");
        ad.setClosedReason(reason);
        ad.setClosedDate(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        wantedAdRepository.save(ad);
    }

    private void closeIfExpired(WantedAd ad) {
        java.time.LocalDate d = parseDeadline(ad.getDeadline());
        if (isOpen(ad) && d != null && d.isBefore(java.time.LocalDate.now())) close(ad, "DEADLINE");
    }

    /** Ads close automatically after their deadline day. */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${harvesthub.wanted-ad-check-ms:3600000}", initialDelay = 120000)
    public void closeExpiredAds() {
        wantedAdRepository.findAll().forEach(this::closeIfExpired);
    }

    /** Accepts yyyy-MM-dd (current app) and "MMM dd, yyyy" (older app builds / seed data). */
    static java.time.LocalDate parseDeadline(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        for (String pattern : new String[]{"yyyy-MM-dd", "MMM dd, yyyy", "MMM d, yyyy", "dd/MM/yyyy"}) {
            try {
                return java.time.LocalDate.parse(v.length() > 10 && pattern.equals("yyyy-MM-dd") ? v.substring(0, 10) : v,
                        java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH));
            } catch (Exception ignored) { }
        }
        return null;
    }
}
