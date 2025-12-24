package com.example.jhapcham.report;

import com.example.jhapcham.Error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.UserRepository;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

        private final ReportService reportService;
        private final UserRepository userRepository;

        @PostMapping
        public ResponseEntity<?> createReport(
                        @RequestBody ReportCreateRequest request,
                        @AuthenticationPrincipal UserDetails userDetails) {
                try {
                        if (userDetails == null) {
                                return ResponseEntity.status(401).body(new ErrorResponse("Please log in to report"));
                        }

                        User user = userRepository.findByEmail(userDetails.getUsername())
                                        .orElseThrow(() -> new RuntimeException("Logged in user not found"));

                        if (request.getType() == null || request.getReportedEntityId() == null) {
                                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid report data"));
                        }

                        return ResponseEntity.ok(reportService.createReport(user.getId(), request));
                } catch (Exception e) {
                        return ResponseEntity.status(500).body(new ErrorResponse(e.getMessage()));
                }
        }

        @GetMapping("/seller/me")
        public ResponseEntity<?> getSellerReports(@AuthenticationPrincipal UserDetails userDetails) {
                if (userDetails == null) {
                        return ResponseEntity.status(401).body(new ErrorResponse("Unauthorized"));
                }
                User user = userRepository.findByEmail(userDetails.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return ResponseEntity.ok(reportService.getReportsForSeller(user.getId()));
        }
}
