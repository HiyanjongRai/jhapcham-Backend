package com.example.jhapcham.banner.application;


import com.example.jhapcham.banner.application.*;
import com.example.jhapcham.banner.domain.*;
import com.example.jhapcham.banner.dto.*;
import com.example.jhapcham.banner.persistence.*;
import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.common.CloudinaryService;
import com.example.jhapcham.product.domain.Product;
import com.example.jhapcham.product.dto.HomepageBannerDto;
import com.example.jhapcham.product.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;
    private final BannerProductRepository bannerProductRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public BannerResponseDTO createBanner(BannerUpsertRequestDTO dto) {
        validateUpsertRequest(dto, false);

        Banner banner = new Banner();
        applyBannerFields(banner, dto, false);
        Banner saved = bannerRepository.save(banner);
        return toResponse(saved);
    }

    @Transactional
    public BannerResponseDTO updateBanner(Long id, BannerUpsertRequestDTO dto) {
        Banner banner = findBanner(id);
        validateUpsertRequest(dto, true);
        applyBannerFields(banner, dto, true);
        Banner saved = bannerRepository.save(banner);
        return toResponse(saved);
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = findBanner(id);
        if (banner.getImageUrl() != null && banner.getImageUrl().contains("cloudinary.com")) {
            cloudinaryService.delete(banner.getImageUrl());
        }
        if (banner.getMobileImageUrl() != null && banner.getMobileImageUrl().contains("cloudinary.com")) {
            cloudinaryService.delete(banner.getMobileImageUrl());
        }
        bannerRepository.delete(banner);
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getAllBanners() {
        return bannerRepository.findAll()
                .stream()
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponseDTO> getActiveBanners() {
        return bannerRepository.findActiveBanners(LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomepageBannerDto> getHomepageBanners() {
        return bannerRepository.findActiveBanners(LocalDateTime.now())
                .stream()
                .map(this::toHomepageBanner)
                .toList();
    }

    @Transactional
    public BannerResponseDTO toggleBanner(Long id) {
        Banner banner = findBanner(id);
        banner.setActive(!Boolean.TRUE.equals(banner.getActive()));
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerProductResponseDTO attachProduct(Long bannerId, BannerProductRequestDTO requestDTO) {
        if (requestDTO.getProductId() == null) {
            throw new BusinessValidationException("Product id is required.");
        }

        Banner banner = findBanner(bannerId);
        Product product = productRepository.findById(requestDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (bannerProductRepository.existsByBannerIdAndProductId(bannerId, product.getId())) {
            throw new BusinessValidationException("Product is already attached to this banner.");
        }

        BannerProduct bannerProduct = BannerProduct.builder()
                .banner(banner)
                .product(product)
                .displayOrder(requestDTO.getDisplayOrder() == null ? 0 : requestDTO.getDisplayOrder())
                .build();

        BannerProduct saved = bannerProductRepository.save(bannerProduct);
        return toProductResponse(saved);
    }

    @Transactional
    public void detachProduct(Long bannerId, Long productId) {
        BannerProduct bannerProduct = bannerProductRepository.findByBannerIdAndProductId(bannerId, productId)
                .orElseThrow(() -> new RuntimeException("Banner product mapping not found"));
        bannerProductRepository.delete(bannerProduct);
    }

    @Transactional
    public void increaseClickCount(Long bannerId) {
        Banner banner = findBanner(bannerId);
        Long currentCount = banner.getClickCount() == null ? 0L : banner.getClickCount();
        banner.setClickCount(currentCount + 1);
        bannerRepository.save(banner);
    }

    public BannerImageUploadResponseDTO uploadImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessValidationException("Image file is required.");
        }
        String imagePath = cloudinaryService.uploadImage(image, "banners");
        return new BannerImageUploadResponseDTO(imagePath);
    }

    private Banner findBanner(Long id) {
        return bannerRepository.findById(id).orElseThrow(() -> new RuntimeException("Banner not found"));
    }

    private void validateUpsertRequest(BannerUpsertRequestDTO dto, boolean isUpdate) {
        if (!isUpdate && dto.getBannerType() == null) {
            throw new BusinessValidationException("Banner type is required.");
        }
        if (!isUpdate && isBlank(dto.getImageUrl()) && (dto.getImage() == null || dto.getImage().isEmpty())) {
            throw new BusinessValidationException("Banner image is required.");
        }
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessValidationException("End date must be after start date.");
        }
        if (dto.getOverlayOpacity() != null && (dto.getOverlayOpacity() < 0 || dto.getOverlayOpacity() > 1)) {
            throw new BusinessValidationException("Overlay opacity must be between 0 and 1.");
        }
    }

    private void applyBannerFields(Banner banner, BannerUpsertRequestDTO dto, boolean isUpdate) {
        if (dto.getTitle() != null) banner.setTitle(dto.getTitle().trim());
        else if (!isUpdate && banner.getTitle() == null) banner.setTitle("");
        if (dto.getSubtitle() != null) banner.setSubtitle(dto.getSubtitle());
        if (dto.getDescription() != null) banner.setDescription(dto.getDescription());
        if (dto.getDiscountText() != null) banner.setDiscountText(dto.getDiscountText().trim());
        if (dto.getButtonText() != null) banner.setButtonText(dto.getButtonText().trim());
        if (dto.getButtonLink() != null) banner.setButtonLink(dto.getButtonLink().trim());
        if (!isBlank(dto.getBackgroundColor())) banner.setBackgroundColor(dto.getBackgroundColor());
        if (!isBlank(dto.getTextColor())) banner.setTextColor(dto.getTextColor());
        if (dto.getBannerType() != null) banner.setBannerType(dto.getBannerType());
        if (dto.getActive() != null) banner.setActive(dto.getActive());
        if (dto.getPriority() != null) banner.setPriority(dto.getPriority());
        if (dto.getStartDate() != null) banner.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) banner.setEndDate(dto.getEndDate());
        if (dto.getOverlayOpacity() != null) banner.setOverlayOpacity(dto.getOverlayOpacity());
        if (dto.getTextPosition() != null) banner.setTextPosition(dto.getTextPosition());
        if (dto.getAnimationType() != null) banner.setAnimationType(dto.getAnimationType());

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            if (banner.getImageUrl() != null && banner.getImageUrl().contains("cloudinary.com")) {
                cloudinaryService.delete(banner.getImageUrl());
            }
            String imagePath = cloudinaryService.uploadImage(dto.getImage(), "banners");
            banner.setImageUrl(imagePath);
        } else if (!isBlank(dto.getImageUrl())) {
            banner.setImageUrl(dto.getImageUrl());
        } else if (!isUpdate && isBlank(banner.getImageUrl())) {
            throw new BusinessValidationException("Banner image is required.");
        }

        if (dto.getMobileImage() != null && !dto.getMobileImage().isEmpty()) {
            if (banner.getMobileImageUrl() != null && banner.getMobileImageUrl().contains("cloudinary.com")) {
                cloudinaryService.delete(banner.getMobileImageUrl());
            }
            String mobilePath = cloudinaryService.uploadImage(dto.getMobileImage(), "banners");
            banner.setMobileImageUrl(mobilePath);
        } else if (!isBlank(dto.getMobileImageUrl())) {
            banner.setMobileImageUrl(dto.getMobileImageUrl());
        }
    }

    private BannerResponseDTO toResponse(Banner banner) {
        List<BannerProductResponseDTO> products = bannerProductRepository.findByBannerIdOrderByDisplayOrderAsc(banner.getId())
                .stream()
                .map(this::toProductResponse)
                .toList();

        return BannerResponseDTO.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .description(banner.getDescription())
                .discountText(banner.getDiscountText())
                .imageUrl(banner.getImageUrl())
                .mobileImageUrl(banner.getMobileImageUrl())
                .buttonText(banner.getButtonText())
                .buttonLink(banner.getButtonLink())
                .backgroundColor(banner.getBackgroundColor())
                .textColor(banner.getTextColor())
                .bannerType(banner.getBannerType())
                .active(banner.getActive())
                .priority(banner.getPriority())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .overlayOpacity(banner.getOverlayOpacity())
                .textPosition(banner.getTextPosition())
                .animationType(banner.getAnimationType())
                .clickCount(banner.getClickCount())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .products(products)
                .build();
    }

    private HomepageBannerDto toHomepageBanner(Banner banner) {
        return HomepageBannerDto.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .image(banner.getImageUrl())
                .redirectUrl(banner.getButtonLink())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .build();
    }

    private BannerProductResponseDTO toProductResponse(BannerProduct bannerProduct) {
        Product product = bannerProduct.getProduct();
        BigDecimal price = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        BigDecimal discountPercentage = deriveDiscountPercentage(product);
        BigDecimal discountedPrice = calculateDiscountedPrice(price, discountPercentage, product.getSalePrice());
        String thumbnail = (product.getImages() == null || product.getImages().isEmpty())
                ? null
                : product.getImages().get(0).getImagePath();

        return BannerProductResponseDTO.builder()
                .id(bannerProduct.getId())
                .productId(product.getId())
                .name(product.getName())
                .thumbnail(thumbnail)
                .stock(product.getStockQuantity())
                .price(price)
                .discountPercentage(discountPercentage)
                .discountedPrice(discountedPrice)
                .displayOrder(bannerProduct.getDisplayOrder())
                .build();
    }

    private BigDecimal deriveDiscountPercentage(Product product) {
        if (product.getSalePercentage() != null && product.getSalePercentage().compareTo(BigDecimal.ZERO) > 0) {
            return product.getSalePercentage();
        }
        if (product.getSalePrice() != null && product.getPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = product.getPrice().subtract(product.getSalePrice());
            return diff.multiply(BigDecimal.valueOf(100))
                    .divide(product.getPrice(), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal price, BigDecimal discountPercentage, BigDecimal salePrice) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = price.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return price.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        }
        if (salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice.setScale(2, RoundingMode.HALF_UP);
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
