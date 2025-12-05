package com.example.jhapcham.wishlist;

import lombok.Data;

@Data
public class WishlistItemDTO {

    private Long id;
    private Long productId;

    private String name;
    private String imagePath;
    private Double price;

    private String shortDescription;
    private Double rating;
    private int views;

    private Double discountPercent;
    private Double salePrice;



}