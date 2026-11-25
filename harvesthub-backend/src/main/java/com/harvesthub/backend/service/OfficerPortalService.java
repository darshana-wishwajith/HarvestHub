package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.OfficerViews.*;
import com.harvesthub.backend.model.*;
import com.harvesthub.backend.repository.*;
import com.harvesthub.backend.security.AuthUser;
import com.harvesthub.backend.security.Roles;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Read models and actions for the Agricultural Officer desktop portal (;
 * Officers also monitor users, orders and disputes, suspend / reactivate accounts and
 * resolve disputes with a note – money unchanged).
 */
@Service
public class OfficerPortalService {

    public static final List<String> QUEUE_STATUSES = List.of("PENDING", "PENDING_VERIFICATION", "SUBMITTED", "UNDER_REVIEW");
    static final Set<String> PROBLEM_STATUSES = Set.of("PRELOAD_REJECTED", "RETURN_JOURNEY", "RETURNED", "CANCELLED");
    /**media must be captured at the registered farm (same 500 m limit as the app's location lock). */
    static final double FARM_RADIUS_M = 500;
    static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");

    private final ListingRepository listings;
    private final UserRepository users;
    private final OrderRepository orders;
    private final DisputeRepository disputes;
    private final WalletRepository wallets;
    private final NotificationRepository notificationRepository;
    private final NotificationService notifications;
    private final TrustService trust;

    public OfficerPortalService(ListingRepository listings, UserRepository users, OrderRepository orders,
                                DisputeRepository disputes, WalletRepository wallets,
                                NotificationRepository notificationRepository, NotificationService notifications,
                                TrustService trust) {
        this.listings = listings;
        this.users = users;
        this.orders = orders;
        this.disputes = disputes;
        this.wallets = wallets;
        this.notificationRepository = notificationRepository;
        this.notifications = notifications;
        this.trust = trust;
    }

    // ================================================================= dashboard

    public Stats stats(AuthUser me) {
        List<Listing> all = listings.findAll();
        String today = LocalDate.now().toString();
        long pending = all.stream().filter(OfficerPortalService::inQueue).count();
        long unclaimed = all.stream().filter(l -> inQueue(l) && blank(l.getClaimedByOfficerId())).count();
        long mine = all.stream().filter(l -> "UNDER_REVIEW".equals(l.getStatus()) && me.id().equals(l.getClaimedByOfficerId())).count();
        List<HistoryItem> decisions = all.stream().flatMap(l -> decisions(l, me.id()).stream()).toList();
        long reviewedToday = decisions.stream().filter(h -> h.at() != null && h.at().startsWith(today)).count();
        List<HistoryItem> myDecisions = decisions.stream().filter(HistoryItem::byMe).toList();
        long myToday = myDecisions.stream().filter(h -> h.at() != null && h.at().startsWith(today)).count();
        long myApproved = myDecisions.stream().filter(h -> "APPROVED".equals(h.decision())).count();
        int rate = myDecisions.isEmpty() ? 0 : (int) Math.round(100.0 * myApproved / myDecisions.size());

        List<Order> allOrders = orders.findAll();
        long active = allOrders.stream().filter(o -> "ACTIVE".equals(stage(o))).count();
        long problem = allOrders.stream().filter(o -> "PROBLEM".equals(stage(o))).count();
        long openDisputes = disputes.findByStatus("OPEN").size();
        List<User> people = users.findAll();
        long suspended = people.stream().filter(User::isAccountSuspended).count();
        long unread = notificationRepository.findByFarmerIdAndIsReadFalse(me.id()).size();
        return new Stats(pending, unclaimed, mine, reviewedToday, myToday, myDecisions.size(), rate,
                openDisputes, active, problem, people.size(), suspended, unread);
    }

