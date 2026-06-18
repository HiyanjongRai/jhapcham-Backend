package com.example.jhapcham.notification.persistence;


import com.example.jhapcham.notification.application.*;
import com.example.jhapcham.notification.domain.*;
import com.example.jhapcham.notification.dto.*;
import com.example.jhapcham.notification.persistence.*;
import com.example.jhapcham.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmsPreferenceRepository extends JpaRepository<SmsPreference, Long> {
    Optional<SmsPreference> findByUser(User user);
}
