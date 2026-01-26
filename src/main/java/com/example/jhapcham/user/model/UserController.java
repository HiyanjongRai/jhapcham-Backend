package com.example.jhapcham.user.model;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long userId) {
        var user = userRepository.findById(Objects.requireNonNull(userId, "User ID cannot be null"));
        if (user.isPresent()) {
            return ResponseEntity.ok(mapToDto(user.get()));
        }
        return ResponseEntity.status(404).body(new ErrorResponse("User not found"));
    }

    private final AuthService authService;

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUserJson(@PathVariable Long userId, @RequestBody UserProfileUpdateRequestDTO dto) {
        try {
            User updated = authService.updateUserById(userId, dto);
            return ResponseEntity.ok(mapToDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{userId}/profile-image")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long userId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            UserProfileUpdateRequestDTO dto = new UserProfileUpdateRequestDTO();
            dto.setProfileImage(file);
            User updated = authService.updateUserById(userId, dto);
            return ResponseEntity.ok(mapToDto(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(new ErrorResponse(e.getMessage()));
        }
    }

    private UserProfileResponseDTO mapToDto(User user) {
        return UserProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .address(user.getAddress())
                .contactNumber(user.getContactNumber())
                .profileImagePath(user.getProfileImagePath())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
