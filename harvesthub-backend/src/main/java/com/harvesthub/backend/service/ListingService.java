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
        List<Listing> list = new java.util.ArrayList<>(listingRepository.findByFarmerIdAndStatusIn(
                farmerId, Arrays.asList("ACTIVE", "LISTED", "ONGOING", "PENDING", "DRAFT", "UNDER_REVIEW", "RETAKE_REQUESTED", "DEACTIVATED", "APPROVED")));
        list.sort((a, b) -> {
            String dateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
            String dateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
            return dateB.compareTo(dateA);
        });
        return list;
    }

        public List<Listing> getMarketplaceListings(String searchQuery, String grade) {
        return listingRepository.findByStatusIn(Arrays.asList("ACTIVE", "LISTED", "APPROVED")).stream()
            .filter(listing -> searchQuery == null || searchQuery.isBlank()
                || containsIgnoreCase(listing.getCropName(), searchQuery)
                || containsIgnoreCase(listing.getLocation(), searchQuery))
            .filter(listing -> grade == null || grade.isBlank()
                || containsIgnoreCase(listing.getGrade(), grade))
            .toList();
        }

        private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query.trim().toLowerCase());
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
            if (updates.getQuantity() != null) existing.setQuantity(updates.getQuantity());
            if (updates.getPrice() != null) existing.setPrice(updates.getPrice());
            if (updates.getLocation() != null) existing.setLocation(updates.getLocation());
            if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
            existing.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
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
