package com.example.jhapcham.SellerFollow;

import com.example.jhapcham.product.model.Product;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FollowedSellerDTO {

    private Long sellerId;
    private String storeName;
    private String address;
    private String logoImagePath;
    private List<Product> products;
}
