package com.example.jhapcham.order;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null for guest checkout
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerPhone;

    private String customerAlternativePhone; // NEW

    @Column(columnDefinition = "TEXT")
    private String deliveryTimePreference; // NEW

    @Column(columnDefinition = "TEXT")
    private String orderNote; // NEW

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String shippingAddress;

    // INSIDE or OUTSIDE
    @Column(nullable = false)
    private String shippingLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String paymentReference;

    // eSewa transaction identifier (e.g. orderId_timestamp) for verification
    private String esewaTransactionUuid;





    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal itemsTotal;

    @Column(nullable = false)
    private BigDecimal shippingFee;

    @Column(nullable = true)
    private BigDecimal vatAmount;

    @Column(nullable = false)
    private BigDecimal discountTotal;

    @Column(nullable = false)
    private BigDecimal grandTotal;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setOrder(this);
    }

    // assign branch
    @Enumerated(EnumType.STRING)
    private DeliveryBranch assignedBranch;

    @Column(nullable = false)
    private BigDecimal sellerGrossAmount;

    @Column(nullable = false)
    private BigDecimal sellerShippingCharge;

    @Column(nullable = false)
    private BigDecimal sellerNetAmount;

    @Column(columnDefinition = "numeric(38,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal marketplaceCommission = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private DeliveryBranch deliveredBranch;

    @Column(nullable = false)
    @Builder.Default
    private boolean sellerAccounted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'PENDING'")
    @Builder.Default
    private CommissionStatus commissionStatus = CommissionStatus.PENDING;

    // Tracks whether stock was deducted for this order.
    // Prevents double-restoration if cancel is called multiple times.
    @Column(nullable = false)
    @Builder.Default
    private boolean stockDeducted = false;

    // Delivery Confirmation OTP
    private String deliveryOtp;
    private LocalDateTime deliveryOtpExpiry;

    @Builder.Default
    private Integer deliveryOtpResendCount = 0;

    // Commission Penalty Tracking
    private LocalDateTime commissionDueDate;
    
    @Column(nullable = false, precision = 38, scale = 2, columnDefinition = "numeric(38,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal commissionFineAmount = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "boolean DEFAULT false")
    @Builder.Default
    private boolean commissionReminderSent = false;

    private LocalDateTime lastFineCalculationDate;

}