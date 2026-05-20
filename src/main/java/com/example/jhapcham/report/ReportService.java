package com.example.jhapcham.report;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.order.*;
import com.example.jhapcham.report.dto.*;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final com.example.jhapcham.notification.NotificationService notificationService;

    private static final int RETURN_WINDOW_DAYS = 7;

    @Transactional
    public ReportResponseDTO createReport(Long customerId, ReportRequestDTO request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getUser() == null || !order.getUser().getId().equals(customerId)) {
            throw new AuthorizationException("This order does not belong to you");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessValidationException("Reports can only be filed for DELIVERED orders");
        }

        if (order.getDeliveredAt() != null && 
            order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessValidationException("Return window of " + RETURN_WINDOW_DAYS + " days has expired");
        }

        OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        if (!item.getOrder().getId().equals(order.getId())) {
            throw new BusinessValidationException("Item does not belong to this order");
        }

        if (reportRepository.existsByOrderItem(item)) {
            throw new BusinessValidationException("A report has already been filed for this order item.");
        }

        User seller = item.getProduct().getSellerProfile().getUser();

        Report report = Report.builder()
                .reportId(generateReportId())
                .order(order)
                .orderItem(item)
                .reportedEntityId(item.getId())
                .reportedEntityName(item.getProductNameSnapshot())
                .reportedEntityImage(item.getImagePathSnapshot())
                .type(ReportType.PRODUCT)
                .customer(customer)
                .reporter(customer)
                .seller(seller)
                .reason(request.getReason())
                .description(request.getDescription())
                .evidenceUrls(request.getEvidenceUrls())
                .status(ReportStatus.OPEN)
                .build();

        Report saved = reportRepository.save(report);
        
        // Notify Seller
        notificationService.createNotification(seller, "New Item Report", 
            "A report has been filed for item: " + item.getProductNameSnapshot(), 
            com.example.jhapcham.notification.NotificationType.REPORT_ALERT, saved.getId());

        return mapToDTO(saved);
    }

    @Transactional
    public ReportResponseDTO sellerAction(Long reportId, Long sellerId, SellerActionDTO action) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (!report.getSeller().getId().equals(sellerId)) {
            throw new AuthorizationException("Only the seller of this item can take action");
        }

        if (report.getStatus() != ReportStatus.OPEN) {
            throw new BusinessValidationException("This report has already been processed");
        }

        report.setSellerComment(action.getComment());
        report.setSellerActionAt(LocalDateTime.now());

        if (action.isApproved()) {
            report.setStatus(ReportStatus.SELLER_APPROVED);
            notificationService.createNotification(report.getCustomer(), "Report accepted by seller",
                    "Seller accepted your report for item: " + report.getOrderItem().getProductNameSnapshot()
                            + ". Use Refunds to request a payment refund if needed.",
                    com.example.jhapcham.notification.NotificationType.REPORT_ALERT, report.getId());
        } else {
            report.setStatus(ReportStatus.SELLER_REJECTED);
            // Notify Admin of dispute
            notifyAdminsOfDispute(report);
        }

        return mapToDTO(reportRepository.save(report));
    }

    @Transactional
    public ReportResponseDTO adminAction(Long reportId, AdminActionDTO action) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        if (report.getStatus() != ReportStatus.SELLER_REJECTED) {
            throw new BusinessValidationException("Only disputed reports (SELLER_REJECTED) can be reviewed by admin");
        }

        report.setAdminComment(action.getComment());
        report.setAdminActionAt(LocalDateTime.now());

        if (action.isApproved()) {
            report.setStatus(ReportStatus.ADMIN_APPROVED);
            notificationService.createNotification(report.getCustomer(), "Report approved by admin",
                    "Admin approved your report #" + report.getId()
                            + ". Refund requests are handled in the dedicated Refunds workflow.",
                    com.example.jhapcham.notification.NotificationType.REPORT_ALERT, report.getId());
        } else {
            report.setStatus(ReportStatus.REJECTED);
        }

        return mapToDTO(reportRepository.save(report));
    }

    private void notifyAdminsOfDispute(Report report) {
        List<User> admins = userRepository.findByRole(com.example.jhapcham.user.model.Role.ADMIN);
        for (User admin : admins) {
            notificationService.createNotification(admin, "Dispute Alert", 
                "Seller rejected a report (#" + report.getId() + "). Admin review required.", 
                com.example.jhapcham.notification.NotificationType.REPORT_ALERT, report.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getMyReports(Long customerId) {
        User customer = userRepository.findById(customerId).orElseThrow();
        return reportRepository.findByCustomerOrderByCreatedAtDesc(customer).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getSellerReports(Long sellerUserId) {
        User seller = userRepository.findById(sellerUserId).orElseThrow();
        return reportRepository.findBySellerOrderByCreatedAtDesc(seller).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getDisputedReports() {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.SELLER_REJECTED).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponseDTO> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 500)).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public ReportResponseDTO getReport(Long id, User actor) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (actor.getRole() != Role.ADMIN
                && !report.getCustomer().getId().equals(actor.getId())
                && !report.getSeller().getId().equals(actor.getId())) {
            throw new AuthorizationException("You do not have permission to view this report");
        }
        return mapToDTO(report);
    }

    private ReportResponseDTO mapToDTO(Report report) {
        String storeName = "";
        if (report.getSeller() != null && report.getSeller().getSellerProfile() != null) {
            storeName = report.getSeller().getSellerProfile().getStoreName();
        } else if (report.getOrderItem() != null && report.getOrderItem().getProduct() != null && 
                   report.getOrderItem().getProduct().getSellerProfile() != null) {
            storeName = report.getOrderItem().getProduct().getSellerProfile().getStoreName();
        }

        java.util.List<String> evidenceUrls = new java.util.ArrayList<>();
        if (report.getEvidenceUrls() != null) {
            evidenceUrls.addAll(report.getEvidenceUrls());
        }

        ReportResponseDTO dto = ReportResponseDTO.builder()
                .id(report.getId())
                .reportId(report.getReportId())
                .orderId(report.getOrder() != null ? report.getOrder().getId() : null)
                .orderItemId(report.getOrderItem() != null ? report.getOrderItem().getId() : null)
                .reportedEntityId(report.getReportedEntityId())
                .reportedEntityName(report.getReportedEntityName())
                .reportedEntityImage(report.getReportedEntityImage())
                .type(report.getType() != null ? report.getType().name() : null)
                .productName(report.getOrderItem() != null ? report.getOrderItem().getProductNameSnapshot() : null)
                .productImage(report.getOrderItem() != null ? report.getOrderItem().getImagePathSnapshot() : null)
                .customerId(report.getCustomer() != null ? report.getCustomer().getId() : null)
                .customerName(report.getCustomer() != null ? report.getCustomer().getFullName() : null)
                .reporterId(report.getReporter() != null ? report.getReporter().getId() : (report.getCustomer() != null ? report.getCustomer().getId() : null))
                .reporterName(report.getReporter() != null ? report.getReporter().getFullName() : (report.getCustomer() != null ? report.getCustomer().getFullName() : null))
                .sellerId(report.getSeller() != null ? report.getSeller().getId() : null)
                .storeName(storeName)
                .reason(report.getReason())
                .description(report.getDescription())
                .evidenceUrls(evidenceUrls)
                .status(report.getStatus())
                .sellerComment(report.getSellerComment())
                .adminComment(report.getAdminComment())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
        
        // Find refund if any (One-to-One might need a query if not mapped in entity)
        // I didn't add @OneToOne(mappedBy) in Report yet, but I can check repository
        // To keep it simple, I'll just check if status is approved
        if (report.getStatus() == ReportStatus.SELLER_APPROVED || report.getStatus() == ReportStatus.ADMIN_APPROVED) {
             // We could fetch actual refund details here
        }
        
        return dto;
    }

    private String generateReportId() {
        LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(java.time.LocalTime.MAX);
        Report lastReport = reportRepository.findTopByCreatedAtBetweenOrderByIdDesc(startOfDay, endOfDay);
        
        int sequence = 1;
        if (lastReport != null && lastReport.getReportId() != null) {
            try {
                String[] parts = lastReport.getReportId().split("-");
                if (parts.length == 3) {
                    sequence = Integer.parseInt(parts[2]) + 1;
                }
            } catch (Exception e) {
                log.warn("Failed to parse last report ID, resetting sequence", e);
            }
        }
        
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        return String.format("RPT-%s-%04d", dateStr, sequence);
    }
}
