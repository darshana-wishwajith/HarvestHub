package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.SupplyOfferRequest;
import com.harvesthub.backend.model.CropBatch;
import com.harvesthub.backend.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BatchController {
    @Autowired
    private BatchService batchService;

    @GetMapping("batches/verified")
    public ResponseEntity<List<CropBatch>> getVerifiedBatches(
            @RequestParam("farmerId") String farmerId,
            @RequestParam("cropType") String cropType) {
        return ResponseEntity.ok(batchService.getVerifiedBatches(farmerId, cropType));
    }

    @PostMapping("offers/create")
    public ResponseEntity<GenericResponse> createSupplyOffer(@RequestBody SupplyOfferRequest request) {
        return ResponseEntity.ok(batchService.createSupplyOffer(request, "current-user"));
    }
}
