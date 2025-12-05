package com.example.jhapcham.user.model;

import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/users/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;

    // Example: user.upload.dir=H:/Project/Ecomm/jhapcham/uploads/customer-profile
    @Value("${user.upload.dir}")
    private String uploadDir;

    // GET profile by id
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getProfile(@PathVariable Long id) {
        User user = getUserOrThrow(id);
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    // UPDATE profile fields
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest req
    ) {
        User user = getUserOrThrow(id);

        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        if (req.getContactNumber() != null) user.setContactNumber(req.getContactNumber());
        // For password you would also encode before saving

        userRepository.save(user);
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    // UPLOAD or CHANGE profile image
    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDTO> uploadProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        User user = getUserOrThrow(id);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(null);
        }

        // max 2 MB
        if (file.getSize() > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(null);
        }

        // Make sure directory exists
        Files.createDirectories(Paths.get(uploadDir));

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = "customer_" + id + "_" + System.currentTimeMillis() + ext;
        Path target = Paths.get(uploadDir, fileName);
        file.transferTo(target.toFile());

        // Store only file name in DB
        user.setProfileImagePath(fileName);
        userRepository.save(user);

        // Response contains only the path or file name
        UserResponseDTO dto = UserResponseDTO.from(user);
        return ResponseEntity.ok(dto);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Request body for update
    public static class UpdateProfileRequest {
        private String fullName;
        private String email;
        private String contactNumber;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    }

    // Response DTO
    public static class UserResponseDTO {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String contactNumber;
        private String role;
        private String status;
        private String profileImagePath;   // only path, no full URL

        public static UserResponseDTO from(User user) {
            UserResponseDTO dto = new UserResponseDTO();
            dto.id = user.getId();
            dto.username = user.getUsername();
            dto.fullName = user.getFullName();
            dto.email = user.getEmail();
            dto.contactNumber = user.getContactNumber();
            dto.role = user.getRole() != null ? user.getRole().name() : null;
            dto.status = user.getStatus() != null ? user.getStatus().name() : null;
            dto.profileImagePath = user.getProfileImagePath();   // stored value from DB
            return dto;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getContactNumber() { return contactNumber; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getProfileImagePath() { return profileImagePath; }
    }
}
