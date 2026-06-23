package com.example.jhapcham.user.application;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
import com.example.jhapcham.Error.AuthenticationException;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.common.CloudinaryService;
import com.example.jhapcham.seller.persistence.SellerProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.List;

import com.example.jhapcham.seller.persistence.SellerApplicationRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SellerApplicationRepository applicationRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final com.example.jhapcham.notification.application.EmailService emailService;
    private final com.example.jhapcham.loyalty.application.LoyaltyService loyaltyService;
    private final String PROFILE_SUBDIR = "customer-profile";
    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    @Value("${app.security.google.client-id}")
    private String googleClientId;

    @Transactional
    public User loginWithGoogle(String credential) {
        // 1. Verify the Google ID token
        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload;
        try {
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
                new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
                    .Builder(new com.google.api.client.http.javanet.NetHttpTransport(),
                             com.google.api.client.json.gson.GsonFactory.getDefaultInstance())
                    .setAudience(java.util.Collections.singletonList(googleClientId))
                    .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                throw new AuthenticationException("Invalid Google token");
            }
            payload = idToken.getPayload();
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthenticationException("Google token verification failed: " + e.getMessage());
        }

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String googleSub = payload.getSubject(); // unique Google user ID

        // 2. Find existing user by email OR auto-create
        return users.findByEmail(email).orElseGet(() -> {
            // Generate a unique username from the email prefix
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            String username = baseUsername;
            int suffix = 1;
            while (users.existsByUsername(username)) {
                username = baseUsername + suffix++;
            }

            // Random secure password (user will never use this - they login via Google)
            byte[] randomBytes = new byte[24];
            OTP_RANDOM.nextBytes(randomBytes);
            String randomPassword = java.util.Base64.getEncoder().encodeToString(randomBytes);

            User newUser = User.builder()
                    .username(username)
                    .fullName(name != null ? name : username)
                    .email(email)
                    .password(passwordEncoder.encode(randomPassword))
                    .role(Role.CUSTOMER)
                    .status(Status.ACTIVE)
                    .build();

            User saved = users.save(newUser);

            try {
                loyaltyService.initializeLoyaltyPoints(saved);
            } catch (Exception ex) {
                log.error("Failed to initialize loyalty points for Google user {}: {}", saved.getId(), ex.getMessage());
            }

            return saved;
        });
    }

    @Transactional
    public User loginWithGoogle(String email, String name, String googleSub) {
        User user = users.findByEmail(email).orElseGet(() -> {
            String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            String username = baseUsername;
            int suffix = 1;
            while (users.existsByUsername(username)) {
                username = baseUsername + suffix++;
            }

            byte[] randomBytes = new byte[24];
            OTP_RANDOM.nextBytes(randomBytes);
            String randomPassword = java.util.Base64.getEncoder().encodeToString(randomBytes);

            User newUser = User.builder()
                    .username(username)
                    .fullName(name != null ? name : username)
                    .email(email)
                    .password(passwordEncoder.encode(randomPassword))
                    .role(Role.CUSTOMER)
                    .status(Status.ACTIVE)
                    .build();

            User saved = users.save(newUser);

            try {
                loyaltyService.initializeLoyaltyPoints(saved);
            } catch (Exception ex) {
                log.error("Failed to initialize loyalty points for Google user {}: {}", saved.getId(), ex.getMessage());
            }

            return saved;
        });

        return validateUserStatus(user);
    }

    @Transactional
    public User upgradeCustomerToSeller(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new AuthorizationException("Admin accounts cannot be converted to seller accounts");
        }

        if (user.getRole() == Role.CUSTOMER) {
            user.setRole(Role.SELLER);
            user.setStatus(Status.PENDING);
            user = users.save(user);
        }

        return validateUserStatus(user);
    }

    @Transactional
    public User registerCustomer(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessValidationException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new BusinessValidationException("Email already exists");
        }

        User user = User.builder()
                .username(req.username())
                .fullName(req.fullName())
                .email(req.email())
                .contactNumber(req.contactNumber())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.CUSTOMER)
                .status(Status.ACTIVE)
                .build();

        User saved = users.save(user);

        // Initialize a loyalty account for new customers
        try {
            loyaltyService.initializeLoyaltyPoints(saved);
        } catch (Exception e) {
            // Non-critical — log and continue
            log.error("Failed to initialize loyalty points for user {}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public User registerSeller(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessValidationException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new BusinessValidationException("Email already exists");
        }

        User user = User.builder()
                .username(req.username())
                .fullName(req.fullName())
                .email(req.email())
                .contactNumber(req.contactNumber())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.SELLER)
                .status(Status.PENDING)
                .build();

        return users.save(user);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public User registerAdmin(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessValidationException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new BusinessValidationException("Email already exists");
        }

        User user = User.builder()
                .username(req.username())
                .fullName(req.fullName())
                .email(req.email())
                .contactNumber(req.contactNumber())
                .password(passwordEncoder.encode(req.password()))
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();

        return users.save(user);
    }

    public User login(LoginRequestDTO req) {
        User user = users.findByUsername(req.usernameOrEmail())
                .or(() -> users.findByEmail(req.usernameOrEmail()))
                .orElseThrow(() -> new AuthenticationException("Incorrect email or password. Please try again."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new AuthenticationException("Incorrect email or password. Please try again.");
        }

        return validateUserStatus(user);
    }

    private User validateUserStatus(User user) {
        if (user.getRole() == Role.SELLER) {
            // Allow PENDING sellers to login so they can see their status page
            if (user.getStatus() == Status.BLOCKED) {
                throw new AuthorizationException("Your account is blocked, contact support");
            }
        } else {
            if (user.getStatus() != Status.ACTIVE) {
                throw new AuthorizationException("Account is not active");
            }
        }
        return user;
    }

    @Transactional
    public User updateUserById(Long userId, UserProfileUpdateRequestDTO dto) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }

        if (dto.getContactNumber() != null && !dto.getContactNumber().isBlank()) {
            user.setContactNumber(dto.getContactNumber());
        }

        if (dto.getAddress() != null) {
            user.setAddress(dto.getAddress());
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null
                    || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new BusinessValidationException("Current password is incorrect");
            }
            if (dto.getConfirmPassword() != null
                    && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
                throw new BusinessValidationException("New password and confirm password do not match");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            if (user.getProfileImagePath() != null && user.getProfileImagePath().contains("cloudinary.com")) {
                cloudinaryService.delete(user.getProfileImagePath());
            }
            String path = cloudinaryService.uploadImage(dto.getProfileImage(), PROFILE_SUBDIR);
            user.setProfileImagePath(path);
        }

        return users.save(user);
    }

    @Transactional
    public void generatePasswordResetOtp(String email) {
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
                
        String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));

        // Store only a hash so the database doesn't contain live OTP secrets.
        user.setResetOtp(null);
        user.setResetOtpHash(hashOtp(email, otp));
        user.setResetOtpExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        users.save(user);
        
        emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), otp);
    }
    
    @Transactional
    public void verifyPasswordResetOtp(String email, String otp) {
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BusinessValidationException("User with this email not found"));

        if (user.getResetOtpExpiry() == null || java.time.LocalDateTime.now().isAfter(user.getResetOtpExpiry())) {
            throw new BusinessValidationException("OTP has expired");
        }

        // Prefer hashed OTP validation; accept legacy plaintext OTP if present and migrate it.
        if (user.getResetOtpHash() != null && !user.getResetOtpHash().isBlank()) {
            String expected = user.getResetOtpHash();
            String actual = hashOtp(email, otp);
            if (!constantTimeEquals(expected, actual)) {
                throw new BusinessValidationException("Invalid OTP");
            }
            return;
        }

        // Legacy fallback (older rows) — migrate to hash on successful check.
        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new BusinessValidationException("Invalid OTP");
        }
        user.setResetOtpHash(hashOtp(email, otp));
        user.setResetOtp(null);
        users.save(user);
    }
    
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        verifyPasswordResetOtp(email, otp); // re-verify before saving
        User user = users.findByEmail(email).get();

        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessValidationException("Password must be at least 8 characters long");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpHash(null);
        user.setResetOtpExpiry(null);
        users.save(user);
    }

    // helper to keep SellerProfile.status same as User.status
    private void syncSellerProfileStatus(User user) {
        if (user.getRole() != Role.SELLER) {
            return;
        }

        sellerProfileRepository.findByUser(user).ifPresent(profile -> {
            profile.setStatus(user.getStatus());
            sellerProfileRepository.save(profile);
        });
    }

    // use this method when admin or system needs to change a user's status
    @Transactional
    public User updateUserStatus(Long userId, Status newStatus) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStatus(newStatus);
        User saved = users.save(user);

        syncSellerProfileStatus(saved);

        return saved;
    }

    public UserProfileResponseDTO convertToProfileDto(User user) {
        String profileImg = user.getProfileImagePath();
        if ((profileImg == null || profileImg.isBlank()) && user.getRole() == Role.SELLER) {
            var sellerProfile = sellerProfileRepository.findByUserId(user.getId());
            if (sellerProfile.isPresent() && sellerProfile.get().getLogoImagePath() != null) {
                profileImg = sellerProfile.get().getLogoImagePath();
            }
        }
        return UserProfileResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .address(user.getAddress())
                .contactNumber(user.getContactNumber())
                .profileImagePath(profileImg)
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    public List<User> getAllUsers() {
        return users.findAll();
    }

    private static String hashOtp(String email, String otp) {
        String emailSalt = email == null ? "" : email.trim().toLowerCase();
        String input = emailSalt + ":" + (otp == null ? "" : otp.trim());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash OTP", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
