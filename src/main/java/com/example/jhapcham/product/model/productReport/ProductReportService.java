package com.example.jhapcham.product.model.productReport;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.repository.ProductRepository;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReportService {

    private final ProductReportRepository reportRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    @Transactional
    public ProductReport submit(Long productId, Long reporterId, String reason) throws Exception {
        if (reason == null || reason.isBlank()) throw new Exception("Reason is required");

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));
        User reporter = userRepo.findById(reporterId)
                .orElseThrow(() -> new Exception("User not found"));

        ProductReport report = ProductReport.builder()
                .product(product)
                .reporter(reporter)
                .reason(reason.trim())
                .status(ProductReport.Status.OPEN)
                .build();

        return reportRepo.save(report);
    }

    public List<ProductReport> listByProduct(Long productId) {
        return reportRepo.findByProduct_Id(productId);
    }

    public List<ProductReport> listByStatus(ProductReport.Status status) {
        return status == null ? reportRepo.findAll() : reportRepo.findByStatus(status);
    }

    @Transactional
    public ProductReport updateStatus(Long reportId, ProductReport.Status status, String adminNote) throws Exception {
        ProductReport r = reportRepo.findById(reportId)
                .orElseThrow(() -> new Exception("Report not found"));
        r.setStatus(status);
        if (adminNote != null) r.setAdminNote(adminNote);
        return reportRepo.save(r);
    }
}
