package com.example.jhapcham.review;

import java.util.List;

public class ReviewDTO {
    private Long id;
    private Integer rating;
    private String comment;
    private List<String> images; // review images
    private Long productId;
    private String productName;
    private Long orderId;
    private String reviewerName;
    private String profileImagePath; // <- add this

    public ReviewDTO(Review review) {
        this.id = review.getId();
        this.rating = review.getRating();
        this.comment = review.getComment();
        this.images = review.getImages();
        this.productId = review.getProduct().getId();
        this.productName = review.getProduct().getName();
        this.orderId = review.getOrder().getId();
        this.reviewerName = review.getCustomer().getFullName();

        // Add reviewer profile image path
        String profileImage = review.getCustomer().getProfileImagePath();
        if (profileImage != null && !profileImage.isBlank()) {
            this.profileImagePath = "/uploads/customer-profile/" + profileImage;
        } else {
            this.profileImagePath = null;
        }
    }

    // getters...
}
