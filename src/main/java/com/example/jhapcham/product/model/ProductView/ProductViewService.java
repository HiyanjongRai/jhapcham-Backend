package com.example.jhapcham.product.model.ProductView;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductViewService {

    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;

    public List<ProductViewDTO> getViewsByUser(Long userId) {
        return productViewRepository.findByUserIdOrderByViewedAtDesc(userId)
                .stream()
                .map(v -> new ProductViewDTO(
                        v.getProduct().getId(),
                        v.getProduct().getName(),
                        v.getProduct().getCategory(),
                        v.getViewedAt()
                ))
                .collect(Collectors.toList());
    }

    public List<Object[]> getTopViewedProducts() {
        return productViewRepository.findTopViewedProducts();
    }
}