    public List<Activity> activity(int limit) {
        List<Activity> out = new ArrayList<>();
        for (Listing l : listings.findAll()) {
            String farmer = farmerName(l);
            if (l.getCreatedDate() != null) {
                out.add(new Activity(l.getId(), l.getCropName(), farmer, l.getLocation(), "SUBMITTED", null, farmer, norm(l.getCreatedDate())));
            }
            if ("UNDER_REVIEW".equals(l.getStatus()) && !blank(l.getClaimedByOfficerName())) {
                out.add(new Activity(l.getId(), l.getCropName(), farmer, l.getLocation(), "CLAIMED", null,
                        l.getClaimedByOfficerName(), norm(l.getUpdatedTime())));
            }
            for (HistoryItem h : decisions(l, null)) {
                out.add(new Activity(l.getId(), l.getCropName(), farmer, l.getLocation(),
                        "RETAKE".equals(h.decision()) ? "RETAKE" : h.decision(), h.grade(), h.officerName(), h.at()));
            }
        }
        Collections.reverse(out); // later events first when timestamps are equal (stable sort)
        out.sort(Comparator.comparing(Activity::at, Comparator.nullsLast(Comparator.reverseOrder())));
        return out.stream().limit(Math.max(1, Math.min(limit, 200))).toList();
    }

    // ================================================================= queue + review

    public List<QueueItem> queue(String tab, String search, AuthUser me) {
        String t = tab == null ? "all" : tab.toLowerCase(Locale.ROOT);
        return listings.findAll().stream()
                .filter(OfficerPortalService::inQueue)
                .filter(l -> switch (t) {
                    case "unclaimed" -> blank(l.getClaimedByOfficerId());
                    case "claimed", "mine" -> me.id().equals(l.getClaimedByOfficerId());
                    default -> true;
                })
                .filter(l -> matches(search, l.getCropName(), l.getVariety(), farmerName(l), l.getLocation(), l.getReferenceCode()))
                .sorted(Comparator.comparing((Listing l) -> norm(l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate()),
                        Comparator.nullsLast(Comparator.naturalOrder()))) // oldest first: first come, first served
                .map(l -> queueItem(l, me))
                .toList();
    }

    QueueItem queueItem(Listing l, AuthUser me) {
        return new QueueItem(l.getId(), l.getCropName(), l.getVariety(), l.getFarmerId(), farmerName(l), l.getLocation(),
                number(l.getQuantity()), number(l.getPrice()), norm(l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate()),
                l.getStatus(), statusLabel(l.getStatus()), l.getClaimedByOfficerId(), l.getClaimedByOfficerName(),
                me != null && me.id().equals(l.getClaimedByOfficerId()),
                l.getMediaUrls() == null ? 0 : l.getMediaUrls().size(), distanceFromFarm(l),
                Boolean.TRUE.equals(l.getCropPending()), "PENDING".equals(l.getStatus()) && hadRetake(l), coverUrl(l));
    }

    /** Thumbnail of the first photo of the capture (the wide-angle shot) for list rows; null when there is none. */
    static String coverUrl(Listing l) {
        if (l.getMediaUrls() == null) return null;
        return l.getMediaUrls().stream().filter(u -> u != null && u.startsWith("/uploads/") && !u.toLowerCase().endsWith(".mp4"))
                .findFirst().map(u -> "/uploads/thumb/" + u.substring("/uploads/".length()) + "?size=160").orElse(null);
    }

    public ListingDetail detail(Listing l, AuthUser me) {
        User farmer = l.getFarmerId() == null ? null : users.findById(l.getFarmerId()).orElse(null);
        FarmLocation farm = farmer == null ? null : farmer.getFarmLocation();
        Double qty = number(l.getQuantity());
        Double price = number(l.getPrice());
        Double total = qty != null && price != null ? qty * price : number(l.getTotalValue());
        return new ListingDetail(l.getId(), l.getReferenceCode(), l.getCropName(), l.getVariety(),
                Boolean.TRUE.equals(l.getCropPending()), l.getStatus(), statusLabel(l.getStatus()), qty, price, total,
                norm(l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate()), norm(l.getCapturedAt()),
                l.getMediaUrls() == null ? List.of() : l.getMediaUrls(), l.getGpsLatitude(), l.getGpsLongitude(),
                distanceFromFarm(l), l.getDeviceModel(),
                farm == null ? null : farm.getLatitude(), farm == null ? null : farm.getLongitude(),
                farm == null ? null : farm.getAddress(),
                l.getClaimedByOfficerId(), l.getClaimedByOfficerName(), me != null && me.id().equals(l.getClaimedByOfficerId()),
                l.getGrade(), l.getReviewNotes(), l.getReviewedByOfficerName(), norm(l.getReviewedAt()),
                farmer == null ? null : brief(farmer), checks(l), decisions(l, me == null ? null : me.id()));
    }

