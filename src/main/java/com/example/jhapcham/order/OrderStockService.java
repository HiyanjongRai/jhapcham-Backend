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

    @Transactional
    public void deductStock(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            log.error("Insufficient stock for product {}: requested {}, available {}",
                    product.getId(), quantity, product.getStockQuantity());
            throw new BusinessValidationException("Not enough stock for " + product.getName());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        log.info("Deducted {} units from product {}", quantity, product.getId());
    }

    @Transactional
    public void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                log.info("Restored {} units to product {} for cancelled order {}",
                        item.getQuantity(), product.getId(), order.getId());
            }
        }
    }
}
