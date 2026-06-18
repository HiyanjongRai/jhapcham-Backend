package com.example.jhapcham.delivery.application;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.order.application.OrderAccountingService;
import com.example.jhapcham.order.persistence.OrderRepository;
import com.example.jhapcham.order.application.OrderStockService;
import com.example.jhapcham.order.domain.PaymentMethod;
import com.example.jhapcham.order.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourierSettlementService {

    private final CourierSettlementRepository settlementRepository;
    private final CourierRepository courierRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final OrderAccountingService orderAccountingService;
    private final OrderStockService orderStockService;

    // ============================================================
    // COD LEDGER: Record cash collected on a COD delivery
    // Called by TrackingService when status == DELIVERED and COD
    // ============================================================
    @Transactional
    public void recordCodCollection(Shipment shipment) {
        if (!shipment.isCashOnDelivery()) {
            return; // Prepaid orders do not go through this flow
        }
        if (shipment.getCourier() == null) {
            log.warn("Cannot record COD collection: shipment {} has no assigned courier", shipment.getTrackingId());
            return;
        }

        // Prevent duplicate settlement records (idempotency guard)
        if (settlementRepository.findByTrackingId(shipment.getTrackingId()).isPresent()) {
            log.warn("COD settlement already recorded for trackingId={}, skipping duplicate", shipment.getTrackingId());
            return;
        }

        BigDecimal amount = shipment.getCodAmount() != null ? shipment.getCodAmount() : BigDecimal.ZERO;

        CourierSettlement settlement = CourierSettlement.builder()
                .courier(shipment.getCourier())
                .trackingId(shipment.getTrackingId())
                .collectedAmount(amount)
                .remittedToHub(false)
                .build();

        settlementRepository.save(settlement);
        log.info("COD collection recorded: Courier {} holds Rs.{} for shipment {}",
                shipment.getCourier().getFullName(), amount, shipment.getTrackingId());
    }

    // ============================================================
    // ADMIN: Mark a single shipment's COD cash as remitted
    // ============================================================
    @Transactional
    public CourierSettlementDTO.SettlementView markRemitted(Long settlementId, CourierSettlementDTO.RemittanceRequest request) {
        CourierSettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement record not found: " + settlementId));

        if (settlement.isRemittedToHub()) {
            throw new BusinessValidationException("This settlement has already been remitted.");
        }

        settlement.setRemittedToHub(true);
        settlement.setRemittedAt(LocalDateTime.now());
        settlement.setRemittanceNote(request != null ? request.getRemittanceNote() : null);
        settlementRepository.save(settlement);

        // Now that cash is physically received, credit the seller's payout
        Shipment shipment = shipmentRepository.findByTrackingId(settlement.getTrackingId())
                .orElse(null);
        if (shipment != null && shipment.getOrder() != null) {
            Order order = shipment.getOrder();
            order.setPaymentStatus(PaymentStatus.COD_REMITTED);
            if (order.getPaidAt() == null) {
                order.setPaidAt(LocalDateTime.now());
            }
            orderAccountingService.finalizeSellerAccounting(order);
            orderRepository.save(order);
            log.info("Seller accounting finalized for order {} after COD remittance", order.getId());
        }

        log.info("Settlement {} marked as remitted. Rs.{} received at hub.", settlementId, settlement.getCollectedAmount());
        return toView(settlement);
    }

    // ============================================================
    // ADMIN: Bulk remit all pending cash from a courier in one shift
    // ============================================================
    @Transactional
    public int bulkRemitCourier(Long courierId, CourierSettlementDTO.RemittanceRequest request) {
        List<CourierSettlement> pending = settlementRepository.findByCourier_IdAndRemittedToHubFalse(courierId);
        if (pending.isEmpty()) {
            throw new BusinessValidationException("No pending COD cash for this courier.");
        }

        for (CourierSettlement s : pending) {
            s.setRemittedToHub(true);
            s.setRemittedAt(LocalDateTime.now());
            s.setRemittanceNote(request != null ? request.getRemittanceNote() : "Bulk remittance");

            Shipment shipment = shipmentRepository.findByTrackingId(s.getTrackingId()).orElse(null);
            if (shipment != null && shipment.getOrder() != null) {
                Order order = shipment.getOrder();
                order.setPaymentStatus(PaymentStatus.COD_REMITTED);
                if (order.getPaidAt() == null) {
                    order.setPaidAt(LocalDateTime.now());
                }
                orderAccountingService.finalizeSellerAccounting(order);
                orderRepository.save(order);
            }
        }

        settlementRepository.saveAll(pending);
        log.info("Bulk remittance completed for courier {}: {} settlements processed.", courierId, pending.size());
        return pending.size();
    }

    // ============================================================
    // REVERSE LOGISTICS: Process a returned shipment
    // Called by TrackingService when status == RETURN_TO_SELLER
    // ============================================================
    @Transactional
    public void processReturn(Shipment shipment) {
        Order order = shipment.getOrder();
        if (order == null) {
            log.error("Return processing failed: shipment {} has no associated order", shipment.getTrackingId());
            return;
        }

        // Step 1: Restore inventory stock
        try {
            orderStockService.restoreStock(order);
            log.info("Stock restored for returned order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to restore stock for order {}: {}", order.getId(), e.getMessage());
        }

        // Step 2: Cancel accounting — zero out commissions
        try {
            orderAccountingService.cancelAccounting(order);
            log.info("Accounting cancelled for returned order {}", order.getId());
        } catch (Exception e) {
            log.error("Failed to cancel accounting for order {}: {}", order.getId(), e.getMessage());
        }

        // Step 3: If prepaid and paid, flag for customer refund
        if (order.getPaymentMethod() != PaymentMethod.COD
                && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setRefundPending(true);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.info("Order {} flagged for customer refund (prepaid return).", order.getId());
        } else {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
        }

        orderRepository.save(order);
        log.info("Return fully processed for order {}, shipment {}", order.getId(), shipment.getTrackingId());
    }

    // ============================================================
    // QUERY: Get pending cash balance for a specific courier
    // ============================================================
    @Transactional(readOnly = true)
    public CourierSettlementDTO.CourierCashSummary getCourierCashSummary(Long courierId) {
        Courier courier = courierRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found: " + courierId));

        BigDecimal pendingCash = settlementRepository.sumPendingCashByCourierId(courierId);
        List<CourierSettlement> pendingList = settlementRepository.findByCourier_IdAndRemittedToHubFalse(courierId);

        return CourierSettlementDTO.CourierCashSummary.builder()
                .courierId(courierId)
                .courierName(courier.getFullName())
                .totalPendingCash(pendingCash)
                .pendingDeliveryCount(pendingList.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourierSettlementDTO.SettlementView> getSettlementsForCourier(Long courierId) {
        return settlementRepository.findByCourier_IdOrderByCreatedAtDesc(courierId)
                .stream().map(this::toView).collect(Collectors.toList());
    }

    private CourierSettlementDTO.SettlementView toView(CourierSettlement s) {
        return CourierSettlementDTO.SettlementView.builder()
                .settlementId(s.getId())
                .trackingId(s.getTrackingId())
                .collectedAmount(s.getCollectedAmount())
                .remittedToHub(s.isRemittedToHub())
                .remittedAt(s.getRemittedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
