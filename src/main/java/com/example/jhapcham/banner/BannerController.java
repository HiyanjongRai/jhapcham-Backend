package com.example.jhapcham.banner;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @PostMapping(value = "/admin/banners", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponseDTO> createBanner(@ModelAttribute BannerUpsertRequestDTO dto) {
        return ResponseEntity.ok(bannerService.createBanner(dto));
    }

    @PutMapping(value = "/admin/banners/{id}", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponseDTO> updateBanner(@PathVariable Long id, @ModelAttribute BannerUpsertRequestDTO dto) {
        return ResponseEntity.ok(bannerService.updateBanner(id, dto));
    }

    @DeleteMapping("/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerResponseDTO>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PatchMapping("/admin/banners/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerResponseDTO> toggleBanner(@PathVariable Long id) {
        return ResponseEntity.ok(bannerService.toggleBanner(id));
    }

    @PostMapping(value = "/admin/banners/upload-image", consumes = { "multipart/form-data" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerImageUploadResponseDTO> uploadImage(@RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(bannerService.uploadImage(image));
    }

    @PostMapping("/admin/banners/{bannerId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerProductResponseDTO> attachProduct(
            @PathVariable Long bannerId,
            @RequestBody BannerProductRequestDTO requestDTO) {
        return ResponseEntity.ok(bannerService.attachProduct(bannerId, requestDTO));
    }

    @DeleteMapping("/admin/banners/{bannerId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> detachProduct(@PathVariable Long bannerId, @PathVariable Long productId) {
        bannerService.detachProduct(bannerId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/banners/active")
    public ResponseEntity<List<BannerResponseDTO>> getActiveBanners() {
        return ResponseEntity.ok(bannerService.getActiveBanners());
    }

    @PostMapping("/banners/{id}/click")
    public ResponseEntity<Void> trackClick(@PathVariable Long id) {
        bannerService.increaseClickCount(id);
        return ResponseEntity.ok().build();
    }
}
