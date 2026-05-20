package com.example.jhapcham.dispute;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.EmailService;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.notification.NotificationType;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.user.model.Role;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final DisputeEvidenceRepository evidenceRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final com.example.jhapcham.common.FileStorageService fileStorageService;

    @Transactional
    public DisputeResponseDTO initiateDispute(Long userId, Long orderId, String title, String description) {
        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        boolean buyerOwnsOrder = order.getUser() != null && order.getUser().getId().equals(userId);
        boolean sellerOwnsOrder = order.getItems().stream()
                .anyMatch(item -> item.getProduct() != null
                        && item.getProduct().getSellerProfile() != null
                        && item.getProduct().getSellerProfile().getUser() != null
                        && item.getProduct().getSellerProfile().getUser().getId().equals(userId));
        if (!buyerOwnsOrder && !sellerOwnsOrder) {
            throw new AuthorizationException("You are not a party to this order");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new RuntimeException("Disputes/reports can only be filed for DELIVERED orders.");
        }

        if (disputeRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("A dispute has already been filed for this order.");
        }

        User otherParty = order.getItems().get(0).getProduct().getSellerProfile().getUser();
        if (otherParty.getId().equals(userId)) {
            otherParty = order.getUser();
            if (otherParty == null) {
                throw new RuntimeException("Cannot initiate dispute against a guest order (no registered user account)");
            }
        }

     
        Dispute dispute = Dispute.builder()
                .reportId(generateReportId())
                .order(order)
                .initiatedByUser(initiator)
                .otherPartyUser(otherParty)
                .title(title)
                .description(description)
                .status(DisputeStatus.WAITING_FOR_SELLER)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        log.info("Dispute initiated by user {} for order {}", userId, orderId);

        notificationService.createNotification(initiator, "Dispute Initiated",
                "You've initiated a dispute for order #" + orderId,
                NotificationType.SYSTEM_ALERT, saved.getId());

        notificationService.createNotification(otherParty, "Dispute Against You",
                "A dispute has been initiated against you for order #" + orderId,
                NotificationType.SYSTEM_ALERT, saved.getId());

        emailService.sendDisputeInitiatedEmail(initiator.getEmail(), initiator.getFullName(), orderId);
        emailService.sendDisputeInitiatedEmail(otherParty.getEmail(), otherParty.getFullName(), orderId);

        return toResponseDTO(saved);
    }

    @Transactional
    public void uploadEvidence(Long disputeId, Long userId, MultipartFile file, String description) throws IOException {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!dispute.getInitiatedByUser().getId().equals(userId) && !dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You're not part of this dispute");
        }

        String fileName = "dispute_" + disputeId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String filePath = fileStorageService.save(file, "dispute_evidence", fileName);
        if (filePath == null) {
            throw new RuntimeException("Failed to save evidence file");
        }

        DisputeEvidence evidence = DisputeEvidence.builder()
                .dispute(dispute)
                .uploadedByUser(user)
                .description(description)
                .filePath(filePath)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .build();

        evidenceRepository.save(evidence);
        log.info("Evidence uploaded for dispute {} by user {}", disputeId, userId);

        if (dispute.getStatus() == DisputeStatus.OPENED || dispute.getStatus() == DisputeStatus.WAITING_FOR_SELLER) {
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);
            disputeRepository.save(dispute);
        }
    }

    @Transactional
    public DisputeResponseDTO resolveDispute(Long disputeId, String resolution, String adminNotes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setAdminNotes(adminNotes);
        dispute.setResolvedAt(LocalDateTime.now());

        Dispute updated = disputeRepository.save(dispute);
        log.info("Dispute {} resolved", disputeId);

        notificationService.createNotification(dispute.getInitiatedByUser(), "Dispute Resolved",
                "Your dispute for order #" + dispute.getOrder().getId() + " has been resolved. Resolution: " + resolution,
                NotificationType.SYSTEM_ALERT, disputeId);

        notificationService.createNotification(dispute.getOtherPartyUser(), "Dispute Resolved",
                "The dispute for order #" + dispute.getOrder().getId() + " has been resolved. Resolution: " + resolution,
                NotificationType.SYSTEM_ALERT, disputeId);

        emailService.sendDisputeResolvedEmail(dispute.getInitiatedByUser().getEmail(), dispute.getInitiatedByUser().getFullName(), resolution);
        emailService.sendDisputeResolvedEmail(dispute.getOtherPartyUser().getEmail(), dispute.getOtherPartyUser().getFullName(), resolution);

        return toResponseDTO(updated);
    }

    public List<DisputeResponseDTO> getUserDisputes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return disputeRepository.findByInitiatedByUserOrOtherPartyUserOrderByCreatedAtDesc(user, user)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<DisputeResponseDTO> getPendingDisputes() {
        return disputeRepository.findByStatus(DisputeStatus.UNDER_REVIEW)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public DisputeResponseDTO getDispute(Long disputeId, User actor) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (actor.getRole() != Role.ADMIN
                && !dispute.getInitiatedByUser().getId().equals(actor.getId())
                && !dispute.getOtherPartyUser().getId().equals(actor.getId())) {
            throw new AuthorizationException("You do not have permission to view this dispute");
        }
        return toResponseDTO(dispute);
    }

    // ── GAP 2: NEW STATUS TRANSITION METHODS ──────────────────────────────────

    @Transactional
    public DisputeResponseDTO escalateDispute(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (!dispute.getInitiatedByUser().getId().equals(userId)
                && !dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not a party to this dispute");
        }
        dispute.setStatus(DisputeStatus.ESCALATED);
        Dispute updated = disputeRepository.save(dispute);
        log.info("Dispute {} escalated to admin by user {}", disputeId, userId);
        return toResponseDTO(updated);
    }

    @Transactional
    public DisputeResponseDTO sellerRespond(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (!dispute.getOtherPartyUser().getId().equals(userId)
                && !dispute.getInitiatedByUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not a party to this dispute");
        }
        dispute.setStatus(DisputeStatus.WAITING_FOR_CUSTOMER);
        Dispute updated = disputeRepository.save(dispute);
        notificationService.createNotification(
                dispute.getInitiatedByUser(),
                "Seller Responded to Dispute",
                "The seller has replied to your dispute for order #" + dispute.getOrder().getId() + ". Please review and respond.",
                NotificationType.SYSTEM_ALERT, disputeId);
        return toResponseDTO(updated);
    }

    @Transactional
    public DisputeResponseDTO customerRespond(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (!dispute.getInitiatedByUser().getId().equals(userId)
                && !dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not a party to this dispute");
        }
        dispute.setStatus(DisputeStatus.WAITING_FOR_SELLER);
        Dispute updated = disputeRepository.save(dispute);
        notificationService.createNotification(
                dispute.getOtherPartyUser(),
                "Customer Responded to Dispute",
                "The customer has replied to dispute #" + disputeId + ". Please review and respond.",
                NotificationType.SYSTEM_ALERT, disputeId);
        return toResponseDTO(updated);
    }

    @Transactional
    public DisputeResponseDTO sellerApprove(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (!dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not authorized to approve this dispute");
        }
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution("Seller approved refund request");
        dispute.setResolvedAt(LocalDateTime.now());
        Dispute updated = disputeRepository.save(dispute);
        
        notificationService.createNotification(dispute.getInitiatedByUser(), "Dispute Approved",
                "The seller has approved your dispute/refund request for order #" + dispute.getOrder().getId(),
                NotificationType.SYSTEM_ALERT, disputeId);
        
        return toResponseDTO(updated);
    }

    @Transactional
    public DisputeResponseDTO sellerDecline(Long disputeId, Long userId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        if (!dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new AuthorizationException("You are not authorized to decline this dispute");
        }
        dispute.setStatus(DisputeStatus.CANCELLED);
        dispute.setResolution("Seller declined dispute/refund request");
        dispute.setResolvedAt(LocalDateTime.now());
        Dispute updated = disputeRepository.save(dispute);
        
        notificationService.createNotification(dispute.getInitiatedByUser(), "Dispute Declined",
                "The seller has declined your dispute request for order #" + dispute.getOrder().getId(),
                NotificationType.SYSTEM_ALERT, disputeId);
        
        return toResponseDTO(updated);
    }

    /**
     * GAP 3/Automation: Auto-escalate disputes where the seller has not responded within 3 days.
     */
    @Transactional
    public void autoEscalateStaleDisputes() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        List<Dispute> staleDisputes = disputeRepository.findAll().stream()
                .filter(d -> d.getStatus() == DisputeStatus.WAITING_FOR_SELLER)
                .filter(d -> d.getUpdatedAt() != null && d.getUpdatedAt().isBefore(threshold))
                .toList();

        for (Dispute dispute : staleDisputes) {
            dispute.setStatus(DisputeStatus.ESCALATED);
            dispute.setAdminNotes("Auto-escalated by system due to lack of seller response after 3 days.");
            disputeRepository.save(dispute);

            // Notify parties
            notificationService.createNotification(
                    dispute.getInitiatedByUser(),
                    "Dispute Auto-Escalated",
                    "Your dispute #" + dispute.getId() + " has been auto-escalated to admin due to lack of seller response.",
                    NotificationType.SYSTEM_ALERT,
                    dispute.getId()
            );
            if (dispute.getOtherPartyUser() != null) {
                notificationService.createNotification(
                        dispute.getOtherPartyUser(),
                        "Dispute Auto-Escalated",
                        "Dispute #" + dispute.getId() + " has been auto-escalated to admin due to lack of response within 3 days.",
                        NotificationType.SYSTEM_ALERT,
                        dispute.getId()
                );
            }
        }
    }

    // ── PRIVATE HELPERS ────────────────────────────────────────────────────────

    private DisputeResponseDTO toResponseDTO(Dispute dispute) {
        List<String> evidenceFiles = evidenceRepository.findByDispute(dispute)
                .stream()
                .map(DisputeEvidence::getFilePath)
                .toList();

        return DisputeResponseDTO.builder()
                .id(dispute.getId())
                .reportId(dispute.getReportId())
                .orderId(dispute.getOrder().getId())
                .initiatedByUserId(dispute.getInitiatedByUser().getId())
                .initiatedByUserName(dispute.getInitiatedByUser().getFullName())
                .otherPartyUserId(dispute.getOtherPartyUser().getId())
                .otherPartyUserName(dispute.getOtherPartyUser().getFullName())
                .title(dispute.getTitle())
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .resolution(dispute.getResolution())
                .adminNotes(dispute.getAdminNotes())
                .evidenceFiles(evidenceFiles)
                .createdAt(dispute.getCreatedAt())
                .updatedAt(dispute.getUpdatedAt())
                .resolvedAt(dispute.getResolvedAt())
                .build();
    }

    private String generateReportId() {
        LocalDateTime startOfDay = LocalDateTime.now().with(java.time.LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(java.time.LocalTime.MAX);
        Dispute lastDispute = disputeRepository.findTopByCreatedAtBetweenOrderByIdDesc(startOfDay, endOfDay);
        
        int sequence = 1;
        if (lastDispute != null && lastDispute.getReportId() != null) {
            try {
                String[] parts = lastDispute.getReportId().split("-");
                if (parts.length == 3) {
                    sequence = Integer.parseInt(parts[2]) + 1;
                }
            } catch (Exception e) {
                log.warn("Failed to parse last dispute report ID, resetting sequence", e);
            }
        }
        
        String dateStr = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
        return String.format("RPT-%s-%04d", dateStr, sequence);
    }
}
