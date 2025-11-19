package com.example.jhapcham.seller.Controller;

import com.example.jhapcham.seller.model.SellerApplication;
import com.example.jhapcham.seller.repository.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller/application")
@RequiredArgsConstructor
public class SellerApplicationStatusController {

    private final SellerApplicationRepository applicationRepo;

    @GetMapping("/{userId}")
    public ResponseEntity<?> getSellerStatus(@PathVariable Long userId) {

        SellerApplication app = applicationRepo.findByUserId(userId)
                .orElse(null);

        if (app == null) {
            return ResponseEntity.ok(
                    new StatusResponse(
                            "No application found",
                            "NONE",
                            null
                    )
            );
        }

        String message = "";

        switch (app.getStatus()) {
            case APPROVED:
                message = "You are accepted";
                break;
            case REJECTED:
                message = "You are rejected";
                break;
            case PENDING:
                message = "Your application is under review";
                break;
        }

        return ResponseEntity.ok(
                new StatusResponse(
                        message,
                        app.getStatus().name(),
                        app.getReviewNote()
                )
        );
    }

    record StatusResponse(String message, String status, String note) {}


}