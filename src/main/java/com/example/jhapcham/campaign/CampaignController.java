package com.example.jhapcham.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    // Admin Endpoints
    @PostMapping("/admin/campaigns")
    public ResponseEntity<CampaignResponseDTO> createCampaign(@RequestBody CampaignCreateRequestDTO dto) {
        return ResponseEntity.ok(campaignService.createCampaign(dto));
    }

    @GetMapping("/admin/campaigns")
    public ResponseEntity<List<CampaignResponseDTO>> getAllCampaigns() {
        return ResponseEntity.ok(campaignService.getAllCampaigns());
    }

    @DeleteMapping("/admin/campaigns/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.deleteCampaign(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/campaigns/approve-product/{id}")
    public ResponseEntity<Void> approveProduct(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.approveProduct(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/campaigns/reject-product/{id}")
    public ResponseEntity<Void> rejectProduct(@PathVariable @org.springframework.lang.NonNull Long id) {
        campaignService.rejectProduct(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/campaigns/{id}/products")
    public ResponseEntity<List<CampaignProductResponseDTO>> getCampaignProducts(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        return ResponseEntity.ok(campaignService.getCampaignProducts(id));
    }

    @GetMapping("/admin/campaigns/{id}/pending-products")
    public ResponseEntity<List<CampaignProductResponseDTO>> getPendingProducts(
            @PathVariable @org.springframework.lang.NonNull Long id) {
        return ResponseEntity.ok(campaignService.getPendingProducts(id));
    }

    // Seller Endpoints
    @GetMapping("/seller/campaigns/upcoming")
    public ResponseEntity<List<CampaignResponseDTO>> getUpcomingCampaigns() {
        return ResponseEntity.ok(campaignService.getUpcomingCampaigns());
    }

    @PostMapping("/seller/campaigns/join")
    public ResponseEntity<Void> joinCampaign(@RequestParam @org.springframework.lang.NonNull Long sellerId,
            @RequestBody CampaignJoinRequestDTO dto) {
        campaignService.joinCampaign(sellerId, dto);
        return ResponseEntity.ok().build();
    }
}
