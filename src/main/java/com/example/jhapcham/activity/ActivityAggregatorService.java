package com.example.jhapcham.activity;

import com.example.jhapcham.cart.CartItem;
import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderItem;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.ProductView.ProductViewDTO;
import com.example.jhapcham.product.model.ProductView.ProductViewRepository;
//import com.example.jhapcham.product.model.comment.Comment;
//import com.example.jhapcham.product.model.comment.CommentRepository;
import com.example.jhapcham.review.Review;
import com.example.jhapcham.review.ReviewRepository;
import com.example.jhapcham.product.model.rating.RatingRepository;
import com.example.jhapcham.wishlist.WishlistRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityAggregatorService {

//    private final CommentRepository commentRepo;
    private final ProductViewRepository viewRepo;
    private final CartItemRepository cartRepo;
    private final OrderRepository orderRepo;
    private final RatingRepository ratingRepo;
    private final ReviewRepository reviewRepo;
    private final WishlistRepository wishlistRepo;


    public PagedActivity fetchUserFeed(Long userId, int page, int size, Integer lastNDays) {

        Instant cutoff = (lastNDays != null && lastNDays > 0)
                ? Instant.now().minus(Duration.ofDays(lastNDays))
                : null;

        List<ActivityItem> feed = new ArrayList<>(300);


//        // COMMENTS
//        var comments = commentRepo.findTop200ByAuthor_IdAndDeletedFalseOrderByCreatedAtDesc(userId);
//        for (Comment c : safeList(comments)) {
//
//            if (cutoff != null && c.getCreatedAt().isBefore(cutoff)) continue;
//
//            Product p = c.getProduct();
//            if (p == null) continue;
//
//            feed.add(ActivityItem.builder()
//                    .type(ActivityType.COMMENT)
//                    .occurredAt(c.getCreatedAt())
//                    .productId(p.getId())
//                    .productName(p.getName())
//                    .category(p.getCategory())
//                    .brand(p.getBrand())
//                    .text(c.getText())
//                    .build());
//        }


        // VIEWS
        var views = viewRepo.findHistoryByUser(userId);
        for (ProductViewDTO v : safeList(views)) {

            Instant t = v.getViewedAt().atZone(ZoneId.systemDefault()).toInstant();
            if (cutoff != null && t.isBefore(cutoff)) continue;

            feed.add(ActivityItem.builder()
                    .type(ActivityType.VIEW)
                    .occurredAt(t)
                    .productId(v.getProductId())
                    .productName(v.getProductName())
                    .category(v.getCategory())
                    .brand(v.getBrand())
                    .build());
        }


        // CART ACTIVITY
        List<CartItem> cartItems = cartRepo.findTop200ByUser_IdOrderByCreatedAtDesc(userId);
        for (CartItem ci : safeList(cartItems)) {

            if (ci.getCreatedAt() == null) continue;
            if (cutoff != null && ci.getCreatedAt().isBefore(cutoff)) continue;

            Product p = ci.getProduct();
            if (p == null) continue;

            feed.add(ActivityItem.builder()
                    .type(ActivityType.CART_ADD)
                    .occurredAt(ci.getCreatedAt())
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .brand(p.getBrand())
                    .quantity(ci.getQuantity())
                    .selectedColor(ci.getSelectedColor())
                    .selectedStorage(ci.getSelectedStorage())
                    .build());
        }


        // ORDERS
        var orders = orderRepo.findTop200ByCustomer_IdOrderByCreatedAtDesc(userId);
        for (Order o : safeList(orders)) {

            Instant t = o.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
            if (cutoff != null && t.isBefore(cutoff)) continue;

            for (OrderItem item : safeList(o.getItems())) {

                Product p = item.getProduct();
                if (p == null) continue;

                feed.add(ActivityItem.builder()
                        .type(ActivityType.ORDER)
                        .occurredAt(t)
                        .productId(p.getId())
                        .productName(p.getName())
                        .category(p.getCategory())
                        .brand(p.getBrand())
                        .quantity(item.getQuantity())
                        .amount(item.lineTotal())
                        .selectedColor(item.getSelectedColor())
                        .selectedStorage(item.getSelectedStorage())
                        .build());
            }
        }


        // WISHLIST
        var wishItems = wishlistRepo.findAllByUserId(userId);
        for (var w : safeList(wishItems)) {

            Instant t = w.getCreatedAt() != null
                    ? w.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
                    : Instant.now();

            if (cutoff != null && t.isBefore(cutoff)) continue;

            Product p = w.getProduct();
            if (p == null) continue;

            feed.add(ActivityItem.builder()
                    .type(ActivityType.WISHLIST)
                    .occurredAt(t)
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .brand(p.getBrand())
                    .build());
        }


        // REVIEWS
        var reviews = reviewRepo.findTop200ByCustomer_IdOrderByCreatedAtDesc(userId);
        for (Review r : safeList(reviews)) {

            if (r.getCreatedAt() == null) continue;
            if (cutoff != null && r.getCreatedAt().isBefore(cutoff)) continue;

            Product p = r.getProduct();
            if (p == null) continue;

            feed.add(ActivityItem.builder()
                    .type(ActivityType.REVIEW)
                    .occurredAt(r.getCreatedAt())
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .brand(p.getBrand())
                    .stars(r.getRating())
                    .text(r.getComment())
                    .images(r.getImages())
                    .verifiedPurchase(r.isVerifiedPurchase())
                    .build());
        }


        // ORDER BY DATE DESC
        feed.sort(Comparator.comparing(ActivityItem::getOccurredAt)
                .reversed());

        // PAGINATION
        int total = feed.size();
        int from = Math.max(0, page * size);
        int to = Math.min(total, from + size);

        List<ActivityItem> pageItems =
                from >= to ? List.of() : feed.subList(from, to);

        return new PagedActivity(total, page, size, pageItems);
    }


    private static <T> List<T> safeList(List<T> in) {
        return in == null ? List.of() : in;
    }


    @Getter
    @AllArgsConstructor
    public static class PagedActivity {
        private final int total;
        private final int page;
        private final int size;
        private final List<ActivityItem> items;
    }


}