package com.harvesthub.backend.service;

import com.harvesthub.backend.model.Order;
import com.harvesthub.backend.model.Rating;
import com.harvesthub.backend.model.User;
import com.harvesthub.backend.repository.OrderRepository;
import com.harvesthub.backend.repository.RatingRepository;
import com.harvesthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Trust score from average rating and reliability. Every user starts at 100 %
 * and it moves with their record:
 * <pre>trust% = 100 × (0.60 × rating/5 + 0.40 × reliability)</pre>
 * rating = average stars received (5 before the first rating); reliability = completed / (completed + failed),
 * 1.0 before the first order. Failed = orders that ended through this user's fault.
 */
@Service
public class TrustService {

    /** Final statuses where this user was at fault */
    static final Set<String> FARMER_FAULT = Set.of("PRELOAD_REJECTED");
    static final Set<String> TRANSPORTER_FAULT = Set.of("RETURN_JOURNEY", "DISPUTED");

    private final UserRepository users;
    private final OrderRepository orders;
    private final RatingRepository ratings;

    public TrustService(UserRepository users, OrderRepository orders, RatingRepository ratings) {
        this.users = users;
        this.orders = orders;
        this.ratings = ratings;
    }

    public static int score(Double ratingAverage, int completed, int failed) {
        double rating = ratingAverage == null ? 1.0 : ratingAverage / 5.0;
        double reliability = completed + failed == 0 ? 1.0 : (double) completed / (completed + failed);
        return (int) Math.round(100 * (0.60 * rating + 0.40 * reliability));
    }

    /** Recalculates every user once at start-up so stored scores follow the current rule. */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void recomputeAllOnStartup() {
        int changed = 0;
        for (User u : users.findAll()) {
            if (u.getRole() == null || !java.util.Set.of("FARMER", "BUYER", "TRANSPORTER").contains(u.getRole())) continue;
            Integer before = u.getTrustScore();
            User after = recompute(u.getId());
            if (after != null && !java.util.Objects.equals(before, after.getTrustScore())) changed++;
        }
        org.slf4j.LoggerFactory.getLogger(TrustService.class).info("Trust scores recalculated ({} changed)", changed);
    }

    public User recompute(String userId) {
        User u = users.findById(userId).orElse(null);
        if (u == null) return null;
        List<Rating> received = ratings.findByToUserId(userId);
        Double avg = received.isEmpty() ? null
                : FeeCalculator.round2(received.stream().mapToInt(Rating::getStars).average().orElse(0));
        List<Order> mine = switch (u.getRole() == null ? "" : u.getRole()) {
            case "FARMER" -> orders.findByFarmerId(userId);
            case "BUYER" -> orders.findByBuyerId(userId);
            case "TRANSPORTER" -> orders.findByTransporterId(userId);
            default -> List.of();
        };
        int completed = (int) mine.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();
        Set<String> fault = "FARMER".equals(u.getRole()) ? FARMER_FAULT
                : "TRANSPORTER".equals(u.getRole()) ? TRANSPORTER_FAULT : Set.of();
        int failed = (int) mine.stream().filter(o -> fault.contains(o.getStatus())).count();
        u.setRatingAverage(avg);
        u.setRatingCount(received.size());
        u.setCompletedOrders(completed);
        u.setFailedOrders(failed);
        u.setTrustScore(score(avg, completed, failed));
        return users.save(u);
    }

    public int trustOf(User u) {
        if (u.getTrustScore() != null) return u.getTrustScore();
        return score(u.getRatingAverage(), u.getCompletedOrders() == null ? 0 : u.getCompletedOrders(),
                u.getFailedOrders() == null ? 0 : u.getFailedOrders());
    }
}
