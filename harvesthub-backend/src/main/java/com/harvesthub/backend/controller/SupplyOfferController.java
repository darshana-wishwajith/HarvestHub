package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.SupplyOfferRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class SupplyOfferController {

    @PostMapping({"/offers/create", "/api/offers/create"})
    public ResponseEntity<GenericResponse> createSupplyOffer(@RequestBody SupplyOfferRequest request) {
        GenericResponse response = new GenericResponse(true, "Supply offer submitted successfully for request: " + request.getRequestId());
        return ResponseEntity.ok(response);
    }
}
