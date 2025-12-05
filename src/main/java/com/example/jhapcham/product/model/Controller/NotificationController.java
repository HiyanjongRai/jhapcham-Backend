package com.example.jhapcham.product.model.Controller;

import com.example.jhapcham.product.model.dto.ProductResponseDTO;
import com.example.jhapcham.product.model.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ProductService productService;

    @GetMapping("/expiring-products")
    public List<ProductResponseDTO> getExpiringProducts() {
        return productService.getExpiringProductsNextTwoWeeks()
                .stream()
                .map(productService::toResponseDTO)
                .toList();
    }
}
