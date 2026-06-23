package com.example.jhapcham.campaign.api;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    // Admin Endpoints
    @PostMapping(value = "/admin/campaigns", consumes = { "application/json" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> createCampaign(@RequestBody CampaignCreateRequestDTO dto) {
        return ResponseEntity.ok(campaignService.createCampaign(dto));
    }

    // Admin Endpoints - with file upload
    @PostMapping(value = "/admin/campaigns/with-image", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> createCampaignWithImage(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam(required = false) String type,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String discountType,
            @RequestParam Double discountValue,
            @RequestParam(required = false) Integer maxProducts,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) MultipartFile image) {
        
        CampaignCreateRequestDTO dto = new CampaignCreateRequestDTO();
        dto.setName(name);
        dto.setDescription(description);
        if (type != null) {
            try {
                dto.setType(CampaignType.valueOf(type));
            } catch (IllegalArgumentException e) {
                dto.setType(CampaignType.SEASONAL);
            }
        }
        dto.setStartTime(java.time.LocalDateTime.parse(startDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setEndTime(java.time.LocalDateTime.parse(endDate, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        dto.setDiscountType(DiscountType.valueOf(discountType));
        dto.setDiscountValue(discountValue);
        dto.setMaxProducts(maxProducts);
        dto.setPriority(priority);
        dto.setImage(image);
        
        return ResponseEntity.ok(campaignService.createCampaign(dto));
    }

    @GetMapping("/admin/campaigns")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CampaignResponseDTO>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }

    @DeleteMapping("/admin/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCampaign(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/campaigns/approve-product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveProduct(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.approveProduct(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/campaigns/reject-product/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectProduct(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.rejectProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/campaigns/{id}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CampaignProductResponseDTO>> getCampaignProducts(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        return ResponseEntity.ok(campaignService.getCampaignProducts(id));
    }

    @GetMapping("/admin/campaigns/{id}/pending-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CampaignProductResponseDTO>> getPendingProducts(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        return ResponseEntity.ok(campaignService.getPendingProducts(id));
    }

    // Public Endpoints
    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponseDTO>> getPublicCampaigns() {
        return ResponseEntity.ok(campaignService.getUpcomingCampaigns());
    }

    @GetMapping("/campaigns/{id}/products")
    public ResponseEntity<List<CampaignProductResponseDTO>> getPublicCampaignProducts(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        return ResponseEntity.ok(campaignService.getPublicCampaignProducts(id));
    }

    // Seller Endpoints
    @GetMapping("/seller/campaigns/upcoming")
    public ResponseEntity<List<CampaignResponseDTO>> getUpcomingCampaigns() {
        return ResponseEntity.ok(campaignService.getUpcomingCampaigns());
    }

    @PostMapping("/seller/campaigns/join")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> joinCampaign(
            @RequestBody CampaignJoinRequestDTO dto,
            Authentication authentication) {
        com.example.jhapcham.user.domain.User seller = currentUserService.requireUser(authentication);
        currentUserService.requireSellerSelfOrAdmin(seller, seller.getId());
        campaignService.joinCampaign(seller.getId(), dto);
        return ResponseEntity.ok().build();
    }
}
