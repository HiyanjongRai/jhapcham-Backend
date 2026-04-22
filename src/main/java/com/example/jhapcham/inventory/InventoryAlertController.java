package com.example.jhapcham.inventory;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final UserRepository userRepository;

    @GetMapping("/my-alerts")
    public ResponseEntity<?> getMyAlerts(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<InventoryAlertDTO> alerts = alertService.getSellerAlerts(user.getId());
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching alerts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/unacknowledged")
    public ResponseEntity<?> getUnacknowledgedAlerts(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<InventoryAlertDTO> alerts = alertService.getUnacknowledgedAlerts(user.getId());
            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            log.error("Error fetching unacknowledged alerts: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<?> getAlert(@PathVariable Long alertId) {
        try {
            InventoryAlertDTO alert = alertService.getAlert(alertId);
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
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            alertService.acknowledgeAlert(alertId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Alert acknowledged"));
        } catch (Exception e) {
            log.error("Error acknowledging alert: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
