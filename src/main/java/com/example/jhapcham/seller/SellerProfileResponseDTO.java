package com.example.jhapcham.seller;

import com.example.jhapcham.product.Product;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class SellerProfileResponseDTO {

    private Long sellerProfileId;
    private Long userId;
    private String username;
    private String email;
    private String contactNumber;

    private String storeName;
    private String address;
    private String description;
    private String about;

    private Double insideValleyDeliveryFee;
    private Double outsideValleyDeliveryFee;
    private Boolean freeShippingEnabled;
    private Double freeShippingMinOrder;

    private String logoImagePath;
    private String profileImagePath;
    private String status;
    private Boolean isVerified;
    private java.time.LocalDateTime joinedDate;

    private List<ProductSummaryDTO> products;

    private Long followerCount;

    public static SellerProfileResponseDTO from(SellerProfile profile, List<Product> products, Long followerCount) {

        return SellerProfileResponseDTO.builder()
                .sellerProfileId(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUser().getUsername())
                .email(profile.getUser().getEmail())
                .contactNumber(profile.getUser().getContactNumber())
                .storeName(profile.getStoreName())
                .address(profile.getAddress())
                .description(profile.getDescription())
                .about(profile.getAbout())
                .insideValleyDeliveryFee(profile.getInsideValleyDeliveryFee())
                .outsideValleyDeliveryFee(profile.getOutsideValleyDeliveryFee())
                .freeShippingEnabled(profile.getFreeShippingEnabled())
                .freeShippingMinOrder(profile.getFreeShippingMinOrder())
                .logoImagePath(profile.getLogoImagePath())
                .profileImagePath(profile.getUser().getProfileImagePath())
                .status(profile.getStatus().name())
                .isVerified(profile.getIsVerified())
                .joinedDate(profile.getJoinedDate())
                .products(products.stream().map(ProductSummaryDTO::from).collect(Collectors.toList()))
                .followerCount(followerCount)
                .build();
    }
}
