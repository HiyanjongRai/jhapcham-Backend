package com.example.jhapcham.delivery;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRequestDTO {
    private Long shipmentId;
    private Long courierId;
    private String notes;
}
