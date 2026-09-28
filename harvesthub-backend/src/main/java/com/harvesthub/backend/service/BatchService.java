package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.SupplyOfferRequest;
import com.harvesthub.backend.model.CropBatch;
import com.harvesthub.backend.model.SupplyOffer;
import com.harvesthub.backend.repository.CropBatchRepository;
import com.harvesthub.backend.repository.SupplyOfferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class BatchService {
    @Autowired
    private CropBatchRepository cropBatchRepository;
    @Autowired
    private SupplyOfferRepository supplyOfferRepository;

    public List<CropBatch> getVerifiedBatches(String farmerId, String cropType) {
        return cropBatchRepository.findByFarmerIdAndCropTypeAndIsVerifiedTrue(farmerId, cropType);
    }

    public GenericResponse createSupplyOffer(SupplyOfferRequest request, String farmerId) {
        SupplyOffer offer = new SupplyOffer();
        offer.setRequestId(request.getRequestId());
        offer.setBatchId(request.getBatchId());
        offer.setFarmerId(farmerId);
        offer.setQuantity(request.getQuantity());
        offer.setPricePerKg(request.getPricePerKg());
        offer.setStatus("PENDING");
        offer.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        supplyOfferRepository.save(offer);
        return new GenericResponse(true, "Supply offer created successfully");
    }
}
