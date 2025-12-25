package com.example.jhapcham.order;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal itemsTotal;

    @Column(nullable = false)
    private BigDecimal shippingFee;

    @Column(nullable = false)
    private BigDecimal discountTotal;

    @Column(nullable = false)
    private BigDecimal grandTotal;

    @Column(nullable = false)
    private LocalDateTime createdAt;

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

    @Enumerated(EnumType.STRING)
    private DeliveryBranch deliveredBranch;

    @Column(nullable = false)
    @Builder.Default
    private boolean sellerAccounted = false;

}