package com.example.jhapcham.seller;

import com.example.jhapcham.common.FileStorageService;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.Status;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private final SellerApplicationRepository applicationRepo;
    private final SellerProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final FileStorageService storage;

    @Transactional
    public SellerApplication submitApplication(
            Long userId,
            String storeName,
            String address,
            MultipartFile idDoc,
            MultipartFile licenseDoc,
            MultipartFile taxDoc) {
        User seller = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Seller user not found"));

        if (seller.getRole() != Role.SELLER) {
            throw new RuntimeException("User is not a seller");
        }

        if (applicationRepo.existsByUser(seller)) {
            throw new RuntimeException("Application already submitted");
        }

        String idPath = idDoc != null
                ? storage.save(idDoc, "seller_docs", "id_" + userId)
                : null;

        String licPath = licenseDoc != null
                ? storage.save(licenseDoc, "seller_docs", "license_" + userId)
                : null;

        String taxPath = taxDoc != null
                ? storage.save(taxDoc, "seller_docs", "tax_" + userId)
                : null;

        SellerApplication app = SellerApplication.builder()
                .user(seller)
                .storeName(storeName)
                .address(address)
                .idDocumentPath(idPath)
                .businessLicensePath(licPath)
                .taxCertificatePath(taxPath)
                .status(ApplicationStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        return applicationRepo.save(app);
    }

    public List<SellerApplication> listPending() {
        return applicationRepo.findByStatus(ApplicationStatus.PENDING);
    }

    public SellerApplication getApplication(Long appId) {
        return applicationRepo.findById(appId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    @Transactional
    public SellerApplication approve(Long appId, String note) {
        SellerApplication app = getApplication(appId);

        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewNote(note);
        app.setReviewedAt(LocalDateTime.now());

        User seller = app.getUser();

        seller.setStatus(Status.ACTIVE);
        userRepo.save(seller);

        SellerProfile profile = profileRepo.findByUser(seller).orElse(null);

        if (profile == null) {
            profile = SellerProfile.builder()
                    .user(seller)
                    .storeName(app.getStoreName())
                    .address(app.getAddress())
                    .status(Status.ACTIVE)
                    .approvedAt(LocalDateTime.now())
                    .totalIncome(java.math.BigDecimal.ZERO)
                    .totalShippingCost(java.math.BigDecimal.ZERO)
                    .netIncome(java.math.BigDecimal.ZERO)
                    .build();
        } else {
            profile.setStatus(Status.ACTIVE);
            profile.setStoreName(app.getStoreName());
            profile.setAddress(app.getAddress());
            profile.setApprovedAt(LocalDateTime.now());
        }

        profileRepo.save(profile);

        return applicationRepo.save(app);
    }

    @Transactional
    public SellerApplication reject(Long appId, String note) {
        SellerApplication app = getApplication(appId);

        app.setStatus(ApplicationStatus.REJECTED);
        app.setReviewNote(note);
        app.setReviewedAt(LocalDateTime.now());

        return applicationRepo.save(app);
    }

    public SellerApplication findByUserId(Long userId) {
        return applicationRepo.findByUserId(userId).orElse(null);
    }
}