    /**step 1 "Media authenticity check": GPS at the farm, all three media, captured close to submission. */
    List<Check> checks(Listing l) {
        List<Check> out = new ArrayList<>();
        Double d = distanceFromFarm(l);
        if (d == null) {
            out.add(new Check("Captured at the registered farm", false, "No GPS position was recorded with this listing"));
        } else {
            out.add(new Check("Captured at the registered farm", d <= FARM_RADIUS_M,
                    String.format(Locale.US, "%.0f m from the registered farm (limit %.0f m)", d, FARM_RADIUS_M)));
        }
        List<String> media = l.getMediaUrls() == null ? List.of() : l.getMediaUrls();
        long photos = media.stream().filter(u -> !u.toLowerCase(Locale.ROOT).endsWith(".mp4")).count();
        long videos = media.size() - photos;
        out.add(new Check("Wide-angle photo, close-up photo and 10 s video", photos >= 2 && videos >= 1,
                photos + " photo(s), " + videos + " video(s) uploaded"));
        LocalDateTime captured = parse(l.getCapturedAt());
        LocalDateTime submitted = parse(l.getUpdatedTime() != null ? l.getUpdatedTime() : l.getCreatedDate());
        if (captured == null || submitted == null) {
            out.add(new Check("Captured recently", false, "Capture time not recorded"));
        } else {
            long minutes = Math.abs(java.time.Duration.between(captured, submitted).toMinutes());
            out.add(new Check("Captured recently", minutes <= 24 * 60,
                    minutes < 60 ? minutes + " min before submitting" : (minutes / 60) + " h before submitting"));
        }
        return out;
    }

    /** Records one officer decision; called by the review endpoint after it has updated the listing. */
    public static void recordDecision(Listing l, String decision, String grade, String notes, AuthUser me, String at) {
        if (l.getReviews() == null) l.setReviews(new ArrayList<>());
        l.getReviews().add(new Listing.ReviewEntry(decision, grade, notes, me.id(), me.fullName(), at));
    }

