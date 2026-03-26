package com.example.jhapcham.seller;

import com.example.jhapcham.order.OrderSummaryDTO;
import com.example.jhapcham.user.model.Status;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SellerCustomerDetailDTO {
    // Basic Profile
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String profileImagePath;
    private Status status;
    private LocalDateTime joinedAt;

    // Seller-Specific Contextual Stats
    private BigDecimal totalSpentWithSeller;
    private Long orderCountWithSeller;
    private LocalDateTime lastOrderDate;
    private String favoriteCategoryWithSeller;

    // History (Only this seller's orders)
    private List<OrderSummaryDTO> orderHistory;
}
