package com.example.jhapcham.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@org.springframework.context.annotation.Profile("delivery-simulation")
@RequiredArgsConstructor
@Slf4j
public class DeliverySimulationScheduler {

    private final ShipmentService shipmentService;

    @Scheduled(fixedDelayString = "${delivery.simulation.fixed-delay-ms:60000}")
    public void advanceShipments() {
        try {
            shipmentService.autoAdvanceShipments();
        } catch (Exception ex) {
            log.warn("Delivery simulation scheduler skipped this cycle: {}", ex.getMessage());
        }
    }
}
