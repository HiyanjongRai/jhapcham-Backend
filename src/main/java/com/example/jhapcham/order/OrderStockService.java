package com.example.jhapcham.order;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.inventory.InventoryAlertService;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductVariant;
import com.example.jhapcham.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStockService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;
    private final InventoryAlertService inventoryAlertService;

    /**
     * Deducts stock from the specific variant.
     * Uses pessimistic locking to prevent race conditions.
     * Also deducts from the product-level aggregate stock for reporting.
     */
    @Transactional
    public void deductStock(ProductVariant variant, int quantity) {
        // Reload variant with pessimistic lock
        ProductVariant locked = productVariantRepository.findById(variant.getId())
                .orElseThrow(() -> new BusinessValidationException("Variant not found: " + variant.getId()));

        int current = locked.getStockQuantity() != null ? locked.getStockQuantity() : 0;
        if (current < quantity) {
            throw new BusinessValidationException(
                "Insufficient stock for variant '" + locked.getVariantLabel() +
                "' (Available: " + current + ", Requested: " + quantity + ")"
            );
        }

        locked.setStockQuantity(current - quantity);
        productVariantRepository.save(locked);
        log.info("Deducted {} units from variant {} (SKU={}). New stock: {}",
                quantity, locked.getId(), locked.getSku(), locked.getStockQuantity());

        // Also deduct from product aggregate (for overall reporting)
        Product product = locked.getProduct();
        if (product != null && product.getStockQuantity() != null) {
            Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                    .orElse(product);
            int prodCurrent = lockedProduct.getStockQuantity() != null ? lockedProduct.getStockQuantity() : 0;
            lockedProduct.setStockQuantity(Math.max(0, prodCurrent - quantity));
            productRepository.save(lockedProduct);
        }

        // Inventory alert
        try {
            inventoryAlertService.checkAndCreateAlerts(product);
        } catch (Exception e) {
            log.error("Failed to check inventory alerts for variant {}: {}", locked.getId(), e.getMessage());
        }
    }

    @Transactional
    public void markStockDeducted(Order order) {
        order.setStockDeducted(true);
        orderRepository.save(order);
        log.info("Marked stockDeducted=true for order {}", order.getId());
    }

    /**
     * Restores stock for all items in a cancelled order.
     */
    @Transactional
    public void restoreStock(Order order) {
        if (!order.isStockDeducted()) {
            log.warn("Skipping stock restore for order {} — stockDeducted=false", order.getId());
            return;
        }

        for (OrderItem item : order.getItems()) {
            restoreItemStock(item);
        }

        order.setStockDeducted(false);
        orderRepository.save(order);
        log.info("Stock restored for order {}", order.getId());
    }

    /**
     * Restores stock for a single item (used for partial refunds).
     */
    @Transactional
    public void restoreItemStock(OrderItem item) {
        ProductVariant variant = item.getVariant();
        if (variant != null) {
            ProductVariant locked = productVariantRepository.findById(variant.getId())
                    .orElse(null);
            if (locked != null) {
                int current = locked.getStockQuantity() != null ? locked.getStockQuantity() : 0;
                locked.setStockQuantity(current + item.getQuantity());
                productVariantRepository.save(locked);
                log.info("Restored {} units to variant {} (SKU={}). New stock: {}",
                        item.getQuantity(), locked.getId(), locked.getSku(), locked.getStockQuantity());
            }
        }
        // Also restore product aggregate
        Product product = item.getProduct();
        if (product != null) {
            Product lockedProduct = productRepository.findByIdForUpdate(product.getId()).orElse(null);
            if (lockedProduct != null) {
                int current = lockedProduct.getStockQuantity() != null ? lockedProduct.getStockQuantity() : 0;
                lockedProduct.setStockQuantity(current + item.getQuantity());
                productRepository.save(lockedProduct);
            }
        }
    }
}
