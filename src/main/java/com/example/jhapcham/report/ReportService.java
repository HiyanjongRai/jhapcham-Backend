package com.example.jhapcham.report;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final com.example.jhapcham.product.ProductRepository productRepository;
    private final com.example.jhapcham.seller.SellerProfileRepository sellerProfileRepository;
    private final com.example.jhapcham.notification.NotificationService notificationService;

    public ReportDTO createReport(Long reporterId, ReportCreateRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Report report = Report.builder()
                .type(request.getType())
                .reportedEntityId(request.getReportedEntityId())
                .reason(request.getReason())
                .reporter(reporter)
                .status(ReportStatus.PENDING)
                .build();

        Report savedReport = reportRepository.save(report);

        // Notify Admin
        notifyAdmins(savedReport);

        // Notify Seller if applicable
        notifySeller(savedReport);

        return mapToDTO(savedReport);
    }

    private void notifyAdmins(Report report) {
        List<User> admins = userRepository.findByRole(com.example.jhapcham.user.model.Role.ADMIN);
        for (User admin : admins) {
            String title = "New Report Submitted";
            String message = "A new report has been submitted by " + report.getReporter().getFullName() + ". ID: "
                    + report.getId();
            notificationService.createNotification(admin, title, message,
                    com.example.jhapcham.notification.NotificationType.REPORT_ALERT, report.getId());
        }
    }

    private void notifySeller(Report report) {
        User sellerUser = null;
        String title = "Report Received";
        String message = "";

        if (report.getType() == ReportType.PRODUCT) {
            var productOpt = productRepository.findById(report.getReportedEntityId());
            if (productOpt.isPresent()) {
                var product = productOpt.get();
                sellerUser = product.getSellerProfile().getUser();
                message = "Your product '" + product.getName() + "' has been reported. Reason: " + report.getReason();
            }
        } else if (report.getType() == ReportType.SELLER) {
            var userOpt = userRepository.findById(report.getReportedEntityId());
            if (userOpt.isPresent()) {
                sellerUser = userOpt.get();
                message = "Your seller profile has been reported. Reason: " + report.getReason();
            }
        }

        if (sellerUser != null) {
            notificationService.createNotification(sellerUser, title, message,
                    com.example.jhapcham.notification.NotificationType.SELLER_ALERT, report.getId());
        }
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<ReportDTO> getReportsForSeller(Long sellerId) {
        return reportRepository.findReportsForSeller(sellerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ReportDTO updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(status);
        return mapToDTO(reportRepository.save(report));
    }

    private ReportDTO mapToDTO(Report report) {
        String entityName = "Unknown";
        String entityImage = null;

        if (report.getType() == ReportType.PRODUCT) {
            var p = productRepository.findById(report.getReportedEntityId()).orElse(null);
            if (p != null) {
                entityName = p.getName();
                if (!p.getImages().isEmpty()) {
                    entityImage = p.getImages().get(0).getImagePath();
                }
            }
        } else if (report.getType() == ReportType.SELLER) {
            // reportedEntityId for SELLER reports is likely the User ID of the seller
            // Let's verify if ReportModal sends sellerId (User ID) or SellerProfile ID.
            // Looking at similar code, it's usually User ID in URLs.
            // Let's assume User ID first, check profile.
            var u = userRepository.findById(report.getReportedEntityId()).orElse(null);
            if (u != null) {
                // Try to find seller profile
                var profile = sellerProfileRepository.findByUser(u).orElse(null);
                if (profile != null) {
                    entityName = profile.getStoreName();
                    entityImage = profile.getLogoImagePath();
                } else {
                    entityName = u.getFullName();
                    entityImage = u.getProfileImagePath();
                }
            }
        }

        return ReportDTO.builder()
                .id(report.getId())
                .type(report.getType())
                .reportedEntityId(report.getReportedEntityId())
                .reportedEntityName(entityName)
                .reportedEntityImage(entityImage)
                .reason(report.getReason())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
