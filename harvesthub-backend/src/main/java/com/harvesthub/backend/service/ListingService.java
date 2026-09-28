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
        listing.setCreatedDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        listing.setStatus("ACTIVE");
        return listingRepository.save(listing);
    }
}
