package com.example.jhapcham.dispute;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;
import com.example.jhapcham.product.Product;
import com.example.jhapcham.product.ProductImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@org.springframework.core.annotation.Order(20) // Run after other seeders/initializers
@RequiredArgsConstructor
@Slf4j
public class DisputeSeeder implements CommandLineRunner {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DisputeEvidenceRepository disputeEvidenceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("DisputeSeeder disabled to preserve real user reports and dynamic data.");
    }
}
