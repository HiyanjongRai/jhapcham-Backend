
package com.example.jhapcham.product;

import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductRepository;
import com.example.jhapcham.product.ProductVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Component
@Profile("debug-products")
@Slf4j
public class DebugProduct {
    @Autowired
    private ProductRepository productRepository;

    @PostConstruct
    public void debug() {
        List<Product> products = productRepository.findAll();
        for (Product p : products) {
            if (p.getName().equalsIgnoreCase("dewd")) {
                List<ProductVariant> variants = p.getVariants();
                log.debug("Product {} has {} variants.", p.getName(), variants.size());
                for (ProductVariant v : variants) {
                    log.debug("Variant SKU: {} Active: {}", v.getSku(), v.getActive());
                }
            }
        }
    }
}
