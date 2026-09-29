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
@CrossOrigin(origins = "*")
public class SupplyOfferController {

    @Autowired
    private SupplyOfferRepository supplyOfferRepository;

    @Autowired
    private WantedAdRepository wantedAdRepository;

    @PostMapping({"/offers/create", "/api/offers/create"})
    public ResponseEntity<GenericResponse> createSupplyOffer(@RequestBody SupplyOfferRequest request) {
        SupplyOffer offer = new SupplyOffer();
        offer.setId("offer_" + System.currentTimeMillis());
        offer.setRequestId(request.getRequestId());
        offer.setBatchId(request.getBatchId());
        offer.setQuantity(request.getQuantity());
        offer.setPricePerKg(request.getPricePerKg());
        offer.setStatus("PENDING");
        offer.setGrade("Grade A");
        offer.setLocation("Dambulla Dedicated Economic Centre");
        offer.setFarmerName("W.B.D.W. Bandara");
        offer.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        supplyOfferRepository.save(offer);

        GenericResponse response = new GenericResponse(true, "Supply offer submitted successfully for request: " + request.getRequestId());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/offers", "/api/offers"})
    public ResponseEntity<List<SupplyOffer>> getAllOffers() {
        return ResponseEntity.ok(supplyOfferRepository.findAll());
    }

    @GetMapping({"/offers/buyer/{buyerId}", "/api/offers/buyer/{buyerId}"})
    public ResponseEntity<List<SupplyOffer>> getOffersForBuyer(@PathVariable String buyerId) {
        List<WantedAd> buyerAds = wantedAdRepository.findByBuyerId(buyerId);
        if (buyerAds == null || buyerAds.isEmpty()) {
            return ResponseEntity.ok(supplyOfferRepository.findAll());
        }
        List<String> requestIds = buyerAds.stream().map(WantedAd::getRequestId).collect(Collectors.toList());
        List<SupplyOffer> offers = supplyOfferRepository.findByRequestIdIn(requestIds);
        if (offers == null || offers.isEmpty()) {
            return ResponseEntity.ok(supplyOfferRepository.findAll());
        }
        return ResponseEntity.ok(offers);
    }

    @GetMapping({"/offers/request/{requestId}", "/api/offers/request/{requestId}"})
    public ResponseEntity<List<SupplyOffer>> getOffersByRequestId(@PathVariable String requestId) {
        List<SupplyOffer> offers = supplyOfferRepository.findByRequestId(requestId);
        if (offers == null || offers.isEmpty()) {
            return ResponseEntity.ok(supplyOfferRepository.findAll());
        }
        return ResponseEntity.ok(offers);
    }
}
