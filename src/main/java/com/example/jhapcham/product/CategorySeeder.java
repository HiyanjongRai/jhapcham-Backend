package com.example.jhapcham.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategorySeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> categoryNames = Arrays.asList(
                "Electronics & Gadgets",
                "Fashion & Apparel",
                "Footwear",
                "Accessories",
                "Computers & Gaming",
                "Home & Living",
                "Beauty & Personal Care",
                "Sports & Fitness",
                "Bags & Travel",
                "Books & Stationery",
                "Toys & Kids",
                "Automotive",
                "Groceries & Essentials",
                "Health & Wellness",
                "Jewelry & Luxury Items"
        );

        for (String categoryName : categoryNames) {
            if (!categoryRepository.existsByName(categoryName)) {
                Category category = Category.builder()
                        .name(categoryName)
                        .description("Default category: " + categoryName)
                        .build();
                categoryRepository.save(category);
                log.info("Seeded category: {}", categoryName);
            }
        }
    }
}
