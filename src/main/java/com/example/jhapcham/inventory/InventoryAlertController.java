package com.example.jhapcham.inventory;

import com.example.jhapcham.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/inventory-alerts")
@RequiredArgsConstructor
public class InventoryAlertController {

    private final InventoryAlertService alertService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    @GetMapping("/my-alerts")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getMyAlerts(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            currentUserService.requireSellerSelfOrAdmin(user, user.getId());

            List<InventoryAlertDTO> alerts = alertService.getSellerAlerts(user.getId());
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching alerts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unacknowledged")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<?> getUnacknowledgedAlerts(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            currentUserService.requireSellerSelfOrAdmin(user, user.getId());

            List<InventoryAlertDTO> alerts = alertService.getUnacknowledgedAlerts(user.getId());
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching unacknowledged alerts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<?> getAlert(@PathVariable Long alertId, Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            InventoryAlertDTO alert = alertService.getAlert(alertId, user);
            return ResponseEntity.ok(alert);
        } catch (Exception e) {
            log.error("Error fetching alert: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{alertId}/acknowledge")
    public ResponseEntity<?> acknowledgeAlert(
            @PathVariable Long alertId,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            currentUserService.requireSellerSelfOrAdmin(user, user.getId());

            alertService.acknowledgeAlert(alertId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Alert acknowledged"));
        } catch (Exception e) {
            log.error("Error acknowledging alert: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
