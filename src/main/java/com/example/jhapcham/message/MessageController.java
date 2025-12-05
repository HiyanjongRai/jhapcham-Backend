package com.example.jhapcham.message;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // Send product enquiry
    @PostMapping("/product")
    public ResponseEntity<MessageResponseDTO> sendProductEnquiry(@RequestBody MessageRequestDTO dto) {
        return ResponseEntity.ok(messageService.sendMessage(dto, "PRODUCT_ENQUIRY"));
    }

    // Send store message
    @PostMapping("/store")
    public ResponseEntity<MessageResponseDTO> sendStoreMessage(@RequestBody MessageRequestDTO dto) {
        return ResponseEntity.ok(messageService.sendMessage(dto, "STORE_MESSAGE"));
    }

    // Send chat reply
    @PostMapping("/reply")
    public ResponseEntity<MessageResponseDTO> replyMessage(@RequestBody MessageRequestDTO dto) {
        return ResponseEntity.ok(messageService.sendMessage(dto, "CHAT_REPLY"));
    }

    // Get conversation between two users
    @GetMapping("/chat/{user1}/{user2}")
    public ResponseEntity<List<MessageResponseDTO>> getConversation(@PathVariable Long user1, @PathVariable Long user2) {
        return ResponseEntity.ok(messageService.getConversation(user1, user2));
    }

    // Get all messages for a specific receiver
    @GetMapping("/receiver/{receiverId}")
    public ResponseEntity<List<MessageResponseDTO>> getMessagesForReceiver(@PathVariable Long receiverId) {
        return ResponseEntity.ok(messageService.getMessagesForReceiver(receiverId));
    }
}
