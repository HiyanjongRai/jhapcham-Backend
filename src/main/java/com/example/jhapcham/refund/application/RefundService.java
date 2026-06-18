package com.example.jhapcham.refund.application;

import com.example.jhapcham.Error.AuthenticationException;
import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.notification.application.NotificationService;
import com.example.jhapcham.notification.domain.NotificationType;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.refund.domain.*;
import com.example.jhapcham.refund.dto.*;
import com.example.jhapcham.refund.persistence.RefundEvidenceRepository;
import com.example.jhapcham.refund.persistence.RefundRepository;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundEvidenceRepository evidenceRepository;
    private final OrderRepository orderRepository;
    private final RefundWorkflowService workflowService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final InspectionService inspectionService;

    @Transactional
    public RefundResponseDTO createRefund(RefundRequestDTO req, User customer) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        // Validate that order belongs to the customer
        if (order.getUser() == null || !order.getUser().getId().equals(customer.getId())) {
            throw new AuthorizationException("You do not own this order");
        }

        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item must be selected for refund");
        }

        // Generate refund number
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        String refundNumber = "RFD-" + dateStr + "-" + randomSuffix;

        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        List<RefundItem> refundItems = new ArrayList<>();
        User seller = null;

        for (RefundItemRequestDTO itemReq : req.getItems()) {
            com.example.jhapcham.order.domain.OrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getId().equals(itemReq.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Item " + itemReq.getOrderItemId() + " does not belong to this order"));

            if (itemReq.getQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException("Refund quantity cannot exceed ordered quantity for item " + orderItem.getProductNameSnapshot());
            }

            if (seller == null) {
                seller = orderItem.getProduct().getSellerProfile().getUser();
            } else if (!seller.getId().equals(orderItem.getProduct().getSellerProfile().getUser().getId())) {
                throw new IllegalArgumentException("All refunded items must belong to the same seller");
            }

            BigDecimal amount = orderItem.getUnitPrice().multiply(new BigDecimal(itemReq.getQuantity()));
            totalRefundAmount = totalRefundAmount.add(amount);

            RefundItem refundItem = RefundItem.builder()
                    .orderItem(orderItem)
                    .quantity(itemReq.getQuantity())
                    .refundAmount(amount)
                    .build();
            refundItems.add(refundItem);
        }

        Refund refund = Refund.builder()
                .refundNumber(refundNumber)
                .order(order)
                .customer(customer)
                .seller(seller)
                .type(req.getType())
                .status(RefundStatus.REQUEST_CREATED)
                .reason(req.getReason())
                .description(req.getDescription())
                .refundAmount(totalRefundAmount)
                .build();

        for (RefundItem ri : refundItems) {
            refund.addRefundItem(ri);
        }

        // Save evidence if any
        if (req.getFileUrls() != null) {
            for (String url : req.getFileUrls()) {
                RefundEvidence evidence = RefundEvidence.builder()
                        .refund(refund)
                        .fileUrl(url)
                        .uploadedBy("CUSTOMER")
                        .build();
                refund.addEvidence(evidence);
            }
        }

        Refund saved = refundRepository.save(refund);

        // Audit Log
        auditService.logTransition(saved, null, RefundStatus.REQUEST_CREATED, customer, "Refund request created");

        // Auto move to UNDER_REVIEW
        saved.setStatus(RefundStatus.UNDER_REVIEW);
        refundRepository.save(saved);
        auditService.logTransition(saved, RefundStatus.REQUEST_CREATED, RefundStatus.UNDER_REVIEW, customer, "Moved to review");

        // Notify Seller
        notificationService.createNotification(seller, "New Refund/Exchange Request",
                "A new refund/exchange request (" + refundNumber + ") has been created for order #" + order.getCustomOrderId(),
                NotificationType.SELLER_ALERT, saved.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO uploadEvidence(Long refundId, EvidenceRequestDTO req, User customer) {
        Refund refund = getVerifiedRefundForCustomer(refundId, customer);

        RefundEvidence evidence = RefundEvidence.builder()
                .refund(refund)
                .fileUrl(req.getFileUrl())
                .note(req.getNote())
                .uploadedBy("CUSTOMER")
                .build();
        evidenceRepository.save(evidence);
        refund.getEvidence().add(evidence);

        // If in MORE_EVIDENCE_REQUESTED, transition back to UNDER_REVIEW or ADMIN_REVIEW
        if (refund.getStatus() == RefundStatus.MORE_EVIDENCE_REQUESTED) {
            RefundStatus old = refund.getStatus();
            
            // Determine if the last request for evidence was made by ADMIN
            boolean isRequestedByAdmin = refund.getAuditLogs().stream()
                    .filter(log -> log.getNewStatus() == RefundStatus.MORE_EVIDENCE_REQUESTED)
                    .max(Comparator.comparing(RefundAuditLog::getCreatedAt))
                    .map(log -> "ADMIN".equalsIgnoreCase(log.getActorRole()))
                    .orElse(false);

            RefundStatus targetStatus = isRequestedByAdmin ? RefundStatus.ADMIN_REVIEW : RefundStatus.UNDER_REVIEW;

            workflowService.validateTransition(old, targetStatus);
            refund.setStatus(targetStatus);
            refundRepository.save(refund);
            auditService.logTransition(refund, old, targetStatus, customer, "Customer submitted additional evidence");

            if (isRequestedByAdmin) {
                // Notify Seller about the update
                notificationService.createNotification(refund.getSeller(), "Dispute Updated",
                        "Customer submitted new evidence for disputed refund " + refund.getRefundNumber() + " under Admin review.",
                        NotificationType.SELLER_ALERT, refund.getId());
            } else {
                // Notify Seller
                notificationService.createNotification(refund.getSeller(), "Evidence Submitted",
                        "Customer submitted new evidence for refund " + refund.getRefundNumber(),
                        NotificationType.SELLER_ALERT, refund.getId());
            }
        }

        return convertToDto(refund);
    }

    @Transactional
    public RefundResponseDTO uploadTracking(Long refundId, TrackingRequestDTO req, User customer) {
        Refund refund = getVerifiedRefundForCustomer(refundId, customer);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.RETURN_SHIPPED);

        refund.setTrackingNumber(req.getTrackingNumber());
        refund.setStatus(RefundStatus.RETURN_SHIPPED);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.RETURN_SHIPPED, customer,
                "Return package shipped. Tracking Number: " + req.getTrackingNumber());

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Return Package Shipped",
                "Customer has shipped the return package for " + refund.getRefundNumber() + ". Tracking: " + req.getTrackingNumber(),
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO appealDecision(Long refundId, AppealRequestDTO req, User customer) {
        Refund refund = getVerifiedRefundForCustomer(refundId, customer);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.ADMIN_REVIEW);

        refund.setStatus(RefundStatus.ADMIN_REVIEW);
        refund.setAdminDecision(req.getNotes());
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.ADMIN_REVIEW, customer,
                "Customer appealed seller decision. Appeal notes: " + req.getNotes());

        // Notify Admins
        notificationService.broadcastToAdmins("New Refund Appeal",
                "Customer has appealed the rejection of refund " + refund.getRefundNumber() + ". Admin review is pending.",
                NotificationType.SYSTEM_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO acceptOffer(Long id, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.CUSTOMER_ACCEPTS);

        refund.setStatus(RefundStatus.CUSTOMER_ACCEPTS);
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.CUSTOMER_ACCEPTS, customer, "Customer accepted the seller's offer");

        // Automatically move to REFUND_PROCESSING or REPLACEMENT_PREPARING
        RefundStatus nextStatus = (refund.getType() == RefundType.EXCHANGE) 
                ? RefundStatus.REPLACEMENT_PREPARING 
                : RefundStatus.REFUND_PROCESSING;
                
        workflowService.validateTransition(RefundStatus.CUSTOMER_ACCEPTS, nextStatus);
        refund.setStatus(nextStatus);
        Refund saved = refundRepository.save(refund);
        
        String logMessage = (refund.getType() == RefundType.EXCHANGE)
                ? "Moved to replacement preparing after offer accepted"
                : "Moved to refund processing after offer accepted";
        auditService.logTransition(saved, RefundStatus.CUSTOMER_ACCEPTS, nextStatus, customer, logMessage);

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Offer Accepted",
                "Customer has accepted your refund/exchange offer for request " + refund.getRefundNumber() + ". Proceeding to payout/replacement.",
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO submitPayoutDetails(Long refundId, CustomerPayoutDetailsRequestDTO req, User customer) {
        Refund refund = getVerifiedRefundForCustomer(refundId, customer);

        refund.setCustomerQrUrl(req.getCustomerQrUrl());
        refund.setCustomerAccountDetails(req.getCustomerAccountDetails());

        Refund saved = refundRepository.save(refund);

        // Audit Log
        auditService.logTransition(saved, refund.getStatus(), refund.getStatus(), customer,
                "Customer submitted payout account/QR details");

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Payout Details Submitted",
                "Customer submitted payout account/QR details for refund request " + refund.getRefundNumber(),
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO rejectOffer(Long id, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.UNDER_REVIEW);

        refund.setStatus(RefundStatus.UNDER_REVIEW);
        if (refund.getType() == RefundType.PARTIAL_REFUND) {
            refund.setType(RefundType.REFUND);
        }
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.UNDER_REVIEW, customer, "Customer rejected the seller's offer");

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Offer Rejected",
                "Customer has rejected your refund/exchange offer for request " + refund.getRefundNumber(),
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO negotiateOffer(Long id, String notes, java.math.BigDecimal amount, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.UNDER_REVIEW);

        refund.setStatus(RefundStatus.UNDER_REVIEW);
        if (amount != null) {
            refund.setRefundAmount(amount);
        }
        if (refund.getDescription() == null) {
            refund.setDescription("Negotiation: " + notes);
        } else {
            refund.setDescription(refund.getDescription() + " | Negotiation: " + notes);
        }
        Refund saved = refundRepository.save(refund);

        String logMsg = "Customer initiated negotiation. Proposed Amount: " + (amount != null ? "NPR " + amount : "N/A") + ". Notes: " + notes;
        auditService.logTransition(saved, old, RefundStatus.UNDER_REVIEW, customer, logMsg);

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Offer Negotiation",
                "Customer has proposed a negotiation/counter-proposal for refund request " + refund.getRefundNumber() + ". Notes: " + notes,
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO escalateOffer(Long id, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.ADMIN_REVIEW);

        refund.setStatus(RefundStatus.ADMIN_REVIEW);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.ADMIN_REVIEW, customer, "Customer escalated case to Admin (requested full refund)");

        // Notify Admins
        notificationService.broadcastToAdmins("Refund Case Escalated",
                "Customer has escalated refund request " + refund.getRefundNumber() + " to admin arbitration.",
                NotificationType.SYSTEM_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO sellerEscalate(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.ADMIN_REVIEW);

        refund.setStatus(RefundStatus.ADMIN_REVIEW);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.ADMIN_REVIEW, seller, "Seller escalated the dispute to Admin arbitration");

        // Notify Admins
        notificationService.broadcastToAdmins("Refund Case Escalated by Seller",
                "Seller has escalated refund request " + refund.getRefundNumber() + " to admin arbitration.",
                NotificationType.SYSTEM_ALERT, refund.getId());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Case Escalated to Admin",
                "The seller has escalated your refund request " + refund.getRefundNumber() + " to Admin arbitration.",
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<RefundResponseDTO> getMyRefunds(User customer) {
        return refundRepository.findByCustomerOrderByCreatedAtDesc(customer)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefundResponseDTO getRefundDetails(Long id, User user) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        // Check permissions
        if (user.getRole() == Role.CUSTOMER) {
            if (!refund.getCustomer().getId().equals(user.getId())) {
                throw new AuthorizationException("Access denied to this refund");
            }
        } else if (user.getRole() == Role.SELLER) {
            if (!refund.getSeller().getId().equals(user.getId())) {
                throw new AuthorizationException("Access denied to this seller's refund");
            }
        }

        return convertToDto(refund);
    }

    // ───────────────── SELLER SERVICES ─────────────────

    @Transactional(readOnly = true)
    public List<RefundResponseDTO> getSellerRefunds(User seller) {
        return refundRepository.findBySellerOrderByCreatedAtDesc(seller)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public RefundResponseDTO approveRefund(Long id, boolean returnRequired, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.SELLER_APPROVED);

        refund.setStatus(RefundStatus.SELLER_APPROVED);
        refund.setReturnRequired(returnRequired);
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.SELLER_APPROVED, seller, "Seller approved refund request. Return required: " + returnRequired);

        RefundStatus nextStatus = returnRequired ? RefundStatus.RETURN_PENDING : RefundStatus.REFUND_PROCESSING;
        workflowService.validateTransition(RefundStatus.SELLER_APPROVED, nextStatus);
        refund.setStatus(nextStatus);
        Refund saved = refundRepository.save(refund);
        
        String logMessage = returnRequired 
            ? "Moved to return pending (Product return is required)" 
            : "Moved to refund processing (Product return is NOT required)";
        auditService.logTransition(saved, RefundStatus.SELLER_APPROVED, nextStatus, seller, logMessage);

        // Notify Customer
        String notificationMessage = returnRequired
            ? "Your refund/exchange request " + refund.getRefundNumber() + " was approved. Please ship the item back and enter tracking details."
            : "Your refund/exchange request " + refund.getRefundNumber() + " was approved. Product return is not required; refund will be processed directly.";

        notificationService.createNotification(refund.getCustomer(), "Refund Request Approved",
                notificationMessage,
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO acceptNegotiation(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.SELLER_APPROVED);

        refund.setStatus(RefundStatus.SELLER_APPROVED);
        refund.setReturnRequired(false);
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.SELLER_APPROVED, seller, "Seller accepted customer's negotiated price");

        RefundStatus nextStatus = (refund.getType() == RefundType.EXCHANGE)
                ? RefundStatus.REPLACEMENT_PREPARING
                : RefundStatus.REFUND_PROCESSING;

        workflowService.validateTransition(RefundStatus.SELLER_APPROVED, nextStatus);
        refund.setStatus(nextStatus);
        Refund saved = refundRepository.save(refund);

        String logMessage = (refund.getType() == RefundType.EXCHANGE)
                ? "Moved to replacement preparing directly (Negotiation accepted)"
                : "Moved to refund processing directly (Negotiation accepted)";
        auditService.logTransition(saved, RefundStatus.SELLER_APPROVED, nextStatus, seller, logMessage);

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Negotiation Accepted",
                "Seller accepted your negotiated terms. Proceeding to fulfillment.",
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO rejectRefund(Long id, String notes, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.SELLER_REJECTED);

        refund.setStatus(RefundStatus.SELLER_REJECTED);
        if (notes != null && !notes.trim().isEmpty()) {
            refund.setDescription((refund.getDescription() == null ? "" : refund.getDescription() + " | ") + "Rejection Reason: " + notes);
        }
        Refund saved = refundRepository.save(refund);

        String logMsg = "Seller rejected refund request" + (notes != null && !notes.trim().isEmpty() ? ". Reason: " + notes : "");
        auditService.logTransition(saved, old, RefundStatus.SELLER_REJECTED, seller, logMsg);

        // Notify Customer
        String notifyMsg = "Your refund/exchange request " + refund.getRefundNumber() + " was rejected by the seller." 
                + (notes != null && !notes.trim().isEmpty() ? " Reason: " + notes : "");
        notificationService.createNotification(refund.getCustomer(), "Refund Request Rejected",
                notifyMsg,
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO requestEvidence(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.MORE_EVIDENCE_REQUESTED);

        refund.setStatus(RefundStatus.MORE_EVIDENCE_REQUESTED);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.MORE_EVIDENCE_REQUESTED, seller, "Seller requested more evidence");

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Evidence Requested",
                "The seller has requested additional evidence for your refund request " + refund.getRefundNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO confirmReturnReceived(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.RETURN_RECEIVED);

        refund.setStatus(RefundStatus.RETURN_RECEIVED);
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.RETURN_RECEIVED, seller, "Seller confirmed return package received");

        // Move immediately to PRODUCT_INSPECTION
        workflowService.validateTransition(RefundStatus.RETURN_RECEIVED, RefundStatus.PRODUCT_INSPECTION);
        refund.setStatus(RefundStatus.PRODUCT_INSPECTION);
        Refund saved = refundRepository.save(refund);
        auditService.logTransition(saved, RefundStatus.RETURN_RECEIVED, RefundStatus.PRODUCT_INSPECTION, seller, "Status changed to inspection queue");

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO performInspection(Long id, InspectionRequestDTO dto, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        Refund inspected = inspectionService.performInspection(refund, dto, seller);
        return convertToDto(inspected);
    }

    @Transactional
    public RefundResponseDTO offerPartialRefund(Long id, PartialRefundRequestDTO req, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.OFFER_MADE);

        refund.setType(RefundType.PARTIAL_REFUND);
        refund.setRefundAmount(req.getAmount());
        refund.setStatus(RefundStatus.OFFER_MADE);
        if (refund.getReturnRequired() == null) {
            refund.setReturnRequired(false);
        }
        refund.setDescription(refund.getDescription() + " | Partial Refund Offered: " + req.getNotes());
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.OFFER_MADE, seller,
                "Seller offered partial refund of NPR " + req.getAmount() + ". Note: " + req.getNotes());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Partial Refund Offered",
                "Seller offered a partial refund of NPR " + req.getAmount() + " for request " + refund.getRefundNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO offerFullRefund(Long id, FullRefundOfferRequestDTO req, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.OFFER_MADE);

        refund.setType(RefundType.REFUND);
        refund.setStatus(RefundStatus.OFFER_MADE);
        if (refund.getReturnRequired() == null) {
            refund.setReturnRequired(false);
        }
        refund.setDescription(refund.getDescription() + " | Full Refund Offered: " + req.getNotes());
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.OFFER_MADE, seller,
                "Seller offered full refund of NPR " + refund.getRefundAmount() + ". Note: " + req.getNotes());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Full Refund Offered",
                "Seller offered a full refund of NPR " + refund.getRefundAmount() + " for request " + refund.getRefundNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO offerExchange(Long id, ExchangeRequestDTO req, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.OFFER_MADE);

        refund.setType(RefundType.EXCHANGE);
        refund.setStatus(RefundStatus.OFFER_MADE);
        if (refund.getReturnRequired() == null) {
            refund.setReturnRequired(false);
        }
        refund.setDescription(refund.getDescription() + " | Exchange Offered: " + req.getNotes());
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.OFFER_MADE, seller,
                "Seller offered exchange replacement. Note: " + req.getNotes());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Exchange Option Offered",
                "Seller offered a replacement exchange option for request " + refund.getRefundNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    // ───────────────── ADMIN SERVICES ─────────────────

    @Transactional(readOnly = true)
    public List<RefundResponseDTO> getAdminRefunds() {
        return refundRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RefundResponseDTO> getDisputes() {
        return refundRepository.findByStatusOrderByCreatedAtDesc(RefundStatus.ADMIN_REVIEW)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public RefundResponseDTO adminRequestEvidence(Long id, User admin) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.MORE_EVIDENCE_REQUESTED);

        refund.setStatus(RefundStatus.MORE_EVIDENCE_REQUESTED);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.MORE_EVIDENCE_REQUESTED, admin, "Admin requested more evidence");

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Dispute Evidence Requested",
                "The Admin has requested additional evidence for your disputed refund request " + refund.getRefundNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Dispute Evidence Requested",
                "The Admin has requested additional evidence from the customer for refund request " + refund.getRefundNumber(),
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO adminApproveRefund(Long id, User admin) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.ADMIN_APPROVED_REFUND);

        refund.setStatus(RefundStatus.ADMIN_APPROVED_REFUND);
        refund.setAdminDecision("Approved by Admin override");
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.ADMIN_APPROVED_REFUND, admin, "Admin approved refund by override");

        // Check if the product was already returned to the seller (audit log shows return received/inspection)
        boolean alreadyReturned = refund.getAuditLogs().stream()
                .anyMatch(log -> log.getNewStatus() == RefundStatus.RETURN_RECEIVED
                              || log.getNewStatus() == RefundStatus.PRODUCT_INSPECTION
                              || log.getNewStatus() == RefundStatus.INSPECTION_COMPLETE);

        RefundStatus nextStatus;
        String logMessage;

        if (alreadyReturned) {
            if (refund.getType() == RefundType.EXCHANGE) {
                nextStatus = RefundStatus.REPLACEMENT_PREPARING;
                logMessage = "Dispute resolved: Replacement preparing (Product already returned)";
            } else {
                nextStatus = RefundStatus.REFUND_PROCESSING;
                logMessage = "Dispute resolved: Refund processing (Product already returned)";
            }
        } else {
            nextStatus = RefundStatus.RETURN_PENDING;
            logMessage = "Dispute resolved: Return pending";
        }

        workflowService.validateTransition(RefundStatus.ADMIN_APPROVED_REFUND, nextStatus);
        refund.setStatus(nextStatus);
        Refund saved = refundRepository.save(refund);
        auditService.logTransition(saved, RefundStatus.ADMIN_APPROVED_REFUND, nextStatus, admin, logMessage);

        // Notify Customer & Seller
        if (alreadyReturned) {
            notificationService.createNotification(refund.getCustomer(), "Dispute Resolved - Approved",
                    "Admin resolved your appeal and approved refund/exchange for " + refund.getRefundNumber() + ". Processing fulfillment.",
                    NotificationType.REFUND_UPDATE, refund.getId());
            notificationService.createNotification(refund.getSeller(), "Dispute Override",
                    "Admin overruled your decision for refund " + refund.getRefundNumber() + ". Processing fulfillment.",
                    NotificationType.SELLER_ALERT, refund.getId());
        } else {
            notificationService.createNotification(refund.getCustomer(), "Dispute Resolved - Approved",
                    "Admin resolved your appeal and approved return for " + refund.getRefundNumber(),
                    NotificationType.REFUND_UPDATE, refund.getId());
            notificationService.createNotification(refund.getSeller(), "Dispute Override",
                    "Admin overruled your rejection for refund " + refund.getRefundNumber() + ". Item returns process started.",
                    NotificationType.SELLER_ALERT, refund.getId());
        }

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO adminRejectRefund(Long id, User admin) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.ADMIN_REJECTED_REFUND);

        refund.setStatus(RefundStatus.ADMIN_REJECTED_REFUND);
        refund.setAdminDecision("Rejected by Admin override");
        refundRepository.save(refund);
        auditService.logTransition(refund, old, RefundStatus.ADMIN_REJECTED_REFUND, admin, "Admin rejected refund by override");

        // Move to Closed
        workflowService.validateTransition(RefundStatus.ADMIN_REJECTED_REFUND, RefundStatus.CLOSED);
        refund.setStatus(RefundStatus.CLOSED);
        Refund saved = refundRepository.save(refund);
        auditService.logTransition(saved, RefundStatus.ADMIN_REJECTED_REFUND, RefundStatus.CLOSED, admin, "Dispute resolved: Closed and rejected");

        // Notify Customer & Seller
        notificationService.createNotification(refund.getCustomer(), "Dispute Resolved - Rejected",
                "Admin resolved your appeal and rejected refund " + refund.getRefundNumber() + ". Case closed.",
                NotificationType.REFUND_UPDATE, refund.getId());
        notificationService.createNotification(refund.getSeller(), "Dispute Overruled",
                "Admin upheld your rejection for refund " + refund.getRefundNumber() + ". Case closed.",
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO processPayment(Long id, User admin) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        RefundStatus old = refund.getStatus();

        if (old == RefundStatus.PENDING_ADMIN_VERIFICATION) {
            RefundStatus finalStatus = (refund.getType() == RefundType.EXCHANGE)
                    ? RefundStatus.EXCHANGE_COMPLETED
                    : RefundStatus.REFUND_COMPLETED;

            workflowService.validateTransition(old, finalStatus);
            refund.setStatus(finalStatus);
            Refund saved = refundRepository.save(refund);

            auditService.logTransition(saved, old, finalStatus, admin, "Admin verified and approved payment proof. Case resolved.");

            // Notify Customer
            notificationService.createNotification(refund.getCustomer(),
                    finalStatus == RefundStatus.EXCHANGE_COMPLETED ? "Exchange Fulfilled" : "Refund Payment Completed",
                    "Your refund/exchange case " + refund.getRefundNumber() + " is finalized.",
                    NotificationType.REFUND_UPDATE, refund.getId());

            // Notify Seller
            notificationService.createNotification(refund.getSeller(),
                    finalStatus == RefundStatus.EXCHANGE_COMPLETED ? "Exchange Fulfilled" : "Refund Payment Verified",
                    "The payment proof for case " + refund.getRefundNumber() + " has been verified and finalized by Admin.",
                    NotificationType.SELLER_ALERT, refund.getId());

            return convertToDto(saved);
        }

        RefundStatus target;
        if (refund.getType() == RefundType.EXCHANGE) {
            target = RefundStatus.REPLACEMENT_PREPARING;
        } else {
            target = RefundStatus.REFUND_PROCESSING;
        }

        workflowService.validateTransition(old, target);
        refund.setStatus(target);
        Refund saved = refundRepository.save(refund);
        auditService.logTransition(saved, old, target, admin, "Payment/Replacement approved and initiated");

        // Notify Customer
        notificationService.createNotification(saved.getCustomer(),
                target == RefundStatus.REPLACEMENT_PREPARING ? "Replacement Approved" : "Refund Approved",
                "Your dispute for request " + saved.getRefundNumber() + " was approved by Admin. Status: " + target,
                NotificationType.REFUND_UPDATE, saved.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO submitPayoutProof(Long id, PayoutProofRequestDTO dto, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.PENDING_ADMIN_VERIFICATION);

        refund.setPaymentProofUrl(dto.getPaymentProofUrl());
        refund.setPaymentReference(dto.getPaymentReference());
        refund.setPaymentComment(dto.getPaymentComment());
        refund.setStatus(RefundStatus.PENDING_ADMIN_VERIFICATION);

        Refund saved = refundRepository.save(refund);
        auditService.logTransition(saved, old, RefundStatus.PENDING_ADMIN_VERIFICATION, seller,
                "Seller submitted refund payment proof. Reference: " + dto.getPaymentReference());

        // Notify Admins
        notificationService.broadcastToAdmins("Refund Payout Verification Pending",
                "Seller submitted payment proof for refund " + refund.getRefundNumber() + ". Verification required.",
                NotificationType.SYSTEM_ALERT, refund.getId());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Refund Payout Proof Uploaded",
                "Seller has uploaded the payout proof for your refund " + refund.getRefundNumber() + ". Awaiting Admin confirmation.",
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO rejectPayoutProof(Long id, String reason, User admin) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.REFUND_PROCESSING);

        refund.setStatus(RefundStatus.REFUND_PROCESSING);
        refund.setAdminDecision("Payout proof rejected by Admin: " + reason);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.REFUND_PROCESSING, admin, "Admin rejected payment proof. Reason: " + reason);

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Refund Payment Proof Rejected",
                "Admin rejected your payment proof for refund " + refund.getRefundNumber() + ". Reason: " + reason,
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    // ───────────────── HELPER METHODS ─────────────────

    private Refund getVerifiedRefundForCustomer(Long id, User customer) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));
        if (!refund.getCustomer().getId().equals(customer.getId())) {
            throw new AuthorizationException("You do not own this refund");
        }
        return refund;
    }

    private Refund getVerifiedRefundForSeller(Long id, User seller) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));
        if (!refund.getSeller().getId().equals(seller.getId())) {
            throw new AuthorizationException("You are not the seller for this refund");
        }
        return refund;
    }

    @Transactional
    public RefundResponseDTO processRefundAfterInspection(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.REFUND_PROCESSING);

        refund.setStatus(RefundStatus.REFUND_PROCESSING);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.REFUND_PROCESSING, seller,
                "Seller approved full refund after product inspection");

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Refund Processing",
                "Seller completed the inspection of your returned item and has approved the full refund.",
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO processExchangeAfterInspection(Long id, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.REPLACEMENT_PREPARING);

        refund.setStatus(RefundStatus.REPLACEMENT_PREPARING);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.REPLACEMENT_PREPARING, seller,
                "Seller approved replacement exchange after product inspection");

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Replacement Preparing",
                "Seller completed the inspection of your returned item and is preparing your replacement item.",
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO shipReplacement(Long id, ReplacementTrackingRequestDTO dto, User seller) {
        Refund refund = getVerifiedRefundForSeller(id, seller);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.REPLACEMENT_SHIPPED);

        refund.setReplacementCourier(dto.getReplacementCourier());
        refund.setReplacementTrackingNumber(dto.getReplacementTrackingNumber());
        refund.setReplacementShippedAt(LocalDateTime.now());
        refund.setStatus(RefundStatus.REPLACEMENT_SHIPPED);

        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.REPLACEMENT_SHIPPED, seller,
                "Seller shipped replacement item. Courier: " + dto.getReplacementCourier() + 
                ", Tracking: " + dto.getReplacementTrackingNumber());

        // Notify Customer
        notificationService.createNotification(refund.getCustomer(), "Replacement Shipped",
                "Your replacement item has been shipped by the seller. Tracking number: " + dto.getReplacementTrackingNumber(),
                NotificationType.REFUND_UPDATE, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO acceptExchange(Long id, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.EXCHANGE_COMPLETED);

        refund.setStatus(RefundStatus.EXCHANGE_COMPLETED);
        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.EXCHANGE_COMPLETED, customer,
                "Customer accepted replacement item. Exchange resolved successfully.");

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Exchange Completed",
                "Customer has received and accepted the replacement product. Dispute resolved.",
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    @Transactional
    public RefundResponseDTO rejectExchange(Long id, RejectExchangeRequestDTO dto, User customer) {
        Refund refund = getVerifiedRefundForCustomer(id, customer);
        RefundStatus old = refund.getStatus();
        workflowService.validateTransition(old, RefundStatus.UNDER_REVIEW);

        // Convert request type to REFUND
        refund.setType(RefundType.REFUND);
        refund.setStatus(RefundStatus.UNDER_REVIEW);
        
        String rejectionNote = "Customer rejected replacement product. Reason: " + dto.getNotes();
        refund.setDescription((refund.getDescription() == null ? "" : refund.getDescription() + " | ") + rejectionNote);

        Refund saved = refundRepository.save(refund);

        auditService.logTransition(saved, old, RefundStatus.UNDER_REVIEW, customer, rejectionNote);

        // Notify Seller
        notificationService.createNotification(refund.getSeller(), "Replacement Rejected",
                "Customer has rejected the replacement item and is asking for a refund. Reason: " + dto.getNotes(),
                NotificationType.SELLER_ALERT, refund.getId());

        return convertToDto(saved);
    }

    public RefundResponseDTO convertToDto(Refund refund) {
        if (refund == null) return null;

        List<EvidenceResponseDTO> evidenceDTOs = refund.getEvidence() == null ? Collections.emptyList() :
                refund.getEvidence().stream()
                        .map(e -> EvidenceResponseDTO.builder()
                                .id(e.getId())
                                .fileUrl(e.getFileUrl())
                                .note(e.getNote())
                                .uploadedBy(e.getUploadedBy())
                                .uploadedAt(e.getUploadedAt())
                                .build())
                        .collect(Collectors.toList());

        InspectionResponseDTO inspectionDTO = null;
        if (refund.getInspection() != null) {
            RefundInspection i = refund.getInspection();
            inspectionDTO = InspectionResponseDTO.builder()
                    .id(i.getId())
                    .physicalDamage(i.isPhysicalDamage())
                    .waterDamage(i.isWaterDamage())
                    .missingParts(i.isMissingParts())
                    .burnDamage(i.isBurnDamage())
                    .tampering(i.isTampering())
                    .packagingIntact(i.isPackagingIntact())
                    .productMatches(i.isProductMatches())
                    .severityScore(i.getSeverityScore())
                    .inspectorNotes(i.getInspectorNotes())
                    .verdict(i.getVerdict())
                    .createdAt(i.getCreatedAt())
                    .build();
        }

        List<AuditLogResponseDTO> auditDTOs = refund.getAuditLogs() == null ? Collections.emptyList() :
                refund.getAuditLogs().stream()
                        .map(a -> AuditLogResponseDTO.builder()
                                .id(a.getId())
                                .oldStatus(a.getOldStatus())
                                .newStatus(a.getNewStatus())
                                .actorId(a.getActorId())
                                .actorRole(a.getActorRole())
                                .notes(a.getNotes())
                                .createdAt(a.getCreatedAt())
                                .build())
                        .collect(Collectors.toList());

        List<RefundItemResponseDTO> itemDTOs = refund.getItems() == null ? Collections.emptyList() :
                refund.getItems().stream()
                        .map(i -> RefundItemResponseDTO.builder()
                                .id(i.getId())
                                .orderItemId(i.getOrderItem().getId())
                                .productName(i.getOrderItem().getProductNameSnapshot())
                                .imagePath(i.getOrderItem().getImagePathSnapshot())
                                .quantity(i.getQuantity())
                                .refundAmount(i.getRefundAmount())
                                .build())
                        .collect(Collectors.toList());

        return RefundResponseDTO.builder()
                .id(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .orderId(refund.getOrder() != null ? refund.getOrder().getId() : null)
                .orderNumber(refund.getOrder() != null ? refund.getOrder().getCustomOrderId() : null)
                .customerId(refund.getCustomer() != null ? refund.getCustomer().getId() : null)
                .customerName(refund.getCustomer() != null ? refund.getCustomer().getFullName() : null)
                .customerEmail(refund.getCustomer() != null ? refund.getCustomer().getEmail() : null)
                .sellerId(refund.getSeller() != null ? refund.getSeller().getId() : null)
                .sellerName(refund.getSeller() != null ? refund.getSeller().getFullName() : null)
                .type(refund.getType())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .description(refund.getDescription())
                .verdict(refund.getVerdict())
                .refundAmount(refund.getRefundAmount())
                .damageScore(refund.getDamageScore())
                .inspectionNotes(refund.getInspectionNotes())
                .trackingNumber(refund.getTrackingNumber())
                .adminDecision(refund.getAdminDecision())
                .returnRequired(refund.getReturnRequired())
                .paymentProofUrl(refund.getPaymentProofUrl())
                .paymentReference(refund.getPaymentReference())
                .paymentComment(refund.getPaymentComment())
                .customerQrUrl(refund.getCustomerQrUrl())
                .customerAccountDetails(refund.getCustomerAccountDetails())
                .replacementCourier(refund.getReplacementCourier())
                .replacementTrackingNumber(refund.getReplacementTrackingNumber())
                .replacementShippedAt(refund.getReplacementShippedAt())
                .items(itemDTOs)
                .evidence(evidenceDTOs)
                .inspection(inspectionDTO)
                .auditLogs(auditDTOs)
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
