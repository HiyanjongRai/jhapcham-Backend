package com.example.jhapcham.message;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductImage;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
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
    private final com.example.jhapcham.seller.SellerProfileRepository sellerProfileRepository;

    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(Objects.requireNonNull(senderId, "Sender ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository
                .findById(Objects.requireNonNull(request.getReceiverId(), "Receiver ID cannot be null"))
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository
                    .findById(Objects.requireNonNull(request.getProductId(), "Product ID cannot be null"))
                    .orElse(null);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .product(product)
                .isRead(false)
                .build();

        Message savedMessage = Objects.requireNonNull(messageRepository.save(message), "Saved message cannot be null");
        return convertToDTO(savedMessage);
    }

    public List<MessageDTO> getMessagesForUser(Long userId) {
        // This gets ALL messages where user is receiver
        List<Message> messages = messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<MessageDTO> getSentMessages(Long userId) {
        List<Message> messages = messageRepository.findBySenderIdOrderByCreatedAtDesc(userId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Get conversation between two users
    public List<MessageDTO> getConversation(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findConversation(userId1, userId2);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long receiverId, Long senderId) {
        messageRepository.markConversationAsRead(receiverId, senderId);
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());

        User sender = message.getSender();
        User receiver = message.getReceiver();

        // Sender Info
        dto.setSenderId(sender.getId());
        var senderSeller = sellerProfileRepository.findByUser(sender);
        if (senderSeller.isPresent()) {
            dto.setSenderName(senderSeller.get().getStoreName());
            String logo = senderSeller.get().getLogoImagePath();
            dto.setSenderProfileImage(logo != null ? logo : sender.getProfileImagePath());
        } else {
            dto.setSenderName(sender.getFullName() != null ? sender.getFullName() : sender.getUsername());
            dto.setSenderProfileImage(sender.getProfileImagePath());
        }

        // Receiver Info
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

            // Find main image
            Optional<ProductImage> mainImage = message.getProduct().getImages().stream()
                    .filter(ProductImage::isMainImage)
                    .findFirst();

            if (mainImage.isPresent()) {
                dto.setProductImage(mainImage.get().getImagePath());
            } else if (!message.getProduct().getImages().isEmpty()) {
                dto.setProductImage(message.getProduct().getImages().stream().findFirst()
                        .map(ProductImage::getImagePath).orElse(null));
            }
        }

        dto.setCreatedAt(message.getCreatedAt());
        dto.setRead(message.isRead());
        return dto;
    }
}
