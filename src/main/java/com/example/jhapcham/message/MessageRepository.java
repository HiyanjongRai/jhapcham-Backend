package com.example.jhapcham.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    List<Message> findBySenderIdOrderByCreatedAtDesc(Long senderId);

    @Query("""
            SELECT DISTINCT m
            FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            LEFT JOIN FETCH m.product p
            LEFT JOIN FETCH p.images
            WHERE m.receiver.id = :receiverId
            ORDER BY m.createdAt DESC
            """)
    List<Message> findInboxWithDetails(@Param("receiverId") Long receiverId);

    @Query("""
            SELECT DISTINCT m
            FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            LEFT JOIN FETCH m.product p
            LEFT JOIN FETCH p.images
            WHERE m.sender.id = :senderId
            ORDER BY m.createdAt DESC
            """)
    List<Message> findSentWithDetails(@Param("senderId") Long senderId);

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :user1 AND m.receiver.id = :user2) OR (m.sender.id = :user2 AND m.receiver.id = :user1) ORDER BY m.createdAt DESC")
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);

    @Query("""
            SELECT DISTINCT m
            FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            LEFT JOIN FETCH m.product p
            LEFT JOIN FETCH p.images
            WHERE (m.sender.id = :user1 AND m.receiver.id = :user2)
               OR (m.sender.id = :user2 AND m.receiver.id = :user1)
            ORDER BY m.createdAt DESC
            """)
    List<Message> findConversationWithDetails(@Param("user1") Long user1, @Param("user2") Long user2);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId AND m.isRead = false")
    void markConversationAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    // ── GAP 1: Case-linked message queries ─────────────────────────────────────

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            WHERE m.dispute.id = :disputeId
            ORDER BY m.createdAt ASC
            """)
    List<Message> findByDisputeIdOrderByCreatedAtAsc(@Param("disputeId") Long disputeId);

    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            JOIN FETCH m.receiver
            WHERE m.refundRequest.id = :refundRequestId
            ORDER BY m.createdAt ASC
            """)
    List<Message> findByRefundRequestIdOrderByCreatedAtAsc(@Param("refundRequestId") Long refundRequestId);
}
