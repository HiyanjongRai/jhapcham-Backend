package com.example.jhapcham.user.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 50)
    private String label;

    @Column(name = "receiver_name", length = 150)
    private String receiverName;

    @Column(name = "receiver_phone", length = 30)
    private String receiverPhone;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 255)
    private String street;

    @Column(name = "land_mark", length = 255)
    private String landMark;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "is_default")
    private Boolean isDefault = false;
}
