package com.example.jhapcham.report;

import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ReportDTO createReport(Long reporterId, ReportCreateRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Report report = Report.builder()
                .type(request.getType())
                .reportedEntityId(request.getReportedEntityId())
                .reason(request.getReason())
                .reporter(reporter)
                .status(ReportStatus.NEW)
                .build();

        // Populate details before saving (sets Name and Image)
        try {
            populateEntityDetails(report);
        } catch (Exception e) {
            // Log but don't fail report submission
            System.err.println("Failed to populate report entity details: " + e.getMessage());
        }

        Report savedReport = reportRepository.save(report);

        // Async notifications (simulated via transactional context)
        try {
            notifyAdmins(savedReport);
            notifySeller(savedReport);
        } catch (Exception e) {
            System.err.println("Notification failed during report creation: " + e.getMessage());
        }

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

    @Transactional(readOnly = true)
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

    public Report getReportById(Long id) {
        return reportRepository.findById(id).orElse(null);
    }

    @Transactional
    public ReportDTO updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));
        report.setStatus(status);
        return mapToDTO(reportRepository.save(report));
    }

    private void populateEntityDetails(Report report) {
        if (report.getType() == ReportType.PRODUCT) {
            var p = productRepository.findById(report.getReportedEntityId()).orElse(null);
            if (p != null) {
                report.setReportedEntityName(p.getName());
                if (!p.getImages().isEmpty()) {
                    report.setReportedEntityImage(p.getImages().get(0).getImagePath());
                }
            }
        } else if (report.getType() == ReportType.SELLER) {
            var u = userRepository.findById(report.getReportedEntityId()).orElse(null);
            if (u != null) {
                var profile = sellerProfileRepository.findByUser(u).orElse(null);
                if (profile != null) {
                    report.setReportedEntityName(profile.getStoreName());
                    report.setReportedEntityImage(profile.getLogoImagePath());
                } else {
                    report.setReportedEntityName(u.getFullName());
                    report.setReportedEntityImage(u.getProfileImagePath());
                }
            }
        }
    }

    private ReportDTO mapToDTO(Report report) {
        // Fallback for older reports that don't have these fields populated
        String entityName = report.getReportedEntityName();
        String entityImage = report.getReportedEntityImage();

        if (entityName == null) {
            // Quick on-the-fly fetch if missing (legacy support)
            if (report.getType() == ReportType.PRODUCT) {
                var p = productRepository.findById(report.getReportedEntityId()).orElse(null);
                if (p != null) {
                    entityName = p.getName();
                    if (!p.getImages().isEmpty())
                        entityImage = p.getImages().get(0).getImagePath();
                }
            } else {
                var u = userRepository.findById(report.getReportedEntityId()).orElse(null);
                if (u != null)
                    entityName = u.getFullName();
            }
        }

        Long sellerUserId = null;
        if (report.getType() == ReportType.PRODUCT) {
            var p = productRepository.findById(report.getReportedEntityId()).orElse(null);
            if (p != null)
                sellerUserId = p.getSellerProfile().getUser().getId();
        } else {
            // If reporting a seller, the entity ID is the seller's user ID
            sellerUserId = report.getReportedEntityId();
        }

        return ReportDTO.builder()
                .id(report.getId())
                .type(report.getType())
                .reportedEntityId(report.getReportedEntityId())
                .reportedEntityName(entityName != null ? entityName : "Unknown Entity")
                .reportedEntityImage(entityImage)
                .reason(report.getReason())
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : null)
                .reporterName(report.getReporter() != null ? report.getReporter().getFullName() : "System/Guest")
                .sellerUserId(sellerUserId)
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
