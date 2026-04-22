package com.example.jhapcham.report;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.order.*;
import com.example.jhapcham.refund.*;
import com.example.jhapcham.report.dto.*;
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
    private final RefundRepository refundRepository;
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

        User seller = item.getProduct().getSellerProfile().getUser();

        Report report = Report.builder()
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
            createRefund(report, PayerType.SELLER);
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
            PayerType payer = action.getPayerType() != null ? action.getPayerType() : PayerType.SELLER;
            createRefund(report, payer);
        } else {
            report.setStatus(ReportStatus.REJECTED);
        }

        return mapToDTO(reportRepository.save(report));
    }

    private void createRefund(Report report, PayerType payerType) {
        Refund refund = Refund.builder()
                .report(report)
                .order(report.getOrder())
                .orderItem(report.getOrderItem())
                .customer(report.getCustomer())
                .seller(report.getSeller())
                .amount(report.getOrderItem().getLineTotal())
                .status(RefundStatus.PENDING)
                .payerType(payerType)
                .build();
        
        refundRepository.save(refund);
        
        // Notify Customer
        notificationService.createNotification(report.getCustomer(), "Refund Initiated", 
            "A refund of Rs. " + refund.getAmount() + " has been initiated for your report #" + report.getId(), 
            com.example.jhapcham.notification.NotificationType.REPORT_ALERT, report.getId());
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
        return reportRepository.findAll().stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    public ReportResponseDTO getReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToDTO(report);
    }

    private ReportResponseDTO mapToDTO(Report report) {
        ReportResponseDTO dto = ReportResponseDTO.builder()
                .id(report.getId())
                .orderId(report.getOrder().getId())
                .orderItemId(report.getOrderItem().getId())
                .productName(report.getOrderItem().getProductNameSnapshot())
                .productImage(report.getOrderItem().getImagePathSnapshot())
                .customerId(report.getCustomer().getId())
                .customerName(report.getCustomer().getFullName())
                .sellerId(report.getSeller().getId())
                .storeName(report.getOrderItem().getProduct().getSellerProfile().getStoreName())
                .reason(report.getReason())
                .description(report.getDescription())
                .evidenceUrls(report.getEvidenceUrls())
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
}
