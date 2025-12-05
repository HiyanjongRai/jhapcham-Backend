package com.example.jhapcham.order;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullAddress;

    private Double latitude;
    private Double longitude;

    @OneToOne(mappedBy = "deliveryAddress")
    private Order order;
}
