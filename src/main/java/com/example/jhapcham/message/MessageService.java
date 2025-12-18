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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElse(null);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .product(product)
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);
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

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getUsername()); // Or FullName
        dto.setSenderProfileImage(message.getSender().getProfileImagePath());

        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverName(message.getReceiver().getUsername());

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
                dto.setProductImage(message.getProduct().getImages().get(0).getImagePath());
            }
        }

        dto.setCreatedAt(message.getCreatedAt());
        dto.setRead(message.isRead());
        return dto;
    }
}
