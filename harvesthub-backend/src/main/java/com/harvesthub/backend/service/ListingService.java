package com.harvesthub.backend.service;

import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ListingService {
    @Autowired
    private ListingRepository listingRepository;

    public List<Listing> getActiveListings(String farmerId) {
        return listingRepository.findByFarmerIdAndStatusIn(
                farmerId, Arrays.asList("ACTIVE", "LISTED", "ONGOING", "PENDING"));
    }

    public List<Listing> getListingHistory(String farmerId, String searchQuery) {
        List<Listing> listings;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            listings = listingRepository.findByFarmerIdAndCropNameContainingIgnoreCase(farmerId, searchQuery);
        } else {
            listings = listingRepository.findByFarmerId(farmerId);
        }
        return listings.stream()
                .filter(l -> Arrays.asList("DELIVERED", "CLOSED", "COMPLETED", "CANCELLED").contains(l.getStatus()))
                .collect(Collectors.toList());
    }

    public Listing createListing(Listing listing) {
        if (listing.getCreatedDate() == null) {
            listing.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        if (listing.getStatus() == null) {
            listing.setStatus("ACTIVE");
        }
        return listingRepository.save(listing);
    }

    public Listing getListingById(String id) {
        return listingRepository.findById(id).orElse(null);
    }

    public Listing updateListing(String id, Listing updates) {
        Listing existing = listingRepository.findById(id).orElse(null);
        if (existing != null) {
            // Update fields (simplistic approach)
            if (updates.getCropName() != null) existing.setCropName(updates.getCropName());
            if (updates.getQuantity() > 0) existing.setQuantity(updates.getQuantity());
            if (updates.getPricePerKg() > 0) existing.setPricePerKg(updates.getPricePerKg());
            return listingRepository.save(existing);
        }
        return null;
    }

    public Listing changeStatus(String id, String newStatus) {
        Listing existing = listingRepository.findById(id).orElse(null);
        if (existing != null) {
            existing.setStatus(newStatus);
            return listingRepository.save(existing);
        }
        return null;
    }

    public void deleteListing(String id) {
        listingRepository.deleteById(id);
    }
}
