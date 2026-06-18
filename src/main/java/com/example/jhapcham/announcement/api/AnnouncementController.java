package com.example.jhapcham.announcement.api;

import com.example.jhapcham.announcement.application.AnnouncementService;
import com.example.jhapcham.announcement.dto.AnnouncementDTO;
import com.example.jhapcham.Error.ErrorResponse;
import com.example.jhapcham.security.CurrentUserService;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private static final int MAX_PAGE_SIZE = 60;

    private final AnnouncementService announcementService;
    private final CurrentUserService currentUserService;

    /**
     * GET /api/announcements
     * Retrieve active announcements with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = pageable(page, size);
            Page<AnnouncementDTO> result = announcementService.getActiveAnnouncements(pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch announcements: " + e.getMessage()));
        }
    }

    /**
     * GET /api/announcements/list
     * Retrieve all active announcements as a list
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAnnouncementsList() {
        try {
            List<AnnouncementDTO> result = announcementService.getActiveAnnouncementsList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch announcements: " + e.getMessage()));
        }
    }

    /**
     * GET /api/announcements/latest
     * Retrieve latest announcements
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestAnnouncements(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnnouncementDTO> result = announcementService.getLatestAnnouncements(Math.min(limit, 100));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch latest announcements: " + e.getMessage()));
        }
    }

    /**
     * GET /api/announcements/priority/{priority}
     * Retrieve announcements by priority
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<?> getAnnouncementsByPriority(@PathVariable String priority) {
        try {
            List<AnnouncementDTO> result = announcementService.getAnnouncementsByPriority(priority);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch announcements by priority: " + e.getMessage()));
        }
    }

    /**
     * GET /api/announcements/type/{type}
     * Retrieve announcements by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getAnnouncementsByType(@PathVariable String type) {
        try {
            List<AnnouncementDTO> result = announcementService.getAnnouncementsByType(type);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to fetch announcements by type: " + e.getMessage()));
        }
    }

    /**
     * POST /api/announcements
     * Create a new announcement (admin only)
     */
    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            @RequestBody AnnouncementDTO dto,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            
            // Verify admin access
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            AnnouncementDTO result = announcementService.createAnnouncement(dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to create announcement: " + e.getMessage()));
        }
    }

    /**
     * PUT /api/announcements/{id}
     * Update an announcement (admin only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementDTO dto,
            Authentication authentication) {
        try {
            User user = currentUserService.requireUser(authentication);
            
            // Verify admin access
            if (user.getRole() != Role.ADMIN) {
                return ResponseEntity.status(403).body(new ErrorResponse("Access denied. Admin role required."));
            }
            
            AnnouncementDTO result = announcementService.updateAnnouncement(id, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Failed to update announcement: " + e.getMessage()));
        }
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "displayOrder"));
    }
}
