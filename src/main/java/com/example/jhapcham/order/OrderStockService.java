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
     * Deducts stock for each item in the order
     * and marks the order as stockDeducted = true.
     * Safe to call per-product during order creation.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public void deductStock(Product product, int quantity, String color, String size, String storage) {
        // Reload product with pessimistic lock to prevent concurrent stock modifications
        Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                .orElseThrow(() -> new BusinessValidationException("Product not found: " + product.getId()));

        Integer current = lockedProduct.getStockQuantity();
        if (current == null) current = 0;

        if (current < quantity) {
            log.error("Insufficient stock for product id={} ({}): requested {}, available {}",
                    lockedProduct.getId(), lockedProduct.getName(), quantity, current);
            throw new BusinessValidationException("Not enough stock for " + lockedProduct.getName());
        }

        lockedProduct.setStockQuantity(current - quantity);
        productRepository.saveAndFlush(lockedProduct);
        log.info("Deducted {} units from product {}. New stock: {}", quantity, lockedProduct.getId(), lockedProduct.getStockQuantity());

        // Deduct Variant stock as well if exact features were provided
        if (color != null || size != null || storage != null) {
            java.util.List<ProductVariant> variants = productVariantRepository.findMatchingVariants(lockedProduct, color, size, storage);
            for (ProductVariant v : variants) {
                // Deduct from matched variant (usually just 1 matches perfectly)
                if (v.getStockQuantity() != null && v.getStockQuantity() >= quantity) {
                    v.setStockQuantity(v.getStockQuantity() - quantity);
                    productVariantRepository.save(v);
                    log.info("Deducted {} units from variant {}. New variant stock: {}", quantity, v.getSku(), v.getStockQuantity());
                } else if (v.getStockQuantity() != null) {
                    log.warn("Variant {} does not have enough stock. Required: {}, Available: {}", v.getSku(), quantity, v.getStockQuantity());
                }
            }
        }

        // Trigger inventory alerts if stock has crossed a threshold
        try {
            inventoryAlertService.checkAndCreateAlerts(lockedProduct);
        } catch (Exception e) {
            log.error("Failed to check inventory alerts for product {}: {}", lockedProduct.getId(), e.getMessage());
        }
    }

    /**
     * Marks the order's stockDeducted flag to true.
     * Must be called after all items have been deducted in placeOrder.
     */
    @Transactional
    public void markStockDeducted(Order order) {
        order.setStockDeducted(true);
        orderRepository.save(order);
        log.info("Marked stockDeducted=true for order {}", order.getId());
    }

    /**
     * Restores stock for a cancelled order ONLY if stock was previously deducted.
     * This prevents double-restoration (e.g. eSewa cancel + manual cancel).
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional
    public void restoreStock(Order order) {
        if (!order.isStockDeducted()) {
            log.warn("Skipping stock restore for order {} — stock was never deducted (stockDeducted=false)", order.getId());
            return;
        }

        log.info("Restoring stock for order {}", order.getId());
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                // Reload product with pessimistic lock to prevent concurrent stock modifications
                Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                        .orElseThrow(() -> new BusinessValidationException("Product not found: " + product.getId()));

                Integer current = lockedProduct.getStockQuantity();
                if (current == null) current = 0;

                lockedProduct.setStockQuantity(current + item.getQuantity());
                productRepository.saveAndFlush(lockedProduct);
                log.info("Restored {} units to product {} ({}). New stock: {}",
                        item.getQuantity(), lockedProduct.getId(), lockedProduct.getName(), lockedProduct.getStockQuantity());

                // Restore variant stock
                String color = item.getSelectedColorSnapshot();
                String size = item.getSelectedSizeSnapshot();
                String storage = item.getSelectedStorageSnapshot();
                if (color != null || size != null || storage != null) {
                    java.util.List<ProductVariant> variants = productVariantRepository.findMatchingVariants(lockedProduct, color, size, storage);
                    for (ProductVariant v : variants) {
                        if (v.getStockQuantity() != null) {
                            v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
                            productVariantRepository.save(v);
                            log.info("Restored {} units to variant {}. New variant stock: {}", item.getQuantity(), v.getSku(), v.getStockQuantity());
                        }
                    }
                }
            } else {
                log.warn("Could not restore stock for item {} as product is null", item.getId());
            }
        }

        // Mark as no longer deducted so future cancel calls are no-ops
        order.setStockDeducted(false);
        orderRepository.save(order);
    }

    /**
     * Restores stock for a single item (used for partial refunds)
     */
    @Transactional
    public void restoreItemStock(OrderItem item) {
        log.info("Restoring stock for single item {}", item.getId());
        Product product = item.getProduct();
        if (product != null) {
            Product lockedProduct = productRepository.findByIdForUpdate(product.getId())
                    .orElseThrow(() -> new BusinessValidationException("Product not found: " + product.getId()));

            Integer current = lockedProduct.getStockQuantity();
            if (current == null) current = 0;

            lockedProduct.setStockQuantity(current + item.getQuantity());
            productRepository.saveAndFlush(lockedProduct);

            // Restore variant stock
            String color = item.getSelectedColorSnapshot();
            String size = item.getSelectedSizeSnapshot();
            String storage = item.getSelectedStorageSnapshot();
            if (color != null || size != null || storage != null) {
                java.util.List<ProductVariant> variants = productVariantRepository.findMatchingVariants(lockedProduct, color, size, storage);
                for (ProductVariant v : variants) {
                    if (v.getStockQuantity() != null) {
                        v.setStockQuantity(v.getStockQuantity() + item.getQuantity());
                        productVariantRepository.save(v);
                    }
                }
            }
        }
    }
}
