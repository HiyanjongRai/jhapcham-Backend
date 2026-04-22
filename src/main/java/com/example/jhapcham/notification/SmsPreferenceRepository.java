package com.example.jhapcham.notification;

import com.example.jhapcham.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsPreferenceRepository extends JpaRepository<SmsPreference, Long> {
    Optional<SmsPreference> findByUser(User user);
}
