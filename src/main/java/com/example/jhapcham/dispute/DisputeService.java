package com.example.jhapcham.dispute;

import com.example.jhapcham.Error.AuthorizationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.EmailService;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.notification.NotificationType;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
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
import java.util.ArrayList;
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

        // Find the other party (seller if initiator is buyer, or buyer if initiator is seller)
        User otherParty = order.getItems().get(0).getProduct().getSellerProfile().getUser();
        if (otherParty.getId().equals(userId)) {
            otherParty = order.getUser();
            if (otherParty == null) {
                throw new RuntimeException("Cannot initiate dispute against a guest order (no registered user account)");
            }
        }

        // Check if dispute already exists
        disputeRepository.findByOrder(order)
                .ifPresent(d -> {
                    throw new RuntimeException("Dispute already exists for this order");
                });

        Dispute dispute = Dispute.builder()
                .order(order)
                .initiatedByUser(initiator)
                .otherPartyUser(otherParty)
                .title(title)
                .description(description)
                .status(DisputeStatus.OPENED)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        log.info("Dispute initiated by user {} for order {}", userId, orderId);

        // Notify both parties
        notificationService.createNotification(initiator, "Dispute Initiated",
                "You've initiated a dispute for order #" + orderId,
                NotificationType.SYSTEM_ALERT, saved.getId());

        notificationService.createNotification(otherParty, "Dispute Against You",
                "A dispute has been initiated against you for order #" + orderId,
                NotificationType.SYSTEM_ALERT, saved.getId());

        // Send emails
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

        // Verify user is part of the dispute
        if (!dispute.getInitiatedByUser().getId().equals(userId) && !dispute.getOtherPartyUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You're not part of this dispute");
        }

        String fileName = "dispute_" + disputeId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        // Store file to disk via FileStorageService
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

        // Update dispute status if needed
        if (dispute.getStatus() == DisputeStatus.OPENED) {
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

        // Notify both parties
        notificationService.createNotification(dispute.getInitiatedByUser(), "Dispute Resolved",
                "Your dispute for order #" + dispute.getOrder().getId() + " has been resolved. Resolution: " + resolution,
                NotificationType.SYSTEM_ALERT, disputeId);

        notificationService.createNotification(dispute.getOtherPartyUser(), "Dispute Resolved",
                "The dispute for order #" + dispute.getOrder().getId() + " has been resolved. Resolution: " + resolution,
                NotificationType.SYSTEM_ALERT, disputeId);

        // Send emails
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

    private DisputeResponseDTO toResponseDTO(Dispute dispute) {
        List<String> evidenceFiles = evidenceRepository.findByDispute(dispute)
                .stream()
                .map(DisputeEvidence::getFilePath)
                .toList();

        return DisputeResponseDTO.builder()
                .id(dispute.getId())
                .orderId(dispute.getOrder().getId())
                .initiatedByUserName(dispute.getInitiatedByUser().getFullName())
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
}
