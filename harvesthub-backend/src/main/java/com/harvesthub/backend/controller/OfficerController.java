package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.OfficerDto;
import com.harvesthub.backend.dto.OfficerViews.*;
import com.harvesthub.backend.dto.ReviewDecisionRequestDto;
import com.harvesthub.backend.model.Listing;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.CropRepository;
import com.harvesthub.backend.repository.ListingRepository;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.AuthUser;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.NotificationService;
import com.harvesthub.backend.service.OfficerPortalService;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Listing verification for Agricultural Officers (proposal 3.3, workflow 1.5): shared queue, atomic claim,
 * approve with grade / request retake / reject. All values come from the database; nothing is invented.
 * Access: OFFICER (or the legacy ADMIN account) – see SecurityConfig.OFFICER_AREA.
 */
@RestController
@RequestMapping("/api")
public class OfficerController {

    private static final Set<String> CLAIMABLE = Set.of("PENDING", "PENDING_VERIFICATION", "SUBMITTED");

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final CropRepository cropRepository;
    private final CurrentUser currentUser;
    private final MongoTemplate mongoTemplate;
    private final OfficerPortalService portal;
    private final NotificationService notifications;

    public OfficerController(ListingRepository listingRepository, UserRepository userRepository,
                             CropRepository cropRepository, CurrentUser currentUser, MongoTemplate mongoTemplate,
                             OfficerPortalService portal, NotificationService notifications) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.cropRepository = cropRepository;
        this.currentUser = currentUser;
        this.mongoTemplate = mongoTemplate;
        this.portal = portal;
        this.notifications = notifications;
    }

    // ---- Current officer --------------------------------------------------------------------

    @GetMapping("/officers/me")
    public ResponseEntity<OfficerDto> getCurrentOfficer() {
        return userRepository.findById(currentUser.id())
                .map(u -> ResponseEntity.ok(toOfficerDto(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    public static OfficerDto toOfficerDto(User user) {
        String region = user.getFarmLocation() != null ? user.getFarmLocation().getAddress() : null;
        String nic = user.getNic();
        String code = nic != null && nic.length() >= 4 ? "HH-" + nic.substring(nic.length() - 4).toUpperCase() : null;
        return OfficerDto.builder()
                .id(user.getId())
                .username(user.getEmail())
                .fullName(user.getFullName())
                .officerIdCode(code)
                .region(region)
                .email(user.getEmail())
                .phone(user.getMobileNumber() != null ? user.getMobileNumber() : user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }

    // ---- Dashboard -----------------------------------------------------------------------------

    @GetMapping("/dashboard/stats")
    public Stats getDashboardStats() {
        return portal.stats(currentUser.get());
    }

    @GetMapping("/dashboard/activity")
    public List<Activity> getDashboardActivity(@RequestParam(value = "limit", defaultValue = "30") int limit) {
        return portal.activity(limit);
    }

    // ---- Verification queue ----------------------------------------------------------------------

    /** tab = all | unclaimed | claimed (claimed by me). Oldest first. */
    @GetMapping("/queue")
    public List<QueueItem> getQueue(@RequestParam(value = "tab", defaultValue = "all") String tab,
                                    @RequestParam(value = "search", required = false) String search) {
        return portal.queue(tab, search, currentUser.get());
    }

    @PostMapping("/queue/{listingId}/claim")
    public ResponseEntity<?> claimListing(@PathVariable String listingId) {
        if (!listingRepository.existsById(listingId)) {
            return ResponseEntity.notFound().build();
        }
        AuthUser me = currentUser.get();
        // Atomic claim: succeeds only if the listing is still pending and unclaimed (or already claimed by me),
        // so two officers can never grade the same listing at the same time (workflow 1.5).
        Query query = new Query(Criteria.where("_id").is(listingId).andOperator(
                new Criteria().orOperator(
                        Criteria.where("status").in(CLAIMABLE).and("claimedByOfficerId").in(null, ""),
                        // orphaned review (status set without an officer, e.g. older data) – anyone may take it
                        Criteria.where("status").is("UNDER_REVIEW").and("claimedByOfficerId").in(null, ""),
                        Criteria.where("status").is("UNDER_REVIEW").and("claimedByOfficerId").is(me.id()))));
        Update update = new Update()
                .set("status", "UNDER_REVIEW")
                .set("claimedByOfficerId", me.id())
                .set("claimedByOfficerName", me.fullName())
                .set("updatedTime", now());
        Listing claimed = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Listing.class);
        if (claimed == null) {
            Listing current = listingRepository.findById(listingId).orElse(null);
            String who = current != null && current.getClaimedByOfficerName() != null ? current.getClaimedByOfficerName() : "another officer";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",
                    current != null && "UNDER_REVIEW".equals(current.getStatus())
                            ? "This listing is already being reviewed by " + who + "."
                            : "This listing is no longer waiting for verification."));
        }
        return ResponseEntity.ok(portal.detail(claimed, me));
    }

    /** The claimant hands the listing back to the shared queue without a decision. */
    @PostMapping("/queue/{listingId}/release")
    public ResponseEntity<?> releaseListing(@PathVariable String listingId) {
        AuthUser me = currentUser.get();
        Query query = new Query(Criteria.where("_id").is(listingId).and("status").is("UNDER_REVIEW")
                .and("claimedByOfficerId").is(me.id()));
        Update update = new Update().set("status", "PENDING").unset("claimedByOfficerId").unset("claimedByOfficerName");
        Listing released = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Listing.class);
        if (released == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Only the officer reviewing this listing can release it."));
        }
        return ResponseEntity.ok(portal.detail(released, me));
    }

    // ---- Listing detail & decision -------------------------------------------------------------

    @GetMapping("/listings/{listingId}")
    public ResponseEntity<ListingDetail> getListingDetail(@PathVariable String listingId) {
        return listingRepository.findById(listingId)
                .map(l -> ResponseEntity.ok(portal.detail(l, currentUser.get())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/listings/{listingId}/review")
    public ResponseEntity<?> submitReview(@PathVariable String listingId, @RequestBody ReviewDecisionRequestDto request) {
        Listing listing = listingRepository.findById(listingId).orElse(null);
        if (listing == null) {
            return ResponseEntity.notFound().build();
        }
        AuthUser me = currentUser.get();
        if (!"UNDER_REVIEW".equals(listing.getStatus()) || !me.id().equals(listing.getClaimedByOfficerId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Claim this listing before grading it."));
        }
        String decision = request.getDecision() == null ? "" : request.getDecision().trim().toUpperCase();
        String notes = request.getNotes() == null ? null : request.getNotes().trim();
        if (!Set.of("APPROVE", "RETAKE", "REJECT").contains(decision)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Choose Approve, Request retake or Reject."));
        }
        if ("APPROVE".equals(decision) && (request.getGrade() == null
                || !Set.of("A", "B", "C").contains(request.getGrade().trim().toUpperCase()))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Select Grade A, B or C before approving."));
        }
        if (!"APPROVE".equals(decision) && (notes == null || notes.length() < 5)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tell the farmer why (at least 5 characters)."));
        }
        if (notes != null && notes.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("error", "Keep the notes under 1000 characters."));
        }
        String now = now();
        listing.setReviewedAt(now);
        listing.setReviewedByOfficerId(me.id());
        listing.setReviewedByOfficerName(me.fullName());
        listing.setReviewNotes(notes == null || notes.isEmpty() ? null : notes);
        listing.setClaimedByOfficerId(null);
        listing.setClaimedByOfficerName(null);
        listing.setUpdatedTime(now);

        String title;
        String body;
        String grade = null;
        if ("APPROVE".equals(decision)) {
            grade = request.getGrade().trim().toUpperCase();
            listing.setStatus("ACTIVE");
            listing.setGrade(grade);
            if (Boolean.TRUE.equals(listing.getCropPending()) && listing.getCropName() != null) {
                // approving the listing also accepts the crop the farmer added via "Add New"
                cropRepository.findFirstByNameIgnoreCase(listing.getCropName()).ifPresent(c -> {
                    c.setVerified(true);
                    cropRepository.save(c);
                });
                listing.setCropPending(false);
            }
            title = "Listing approved: " + listing.getCropName();
            body = "Your " + listing.getCropName() + " listing was verified as Grade " + grade
                    + " and is now on the marketplace." + (notes == null || notes.isEmpty() ? "" : " Officer note: " + notes);
            portalDecision(listing, "APPROVED", grade, notes, me, now);
        } else if ("RETAKE".equals(decision)) {
            listing.setStatus("RETAKE_REQUESTED");
            listing.setGrade(null);
            title = "New photos needed: " + listing.getCropName();
            body = "An Agricultural Officer asked you to capture the photos/video again at your farm. Reason: " + notes;
            portalDecision(listing, "RETAKE", null, notes, me, now);
        } else {
            listing.setStatus("REJECTED");
            listing.setGrade(null);
            title = "Listing rejected: " + listing.getCropName();
            body = "Your " + listing.getCropName() + " listing was rejected. Reason: " + notes;
            portalDecision(listing, "REJECTED", null, notes, me, now);
        }
        listingRepository.save(listing);
        if (listing.getFarmerId() != null) {
            notifications.notify(listing.getFarmerId(), "LISTINGS", "VERIFICATION", title, body);
        }
        return ResponseEntity.ok(portal.detail(listing, me));
    }

    private static void portalDecision(Listing l, String decision, String grade, String notes, AuthUser me, String at) {
        OfficerPortalService.recordDecision(l, decision, grade, notes == null || notes.isEmpty() ? null : notes, me, at);
    }

    // ---- Review history ------------------------------------------------------------------------------

    /** scope = mine (default) | all; decision = APPROVED | RETAKE | REJECTED; from/to = yyyy-MM-dd. */
    @GetMapping("/review-history")
    public List<HistoryItem> getReviewHistory(@RequestParam(value = "scope", defaultValue = "mine") String scope,
                                              @RequestParam(value = "decision", required = false) String decision,
                                              @RequestParam(value = "crop", required = false) String crop,
                                              @RequestParam(value = "from", required = false) String from,
                                              @RequestParam(value = "to", required = false) String to,
                                              @RequestParam(value = "search", required = false) String search) {
        return portal.history(scope, decision, crop, from, to, search, currentUser.get());
    }

    private static String now() {
        return LocalDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
