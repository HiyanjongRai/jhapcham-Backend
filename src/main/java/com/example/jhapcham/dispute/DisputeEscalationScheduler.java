package com.example.jhapcham.dispute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DisputeEscalationScheduler {

    private final DisputeService disputeService;

    /**
     * Runs every hour to check for disputes that need auto-escalation.
     * Example: if a dispute has been WAITING_FOR_SELLER for > 3 days.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void autoEscalateDisputes() {
        log.info("Starting auto-escalation check for stale disputes...");
        try {
            disputeService.autoEscalateStaleDisputes();
            log.info("Auto-escalation check completed.");
        } catch (Exception e) {
            log.error("Error during dispute auto-escalation: ", e);
        }
    }
}
