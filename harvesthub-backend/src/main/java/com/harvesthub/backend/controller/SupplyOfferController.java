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

    @PostMapping({"/offers/create", "/api/offers/create"})
    public ResponseEntity<GenericResponse> createSupplyOffer(@RequestBody SupplyOfferRequest request) {
        currentUser.requireRole("FARMER");
        var me = userRepository.findById(currentUser.id()).orElseThrow();
        if (request.getQuantity() <= 0 || request.getPricePerKg() <= 0) {
            return ResponseEntity.badRequest().body(new GenericResponse(false, "Quantity and price must be positive"));
        }
        SupplyOffer offer = new SupplyOffer();
        offer.setFarmerId(me.getId());
        offer.setId("offer_" + System.currentTimeMillis());
        offer.setRequestId(request.getRequestId());
        offer.setBatchId(request.getBatchId());
        offer.setQuantity(request.getQuantity());
        offer.setPricePerKg(request.getPricePerKg());
        offer.setStatus("PENDING");
        offer.setLocation(me.getFarmLocation() != null ? me.getFarmLocation().getAddress() : null);
        offer.setFarmerName(me.getFullName());
        offer.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        supplyOfferRepository.save(offer);

        GenericResponse response = new GenericResponse(true, "Supply offer submitted successfully for request: " + request.getRequestId());
        return ResponseEntity.ok(response);
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
