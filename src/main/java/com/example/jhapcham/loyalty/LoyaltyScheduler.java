package com.example.jhapcham.loyalty;

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
