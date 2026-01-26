package com.example.jhapcham.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserId(Long userId);

    List<UserActivity> findByUserIdAndProductIdAndActivityType(Long userId, Long productId,
            ActivityType activityType);

}
