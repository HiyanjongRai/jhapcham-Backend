package com.example.jhapcham.admin.api;

import com.example.jhapcham.admin.application.DashboardStatisticsService;
import com.example.jhapcham.admin.dto.DashboardStatisticsDTO;
import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final DashboardStatisticsService dashboardStatisticsService;
    private final CurrentUserService currentUserService;

    /**
     * GET /api/admin/statistics/dashboard
     * Retrieve dashboard statistics (admin only)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStatistics(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            
            // Verify admin access
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            DashboardStatisticsDTO result = dashboardStatisticsService.getDashboardStatistics();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch dashboard statistics: " + e.getMessage()));
        }
    }

    /**
     * GET /api/admin/statistics/dashboard/refresh
     * Force refresh dashboard statistics cache (admin only)
     */
    @PostMapping("/dashboard/refresh")
    public ResponseEntity<?> refreshDashboardStatistics(Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            
            // Verify admin access
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            DashboardStatisticsDTO result = dashboardStatisticsService.getDashboardStatistics();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to refresh dashboard statistics: " + e.getMessage()));
        }
    }
}
