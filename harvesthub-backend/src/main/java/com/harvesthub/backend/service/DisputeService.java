package com.harvesthub.backend.service;

import com.harvesthub.backend.model.Dispute;
import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.DisputeRepository;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/** Dispute tickets after handover: recorded on the order, officers notified, no money moves. */
@Service
public class DisputeService {

    /** After the buyer accepted the goods; earlier problems use REJECT_LOADING / REJECT_DELIVERY. */
    static final Set<String> ALLOWED = Set.of("BALANCE_PAID", "DELIVERED", "COMPLETED");

    private final DisputeRepository disputes;
    private final OrderRepository orders;
    private final UserRepository users;
    private final NotificationService notifications;

    public DisputeService(DisputeRepository disputes, OrderRepository orders, UserRepository users,
                          NotificationService notifications) {
        this.disputes = disputes;
        this.orders = orders;
        this.users = users;
        this.notifications = notifications;
    }

    /** Driver appeal after a post-delivery rejection: ticket only, the damage charge stays. */
    static final Set<String> APPEAL_ALLOWED = Set.of("RETURN_JOURNEY", "RETURNED");

    public Dispute open(String orderId, String userId, String reason, String details) {
        return open(orderId, userId, reason, details, null);
    }

    public Dispute open(String orderId, String userId, String reason, String details, List<String> photoUrls) {
        Order o = orders.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found"));
        boolean appeal = userId.equals(o.getTransporterId()) && APPEAL_ALLOWED.contains(o.getStatus());
        if (!appeal && !ALLOWED.contains(o.getStatus())) {
            throw new IllegalStateException("Report an issue after the goods were accepted. Before that, reject the delivery instead.");
        }
        if (photoUrls != null && photoUrls.size() > 4) throw new IllegalStateException("Attach at most 4 photos");
        if (reason == null || reason.isBlank()) throw new IllegalStateException("Choose what went wrong");
        if (details != null && details.length() > 1000) throw new IllegalStateException("Keep the details under 1000 characters");
        String role = userId.equals(o.getBuyerId()) ? "BUYER"
                : userId.equals(o.getFarmerId()) ? "FARMER"
                : userId.equals(o.getTransporterId()) ? "TRANSPORTER" : "STAFF";
        boolean alreadyOpen = disputes.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .anyMatch(d -> "OPEN".equals(d.getStatus()) && userId.equals(d.getRaisedByUserId()));
        if (alreadyOpen) throw new IllegalStateException("You already have an open issue for this order");

        Dispute d = new Dispute();
        d.setOrderId(orderId);
        d.setRaisedByUserId(userId);
        d.setRaisedByName(users.findById(userId).map(User::getFullName).orElse(null));
        d.setRaisedByRole(role);
        d.setReason(reason.trim());
        d.setDetails(details == null ? null : details.trim());
        d.setOrderStatusAtOpen(o.getStatus());
        d.setType(appeal ? "APPEAL" : "ISSUE");
        d.setPhotoUrls(photoUrls == null ? new java.util.ArrayList<>()
                : new java.util.ArrayList<>(photoUrls.stream().filter(u -> u != null && !u.isBlank()).toList()));
        d.setStatus("OPEN");
        d.setCreatedAt(OrderService.now());
        Dispute saved = disputes.save(d);

        o.setDisputeOpen(true);
        orders.save(o);

        String summary = "Order " + orderId + " (" + o.getCropName() + "): " + d.getReason();
        for (String staffRole : new String[]{"OFFICER", "ADMIN"}) {
            for (User staff : users.findByRole(staffRole)) {
                notifications.notify(staff.getId(), "ORDERS", "DISPUTE_OPENED", "New dispute from a " + role.toLowerCase(), summary);
            }
        }
        for (String other : new String[]{o.getBuyerId(), o.getFarmerId(), o.getTransporterId()}) {
            if (other != null && !other.equals(userId)) {
                notifications.notify(other, "ORDERS", "DISPUTE_OPENED", "Issue reported on your order",
                        "An issue was reported on order " + orderId + ". An agricultural officer will review it.");
            }
        }
        notifications.notify(userId, "ORDERS", "DISPUTE_OPENED", "Issue reported",
                (appeal ? "Your appeal " : "Ticket ") + saved.getId() + " was sent to the officers. Payments stay as they are while it is reviewed.");
        return saved;
    }

    /**
     * Officer decision on a ticket or a driver's appeal: RESOLVED or DISMISSED with a written
     * note. Money is not moved – the workflow already settled it; the note tells everyone what was decided.
     */
    public Dispute resolve(String disputeId, com.harvesthub.backend.security.AuthUser officer, String outcome, String note) {
        Dispute d = disputes.findById(disputeId).orElseThrow(() -> new java.util.NoSuchElementException("Dispute not found"));
        if (!"OPEN".equals(d.getStatus())) throw new IllegalStateException("This case is already closed");
        String result = outcome == null ? "" : outcome.trim().toUpperCase();
        if (!List.of("RESOLVED", "DISMISSED").contains(result)) throw new IllegalStateException("Choose Resolve or Dismiss");
        String text = note == null ? "" : note.trim();
        if (text.length() < 10) throw new IllegalStateException("Write the decision for the parties (at least 10 characters)");
        if (text.length() > 1000) throw new IllegalStateException("Keep the decision under 1000 characters");
        d.setStatus(result);
        d.setResolvedAt(OrderService.now());
        d.setResolutionNote(text);
        d.setResolvedById(officer.id());
        d.setResolvedByName(officer.fullName());
        Dispute saved = disputes.save(d);

        orders.findById(d.getOrderId()).ifPresent(o -> {
            boolean stillOpen = disputes.findByOrderIdOrderByCreatedAtDesc(o.getOrderId()).stream()
                    .anyMatch(x -> "OPEN".equals(x.getStatus()));
            o.setDisputeOpen(stillOpen);
            orders.save(o);
            String what = "APPEAL".equals(d.getType()) ? "appeal" : "issue";
            String verdict = "RESOLVED".equals(result) ? "resolved" : "dismissed";
            notifications.notify(d.getRaisedByUserId(), "ORDERS", "DISPUTE_" + result,
                    "Your " + what + " was " + verdict,
                    "Order " + o.getOrderId() + " (" + o.getCropName() + "). Officer decision: " + text);
            for (String other : new String[]{o.getBuyerId(), o.getFarmerId(), o.getTransporterId()}) {
                if (other != null && !other.equals(d.getRaisedByUserId())) {
                    notifications.notify(other, "ORDERS", "DISPUTE_" + result, "Issue on your order was " + verdict,
                            "Order " + o.getOrderId() + " (" + o.getCropName() + "). Officer decision: " + text);
                }
            }
        });
        return saved;
    }

    public List<Dispute> forOrder(String orderId) {
        return disputes.findByOrderIdOrderByCreatedAtDesc(orderId);
    }
}
