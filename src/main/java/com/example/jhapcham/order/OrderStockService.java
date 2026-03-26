package com.example.jhapcham.order;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStockService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    /**
     * Deducts stock for each item in the order
     * and marks the order as stockDeducted = true.
     * Safe to call per-product during order creation.
     */
    @Transactional
    public void deductStock(Product product, int quantity) {
        Integer current = product.getStockQuantity();
        if (current == null) current = 0;

        if (current < quantity) {
            log.error("Insufficient stock for product id={} ({}): requested {}, available {}",
                    product.getId(), product.getName(), quantity, current);
            throw new BusinessValidationException("Not enough stock for " + product.getName());
        }

        product.setStockQuantity(current - quantity);
        productRepository.saveAndFlush(product);
        log.info("Deducted {} units from product {}. New stock: {}", quantity, product.getId(), product.getStockQuantity());
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
                Integer current = product.getStockQuantity();
                if (current == null) current = 0;

                product.setStockQuantity(current + item.getQuantity());
                productRepository.saveAndFlush(product);
                log.info("Restored {} units to product {} ({}). New stock: {}",
                        item.getQuantity(), product.getId(), product.getName(), product.getStockQuantity());
            } else {
                log.warn("Could not restore stock for item {} as product is null", item.getId());
            }
        }

        // Mark as no longer deducted so future cancel calls are no-ops
        order.setStockDeducted(false);
        orderRepository.save(order);
    }
}
