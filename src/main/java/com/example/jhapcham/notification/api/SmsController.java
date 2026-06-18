package com.example.jhapcham.notification.api;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
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
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;
    private final SmsPreferenceRepository smsPreferenceRepository;
    private final UserRepository userRepository;

    /**
     * Get current SMS preferences for user
     */
    @GetMapping("/preferences")
    public ResponseEntity<?> getSmsPreferences(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SmsPreference preference = getOrInitializeSmsPreference(user);

            SmsPreferenceDTO dto = toDTO(preference);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error fetching SMS preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update SMS preferences
     */
    @PutMapping("/preferences")
    public ResponseEntity<?> updateSmsPreferences(
            @RequestBody SmsPreferenceDTO dto,
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

            SmsPreference preference = getOrInitializeSmsPreference(user);

            // Update preferences
            if (dto.getOrderConfirmation() != null) preference.setOrderConfirmation(dto.getOrderConfirmation());
            if (dto.getShipmentUpdates() != null) preference.setShipmentUpdates(dto.getShipmentUpdates());
            if (dto.getDeliveryNotifications() != null) preference.setDeliveryNotifications(dto.getDeliveryNotifications());
            if (dto.getPromotionalSms() != null) preference.setPromotionalSms(dto.getPromotionalSms());
            if (dto.getInventoryAlerts() != null) preference.setInventoryAlerts(dto.getInventoryAlerts());
            if (dto.getAllSmsEnabled() != null) {
                preference.updateAllSmsStatus(dto.getAllSmsEnabled());
            }

            SmsPreference updated = smsPreferenceRepository.save(preference);
            log.info("SMS preferences updated for user: {}", user.getId());
            return ResponseEntity.ok(toDTO(updated));
        } catch (Exception e) {
            log.error("Error updating SMS preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Disable all SMS notifications
     */
    @PostMapping("/disable-all")
    public ResponseEntity<?> disableAllSms(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SmsPreference preference = getOrInitializeSmsPreference(user);

            preference.updateAllSmsStatus(false);
            smsPreferenceRepository.save(preference);

            log.info("All SMS disabled for user: {}", user.getId());
            return ResponseEntity.ok(Map.of("message", "All SMS notifications disabled"));
        } catch (Exception e) {
            log.error("Error disabling SMS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Enable all SMS notifications
     */
    @PostMapping("/enable-all")
    public ResponseEntity<?> enableAllSms(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SmsPreference preference = getOrInitializeSmsPreference(user);

            preference.updateAllSmsStatus(true);
            smsPreferenceRepository.save(preference);

            log.info("All SMS enabled for user: {}", user.getId());
            return ResponseEntity.ok(Map.of("message", "All SMS notifications enabled"));
        } catch (Exception e) {
            log.error("Error enabling SMS: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get SMS history for user
     */
    @GetMapping("/history")
    public ResponseEntity<?> getSmsHistory(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Please login"));
            }
            String principal = authentication.getName();
            User user = userRepository.findByUsername(principal)
                    .or(() -> userRepository.findByEmail(principal))
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<SmsLog> history = smsService.getUserSmsHistory(user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching SMS history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private SmsPreferenceDTO toDTO(SmsPreference preference) {
        return SmsPreferenceDTO.builder()
                .id(preference.getId())
                .orderConfirmation(preference.getOrderConfirmation())
                .shipmentUpdates(preference.getShipmentUpdates())
                .deliveryNotifications(preference.getDeliveryNotifications())
                .promotionalSms(preference.getPromotionalSms())
                .inventoryAlerts(preference.getInventoryAlerts())
                .allSmsEnabled(preference.getAllSmsEnabled())
                .build();
    }

    private SmsPreference getOrInitializeSmsPreference(User user) {
        return smsPreferenceRepository.findByUser(user)
                .orElseGet(() -> smsPreferenceRepository.save(SmsPreference.builder().user(user).build()));
    }
}
