package com.example.jhapcham.seller;

import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SellerProfileDTO {

    private Long sellerProfileId;
    private Long sellerUserId;

    private String storeName;
    private String address;
    private String description;
    private String about;

    private Double insideValleyDeliveryFee;
    private Double outsideValleyDeliveryFee;

    private Boolean freeShippingEnabled;
    private Double freeShippingMinOrder;

    private Boolean isVerified;
    private Status status;

    private LocalDateTime approvedAt;
    private LocalDateTime joinedDate;

    // user info
    private String sellerUsername;
    private String sellerEmail;
    private String sellerFullName;
    private String sellerContactNumber;
    private String contactNumber;
    private String logoImagePath;

}
