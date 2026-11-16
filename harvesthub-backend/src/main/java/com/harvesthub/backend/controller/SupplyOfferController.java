package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.SupplyOfferRequest;
import com.harvesthub.backend.model.SupplyOffer;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.repository.SupplyOfferRepository;
import com.harvesthub.backend.repository.WantedAdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class SupplyOfferController {

    @Autowired
    private com.harvesthub.backend.security.CurrentUser currentUser;

    @Autowired
    private com.harvesthub.backend.repository.UserRepository userRepository;

    @Autowired
    private SupplyOfferRepository supplyOfferRepository;

    @Autowired
    private WantedAdRepository wantedAdRepository;

    @Autowired
    private com.harvesthub.backend.repository.ListingRepository listingRepository;
    @Autowired
    private com.harvesthub.backend.service.NotificationService notificationService;

    /**
     * Scenario B (workflow 2.1): a farmer answers a buyer's wanted ad with one of their verified
     * (ACTIVE, graded) listings of the same crop — the "batch".
     */
    @PostMapping({"/offers/create", "/api/offers/create"})
    public ResponseEntity<GenericResponse> createSupplyOffer(@RequestBody SupplyOfferRequest request) {
        currentUser.requireRole("FARMER");
        var me = userRepository.findById(currentUser.id()).orElseThrow();
        if (request.getQuantity() <= 0 || request.getPricePerKg() <= 0) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "Quantity and price must be positive"));
        }
        WantedAd ad = request.getRequestId() == null ? null : wantedAdRepository.findById(request.getRequestId()).orElse(null);
        if (ad == null) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "This wanted ad no longer exists"));
        }
        if ("CLOSED".equals(ad.getStatus())) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "This wanted ad is closed"));
        }
        var batch = request.getBatchId() == null ? null : listingRepository.findById(request.getBatchId()).orElse(null);
        if (batch == null || !me.getId().equals(batch.getFarmerId()) || !"ACTIVE".equals(batch.getStatus())) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "Attach one of your verified listings"));
        }
        if (ad.getCropName() != null && batch.getCropName() != null && !ad.getCropName().equalsIgnoreCase(batch.getCropName())) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "The batch must be " + ad.getCropName()));
        }
        if (batch.quantityKgValue() > 0 && request.getQuantity() > batch.quantityKgValue() * 1.05) {
            return ResponseEntity.badRequest().body(new GenericResponse(false,
                    String.format("Batch has only about %,.0f kg", batch.quantityKgValue())));
        }
        SupplyOffer offer = new SupplyOffer();
        offer.setFarmerId(me.getId());
        offer.setId("offer_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 6));
        offer.setRequestId(request.getRequestId());
        offer.setBatchId(request.getBatchId());
        offer.setQuantity(request.getQuantity());
        offer.setPricePerKg(request.getPricePerKg());
        offer.setStatus("PENDING");
        offer.setCropName(batch.getCropName());
        offer.setGrade(batch.getGrade());
        offer.setLocation(me.getFarmLocation() != null ? me.getFarmLocation().getAddress() : batch.getLocation());
        offer.setFarmerName(me.getFullName());
        offer.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        supplyOfferRepository.save(offer);
        notificationService.notify(ad.getBuyerId(), "ORDERS", "SUPPLY_OFFER", "New supply offer",
                String.format("%s offers %,.0f kg of Grade %s %s at LKR %,.0f/kg.", me.getFullName(),
                        request.getQuantity(), batch.getGrade() == null ? "-" : batch.getGrade(), batch.getCropName(),
                        request.getPricePerKg()));
        return ResponseEntity.ok(new GenericResponse(true, "Supply offer sent to the buyer"));
    }

    @GetMapping({"/offers", "/api/offers"})
    public ResponseEntity<List<SupplyOffer>> getAllOffers() {
        if (!currentUser.isStaff()) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(supplyOfferRepository.findAll());
    }

    @GetMapping({"/offers/buyer/{buyerId}", "/api/offers/buyer/{buyerId}"})
    public ResponseEntity<List<SupplyOffer>> getOffersForBuyer(@PathVariable String buyerId) {
        currentUser.requireSelfOrStaff(buyerId);
        List<WantedAd> buyerAds = wantedAdRepository.findByBuyerId(buyerId);
        if (buyerAds == null || buyerAds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<String> requestIds = buyerAds.stream().map(WantedAd::getRequestId).collect(Collectors.toList());
        return ResponseEntity.ok(supplyOfferRepository.findByRequestIdIn(requestIds));
    }

    @GetMapping({"/offers/request/{requestId}", "/api/offers/request/{requestId}"})
    public ResponseEntity<List<SupplyOffer>> getOffersByRequestId(@PathVariable String requestId) {
        WantedAd ad = wantedAdRepository.findById(requestId).orElse(null);
        if (ad == null) {
            return ResponseEntity.notFound().build();
        }
        currentUser.requireSelfOrStaff(ad.getBuyerId());
        return ResponseEntity.ok(supplyOfferRepository.findByRequestId(requestId));
    }
}
