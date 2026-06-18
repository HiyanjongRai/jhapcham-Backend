package com.example.jhapcham.user.api;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final AuthService authService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    /**
     * Update Customer Profile
     * Updates: Full Name, Contact Number, Password, Profile Image
     * Note: Address is currently part of Order, not User profile.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateCustomerProfile(
            @PathVariable Long userId,
            @ModelAttribute UserProfileUpdateRequestDTO dto,
            Authentication authentication) {
        try {
            currentUserService.requireSelfOrAdmin(currentUserService.requireUser(authentication), userId);
            User updated = authService.updateUserById(userId, dto);
            UserProfileResponseDTO response = authService.convertToProfileDto(updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update customer profile"));
        }
    }
}
