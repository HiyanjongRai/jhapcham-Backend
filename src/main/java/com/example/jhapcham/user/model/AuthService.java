package com.example.jhapcham.user.model;

import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.seller.SellerProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

import com.example.jhapcham.seller.SellerApplicationRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SellerApplicationRepository applicationRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final String PROFILE_SUBDIR = "customer-profile";

    @Transactional
    public User registerCustomer(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
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

        return users.save(user);
    }

    @Transactional
    public User registerSeller(RegisterRequestDTO req) {
        if (users.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .username(req.username())
                .email(req.email())
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
            throw new IllegalArgumentException("Username already exists");
        }
        if (users.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already exists");
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
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (user.getRole() == Role.SELLER) {
            if (user.getStatus() == Status.PENDING) {
                // "Smart Solution": Block only if they have already submitted an application
                // If they haven't submitted, allow them to login so they can submit.
                boolean hasApplication = applicationRepository.existsByUser(user);
                if (hasApplication) {
                    throw new IllegalStateException("Your application is pending approval");
                }
                // If no application, proceed (allow login)
            }
            if (user.getStatus() == Status.BLOCKED) {
                throw new IllegalStateException("Your account is blocked, contact support");
            }
        } else {
            if (user.getStatus() != Status.ACTIVE) {
                throw new RuntimeException("Account is not active");
            }
        }

        return user;
    }

    @Transactional
    public User updateUserById(Long userId, UserProfileUpdateRequestDTO dto) {
        User user = users.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            user.setFullName(dto.getFullName());
        }

        if (dto.getContactNumber() != null && !dto.getContactNumber().isBlank()) {
            user.setContactNumber(dto.getContactNumber());
        }

        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            if (dto.getCurrentPassword() == null
                    || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            if (dto.getConfirmPassword() != null
                    && !dto.getNewPassword().equals(dto.getConfirmPassword())) {
                throw new IllegalArgumentException("New password and confirm password do not match");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            String path = fileStorageService.save(
                    dto.getProfileImage(),
                    PROFILE_SUBDIR,
                    "user_" + user.getId());
            user.setProfileImagePath(path);
        }

        return users.save(user);
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(newStatus);
        User saved = users.save(user);

        syncSellerProfileStatus(saved);

        return saved;
    }

    public List<User> getAllUsers() {
        return users.findAll();
    }
}