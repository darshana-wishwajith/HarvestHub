package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.GenericResponse;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.NotificationService;
import com.harvesthub.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** Settings for the logged-in user, any role. */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final UserRepository users;

    public MeController(CurrentUser currentUser, UserService userService, UserRepository users) {
        this.currentUser = currentUser;
        this.userService = userService;
        this.users = users;
    }

    /** body: {"currentPassword": "...", "newPassword": "..."} */
    @PostMapping("/change-password")
    public ResponseEntity<GenericResponse> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(currentUser.id(), body.get("currentPassword"), body.get("newPassword"));
        return ResponseEntity.ok(new GenericResponse(true, "Password updated"));
    }

    /** {"ORDERS": true, "PAYMENTS": true, "LISTINGS": true, "MARKET": true, "language": "en"} */
    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Object>> preferences() {
        var u = users.findById(currentUser.id()).orElseThrow();
        Map<String, Object> out = new LinkedHashMap<>();
        for (String c : NotificationService.CATEGORIES) {
            out.put(c, u.getNotificationPreferences() == null || !Boolean.FALSE.equals(u.getNotificationPreferences().get(c)));
        }
        out.put("language", u.getLanguage() == null ? "en" : u.getLanguage());
        return ResponseEntity.ok(out);
    }

    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(@RequestBody Map<String, Object> body) {
        var u = users.findById(currentUser.id()).orElseThrow();
        Map<String, Boolean> prefs = u.getNotificationPreferences() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(u.getNotificationPreferences());
        for (String c : NotificationService.CATEGORIES) {
            if (body.get(c) instanceof Boolean b) prefs.put(c, b);
        }
        u.setNotificationPreferences(prefs);
        u.setLanguage("en"); // English only for now
        users.save(u);
        return preferences();
    }
}
