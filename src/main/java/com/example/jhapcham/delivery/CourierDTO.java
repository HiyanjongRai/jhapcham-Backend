package com.example.jhapcham.delivery;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierDTO {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String currentDistrict;
    private String vehicleType;
    private Boolean active;
    private Integer assignedShipmentCount;
    private String token;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
}
