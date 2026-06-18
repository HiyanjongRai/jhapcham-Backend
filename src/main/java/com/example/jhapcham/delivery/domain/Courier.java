package com.example.jhapcham.delivery.domain;


import com.example.jhapcham.delivery.application.*;
import com.example.jhapcham.delivery.domain.*;
import com.example.jhapcham.delivery.dto.*;
import com.example.jhapcham.delivery.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String fullName;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 30)
    private String phoneNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 100)
    private String currentDistrict;

    @Column(length = 100)
    private String vehicleType;

    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "courier", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Shipment> assignedShipments = new ArrayList<>();

    @OneToMany(mappedBy = "courier", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Delivery> deliveries = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
