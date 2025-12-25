package com.example.jhapcham.user.model;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final AuthService authService;

    /**
     * Update Customer Profile
     * Updates: Full Name, Contact Number, Password, Profile Image
     * Note: Address is currently part of Order, not User profile.
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateCustomerProfile(
            @PathVariable Long userId,
            @ModelAttribute UserProfileUpdateRequestDTO dto) {
        try {
            // Optional: Verify user is a customer
            // User user = userRepository.findById(userId).orElseThrow(...);
            // if (user.getRole() != Role.CUSTOMER) ...

            User updated = authService.updateUserById(userId, dto);

            UserProfileResponseDTO response = UserProfileResponseDTO.builder()
                    .id(updated.getId())
                    .username(updated.getUsername())
                    .fullName(updated.getFullName())
                    .email(updated.getEmail())
                    .contactNumber(updated.getContactNumber())
                    .profileImagePath(updated.getProfileImagePath())
                    .role(updated.getRole())
                    .status(updated.getStatus())
                    .build();

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
