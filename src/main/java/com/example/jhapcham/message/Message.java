package com.example.jhapcham.message;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sender (customer or seller)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Receiver (customer or seller)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // Optional – only for product enquiries
    @Column(nullable = true)
    private Long productId;

    @Column(nullable = false)
    private String messageType; // PRODUCT_ENQUIRY, STORE_MESSAGE, CHAT_REPLY

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime sentAt;
}
