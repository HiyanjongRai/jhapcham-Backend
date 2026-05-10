package com.example.jhapcham.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final com.example.jhapcham.security.CurrentUserService currentUserService;

    // Admin Endpoints
    @PostMapping(value = "/admin/campaigns", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampaignResponseDTO> createCampaign(@ModelAttribute CampaignCreateRequestDTO dto) {
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
        com.example.jhapcham.user.model.User seller = currentUserService.requireUser(authentication);
        currentUserService.requireSellerSelfOrAdmin(seller, seller.getId());
        campaignService.joinCampaign(seller.getId(), dto);
        return ResponseEntity.ok().build();
    }
}
