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
    @Autowired
    private com.harvesthub.backend.repository.UserRepository userRepository;
    @Autowired
    private com.harvesthub.backend.repository.CropRepository cropRepository;
    @Autowired
    private LedgerService ledger;
    @Autowired
    private MediaStorageService mediaStorage;
    @Autowired
    private com.harvesthub.backend.config.MarketplaceRules rules;

    /** Listing statuses shown on My Listings (everything except history). */
    public static final List<String> MY_LISTING_STATUSES = Arrays.asList(
            "DRAFT", "PENDING", "UNDER_REVIEW", "ACTIVE", "RETAKE_REQUESTED", "REJECTED", "ONGOING", "DEACTIVATED");

    public java.util.Map<String, Object> locationCheck(String farmerId, double lat, double lng) {
        var farmer = userRepository.findById(farmerId).orElseThrow(() -> new IllegalStateException("User not found"));
        var farm = farmer.getFarmLocation();
        if (farm == null || farm.getLatitude() == null || farm.getLongitude() == null) {
            throw new IllegalStateException("Register your farm location first");
        }
        double meters = FeeCalculator.haversineKm(lat, lng, farm.getLatitude(), farm.getLongitude()) * 1000;
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("withinFarm", meters <= rules.getFarmRadiusMeters());
        out.put("distanceMeters", Math.round(meters));
        out.put("radiusMeters", rules.getFarmRadiusMeters());
        out.put("farmAddress", farm.getAddress());
        return out;
    }

    /**
     * Creates (existingId == null) or re-submits after a retake request. Enforces workflow 1.2-1.4 and 5.3:
     * not suspended, physically at the registered farm, 2 photos + 1 video captured in the app.
     */
    public Listing submitListing(Listing in, String farmerId, String existingId) {
        var farmer = userRepository.findById(farmerId).orElseThrow(() -> new IllegalStateException("User not found"));
        if (ledger.wallet(farmerId).isSuspended()) {
            throw new IllegalStateException("Your wallet balance is negative. Pay back the Empty Trip Fee before listing new crops.");
        }
        if (in.getGpsLatitude() == null || in.getGpsLongitude() == null) {
            throw new IllegalStateException("Location is required. Turn on GPS and try again.");
        }
        var check = locationCheck(farmerId, in.getGpsLatitude(), in.getGpsLongitude());
        if (!Boolean.TRUE.equals(check.get("withinFarm"))) {
            throw new IllegalStateException("You must be at your registered farm to submit a listing (you are "
                    + check.get("distanceMeters") + " m away).");
        }
        List<String> media = in.getMediaUrls();
        if (media == null || media.size() < 3) {
            throw new IllegalStateException("Capture the wide-angle photo, the close-up photo and the 10-second video.");
        }
        for (String url : media) {
            String name = url == null ? "" : url.substring(url.lastIndexOf('/') + 1);
            if (url == null || !url.startsWith("/uploads/") || mediaStorage.resolve(name) == null) {
                throw new IllegalStateException("A captured file was not uploaded. Please capture it again.");
            }
        }

        Listing target;
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        if (existingId != null) {
            target = listingRepository.findById(existingId).orElseThrow(() -> new IllegalStateException("Listing not found"));
            if (!farmerId.equals(target.getFarmerId())) throw new IllegalStateException("Not your listing");
            if (!Arrays.asList("RETAKE_REQUESTED", "DRAFT").contains(target.getStatus())) {
                throw new IllegalStateException("This listing does not need new photos");
            }
        } else {
            double qty = Listing.parseNumber(in.getQuantity());
            double price = Listing.parseNumber(in.getPrice());
            if (in.getCropName() == null || in.getCropName().isBlank()) throw new IllegalStateException("Choose a crop");
            if (qty <= 0) throw new IllegalStateException("Enter the estimated total yield in kg");
            if (price <= 0) throw new IllegalStateException("Enter the expected price per kg");
            target = new Listing();
            target.setFarmerId(farmerId);
            target.setFarmerName(farmer.getFullName());
            target.setCropName(in.getCropName().trim());
            target.setVariety(in.getVariety());
            target.setQuantity(stripZeros(qty));
            target.setPrice(stripZeros(price));
            target.setCreatedDate(now);
            target.setReferenceCode(referenceCode(target.getCropName()));
            target.setVariancePercent("5");
            target.setCropPending(ensureCrop(target.getCropName(), farmerId, price));
        }
        target.setLocation(farmer.getFarmLocation() != null && farmer.getFarmLocation().getAddress() != null
                ? farmer.getFarmLocation().getAddress() : target.getLocation());
        target.setGpsLatitude(in.getGpsLatitude());
        target.setGpsLongitude(in.getGpsLongitude());
        target.setGpsCoordinates(String.format(java.util.Locale.US, "%.6f, %.6f", in.getGpsLatitude(), in.getGpsLongitude()));
        target.setDistanceFromFarmMeters(((Number) check.get("distanceMeters")).doubleValue());
        target.setMediaUrls(new java.util.ArrayList<>(media.subList(0, 3)));
        target.setMediaFileCount(3);
        target.setMediaCaption1("Wide angle - cultivated area");
        target.setMediaCaption2("Close up - harvested sample");
        target.setVideoLabel("10 s field + 360 degree sample video");
        target.setDeviceModel(in.getDeviceModel());
        target.setCapturedAt(in.getCapturedAt() != null ? in.getCapturedAt() : now);
        target.setStatus("PENDING");
        target.setGrade(null);
        target.setClaimedByOfficerId(null);
        target.setClaimedByOfficerName(null);
        target.setUpdatedTime(now);
        return listingRepository.save(target);
    }

    /** true = crop was added as pending (not in the verified catalogue). */
    private boolean ensureCrop(String name, String farmerId, double price) {
        var existing = cropRepository.findFirstByNameIgnoreCase(name);
        if (existing.isPresent()) return existing.get().isPending();
        var crop = new com.harvesthub.backend.model.Crop(null, name, "Added by farmer", "kg", price, false, farmerId,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        cropRepository.save(crop);
        return true;
    }

    static String referenceCode(String crop) {
        String letters = crop.replaceAll("[^A-Za-z ]", "").trim();
        if (letters.isEmpty()) letters = "CR";
        String[] parts = letters.split("\\s+");
        String code = parts.length > 1 ? ("" + parts[0].charAt(0) + parts[1].charAt(0))
                : letters.substring(0, Math.min(2, letters.length()));
        return code.toUpperCase() + "-" + (1000 + new java.security.SecureRandom().nextInt(9000));
    }

    private static String stripZeros(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    public List<Listing> getActiveListings(String farmerId) {
        List<Listing> list = new java.util.ArrayList<>(listingRepository.findByFarmerIdAndStatusIn(farmerId, MY_LISTING_STATUSES));
        list.sort((a, b) -> {
            String dateA = a.getCreatedDate() != null ? a.getCreatedDate() : "";
            String dateB = b.getCreatedDate() != null ? b.getCreatedDate() : "";
            return dateB.compareTo(dateA);
        });
        return list;
    }

        public List<Listing> getMarketplaceListings(String searchQuery, String grade) {
        return getMarketplaceListings(searchQuery, grade, null, null, null, null);
        }

    /**
     * Buyer marketplace (Figma buyer 2.1/2.2). Only verified ACTIVE lots; optional filters: grade(s) "A,B",
     * crop category (from the crop catalogue), district/location text, max price per kg, min quantity. Newest first.
     */
    public List<Listing> getMarketplaceListings(String searchQuery, String grade, String category, String district,
                                                Double maxPrice, Double minQty) {
        java.util.Set<String> grades = grade == null || grade.isBlank() || "All".equalsIgnoreCase(grade) ? java.util.Set.of()
                : Arrays.stream(grade.split(",")).map(g -> g.trim().toUpperCase().replace("GRADE", "").trim())
                .filter(g -> !g.isEmpty()).collect(Collectors.toSet());
        java.util.Set<String> categoryCrops = category == null || category.isBlank() || "All".equalsIgnoreCase(category) ? null
                : cropRepository.findAll().stream()
                .filter(c -> c.getCategory() != null && Arrays.stream(category.split(","))
                        .anyMatch(cat -> c.getCategory().equalsIgnoreCase(cat.trim())
                                || c.getCategory().toLowerCase().startsWith(cat.trim().toLowerCase().replaceAll("s$", ""))))
                .map(c -> c.getName().toLowerCase()).collect(Collectors.toSet());
        return listingRepository.findByStatusIn(Arrays.asList("ACTIVE", "LISTED", "APPROVED")).stream()
            .filter(listing -> searchQuery == null || searchQuery.isBlank()
                || containsIgnoreCase(listing.getCropName(), searchQuery)
                || containsIgnoreCase(listing.getLocation(), searchQuery))
            .filter(listing -> grades.isEmpty() || (listing.getGrade() != null
                && grades.contains(listing.getGrade().toUpperCase().replace("GRADE", "").trim())))
            .filter(listing -> categoryCrops == null || (listing.getCropName() != null
                && categoryCrops.contains(listing.getCropName().toLowerCase())))
            .filter(listing -> district == null || district.isBlank() || containsIgnoreCase(listing.getLocation(), district))
            .filter(listing -> maxPrice == null || maxPrice <= 0 || listing.pricePerKgValue() <= maxPrice)
            .filter(listing -> minQty == null || minQty <= 0 || listing.quantityKgValue() >= minQty)
            .sorted(java.util.Comparator.comparing((Listing l) -> String.valueOf(
                    l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate())).reversed())
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
            boolean materialChange = (updates.getCropName() != null && !updates.getCropName().equals(existing.getCropName()))
                    || (updates.getLocation() != null && !updates.getLocation().equals(existing.getLocation()));
            if (updates.getCropName() != null) existing.setCropName(updates.getCropName());
            if (updates.getQuantity() != null) existing.setQuantity(updates.getQuantity());
            if (updates.getPrice() != null) existing.setPrice(updates.getPrice());
            if (updates.getLocation() != null) existing.setLocation(updates.getLocation());
            // Status is never taken from the client. Changing what/where is sold invalidates the grade.
            if (materialChange && "ACTIVE".equals(existing.getStatus())) {
                existing.setStatus("PENDING");
                existing.setGrade(null);
                existing.setClaimedByOfficerId(null);
                existing.setClaimedByOfficerName(null);
            }
            existing.setUpdatedTime(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return listingRepository.save(existing);
        }
        return null;
    }

    // ---------------------------------------------------------------- farmer card actions (user rules, 16 Nov)

    private Listing ownListing(String id, String farmerId) {
        Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalStateException("Listing not found"));
        if (!farmerId.equals(l.getFarmerId())) throw new IllegalStateException("Not your listing");
        return l;
    }

    private static String nowIso() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Edit = estimated yield and price per kg only. Crop and media stay (they were verified with the photos),
     * so the status and grade are kept. Not while an order is running.
     */
    public Listing editYieldAndPrice(String id, String farmerId, String quantity, String price) {
        Listing l = ownListing(id, farmerId);
        if (!Arrays.asList("DRAFT", "PENDING", "ACTIVE", "DEACTIVATED").contains(l.getStatus()) || l.getActiveOrderId() != null) {
            throw new IllegalStateException("This listing cannot be edited now");
        }
        if (quantity != null) {
            double q = Listing.parseNumber(quantity);
            if (q <= 0) throw new IllegalStateException("Enter the estimated total yield in kg");
            l.setQuantity(stripZeros(q));
        }
        if (price != null) {
            double p = Listing.parseNumber(price);
            if (p <= 0) throw new IllegalStateException("Enter the expected price per kg");
            l.setPrice(stripZeros(p));
        }
        l.setUpdatedTime(nowIso());
        return listingRepository.save(l);
    }

    /** Deactivate = hide from the marketplace and the officer queue (Pending or Listed, no running order). */
    public Listing deactivate(String id, String farmerId) {
        Listing l = ownListing(id, farmerId);
        if (!Arrays.asList("PENDING", "ACTIVE").contains(l.getStatus()) || l.getActiveOrderId() != null) {
            throw new IllegalStateException("Only pending or listed lots without an order can be deactivated");
        }
        l.setPreviousStatus(l.getStatus());
        l.setStatus("DEACTIVATED");
        l.setClaimedByOfficerId(null);
        l.setClaimedByOfficerName(null);
        l.setUpdatedTime(nowIso());
        return listingRepository.save(l);
    }

    /** Re-activate = back to the status it had: Listed stays Listed (same grade), Pending goes back to the queue. */
    public Listing reactivate(String id, String farmerId) {
        Listing l = ownListing(id, farmerId);
        if (!"DEACTIVATED".equals(l.getStatus())) throw new IllegalStateException("This listing is not deactivated");
        boolean wasListed = "ACTIVE".equals(l.getPreviousStatus()) && l.getGrade() != null;
        l.setStatus(wasListed ? "ACTIVE" : "PENDING");
        l.setPreviousStatus(null);
        l.setUpdatedTime(nowIso());
        return listingRepository.save(l);
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
