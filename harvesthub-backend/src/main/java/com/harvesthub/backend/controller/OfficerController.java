package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.*;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.NotificationItem;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.NotificationRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import com.harvesthub.backend.security.AuthUser;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.security.Roles;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class OfficerController {

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // ---- Current Officer Profile --------------------------------------------

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private MongoTemplate mongoTemplate;

    @GetMapping("/officers/me")
    public ResponseEntity<OfficerDto> getCurrentOfficer() {
        return userRepository.findById(currentUser.id())
                .map(u -> ResponseEntity.ok(toOfficerDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    public static OfficerDto toOfficerDto(User user) {
        String region = user.getFarmLocation() != null ? user.getFarmLocation().getAddress() : "";
        String nic = user.getNic();
        String code = nic != null && nic.length() >= 4 ? "#HH-" + nic.substring(nic.length() - 4) : "";
        return OfficerDto.builder()
                .id(user.getId())
                .username(user.getEmail() != null ? user.getEmail().split("@")[0] : user.getId())
                .fullName(user.getFullName())
                .officerIdCode(code)
                .region(region)
                .email(user.getEmail())
                .phone(user.getMobileNumber() != null ? user.getMobileNumber() : user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }

    // ---- Dashboard Activity & Stats -----------------------------------------

    @GetMapping("/dashboard/activity")
    public ResponseEntity<List<ActivityLogDto>> getDashboardActivity() {
        List<Listing> listings = listingRepository.findAll();
        List<ActivityLogDto> logs = listings.stream()
                .map(this::toActivityLogDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<OfficerStatsDto> getDashboardStats() {
        List<Listing> listings = listingRepository.findAll();
        long pending = listings.stream()
                .filter(l -> l.getStatus() == null || isPendingStatus(l.getStatus()))
                .count();

        long reviewedToday = listings.stream()
                .filter(l -> l.getReviewedAt() != null || "ACTIVE".equalsIgnoreCase(l.getStatus()) || "APPROVED".equalsIgnoreCase(l.getStatus()))
                .count();

        int rate = 0;
        if (!listings.isEmpty()) {
            long approved = listings.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.getStatus()) || "APPROVED".equalsIgnoreCase(l.getStatus())).count();
            rate = (int) Math.round(((double) approved / (double) listings.size()) * 100);
        }

        return ResponseEntity.ok(new OfficerStatsDto(pending, reviewedToday, rate));
    }

    // ---- Verification Queue -------------------------------------------------

    @GetMapping("/queue")
    public ResponseEntity<List<QueueListingDto>> getQueue(
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "search", required = false) String search) {

        List<Listing> listings = listingRepository.findAll();

        List<QueueListingDto> queue = listings.stream()
                .filter(l -> matchesTab(l, tab))
                .filter(l -> matchesSearch(l, search))
                .map(this::toQueueListingDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(queue);
    }

    @PostMapping("/queue/{listingId}/claim")
    public ResponseEntity<ListingDetailDto> claimListing(@PathVariable String listingId) {
        if (!listingRepository.existsById(listingId)) {
            return ResponseEntity.notFound().build();
        }
        AuthUser me = currentUser.get();
        // Atomic claim: succeeds only if the listing is still pending and unclaimed (or already claimed by me),
        // so two officers can never grade the same listing at the same time (proposal 6.7).
        Query query = new Query(Criteria.where("_id").is(listingId).andOperator(
                new Criteria().orOperator(
                        Criteria.where("status").in("PENDING", "PENDING_VERIFICATION").and("claimedByOfficerId").in(null, ""),
                        Criteria.where("status").is("UNDER_REVIEW").and("claimedByOfficerId").is(me.id()))));
        Update update = new Update()
                .set("status", "UNDER_REVIEW")
                .set("claimedByOfficerId", me.id())
                .set("claimedByOfficerName", me.fullName())
                .set("updatedTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Listing claimed = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Listing.class);
        if (claimed == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // already claimed by another officer or not pending
        }
        return ResponseEntity.ok(toListingDetailDto(claimed));
    }

    // ---- Listing Detail & Review Decision -----------------------------------

    @GetMapping("/listings/{listingId}")
    public ResponseEntity<ListingDetailDto> getListingDetail(@PathVariable String listingId) {
        Optional<Listing> opt = listingRepository.findById(listingId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toListingDetailDto(opt.get()));
    }

    @PostMapping("/listings/{listingId}/review")
    public ResponseEntity<Void> submitReview(
            @PathVariable String listingId,
            @RequestBody ReviewDecisionRequestDto request) {

        Optional<Listing> opt = listingRepository.findById(listingId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Listing listing = opt.get();
        AuthUser me = currentUser.get();
        // Only the officer holding the claim may grade (admins may override).
        if (!"UNDER_REVIEW".equals(listing.getStatus())
                || (!me.id().equals(listing.getClaimedByOfficerId()) && !Roles.ADMIN.equals(me.role()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        if (request.getDecision() == null) {
            return ResponseEntity.badRequest().build();
        }
        String decision = request.getDecision().toUpperCase();
        if ("APPROVE".equals(decision) && (request.getGrade() == null
                || !Set.of("A", "B", "C").contains(request.getGrade().toUpperCase()))) {
            return ResponseEntity.badRequest().build(); // a grade A/B/C is mandatory on approval
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        listing.setReviewedAt(now);
        listing.setReviewedByOfficerId(me.id());
        listing.setReviewedByOfficerName(me.fullName());
        listing.setReviewNotes(request.getNotes());

        String notifType = "VERIFICATION";
        String notifTitle;
        String notifBody;

        if ("APPROVE".equals(decision)) {
            listing.setStatus("ACTIVE");
            listing.setGrade(request.getGrade().toUpperCase());
            notifTitle = "Listing Approved: " + listing.getCropName();
            notifBody = "Your " + listing.getCropName() + " listing has been verified and approved as Grade " + listing.getGrade() + ". It is now active on the marketplace.";
        } else if ("RETAKE".equals(decision)) {
            listing.setStatus("RETAKE_REQUESTED");
            notifTitle = "Media Retake Requested: " + listing.getCropName();
            notifBody = "Officer requested retake of verification photos/video. Reason: " + (request.getNotes() != null ? request.getNotes() : "Please provide clearer crop photos.");
        } else {
            listing.setStatus("REJECTED");
            notifTitle = "Listing Rejected: " + listing.getCropName();
            notifBody = "Your " + listing.getCropName() + " listing was rejected. Reason: " + (request.getNotes() != null ? request.getNotes() : "Crop quality does not meet marketplace standards.");
        }

        listingRepository.save(listing);

        // Notify farmer
        if (listing.getFarmerId() != null) {
            NotificationItem notif = new NotificationItem();
            notif.setFarmerId(listing.getFarmerId());
            notif.setType(notifType);
            notif.setTitle(notifTitle);
            notif.setBody(notifBody);
            notif.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")));
            notif.setRead(false);
            notificationRepository.save(notif);
        }

        return ResponseEntity.ok().build();
    }

    // ---- Review History -----------------------------------------------------

    @GetMapping("/review-history")
    public ResponseEntity<List<ReviewHistoryRecordDto>> getReviewHistory(
            @RequestParam(value = "search", required = false) String search) {

        List<Listing> listings = listingRepository.findAll();

        List<ReviewHistoryRecordDto> history = listings.stream()
                .filter(l -> l.getGrade() != null
                        || "ACTIVE".equalsIgnoreCase(l.getStatus())
                        || "APPROVED".equalsIgnoreCase(l.getStatus())
                        || "RETAKE_REQUESTED".equalsIgnoreCase(l.getStatus())
                        || "REJECTED".equalsIgnoreCase(l.getStatus())
                        || "DELIVERED".equalsIgnoreCase(l.getStatus()))
                .filter(l -> matchesSearch(l, search))
                .map(this::toReviewHistoryRecordDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    // ---- Mapping Helpers ----------------------------------------------------

    private boolean isPendingStatus(String status) {
        return "PENDING".equalsIgnoreCase(status)
                || "DRAFT".equalsIgnoreCase(status)
                || "UNDER_REVIEW".equalsIgnoreCase(status)
                || "SUBMITTED".equalsIgnoreCase(status);
    }

    private boolean matchesTab(Listing l, String tab) {
        String status = l.getStatus() != null ? l.getStatus().toUpperCase() : "PENDING";
        boolean isPending = isPendingStatus(status);

        if ("claimed".equalsIgnoreCase(tab)) {
            return (l.getReviewedByOfficerId() != null && !l.getReviewedByOfficerId().isBlank())
                    || (l.getClaimedByOfficerId() != null && !l.getClaimedByOfficerId().isBlank())
                    || "ACTIVE".equals(status) || "APPROVED".equals(status) || "REJECTED".equals(status);
        }

        // Verification queue should only show listings that require verification
        if (!isPending && !"all_records".equalsIgnoreCase(tab)) {
            return false;
        }

        if ("unclaimed".equalsIgnoreCase(tab)) {
            return l.getClaimedByOfficerId() == null || l.getClaimedByOfficerId().isBlank();
        }
        return true;
    }

    private boolean matchesSearch(Listing l, String search) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase();
        return (l.getCropName() != null && l.getCropName().toLowerCase().contains(q))
                || (l.getFarmerName() != null && l.getFarmerName().toLowerCase().contains(q))
                || (l.getLocation() != null && l.getLocation().toLowerCase().contains(q))
                || (l.getVariety() != null && l.getVariety().toLowerCase().contains(q));
    }

    private ActivityLogDto toActivityLogDto(Listing l) {
        String farmer = resolveFarmerName(l);
        String grade = l.getGrade() != null ? l.getGrade() : "-";
        String rawStatus = l.getStatus() != null ? l.getStatus().toUpperCase() : "PENDING";
        String status;
        if ("ACTIVE".equals(rawStatus) || "APPROVED".equals(rawStatus)) {
            status = "Approved";
        } else if ("REJECTED".equals(rawStatus)) {
            status = "Rejected";
        } else if ("RETAKE_REQUESTED".equals(rawStatus)) {
            status = "Retake Requested";
        } else if ("UNDER_REVIEW".equals(rawStatus)) {
            status = "In Review";
        } else {
            status = "Pending";
        }
        String time = l.getUpdatedTime() != null ? formatTime(l.getUpdatedTime()) : "10:30 AM";

        return ActivityLogDto.builder()
                .id(l.getId())
                .cropName(l.getCropName() != null ? l.getCropName() : "Crop")
                .variety(l.getVariety() != null ? l.getVariety() : "Standard")
                .farmerName(farmer)
                .location(l.getLocation() != null ? l.getLocation() : "Sri Lanka")
                .grade(grade)
                .status(status)
                .timestamp(time)
                .build();
    }

    private QueueListingDto toQueueListingDto(Listing l) {
        String farmer = resolveFarmerName(l);
        String rawStatus = l.getStatus() != null ? l.getStatus().toUpperCase() : "PENDING";
        boolean claimable;
        String statusLabel;

        if ("ACTIVE".equals(rawStatus) || "APPROVED".equals(rawStatus)) {
            statusLabel = "Listed";
            claimable = false;
        } else if ("REJECTED".equals(rawStatus)) {
            statusLabel = "Rejected";
            claimable = false;
        } else if ("RETAKE_REQUESTED".equals(rawStatus)) {
            statusLabel = "Retake Requested";
            claimable = false;
        } else if ("UNDER_REVIEW".equals(rawStatus)) {
            statusLabel = "In Review";
            claimable = (l.getClaimedByOfficerId() == null || l.getClaimedByOfficerId().isBlank());
        } else {
            statusLabel = "Pending";
            claimable = (l.getClaimedByOfficerId() == null || l.getClaimedByOfficerId().isBlank());
        }

        String tone = resolveTone(l.getCropName());
        String icon = resolveIcon(l.getCropName());

        return QueueListingDto.builder()
                .id(l.getId())
                .cropName(l.getCropName() != null ? l.getCropName() : "Crop")
                .farmerName(farmer)
                .location(l.getLocation() != null ? l.getLocation() : "Sri Lanka")
                .submittedAgo(formatSubmittedAgo(l.getCreatedDate()))
                .quantity(l.getQuantity() != null ? l.getQuantity() : (l.getQuantityKg() != null ? l.getQuantityKg() + " kg" : "500 kg"))
                .pricePerKg(l.getPrice() != null ? l.getPrice() : (l.getFinalPricePerKg() != null ? "Rs. " + l.getFinalPricePerKg() + "/kg" : "Rs. 250/kg"))
                .statusLabel(statusLabel)
                .claimable(claimable)
                .tone(tone)
                .iconLiteral(icon)
                .build();
    }

    private ListingDetailDto toListingDetailDto(Listing l) {
        String farmer = resolveFarmerName(l);
        String avatar = extractInitials(farmer);
        String qty = l.getQuantity() != null ? l.getQuantity() : (l.getQuantityKg() != null ? l.getQuantityKg() + " kg" : "1,500 kg");
        String price = l.getPrice() != null ? l.getPrice() : "Rs. 250/kg";
        String total = l.getTotalValue() != null ? l.getTotalValue() : "Rs. 375,000";

        String rawStatus = l.getStatus() != null ? l.getStatus().toUpperCase() : "PENDING";
        String displayStatus;
        if ("ACTIVE".equals(rawStatus) || "APPROVED".equals(rawStatus)) {
            displayStatus = l.getGrade() != null ? "Approved (Grade " + l.getGrade() + ")" : "Approved";
        } else if ("REJECTED".equals(rawStatus)) {
            displayStatus = "Rejected";
        } else if ("RETAKE_REQUESTED".equals(rawStatus)) {
            displayStatus = "Retake Requested";
        } else if ("UNDER_REVIEW".equals(rawStatus)) {
            displayStatus = "Under Review";
        } else {
            displayStatus = "Pending Review";
        }

        return ListingDetailDto.builder()
                .id(l.getId())
                .cropName(l.getCropName() != null ? l.getCropName() : "Crop")
                .farmerName(farmer)
                .farmerAvatar(avatar)
                .farmerIdNumber(l.getFarmerIdNumber() != null ? l.getFarmerIdNumber() : "ID: 902182")
                .cooperativeName(l.getCooperativeName() != null ? l.getCooperativeName() : "HarvestHub Central Cooperative")
                .location(l.getLocation() != null ? l.getLocation() : "Dambulla, Sri Lanka")
                .submittedTimeAgo(formatSubmittedAgo(l.getCreatedDate()))
                .submittedTimestamp(l.getCreatedDate() != null ? l.getCreatedDate().replace("T", " ") : "2026-09-28 08:30 AM")
                .gpsCoordinates(l.getGpsCoordinates() != null ? l.getGpsCoordinates() : "7.8731° N, 80.7718° E")
                .deviceModel(l.getDeviceModel() != null ? l.getDeviceModel() : "Android CropScanner v4.2")
                .referenceCode(l.getReferenceCode() != null ? l.getReferenceCode() : ("LIST-" + Math.abs(l.getId().hashCode() % 9000 + 1000)))
                .quantity(qty)
                .variancePercent(l.getVariancePercent() != null ? l.getVariancePercent() : "-0.8% dev")
                .pricePerUnit(price)
                .totalValuation(total)
                .status(displayStatus)
                .mediaFileCount(l.getMediaFileCount() != null ? l.getMediaFileCount() : 3)
                .mediaCaption1(l.getMediaCaption1() != null ? l.getMediaCaption1() : "Wide Angle - Field View")
                .mediaCaption2(l.getMediaCaption2() != null ? l.getMediaCaption2() : "Close-Up - Harvest Quality")
                .videoLabel(l.getVideoLabel() != null ? l.getVideoLabel() : "360° Field & Sample Video (10s)")
                .build();
    }

    private ReviewHistoryRecordDto toReviewHistoryRecordDto(Listing l) {
        String farmer = resolveFarmerName(l);
        String avatar = extractInitials(l.getCropName());
        String decisionLabel = "Grade A";
        String decisionKind = "GRADE_A";

        if ("RETAKE_REQUESTED".equalsIgnoreCase(l.getStatus())) {
            decisionLabel = "Retake Requested";
            decisionKind = "RETAKE";
        } else if ("REJECTED".equalsIgnoreCase(l.getStatus())) {
            decisionLabel = "Rejected";
            decisionKind = "REJECTED";
        } else if (l.getGrade() != null) {
            decisionLabel = "Grade " + l.getGrade();
            decisionKind = "GRADE_" + l.getGrade().toUpperCase();
        }

        String dateReviewed = l.getReviewedAt() != null ? l.getReviewedAt().split("T")[0] : (l.getCreatedDate() != null ? l.getCreatedDate().split("T")[0] : "2026-09-28");

        return ReviewHistoryRecordDto.builder()
                .id(l.getId())
                .avatarCode(avatar)
                .cropName(l.getCropName() != null ? l.getCropName() : "Crop")
                .variety(l.getVariety() != null ? l.getVariety() : "Standard")
                .farmerName(farmer)
                .location(l.getLocation() != null ? l.getLocation() : "Sri Lanka")
                .decisionLabel(decisionLabel)
                .decisionKind(decisionKind)
                .dateReviewed(dateReviewed)
                .build();
    }

    private String resolveFarmerName(Listing l) {
        if (l.getFarmerName() != null && !l.getFarmerName().isBlank()) {
            return l.getFarmerName();
        }
        if (l.getFarmerId() != null) {
            Optional<User> u = userRepository.findById(l.getFarmerId());
            if (u.isPresent() && u.get().getFullName() != null) {
                return u.get().getFullName();
            }
        }
        return "Kamal Perera";
    }

    private String extractInitials(String text) {
        if (text == null || text.isBlank()) return "HH";
        String[] parts = text.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String formatSubmittedAgo(String dateStr) {
        return "2 hours ago";
    }

    private String formatTime(String dateStr) {
        if (dateStr == null) return "Today";
        if (dateStr.contains("T")) {
            String[] parts = dateStr.split("T");
            if (parts.length > 1 && parts[1].length() >= 5) {
                return parts[1].substring(0, 5);
            }
        }
        return "10:30 AM";
    }

    private String resolveTone(String crop) {
        if (crop == null) return "AMBER";
        String lower = crop.toLowerCase();
        if (lower.contains("paddy") || lower.contains("rice")) return "AMBER";
        if (lower.contains("tea") || lower.contains("leek") || lower.contains("cabbage")) return "TEAL";
        if (lower.contains("cinnamon") || lower.contains("pepper")) return "BROWN";
        return "PURPLE";
    }

    private String resolveIcon(String crop) {
        if (crop == null) return "fas-seedling";
        String lower = crop.toLowerCase();
        if (lower.contains("tea") || lower.contains("cinnamon") || lower.contains("leaf")) return "fas-leaf";
        return "fas-seedling";
    }
}
