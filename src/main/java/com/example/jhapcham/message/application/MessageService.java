package com.example.jhapcham.message.application;


import com.example.jhapcham.message.application.*;
import com.example.jhapcham.message.domain.*;
import com.example.jhapcham.message.dto.*;
import com.example.jhapcham.message.persistence.*;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.domain.ProductImage;
import com.example.jhapcham.product.persistence.ProductRepository;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import com.example.jhapcham.user.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final com.example.jhapcham.seller.persistence.SellerProfileRepository sellerProfileRepository;
    private final com.example.jhapcham.notification.application.NotificationService notificationService;

    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(Objects.requireNonNull(senderId, "Sender ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository
                .findById(Objects.requireNonNull(request.getReceiverId(), "Receiver ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId()).orElse(null);
        }

        validateConversationPolicy(sender, receiver, product);

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .product(product)
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);

        String senderName = sender.getFullName() != null && !sender.getFullName().isBlank()
                ? sender.getFullName()
                : sender.getUsername();
        if (senderName == null) senderName = "A user";

        notificationService.createNotification(
            receiver,
            "New Message",
            "You have a new message from " + senderName + ".",
            com.example.jhapcham.notification.domain.NotificationType.MESSAGE_RECEIVED,
            sender.getId()
        );

        return convertToDTO(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getMessagesForUser(Long userId) {
        List<Message> messages = messageRepository.findInboxWithDetails(userId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getSentMessages(Long userId) {
        List<Message> messages = messageRepository.findSentWithDetails(userId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getConversation(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findConversationWithDetails(userId1, userId2);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ── GAP 1: Case thread fetchers ─────────────────────────────────────────────

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long receiverId, Long senderId) {
        messageRepository.markConversationAsRead(receiverId, senderId);
    }

    private void validateConversationPolicy(User sender, User receiver, Product product) {
        if (sender.getId().equals(receiver.getId())) {
            throw new RuntimeException("Cannot send a message to yourself");
        }
        if (sender.getRole() == Role.ADMIN || receiver.getRole() == Role.ADMIN) {
            return;
        }
        if ((sender.getRole() == Role.CUSTOMER && receiver.getRole() == Role.SELLER)
                || (sender.getRole() == Role.SELLER && receiver.getRole() == Role.CUSTOMER)) {
            if (product != null) {
                Long productSellerUserId = product.getSellerProfile() != null
                        && product.getSellerProfile().getUser() != null
                        ? product.getSellerProfile().getUser().getId()
                        : null;
                Long sellerUserId = sender.getRole() == Role.SELLER ? sender.getId() : receiver.getId();
                if (!Objects.equals(productSellerUserId, sellerUserId)) {
                    throw new RuntimeException("Messages for this product must include the owning seller");
                }
            }
            return;
        }
        throw new RuntimeException("This message route is not allowed");
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());

        User sender = message.getSender();
        User receiver = message.getReceiver();

        dto.setSenderId(sender.getId());
        dto.setSenderRole(sender.getRole().name());

        var senderSeller = sellerProfileRepository.findByUser(sender);
        if (senderSeller.isPresent()) {
            dto.setSenderName(senderSeller.get().getStoreName());
            String logo = senderSeller.get().getLogoImagePath();
            dto.setSenderProfileImage(logo != null ? logo : sender.getProfileImagePath());
        } else if (sender.getRole() == Role.ADMIN) {
            dto.setSenderName("Jhapcham Official Admin");
            dto.setSenderProfileImage(sender.getProfileImagePath());
        } else {
            dto.setSenderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername());
            dto.setSenderProfileImage(sender.getProfileImagePath());
        }

        dto.setReceiverId(receiver.getId());
        var receiverSeller = sellerProfileRepository.findByUser(receiver);
        if (receiverSeller.isPresent()) {
            dto.setReceiverName(receiverSeller.get().getStoreName());
            String logo = receiverSeller.get().getLogoImagePath();
            dto.setReceiverProfileImage(logo != null ? logo : receiver.getProfileImagePath());
        } else {
            dto.setReceiverName(receiver.getFullName() != null ? receiver.getFullName() : receiver.getUsername());
            dto.setReceiverProfileImage(receiver.getProfileImagePath());
        }

        dto.setContent(message.getContent());

        if (message.getProduct() != null) {
            dto.setProductId(message.getProduct().getId());
            dto.setProductName(message.getProduct().getName());
            Optional<ProductImage> mainImage = message.getProduct().getImages().stream()
                    .filter(ProductImage::isMainImage)
                    .findFirst();
            if (mainImage.isPresent()) {
                dto.setProductImage(mainImage.get().getImagePath());
            } else if (!message.getProduct().getImages().isEmpty()) {
                dto.setProductImage(message.getProduct().getImages().get(0).getImagePath());
            }
        }

        dto.setCreatedAt(message.getCreatedAt());
        dto.setRead(message.isRead());
        return dto;
    }
}
