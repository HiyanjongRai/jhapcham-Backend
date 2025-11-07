// src/main/java/com/example/jhapcham/activity/ActivityAggregatorService.java
package com.example.jhapcham.activity;

import com.example.jhapcham.cart.CartItemRepository;
import com.example.jhapcham.order.Order;
import com.example.jhapcham.order.OrderItem;
import com.example.jhapcham.order.OrderRepository;
import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.product.model.ProductView.ProductViewDTO;
import com.example.jhapcham.product.model.ProductView.ProductViewRepository;
import com.example.jhapcham.product.model.comment.Comment;
import com.example.jhapcham.product.model.comment.CommentRepository;
import com.example.jhapcham.product.model.rating.Rating;
import com.example.jhapcham.product.model.rating.RatingRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ActivityAggregatorService {

    private final CommentRepository commentRepo;
    private final ProductViewRepository viewRepo;
    private final CartItemRepository cartRepo;
    private final OrderRepository orderRepo;
    private final RatingRepository ratingRepo;

    public PagedActivity fetchUserFeed(Long userId, int page, int size, Integer lastNDays) {
        Instant cutoff = (lastNDays != null && lastNDays > 0)
                ? Instant.now().minus(Duration.ofDays(lastNDays))
                : null;

        List<ActivityItem> feed = new ArrayList<>(256);

        // COMMENTS
        var comments = commentRepo.findTop200ByAuthor_IdAndDeletedFalseOrderByCreatedAtDesc(userId);
        for (Comment c : safeList(comments)) {
            if (cutoff != null && c.getCreatedAt().isBefore(cutoff)) continue;
            Product p = c.getProduct();
            feed.add(ActivityItem.builder()
                    .type(ActivityType.COMMENT)
                    .occurredAt(c.getCreatedAt())
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .text(c.getText())
                    .build());
        }

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
                    .build());
        }

        // CART
        var cartItems = cartRepo.findTop200ByUser_IdOrderByCreatedAtDesc(userId);
        for (var ci : safeList(cartItems)) {
            if (ci.getCreatedAt() == null) continue;
            if (cutoff != null && ci.getCreatedAt().isBefore(cutoff)) continue;
            Product p = ci.getProduct();
            feed.add(ActivityItem.builder()
                    .type(ActivityType.CART_ADD)
                    .occurredAt(ci.getCreatedAt())
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .quantity(ci.getQuantity())
                    .build());
        }

        // ORDERS
        var orders = orderRepo.findTop200ByCustomer_IdOrderByCreatedAtDesc(userId);
        for (Order o : safeList(orders)) {
            Instant t = o.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
            if (cutoff != null && t.isBefore(cutoff)) continue;

            var itemsList = (o.getItems() == null || o.getItems().isEmpty())
                    ? List.<OrderItem>of()
                    : o.getItems();

            if (itemsList.isEmpty()) {
                feed.add(ActivityItem.builder()
                        .type(ActivityType.ORDER)
                        .occurredAt(t)
                        .amount(o.getTotalPrice())
                        .build());
            } else {
                for (OrderItem item : itemsList) {
                    Product p = item.getProduct();
                    feed.add(ActivityItem.builder()
                            .type(ActivityType.ORDER)
                            .occurredAt(t)
                            .productId(p != null ? p.getId() : null)
                            .productName(p != null ? p.getName() : null)
                            .category(p != null ? p.getCategory() : null)
                            .quantity(item.getQuantity())
                            .amount(item.lineTotal())
                            .build());
                }
            }
        }

        // RATINGS
        var ratings = ratingRepo.findTop200ByUserIdOrderByCreatedAtDesc(userId);
        for (Rating r : safeList(ratings)) {
            if (r.getCreatedAt() == null) continue;
            if (cutoff != null && r.getCreatedAt().isBefore(cutoff)) continue;
            Product p = r.getProduct();
            feed.add(ActivityItem.builder()
                    .type(ActivityType.RATING)
                    .occurredAt(r.getCreatedAt())
                    .productId(p.getId())
                    .productName(p.getName())
                    .category(p.getCategory())
                    .stars(r.getStars())
                    .build());
        }

        // sort desc by time
        feed.sort(Comparator.comparing(ActivityItem::getOccurredAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        // in-memory paging
        int total = feed.size();
        int from = Math.max(0, page * size);
        int to = Math.min(total, from + size);
        List<ActivityItem> pageItems = from >= to ? List.of() : feed.subList(from, to);

        return new PagedActivity(total, page, size, pageItems);
    }

    private static <T> List<T> safeList(List<T> in) { return in == null ? List.of() : in; }

    @Getter @AllArgsConstructor
    public static class PagedActivity {
        private final int total;
        private final int page;
        private final int size;
        private final List<ActivityItem> items;
    }
}
