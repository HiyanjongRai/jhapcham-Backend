package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {
    
    List<SmsLog> findByUser(User user);
    
    List<SmsLog> findByPhoneNumber(String phoneNumber);
    
    List<SmsLog> findBySmsType(SmsType smsType);
    
    List<SmsLog> findByUserAndSent(User user, Boolean sent);
    
    List<SmsLog> findBySentFalse();  // Find unsent messages for retry
}
