package com.example.jhapcham.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Order order;
}
