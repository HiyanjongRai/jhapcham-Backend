package com.example.jhapcham.inventory;

import com.example.jhapcham.Error.ResourceNotFoundException;
import com.example.jhapcham.notification.EmailService;
import com.example.jhapcham.notification.NotificationService;
import com.example.jhapcham.notification.NotificationType;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.seller.SellerProfile;
import com.example.jhapcham.seller.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAlertService {

    private final InventoryAlertRepository alertRepository;
    private final SellerProfileRepository sellerRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    private static final Integer LOW_STOCK_THRESHOLD = 10;
    private static final Integer RESTOCK_THRESHOLD = 5;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndCreateAlerts(Product product) {
        SellerProfile seller = product.getSellerProfile();

        int currentStock = product.getStockQuantity();

        // Check for out of stock
        if (currentStock == 0) {
            createAlert(product, seller, InventoryAlertType.OUT_OF_STOCK, currentStock,
                    "Product is out of stock! Order #" + product.getId());
        }
        // Check for restock reminder
        else if (currentStock <= RESTOCK_THRESHOLD) {
            createAlert(product, seller, InventoryAlertType.RESTOCK_REMINDER, currentStock,
                    "Only " + currentStock + " items left. Time to reorder!");
        }
        // Check for low stock
        else if (currentStock <= LOW_STOCK_THRESHOLD) {
            createAlert(product, seller, InventoryAlertType.LOW_STOCK, currentStock,
                    "Stock is running low. Only " + currentStock + " items available.");
        }
    }

    private void createAlert(Product product, SellerProfile seller, InventoryAlertType type,
                             Integer currentStock, String message) {
        // Don't create duplicate alerts
        alertRepository.findByProductAndAlertType(product, type)
                .ifPresent(alert -> {
                    if (!alert.getAcknowledged()) {
                        return;  // Alert already exists and not acknowledged
                    }
                    alertRepository.delete(alert);  // Remove old acknowledged alert
                });

        InventoryAlert alert = InventoryAlert.builder()
                .product(product)
                .seller(seller)
                .alertType(type)
                .currentStock(currentStock)
                .thresholdStock(type == InventoryAlertType.OUT_OF_STOCK ? 0 :
                        type == InventoryAlertType.RESTOCK_REMINDER ? RESTOCK_THRESHOLD : LOW_STOCK_THRESHOLD)
                .message(message)
                .acknowledged(false)
                .build();

        InventoryAlert saved = alertRepository.save(alert);
        log.info("Inventory alert created: {} for product {}", type, product.getId());

        // Send notification to seller
        notificationService.createNotification(seller.getUser(), "Inventory Alert: " + type.getDescription(),
                message + " - Product: " + product.getName(),
                NotificationType.SYSTEM_ALERT, saved.getId());

        // Send email to seller
        emailService.sendInventoryAlertEmail(seller.getUser().getEmail(), seller.getUser().getFullName(),
                product.getName(), type.getDescription(), currentStock);
    }

    @Transactional
    public void acknowledgeAlert(Long alertId, Long sellerId) {
        InventoryAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        if (!alert.getSeller().getUser().getId().equals(sellerId)) {
            throw new RuntimeException("Unauthorized: You can't acknowledge this alert");
        }

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.save(alert);
        log.info("Inventory alert {} acknowledged", alertId);
    }

    public List<InventoryAlertDTO> getSellerAlerts(Long sellerUserId) {
        SellerProfile seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        return alertRepository.findBySellerOrderByCreatedAtDesc(seller)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<InventoryAlertDTO> getUnacknowledgedAlerts(Long sellerUserId) {
        SellerProfile seller = sellerRepository.findByUserId(sellerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        return alertRepository.findBySellerAndAcknowledgedFalse(seller)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public InventoryAlertDTO getAlert(Long alertId) {
        InventoryAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));
        return toDTO(alert);
    }

    private InventoryAlertDTO toDTO(InventoryAlert alert) {
        return InventoryAlertDTO.builder()
                .id(alert.getId())
                .productId(alert.getProduct().getId())
                .productName(alert.getProduct().getName())
                .alertType(alert.getAlertType())
                .currentStock(alert.getCurrentStock())
                .thresholdStock(alert.getThresholdStock())
                .message(alert.getMessage())
                .acknowledged(alert.getAcknowledged())
                .createdAt(alert.getCreatedAt())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .build();
    }
}
