package com.example.jhapcham.seller.dto;


import com.example.jhapcham.seller.application.*;
import com.example.jhapcham.seller.domain.*;
import com.example.jhapcham.seller.dto.*;
import com.example.jhapcham.seller.persistence.*;
import com.example.jhapcham.user.domain.Status;
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
    private String sellerFullName;
    private String logoImagePath;

}
