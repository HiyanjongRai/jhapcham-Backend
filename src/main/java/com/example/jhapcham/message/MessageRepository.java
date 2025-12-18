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

    // Find conversation between two users
    @Query("SELECT m FROM Message m WHERE (m.sender.id = :user1 AND m.receiver.id = :user2) OR (m.sender.id = :user2 AND m.receiver.id = :user1) ORDER BY m.createdAt DESC")
    List<Message> findConversation(@Param("user1") Long user1, @Param("user2") Long user2);
}
