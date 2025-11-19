package com.example.jhapcham.review;

import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.order.OrderStatus;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.user.model.User;
import com.example.jhapcham.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final OrderRepository orderRepo;
    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;

    /* -------------------------------------------------------
       SUBMIT REVIEW
    ------------------------------------------------------- */
    @Transactional
    public Review submitReview(Long userId, ReviewRequestDTO req) throws Exception {

        Order order = orderRepo.findById(req.getOrderId())
                .orElseThrow(() -> new Exception("Order not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new Exception("User not found"));

        Product product = order.getItems().get(0).getProduct();

        if (!order.getCustomer().getId().equals(userId))
            throw new Exception("You cannot review this order");

        if (order.getStatus() != OrderStatus.DELIVERED)
            throw new Exception("Review allowed only after delivery");

        if (product.getSellerId() != null && product.getSellerId().equals(userId))
            throw new Exception("Seller cannot review their own product");

        if (reviewRepo.existsByOrder_Id(order.getId()))
            throw new Exception("Review already submitted for this order");

        boolean noRating = req.getRating() == null;
        boolean noComment = req.getComment() == null || req.getComment().isBlank();
        boolean noImages = req.getImages() == null || req.getImages().isEmpty();

        if (noRating && noComment && noImages)
            throw new Exception("Submit rating, comment, or images");

        List<String> imageUrls = validateAndProcessImages(req.getImages());

        Review review = Review.builder()
                .customer(user)
                .product(product)
                .order(order)
                .rating(noRating ? null : req.getRating())
                .comment(noComment ? null : req.getComment())
                .images(imageUrls)
                .verifiedPurchase(true)
                .build();

        return reviewRepo.save(review);
    }


    /* -------------------------------------------------------
       EDIT REVIEW
    ------------------------------------------------------- */
    @Transactional
    public Review editReview(Long reviewId, Long userId, ReviewRequestDTO req) throws Exception {

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new Exception("Review not found"));

        if (!review.getCustomer().getId().equals(userId))
            throw new Exception("You can edit only your own review");

        List<String> imageUrls = new ArrayList<>();

        if (req.getImages() != null && !req.getImages().isEmpty())
            imageUrls = validateAndProcessImages(req.getImages());
        else
            imageUrls = review.getImages();

        if (req.getRating() != null) {
            if (req.getRating() < 1 || req.getRating() > 5)
                throw new Exception("Rating must be 1 to 5");
            review.setRating(req.getRating());
        }

        if (req.getComment() != null)
            review.setComment(req.getComment());

        review.setImages(imageUrls);

        return reviewRepo.save(review);
    }


    /* -------------------------------------------------------
       DELETE REVIEW
    ------------------------------------------------------- */
    @Transactional
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) throws Exception {

        Review review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new Exception("Review not found"));

        if (!isAdmin && !review.getCustomer().getId().equals(userId))
            throw new Exception("You can delete only your own review");

        reviewRepo.delete(review);
    }


    /* -------------------------------------------------------
       LIST REVIEWS FOR PRODUCT
    ------------------------------------------------------- */
    public List<Review> listReviewsForProduct(Long productId) {
        return reviewRepo.findAll().stream()
                .filter(r -> r.getProduct().getId().equals(productId))
                .toList();
    }


    /* -------------------------------------------------------
       IMAGE VALIDATION
    ------------------------------------------------------- */

    /* -------------------------------------------------------
       IMAGE VALIDATION + SAVE TO review-images/
    ------------------------------------------------------- */
    private static final String REVIEW_IMAGE_DIR = "H:/Project/Ecomm/jhapcham/uploads/review-images/";

    private List<String> validateAndProcessImages(List<MultipartFile> images) throws Exception {

        List<String> urls = new ArrayList<>();

        if (images == null || images.isEmpty())
            return urls;

        File folder = new File(REVIEW_IMAGE_DIR);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new Exception("Failed to create review-images directory");
        }

        for (MultipartFile img : images) {

            if (img.getSize() > 2 * 1024 * 1024)
                throw new Exception("Max file size 2MB");

            String original = img.getOriginalFilename();
            if (original == null) throw new Exception("Invalid file name");

            original = original.toLowerCase().trim();

            if (!(original.endsWith(".jpg") || original.endsWith(".jpeg") || original.endsWith(".png")))
                throw new Exception("Only JPG, JPEG, PNG allowed");

            // Final destination
            File dest = new File(REVIEW_IMAGE_DIR + original);

            img.transferTo(dest);

            // URL for frontend
            urls.add("review-images/" + original);
        }

        return urls;
    }
}