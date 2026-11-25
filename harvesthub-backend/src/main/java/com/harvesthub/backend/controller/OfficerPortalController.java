package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.OfficerViews.*;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.DisputeService;
import com.harvesthub.backend.service.OfficerPortalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Monitoring for Agricultural Officers ("monitoring users, orders and disputes";
 * there is no admin role, officers do this). Access: /api/officer/** = OFFICER (or legacy ADMIN), see SecurityConfig.
 */
@RestController
@RequestMapping("/api/officer")
public class OfficerPortalController {

    private final OfficerPortalService portal;
    private final DisputeService disputeService;
    private final CurrentUser currentUser;

    public OfficerPortalController(OfficerPortalService portal, DisputeService disputeService, CurrentUser currentUser) {
        this.portal = portal;
        this.disputeService = disputeService;
        this.currentUser = currentUser;
    }

    // ---- users ---------------------------------------------------------------------------

    /** role = FARMER | BUYER | TRANSPORTER | OFFICER | all; status = active | suspended | all. */
    @GetMapping("/users")
    public List<UserRow> users(@RequestParam(value = "role", required = false) String role,
                               @RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "search", required = false) String search) {
        return portal.users(role, status, search);
    }

    @GetMapping("/users/{id}")
    public UserDetail user(@PathVariable String id) {
        return portal.user(id);
    }

    /** body: {"reason": "..."} – blocks login and every API call until reactivated. */
    @PostMapping("/users/{id}/suspend")
    public UserDetail suspend(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return portal.suspend(id, body == null ? null : body.get("reason"), currentUser.get());
    }

    /** body: {"note": "..."} optional. */
    @PostMapping("/users/{id}/reactivate")
    public UserDetail reactivate(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        return portal.reactivate(id, body == null ? null : body.get("note"), currentUser.get());
    }

    // ---- orders --------------------------------------------------------------------------

    /** stage = active | completed | problem | all. */
    @GetMapping("/orders")
    public List<OrderRow> orders(@RequestParam(value = "stage", required = false) String stage,
                                 @RequestParam(value = "search", required = false) String search) {
        return portal.orders(stage, search);
    }

    @GetMapping("/orders/{orderId}")
    public OrderDetail order(@PathVariable String orderId) {
        return portal.order(orderId);
    }

    // ---- disputes ------------------------------------------------------------------------

    /** status = open | closed | all; type = ISSUE | APPEAL | all. */
    @GetMapping("/disputes")
    public List<DisputeRow> disputes(@RequestParam(value = "status", required = false) String status,
                                     @RequestParam(value = "type", required = false) String type) {
        return portal.disputes(status, type);
    }

    @GetMapping("/disputes/{id}")
    public DisputeDetail dispute(@PathVariable String id) {
        return portal.dispute(id);
    }

    /** body: {"outcome": "RESOLVED" | "DISMISSED", "note": "..."} – money is not changed. */
    @PostMapping("/disputes/{id}/resolve")
    public DisputeDetail resolve(@PathVariable String id, @RequestBody Map<String, String> body) {
        disputeService.resolve(id, currentUser.get(), body.get("outcome"), body.get("note"));
        return portal.dispute(id);
    }

    // ---- errors --------------------------------------------------------------------------

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