    public List<HistoryItem> history(String scope, String decision, String crop, String from, String to,
                                     String search, AuthUser me) {
        boolean mine = !"all".equalsIgnoreCase(scope);
        List<HistoryItem> all = new ArrayList<>(listings.findAll().stream().flatMap(l -> decisions(l, me.id()).stream()).toList());
        Collections.reverse(all); // later entries first when two decisions share a timestamp (stable sort below)
        return all.stream()
                .filter(h -> !mine || h.byMe())
                .filter(h -> blank(decision) || "ALL".equalsIgnoreCase(decision) || decision.equalsIgnoreCase(h.decision()))
                .filter(h -> blank(crop) || (h.cropName() != null && h.cropName().equalsIgnoreCase(crop)))
                .filter(h -> blank(from) || (h.at() != null && h.at().substring(0, Math.min(10, h.at().length())).compareTo(from) >= 0))
                .filter(h -> blank(to) || (h.at() != null && h.at().substring(0, Math.min(10, h.at().length())).compareTo(to) <= 0))
                .filter(h -> matches(search, h.cropName(), h.variety(), h.farmerName(), h.location(), h.officerName()))
                .sorted(Comparator.comparing(HistoryItem::at, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Decisions on a listing; older listings have no review log, so one entry is derived from the listing. */
    List<HistoryItem> decisions(Listing l, String myId) {
        String farmer = farmerName(l);
        if (l.getReviews() != null && !l.getReviews().isEmpty()) {
            return l.getReviews().stream().map(r -> new HistoryItem(l.getId(), l.getCropName(), l.getVariety(), farmer,
                    l.getLocation(), r.getDecision(), r.getGrade(), r.getNotes(), r.getOfficerId(), r.getOfficerName(),
                    myId != null && myId.equals(r.getOfficerId()), norm(r.getAt()), l.getStatus(), coverUrl(l))).toList();
        }
        String decision;
        if ("REJECTED".equals(l.getStatus())) decision = "REJECTED";
        else if ("RETAKE_REQUESTED".equals(l.getStatus())) decision = "RETAKE";
        else if (l.getGrade() != null) decision = "APPROVED";
        else return List.of();
        String at = norm(l.getReviewedAt() != null ? l.getReviewedAt() : l.getUpdatedTime());
        return List.of(new HistoryItem(l.getId(), l.getCropName(), l.getVariety(), farmer, l.getLocation(), decision,
                l.getGrade(), l.getReviewNotes(), l.getReviewedByOfficerId(), l.getReviewedByOfficerName(),
                myId != null && myId.equals(l.getReviewedByOfficerId()), at, l.getStatus(), coverUrl(l)));
    }

    // ================================================================= users

    public List<UserRow> users(String role, String status, String search) {
        return users.findAll().stream()
                .filter(u -> blank(role) || "ALL".equalsIgnoreCase(role) || role.equalsIgnoreCase(u.getRole()))
                .filter(u -> blank(status) || "all".equalsIgnoreCase(status)
                        || ("suspended".equalsIgnoreCase(status) == u.isAccountSuspended()))
                .filter(u -> matches(search, u.getFullName(), u.getEmail(), u.getMobileNumber(), u.getNic(), u.getVehiclePlate(),
                        u.getFarmLocation() == null ? null : u.getFarmLocation().getAddress()))
                .sorted(Comparator.comparing((User u) -> roleOrder(u.getRole())).thenComparing(u -> safe(u.getFullName())))
                .map(this::row)
                .toList();
    }

    public UserDetail user(String id) {
        User u = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
        List<ListingBrief> ls = "FARMER".equals(u.getRole())
                ? listings.findByFarmerId(id).stream()
                .sorted(Comparator.comparing((Listing l) -> norm(l.getCreatedDate()), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(l -> new ListingBrief(l.getId(), l.getCropName(), l.getStatus(), number(l.getQuantity()),
                        number(l.getPrice()), l.getGrade(), norm(l.getCreatedDate())))
                .toList()
                : List.of();
        Map<String, Order> mine = new LinkedHashMap<>();
        Stream.of(orders.findByFarmerId(id), orders.findByBuyerId(id), orders.findByTransporterId(id))
                .flatMap(List::stream).forEach(o -> mine.putIfAbsent(o.getOrderId(), o));
        List<OrderRow> os = mine.values().stream().map(this::orderRow)
                .sorted(Comparator.comparing(OrderRow::createdDate, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
        List<DisputeRow> ds = disputes.findAll().stream()
                .filter(d -> id.equals(d.getRaisedByUserId()) || mine.containsKey(d.getOrderId()))
                .map(this::disputeRow).toList();
        Wallet w = wallets.findByUserId(id).orElse(null);
        FarmLocation loc = u.getFarmLocation();
        return new UserDetail(row(u), u.getNic(), loc == null ? null : loc.getLatitude(), loc == null ? null : loc.getLongitude(),
                u.getVehicleCapacityKg(), u.getVehicleBodyType(), u.getLicenceUrl(), u.getSuspendedAt(), u.getSuspendedByName(),
                u.getReactivatedAt(), u.getFailedOrders(), w == null ? null : w.getTotalPenalties(), ls, os, ds);
    }

    public UserDetail suspend(String id, String reason, AuthUser me) {
        User u = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (me.id().equals(id)) throw new IllegalStateException("You cannot suspend your own account");
        if (Roles.OFFICER.equals(u.getRole()) || Roles.ADMIN.equals(u.getRole())) {
            throw new IllegalStateException("Agricultural Officer accounts cannot be suspended here");
        }
        if (u.isAccountSuspended()) throw new IllegalStateException("This account is already suspended");
        String r = reason == null ? "" : reason.trim();
        if (r.length() < 10) throw new IllegalStateException("Write the reason for the suspension (at least 10 characters)");
        if (r.length() > 500) throw new IllegalStateException("Keep the reason under 500 characters");
        u.setAccountSuspended(true);
        u.setSuspensionReason(r);
        u.setSuspendedAt(now());
        u.setSuspendedById(me.id());
        u.setSuspendedByName(me.fullName());
        u.setReactivatedAt(null);
        if ("TRANSPORTER".equals(u.getRole())) u.setOnline(false); // no new job requests
        users.save(u);
        notifications.notify(id, "ORDERS", "ACCOUNT_SUSPENDED", "Account suspended",
                "An Agricultural Officer suspended your HarvestHub account. Reason: " + r);
        return user(id);
    }

    public UserDetail reactivate(String id, String note, AuthUser me) {
        User u = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!u.isAccountSuspended()) throw new IllegalStateException("This account is not suspended");
        u.setAccountSuspended(false);
        u.setReactivatedAt(now());
        users.save(u);
        notifications.notify(id, "ORDERS", "ACCOUNT_REACTIVATED", "Account reactivated",
                "Your HarvestHub account is active again" + (blank(note) ? "." : ". Officer note: " + note.trim()));
        return user(id);
    }

    // ================================================================= orders

    public List<OrderRow> orders(String stage, String search) {
        return orders.findAll().stream()
                .map(this::orderRow)
                .filter(o -> blank(stage) || "all".equalsIgnoreCase(stage) || stage.equalsIgnoreCase(o.stage()))
                .filter(o -> matches(search, o.orderId(), o.cropName(), o.farmerName(), o.buyerName(), o.transporterName(), o.status()))
                .sorted(Comparator.comparing(OrderRow::createdDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public OrderDetail order(String orderId) {
        Order o = orders.findById(orderId).orElseThrow(() -> new NoSuchElementException("Order not found"));
        List<DisputeRow> ds = disputes.findByOrderIdOrderByCreatedAtDesc(orderId).stream().map(this::disputeRow).toList();
        return new OrderDetail(orderRow(o), o.getPricePerKg(), o.getDeliveryDate(), o.getPickupAddress(), o.getDropAddress(),
                o.getDistanceKm(), o.getVehicleLabel(), o.getVehiclePlate(), o.getPayment(), o.getAdvancePaymentMethod(),
                o.getBalancePaymentMethod(), o.getStatusHistory(), o.getTransportEvents(), o.getLoadingEvidenceUrls(),
                o.getLoadingEvidenceAt(), o.getRating(), o.getRatingComment(),
                briefOf(o.getFarmerId()), briefOf(o.getBuyerId()), briefOf(o.getTransporterId()), ds);
    }

    public static String stage(Order o) {
        if (o.isDisputeOpen() || PROBLEM_STATUSES.contains(o.getStatus())) return "PROBLEM";
        if ("COMPLETED".equals(o.getStatus())) return "COMPLETED";
        return "ACTIVE";
    }

    OrderRow orderRow(Order o) {
        Double cropValue = o.getPayment() != null && o.getPayment().getCropValue() > 0 ? o.getPayment().getCropValue()
                : (o.getQuantity() > 0 && o.getPricePerKg() > 0 ? o.getQuantity() * o.getPricePerKg() : null);
        return new OrderRow(o.getOrderId(), o.getCropName(), o.getQuantity() > 0 ? o.getQuantity() : null,
                o.getFarmerId(), o.getFarmerName() != null ? o.getFarmerName() : nameOf(o.getFarmerId()),
                o.getBuyerId(), o.getBuyerName() != null ? o.getBuyerName() : nameOf(o.getBuyerId()),
                o.getTransporterId(), o.getTransporterName() != null ? o.getTransporterName() : nameOf(o.getTransporterId()),
                o.getStatus(), stage(o), cropValue, o.getTotalAmount() > 0 ? o.getTotalAmount() : null,
                norm(o.getCreatedDate()), o.isDisputeOpen());
    }

    // ================================================================= disputes

    public List<DisputeRow> disputes(String status, String type) {
        return disputes.findAll().stream()
                .filter(d -> blank(status) || "all".equalsIgnoreCase(status)
                        || ("open".equalsIgnoreCase(status) ? "OPEN".equals(d.getStatus()) : "closed".equalsIgnoreCase(status)
                        ? !"OPEN".equals(d.getStatus()) : status.equalsIgnoreCase(d.getStatus())))
                .filter(d -> blank(type) || "all".equalsIgnoreCase(type) || type.equalsIgnoreCase(d.getType()))
                .sorted(Comparator.comparing((Dispute d) -> "OPEN".equals(d.getStatus()) ? 0 : 1)
                        .thenComparing(d -> safe(d.getCreatedAt()), Comparator.reverseOrder()))
                .map(this::disputeRow)
                .toList();
    }

    public DisputeDetail dispute(String id) {
        Dispute d = disputes.findById(id).orElseThrow(() -> new NoSuchElementException("Dispute not found"));
        Order o = d.getOrderId() == null ? null : orders.findById(d.getOrderId()).orElse(null);
        List<DisputeRow> others = d.getOrderId() == null ? List.of()
                : disputes.findByOrderIdOrderByCreatedAtDesc(d.getOrderId()).stream()
                .filter(x -> !x.getId().equals(id)).map(this::disputeRow).toList();
        return new DisputeDetail(disputeRow(d), o == null ? null : orderRow(o), o == null ? null : o.getPayment(),
                briefOf(d.getRaisedByUserId()), o == null ? null : briefOf(o.getFarmerId()),
                o == null ? null : briefOf(o.getBuyerId()), o == null ? null : briefOf(o.getTransporterId()), others);
    }

    DisputeRow disputeRow(Dispute d) {
        Order o = d.getOrderId() == null ? null : orders.findById(d.getOrderId()).orElse(null);
        return new DisputeRow(d.getId(), d.getType() == null ? "ISSUE" : d.getType(), d.getStatus(), d.getReason(),
                d.getDetails(), d.getRaisedByUserId(), d.getRaisedByName(), d.getRaisedByRole(), norm(d.getCreatedAt()),
                norm(d.getResolvedAt()), d.getResolutionNote(), d.getResolvedByName(),
                d.getPhotoUrls() == null ? List.of() : d.getPhotoUrls(), d.getOrderId(),
                o == null ? null : o.getCropName(), o == null ? null : o.getStatus(), d.getOrderStatusAtOpen());
    }

    // ================================================================= helpers

    UserRow row(User u) {
        Wallet w = wallets.findByUserId(u.getId()).orElse(null);
        return new UserRow(u.getId(), u.getFullName(), u.getRole(), u.getEmail(),
                u.getMobileNumber() != null ? u.getMobileNumber() : u.getPhoneNumber(),
                u.getFarmLocation() == null ? null : u.getFarmLocation().getAddress(),
                isStaff(u) ? null : trust.trustOf(u), u.getRatingAverage(), u.getCompletedOrders(),
                u.isAccountSuspended(), u.getSuspensionReason(), w == null ? null : w.getBalance(),
                w != null && w.isSuspended(), u.getVehicleType(), u.getVehiclePlate());
    }

    UserBrief brief(User u) {
        return new UserBrief(u.getId(), u.getFullName(), u.getRole(), u.getEmail(),
                u.getMobileNumber() != null ? u.getMobileNumber() : u.getPhoneNumber(),
                u.getFarmLocation() == null ? null : u.getFarmLocation().getAddress(),
                isStaff(u) ? null : trust.trustOf(u), u.getRatingAverage(), u.getRatingCount(), u.getCompletedOrders(),
                u.isAccountSuspended());
    }

    UserBrief briefOf(String id) {
        return id == null ? null : users.findById(id).map(this::brief).orElse(null);
    }

    private String nameOf(String id) {
        return id == null ? null : users.findById(id).map(User::getFullName).orElse(null);
    }

    String farmerName(Listing l) {
        if (!blank(l.getFarmerName())) return l.getFarmerName();
        return nameOf(l.getFarmerId());
    }

    Double distanceFromFarm(Listing l) {
        if (l.getDistanceFromFarmMeters() != null) return l.getDistanceFromFarmMeters();
        if (l.getGpsLatitude() == null || l.getGpsLongitude() == null || l.getFarmerId() == null) return null;
        FarmLocation farm = users.findById(l.getFarmerId()).map(User::getFarmLocation).orElse(null);
        if (farm == null || farm.getLatitude() == null || farm.getLongitude() == null) return null;
        return metres(l.getGpsLatitude(), l.getGpsLongitude(), farm.getLatitude(), farm.getLongitude());
    }

    static double metres(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    static boolean inQueue(Listing l) {
        return l.getStatus() != null && QUEUE_STATUSES.contains(l.getStatus());
    }

    private static boolean hadRetake(Listing l) {
        if (l.getReviews() != null) return l.getReviews().stream().anyMatch(r -> "RETAKE".equals(r.getDecision()));
        return l.getReviewedAt() != null;
    }

    public static String statusLabel(String s) {
        if (s == null) return "Unknown";
        return switch (s) {
            case "PENDING", "PENDING_VERIFICATION", "SUBMITTED" -> "Pending verification";
            case "UNDER_REVIEW" -> "Under review";
            case "ACTIVE", "APPROVED" -> "Listed";
            case "RETAKE_REQUESTED" -> "Retake requested";
            case "REJECTED" -> "Rejected";
            case "ONGOING" -> "Order in progress";
            case "CLOSED", "COMPLETED", "SOLD" -> "Sold / closed";
            case "DRAFT" -> "Draft";
            default -> s.charAt(0) + s.substring(1).toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }

    /** First number in a free-text value ("450", "450 kg", "Rs. 1,250.50/kg"); null when there is none. */
    public static Double number(String s) {
        if (s == null) return null;
        Matcher m = NUMBER.matcher(s);
        if (!m.find()) return null;
        try {
            return Double.parseDouble(m.group().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Normalises the mixed date formats in the database to ISO "yyyy-MM-ddTHH:mm:ss". */
    public static String norm(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim().replace(' ', 'T');
        int dot = t.indexOf('.');
        if (dot > 0) t = t.substring(0, dot);
        return t.length() == 10 ? t + "T00:00:00" : t;
    }

    static LocalDateTime parse(String s) {
        String n = norm(s);
        if (n == null) return null;
        try {
            return LocalDateTime.parse(n.length() == 16 ? n + ":00" : n, ISO);
        } catch (Exception e) {
            return null;
        }
    }

    static boolean matches(String search, String... values) {
        if (search == null || search.isBlank()) return true;
        String q = search.trim().toLowerCase(Locale.ROOT);
        for (String v : values) {
            if (v != null && v.toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private static boolean isStaff(User u) {
        return Roles.OFFICER.equals(u.getRole()) || Roles.ADMIN.equals(u.getRole());
    }

    private static int roleOrder(String role) {
        return switch (role == null ? "" : role) {
            case "FARMER" -> 0;
            case "BUYER" -> 1;
            case "TRANSPORTER" -> 2;
            default -> 3;
        };
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    static String now() {
        return LocalDateTime.now().withNano(0).format(ISO);
    }
}
