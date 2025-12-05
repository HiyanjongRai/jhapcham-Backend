package com.example.jhapcham.seller.Service;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.seller.dto.SellerProfileRequestDTO;
import com.example.jhapcham.seller.dto.SellerProfileResponseDTO;
import com.example.jhapcham.seller.model.SellerProfile;
import com.example.jhapcham.seller.repository.SellerProfileRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private final String uploadDir = "H:\\Project\\Ecomm\\jhapcham\\uploads\\seller_logos";

    @Transactional
    public SellerProfile createOrUpdateProfile(Long userId, SellerProfileRequestDTO dto) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        SellerProfile profile = sellerProfileRepository.findByUser(user)
                .orElse(SellerProfile.builder().user(user).joinedDate(LocalDateTime.now()).build());

        profile.setStoreName(dto.getStoreName());
        profile.setAddress(dto.getAddress());
        profile.setAbout(dto.getAbout());
        profile.setDescription(dto.getDescription());

        if (dto.getLogoImage() != null && !dto.getLogoImage().isEmpty()) {
            String fileName = saveLogoImage(dto.getLogoImage());
            profile.setLogoImagePath(fileName);
        }

        return sellerProfileRepository.save(profile);
    }

    public Optional<SellerProfileResponseDTO> getSellerProfileWithProducts(Long userId) {
        return userRepository.findById(userId).map(user -> {
            SellerProfile profile = sellerProfileRepository.findByUser(user).orElse(null);
            List<Product> products = profile != null ? productRepository.findBySellerId(userId) : List.of();

            return SellerProfileResponseDTO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .contactNumber(user.getContactNumber())
                    .profileImagePath(user.getProfileImagePath())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .storeName(profile != null ? profile.getStoreName() : null)
                    .address(profile != null ? profile.getAddress() : null)
                    .about(profile != null ? profile.getAbout() : null)
                    .description(profile != null ? profile.getDescription() : null)
                    .isVerified(profile != null ? profile.getIsVerified() : false)
                    .joinedDate(profile != null ? profile.getJoinedDate() : null)
                    .logoImagePath(profile != null ? profile.getLogoImagePath() : null)
                    .products(products)
                    .build();
        });
    }

    private String saveLogoImage(MultipartFile image) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        String original = StringUtils.getFilename(image.getOriginalFilename());
        if (original == null) throw new IOException("Invalid filename");

        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot + 1).toLowerCase();

        if (!List.of("jpg", "jpeg", "png", "webp").contains(ext)) throw new IOException("Unsupported image type");

        String safeBase = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = System.currentTimeMillis() + "_" + safeBase;
        Path filePath = Paths.get(uploadDir, fileName).toAbsolutePath().normalize();
        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }
}
