package com.example.jhapcham.Checkout;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Double subtotal;

    @Builder.Default
    private Double deliveryTotal = 0.0;

    private Double tax;
    private Double grandTotal;

    private String fullAddress;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private CheckoutStatus status;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "checkoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CheckoutItemSnapshot> items;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPaid = false;

}