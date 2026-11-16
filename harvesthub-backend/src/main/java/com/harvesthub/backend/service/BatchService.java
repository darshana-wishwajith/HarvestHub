package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.dto.SupplyOfferRequest;
import com.harvesthub.backend.model.CropBatch;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.SupplyOffer;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.SupplyOfferRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * A "verified batch" (Supply Offer screen) is one of the farmer's officer-approved listings (status ACTIVE)
 * of the requested crop. There is no separate batch collection.
 */
@Service
public class BatchService {

    private final ListingRepository listings;
    private final SupplyOfferRepository supplyOfferRepository;

    public BatchService(ListingRepository listings, SupplyOfferRepository supplyOfferRepository) {
        this.listings = listings;
        this.supplyOfferRepository = supplyOfferRepository;
    }

    public List<CropBatch> getVerifiedBatches(String farmerId, String cropType) {
        return listings.findByFarmerIdAndStatusIn(farmerId, List.of("ACTIVE")).stream()
                .filter(l -> cropType == null || cropType.isBlank() || cropType.equalsIgnoreCase(l.getCropName()))
                .map(BatchService::toBatch)
                .toList();
    }

    static CropBatch toBatch(Listing l) {
        String number = l.getReferenceCode() != null ? l.getReferenceCode()
                : "#" + (l.getId().length() > 6 ? l.getId().substring(l.getId().length() - 6) : l.getId()).toUpperCase();
        return new CropBatch(l.getId(), number + (l.getGrade() == null ? "" : " · Grade " + l.getGrade()),
                l.getFarmerId(), l.getCropName(), l.quantityKgValue(), true, l.getCreatedDate());
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
