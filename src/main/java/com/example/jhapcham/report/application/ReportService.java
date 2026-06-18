package com.example.jhapcham.report.application;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.common.PublicReferenceIdGenerator;
import com.example.jhapcham.common.PublicReferenceType;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.domain.ProductStatus;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.report.domain.*;
import com.example.jhapcham.report.dto.*;
import com.example.jhapcham.report.persistence.*;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.Status;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import com.example.jhapcham.notification.application.NotificationService;
import com.example.jhapcham.notification.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ProductReportRepository productReportRepository;
    private final ReportAttachmentRepository reportAttachmentRepository;
    private final SellerReportRepository sellerReportRepository;
    private final SellerPenaltyRepository sellerPenaltyRepository;
    private final SellerTrustScoreRepository sellerTrustScoreRepository;
    private final CustomerReportRepository customerReportRepository;
    private final CustomerFlagRepository customerFlagRepository;
    private final ReportModerationLogRepository reportModerationLogRepository;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final PublicReferenceIdGenerator publicReferenceIdGenerator;

    private static final Set<ReportStatus> FINAL_REPORT_STATUSES = Set.of(
            ReportStatus.ACTION_TAKEN,
            ReportStatus.CLOSED
    );
    private static final Set<ReportStatus> ACTIVE_REPORT_STATUSES = Set.of(
            ReportStatus.NEW,
            ReportStatus.UNDER_REVIEW,
            ReportStatus.NEEDS_MORE_INFO
    );

    // ==========================================
    // PRODUCT REPORT WORKFLOWS
    // ==========================================

    @Transactional
    public ProductReport createProductReport(User reporter, ProductReportRequestDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        requireText(dto.getReasonCode(), "Report reason is required");
        requireText(dto.getDetails(), "Report details are required");

        ProductReportReason reason = ProductReportReason.valueOf(dto.getReasonCode().toUpperCase());

        // De-duplicate check
        Optional<ProductReport> existingReport = productReportRepository
                .findDuplicateReport(product.getId(), reason, ACTIVE_REPORT_STATUSES);

        if (existingReport.isPresent()) {
            ProductReport duplicate = existingReport.get();
            duplicate.setPriority(duplicate.getPriority() + 1);
            duplicate.setUpdatedAt(LocalDateTime.now());
            return productReportRepository.save(duplicate);
        }

        ProductReport report = ProductReport.builder()
                .publicReferenceId(publicReferenceIdGenerator.generate(
                        PublicReferenceType.PRODUCT_REPORT,
                        productReportRepository::existsByPublicReferenceId))
                .product(product)
                .reporter(reporter)
                .reason(reason)
                .details(dto.getDetails())
                .status(ReportStatus.NEW)
                .priority(0)
                .build();

        ProductReport savedReport = productReportRepository.save(report);

        if (dto.getAttachments() != null) {
            for (String fileUrl : dto.getAttachments()) {
                ReportAttachment attachment = ReportAttachment.builder()
                        .report(savedReport)
                        .fileUrl(fileUrl)
                        .type("IMAGE")
                        .build();
                reportAttachmentRepository.save(attachment);
            }
        }

        // Notify Admins
        try {
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                notificationService.createNotification(
                        admin,
                        "New Product Report",
                        "Product " + product.getName() + " has been reported for " + reason,
                        NotificationType.SYSTEM_ALERT,
                        savedReport.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to notify admins of product report", e);
        }

        return savedReport;
    }

    @Transactional
    public ProductReport resolveProductReport(User admin, Long reportId, ReportResolutionDTO dto) {
        ProductReport report = productReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Product Report not found"));
        assertReportCanTransition(report.getStatus(), dto.getStatus());

        ReportStatus nextStatus = ReportStatus.valueOf(dto.getStatus().toUpperCase());
        report.setStatus(nextStatus);
        report.setUpdatedAt(LocalDateTime.now());

        ProductReport savedReport = productReportRepository.save(report);

        // Apply moderator action
        if ("TAKEDOWN".equalsIgnoreCase(dto.getAction())) {
            Product product = report.getProduct();
            if (product != null) {
                product.setStatus(ProductStatus.INACTIVE);
                productRepository.save(product);

                // Notify seller
                User seller = product.getSellerProfile().getUser();
                notificationService.createNotification(
                        seller,
                        "Listing Taken Down",
                        "Your listing '" + product.getName() + "' was taken down due to violation: " + report.getReason(),
                        NotificationType.SELLER_ALERT,
                        product.getId()
                );
            }
        }

        // Log action
        ReportModerationLog modLog = ReportModerationLog.builder()
                .reportType("PRODUCT")
                .reportId(reportId)
                .moderator(admin)
                .action(dto.getAction())
                .note(dto.getNote())
                .timestamp(LocalDateTime.now())
                .build();
        reportModerationLogRepository.save(modLog);

        return savedReport;
    }

    // ==========================================
    // SELLER REPORT WORKFLOWS
    // ==========================================

    @Transactional
    public SellerReport createSellerReport(User reporter, SellerReportRequestDTO dto) {
        User seller = userRepository.findById(dto.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        requireText(dto.getReasonCode(), "Report reason is required");
        requireText(dto.getDetails(), "Report details are required");

        if (seller.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("Target user is not a Seller");
        }

        // Verify reporter has purchased from the seller before (prevent abuse)
        long orderCount = orderRepository.countDeliveredOrdersByBuyerAndSeller(reporter.getId(), seller.getId());
        if (orderCount == 0 && reporter.getRole() != Role.ADMIN) {
            throw new AuthorizationException("You must have a completed order history with this seller to file a report.");
        }

        SellerReportReason reason = SellerReportReason.valueOf(dto.getReasonCode().toUpperCase());
        if (sellerReportRepository.existsBySellerIdAndReporterIdAndReasonAndStatusIn(
                seller.getId(), reporter.getId(), reason, ACTIVE_REPORT_STATUSES)) {
            throw new BusinessValidationException("You already have an active report for this seller and reason");
        }

        SellerReport report = SellerReport.builder()
                .publicReferenceId(publicReferenceIdGenerator.generate(
                        PublicReferenceType.SELLER_REPORT,
                        sellerReportRepository::existsByPublicReferenceId))
                .seller(seller)
                .reporter(reporter)
                .reason(reason)
                .details(dto.getDetails())
                .status(ReportStatus.NEW)
                .build();

        return sellerReportRepository.save(report);
    }

    @Transactional
    public SellerReport resolveSellerReport(User admin, Long reportId, ReportResolutionDTO dto) {
        SellerReport report = sellerReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller Report not found"));
        assertReportCanTransition(report.getStatus(), dto.getStatus());

        ReportStatus nextStatus = ReportStatus.valueOf(dto.getStatus().toUpperCase());
        report.setStatus(nextStatus);
        report.setUpdatedAt(LocalDateTime.now());

        SellerReport savedReport = sellerReportRepository.save(report);

        // Apply Penalty if action is taken
        if ("PENALIZE".equalsIgnoreCase(dto.getAction()) && dto.getPenaltyType() != null) {
            SellerPenaltyType penaltyType = SellerPenaltyType.valueOf(dto.getPenaltyType().toUpperCase());
            User seller = report.getSeller();

            SellerPenalty penalty = SellerPenalty.builder()
                    .seller(seller)
                    .type(penaltyType)
                    .description(dto.getNote())
                    .issuedBy(admin)
                    .issuedAt(LocalDateTime.now())
                    .build();

            if (penaltyType == SellerPenaltyType.SUSPENSION) {
                penalty.setExpiresAt(LocalDateTime.now().plusDays(14)); // 2 weeks suspension
                seller.setStatus(Status.DEACTIVATED); // Temporary deactivate
                userRepository.save(seller);
            } else if (penaltyType == SellerPenaltyType.BAN) {
                seller.setStatus(Status.BLOCKED); // Permanent block
                userRepository.save(seller);
            }

            sellerPenaltyRepository.save(penalty);

            // Update Seller Trust Score
            updateSellerTrustScore(seller, penaltyType);

            // Notify Seller
            notificationService.createNotification(
                    seller,
                    "Account Penalty Issued",
                    "Your seller account received a penalty: " + penaltyType + ". Details: " + dto.getNote(),
                    NotificationType.SELLER_ALERT,
                    penalty.getId()
            );
        }

        // Log action
        ReportModerationLog modLog = ReportModerationLog.builder()
                .reportType("SELLER")
                .reportId(reportId)
                .moderator(admin)
                .action(dto.getAction())
                .note(dto.getNote())
                .timestamp(LocalDateTime.now())
                .build();
        reportModerationLogRepository.save(modLog);

        return savedReport;
    }

    private void updateSellerTrustScore(User seller, SellerPenaltyType penaltyType) {
        SellerTrustScore trustScore = sellerTrustScoreRepository.findById(seller.getId())
                .orElse(SellerTrustScore.builder()
                        .seller(seller)
                        .score(100)
                        .fraudRiskScore(0)
                        .lastUpdated(LocalDateTime.now())
                        .build());

        int deduction = 0;
        if (penaltyType == SellerPenaltyType.WARNING) {
            deduction = 10;
        } else if (penaltyType == SellerPenaltyType.SUSPENSION) {
            deduction = 40;
            trustScore.setFraudRiskScore(trustScore.getFraudRiskScore() + 20);
        } else if (penaltyType == SellerPenaltyType.BAN) {
            deduction = 100;
            trustScore.setFraudRiskScore(100);
        }

        trustScore.setScore(Math.max(0, trustScore.getScore() - deduction));
        trustScore.setLastUpdated(LocalDateTime.now());
        sellerTrustScoreRepository.save(trustScore);
    }

    // ==========================================
    // CUSTOMER REPORT WORKFLOWS
    // ==========================================

    @Transactional
    public CustomerReport createCustomerReport(User reporter, CustomerReportRequestDTO dto) {
        User customer = userRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        requireText(dto.getReasonCode(), "Report reason is required");
        requireText(dto.getDetails(), "Report details are required");

        if (customer.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException("Target user is not a Customer");
        }

        // Verify order links reporter (seller) and customer
        Order order = null;
        if (dto.getOrderId() != null) {
            order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            // Seller must be the owner of the items in the order
            boolean ownsOrder = order.getItems().stream()
                    .anyMatch(i -> i.getProduct().getSellerProfile().getUser().getId().equals(reporter.getId()));
            if (!ownsOrder && reporter.getRole() != Role.ADMIN) {
                throw new AuthorizationException("You do not have a transaction history with this customer for order " + dto.getOrderId());
            }
        }

        CustomerReportReason reason = CustomerReportReason.valueOf(dto.getReasonCode().toUpperCase());
        if (customerReportRepository.existsByCustomerIdAndReporterIdAndReasonAndStatusIn(
                customer.getId(), reporter.getId(), reason, ACTIVE_REPORT_STATUSES)) {
            throw new BusinessValidationException("You already have an active report for this customer and reason");
        }

        CustomerReport report = CustomerReport.builder()
                .publicReferenceId(publicReferenceIdGenerator.generate(
                        PublicReferenceType.CUSTOMER_REPORT,
                        customerReportRepository::existsByPublicReferenceId))
                .customer(customer)
                .reporter(reporter)
                .order(order)
                .reason(reason)
                .details(dto.getDetails())
                .status(ReportStatus.NEW)
                .build();

        return customerReportRepository.save(report);
    }

    @Transactional
    public CustomerReport resolveCustomerReport(User admin, Long reportId, ReportResolutionDTO dto) {
        CustomerReport report = customerReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Report not found"));
        assertReportCanTransition(report.getStatus(), dto.getStatus());

        ReportStatus nextStatus = ReportStatus.valueOf(dto.getStatus().toUpperCase());
        report.setStatus(nextStatus);
        report.setUpdatedAt(LocalDateTime.now());

        CustomerReport savedReport = customerReportRepository.save(report);

        // Apply customer flag
        if ("FLAG".equalsIgnoreCase(dto.getAction()) && dto.getFlagType() != null) {
            CustomerFlagType flagType = CustomerFlagType.valueOf(dto.getFlagType().toUpperCase());
            User customer = report.getCustomer();

            CustomerFlag flag = CustomerFlag.builder()
                    .customer(customer)
                    .type(flagType)
                    .description(dto.getNote())
                    .issuedAt(LocalDateTime.now())
                    .build();

            if (flagType == CustomerFlagType.BLOCKED) {
                customer.setStatus(Status.BLOCKED);
                userRepository.save(customer);
            }

            customerFlagRepository.save(flag);

            // Notify Customer
            notificationService.createNotification(
                    customer,
                    "Account Notice Issued",
                    "Your account has been flagged: " + flagType + ". Violation details: " + dto.getNote(),
                    NotificationType.SYSTEM_ALERT,
                    flag.getId()
            );
        }

        // Log action
        ReportModerationLog modLog = ReportModerationLog.builder()
                .reportType("CUSTOMER")
                .reportId(reportId)
                .moderator(admin)
                .action(dto.getAction())
                .note(dto.getNote())
                .timestamp(LocalDateTime.now())
                .build();
        reportModerationLogRepository.save(modLog);

        return savedReport;
    }

    // ==========================================
    // ADMIN UTILS
    // ==========================================

    @Transactional(readOnly = true)
    public List<ProductReport> getProductReports(ReportStatus status) {
        if (status == null) {
            return productReportRepository.findAll();
        }
        return productReportRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public ProductReport getProductReportByReference(String publicReferenceId) {
        String normalized = publicReferenceIdGenerator.requireValid(publicReferenceId);
        return productReportRepository.findByPublicReferenceId(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Product Report not found"));
    }

    @Transactional(readOnly = true)
    public List<SellerReport> getSellerReports(ReportStatus status) {
        if (status == null) {
            return sellerReportRepository.findAll();
        }
        return sellerReportRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public SellerReport getSellerReportByReference(String publicReferenceId) {
        String normalized = publicReferenceIdGenerator.requireValid(publicReferenceId);
        return sellerReportRepository.findByPublicReferenceId(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Seller Report not found"));
    }

    @Transactional(readOnly = true)
    public List<CustomerReport> getCustomerReports(ReportStatus status) {
        if (status == null) {
            return customerReportRepository.findAll();
        }
        return customerReportRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public CustomerReport getCustomerReportByReference(String publicReferenceId) {
        String normalized = publicReferenceIdGenerator.requireValid(publicReferenceId);
        return customerReportRepository.findByPublicReferenceId(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Customer Report not found"));
    }

    private void assertReportCanTransition(ReportStatus currentStatus, String requestedStatus) {
        requireText(requestedStatus, "Report status is required");
        if (FINAL_REPORT_STATUSES.contains(currentStatus)) {
            throw new BusinessValidationException("Report is already finalized and cannot be changed");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessValidationException(message);
        }
    }
}
