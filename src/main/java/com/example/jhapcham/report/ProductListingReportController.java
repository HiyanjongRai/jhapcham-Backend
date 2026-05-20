package com.example.jhapcham.report;

import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.report.dto.ProductListingReportRequestDTO;
import com.example.jhapcham.report.dto.ProductListingReportResponseDTO;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductListingReportController {

    private final ProductListingReportRepository productListingReportRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @PostMapping("/products/{productId}/flag")
    @Transactional
    public ResponseEntity<?> flagProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductListingReportRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (productListingReportRepository.existsByProductIdAndReporterId(productId, user.getId())) {
                return ResponseEntity.badRequest().body(new ErrorResponse("You have already flagged this product listing."));
            }

            // Generate report ID PLR-YYYYMMDD-UUID (shortened)
            String customId = "PLR-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();

            ProductListingReport report = ProductListingReport.builder()
                    .reportId(customId)
                    .product(product)
                    .reporter(user)
                    .reason(request.getReason())
                    .description(request.getDescription())
                    .status(ProductListingReportStatus.PENDING)
                    .build();

            ProductListingReport saved = productListingReportRepository.save(report);
            return ResponseEntity.ok(mapToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/admin/product-flags")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProductFlags() {
        try {
            List<ProductListingReport> reports = productListingReportRepository.findAllByOrderByCreatedAtDesc();
            List<ProductListingReportResponseDTO> dtos = reports.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/seller/product-flags")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getSellerProductFlags(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = getAuthenticatedUser(userDetails);
            List<ProductListingReport> reports = productListingReportRepository
                    .findAllByProductSellerProfileUserIdOrderByCreatedAtDesc(user.getId());
            List<ProductListingReportResponseDTO> dtos = reports.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/admin/product-flags/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateFlagStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            ProductListingReport report = productListingReportRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Report not found"));

            if (body.containsKey("status")) {
                report.setStatus(ProductListingReportStatus.valueOf(body.get("status").toUpperCase()));
            }
            if (body.containsKey("adminComment")) {
                report.setAdminComment(body.get("adminComment"));
            }

            ProductListingReport saved = productListingReportRepository.save(report);
            return ResponseEntity.ok(mapToDTO(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        if (userDetails == null) throw new RuntimeException("Unauthorized");
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ProductListingReportResponseDTO mapToDTO(ProductListingReport report) {
        Product p = report.getProduct();
        User reporter = report.getReporter();
        
        String imagePath = "";
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            imagePath = p.getImages().get(0).getImagePath();
        }

        return ProductListingReportResponseDTO.builder()
                .id(report.getId())
                .reportId(report.getReportId())
                .productId(p.getId())
                .productName(p.getName())
                .productSlug(p.getSlug())
                .productImageUrl(imagePath)
                .reporterId(reporter.getId())
                .reporterName(reporter.getFullName() != null ? reporter.getFullName() : reporter.getUsername())
                .reason(report.getReason().name())
                .reasonDescription(report.getReason().getDescription())
                .description(report.getDescription())
                .status(report.getStatus().name())
                .adminComment(report.getAdminComment())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
