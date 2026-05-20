package com.example.jhapcham.refund;

import com.example.jhapcham.order.OrderItem;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "refund_line_items",
        indexes = {
                @Index(name = "idx_refund_line_items_request", columnList = "refund_request_id"),
                @Index(name = "idx_refund_line_items_order_item", columnList = "order_item_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_request_id", nullable = false)
    @JsonIgnore
    private RefundRequest refundRequest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Long productIdSnapshot;

    @Column(nullable = false)
    private String productNameSnapshot;

    private String productImageSnapshot;

    @Column(nullable = false)
    private Integer quantityRequested;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal itemSubtotal;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal taxRefund;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal discountAdjustment;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal totalRefund;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal sellerCommissionReversal;

    @Column(nullable = false)
    private boolean restockInventory;
}
