package com.example.jhapcham.Checkout;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutItemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double deliveryFee;
    private double lineTotal;

    private String selectedColor;
    private String selectedStorage;

    @ManyToOne
    @JoinColumn(name = "checkout_id")
    @JsonBackReference
    private CheckoutSession checkoutSession;


}