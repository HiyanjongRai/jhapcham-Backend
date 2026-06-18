package com.example.jhapcham.loyalty.application;


import com.example.jhapcham.loyalty.application.*;
import com.example.jhapcham.loyalty.domain.*;
import com.example.jhapcham.loyalty.dto.*;
import com.example.jhapcham.loyalty.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoyaltyScheduler {
    private final LoyaltyService loyaltyService;

    @Scheduled(cron = "0 15 2 * * *")
    public void expirePoints() {
        loyaltyService.processExpiry();
    }

    @Scheduled(cron = "0 30 9 * * *")
    public void notifyExpiringSoon() {
        loyaltyService.notifyExpiringSoon();
    }
}
