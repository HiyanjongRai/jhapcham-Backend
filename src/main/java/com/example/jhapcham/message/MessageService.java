package com.example.jhapcham.message;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;  // inject product repo

    public MessageResponseDTO sendMessage(MessageRequestDTO dto, String type) {

        User sender = userRepo.findById(dto.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        User receiver = userRepo.findById(dto.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Message msg = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .productId(dto.getProductId())
                .content(dto.getContent())
                .messageType(type)
                .sentAt(LocalDateTime.now())
                .build();

        Message saved = messageRepo.save(msg);

        return mapToDTO(saved);
    }

    public List<MessageResponseDTO> getConversation(Long user1, Long user2) {
        List<Message> messages = messageRepo.findBySenderIdAndReceiverIdOrReceiverIdAndSenderId(
                user1, user2, user1, user2
        );

        return messages.stream().map(this::mapToDTO).toList();
    }

    public List<MessageResponseDTO> getMessagesForReceiver(Long receiverId) {
        List<Message> messages = messageRepo.findByReceiverId(receiverId);
        return messages.stream().map(this::mapToDTO).toList();
    }

    private MessageResponseDTO mapToDTO(Message msg) {
        String productName = null;
        String productImage = null;

        if (msg.getProductId() != null) {
            Product product = productRepo.findById(msg.getProductId()).orElse(null);
            if (product != null) {
                productName = product.getName();
                productImage = product.getImagePath();
            }
        }

        return MessageResponseDTO.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderUsername(msg.getSender().getUsername())
                .receiverId(msg.getReceiver().getId())
                .receiverUsername(msg.getReceiver().getUsername())
                .productId(msg.getProductId())
                .productName(productName)        // added
                .productImage(productImage)      // added
                .messageType(msg.getMessageType())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .build();
    }
}
