package com.example.jhapcham.user.model;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${user.upload.dir}")
    private String uploadDir;


    // Update user info
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request
    ) {
        User user = getUserOrThrow(id);

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getContactNumber() != null) user.setContactNumber(request.getContactNumber());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    // Get user profile image
    @GetMapping("/{id}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long id) {
        User user = getUserOrThrow(id);

        String fileName = user.getProfileImagePath();
        if (fileName == null || fileName.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path imagePath = Paths.get(uploadDir).resolve(fileName).normalize();

            if (!Files.exists(imagePath) || !Files.isReadable(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + imagePath.getFileName().toString() + "\"")
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // Upload or update profile image
    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDTO> uploadProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        User user = getUserOrThrow(id);

        if (file.isEmpty()) return ResponseEntity.badRequest().build();

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().build();
        }

        if (file.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().build(); // 5MB max

        Files.createDirectories(Paths.get(uploadDir));

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        String fileName = "user_" + id + "_" + System.currentTimeMillis() + ext;
        Path target = Paths.get(uploadDir, fileName);
        file.transferTo(target.toFile());

        user.setProfileImagePath(fileName);
        userRepository.save(user);

        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Request DTO
    public static class UpdateUserRequest {
        private String fullName;
        private String email;
        private String contactNumber;
        private String password;
        // getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getContactNumber() { return contactNumber; }
        public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
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
        private String profileImagePath;

        public static UserResponseDTO from(User user) {
            UserResponseDTO dto = new UserResponseDTO();
            dto.id = user.getId();
            dto.username = user.getUsername();
            dto.fullName = user.getFullName();
            dto.email = user.getEmail();
            dto.contactNumber = user.getContactNumber();
            dto.role = user.getRole() != null ? user.getRole().name() : null;
            dto.status = user.getStatus() != null ? user.getStatus().name() : null;
            dto.profileImagePath = user.getProfileImagePath();
            return dto;
        }

        // getters
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
