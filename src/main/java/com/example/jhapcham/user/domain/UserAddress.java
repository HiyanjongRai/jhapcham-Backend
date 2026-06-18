package com.example.jhapcham.user.domain;


import com.example.jhapcham.user.application.*;
import com.example.jhapcham.user.domain.*;
import com.example.jhapcham.user.dto.*;
import com.example.jhapcham.user.persistence.*;
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
    private String province;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String municipality;

    @Column(name = "ward_no", length = 20)
    private String wardNo;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 255)
    private String street;

    @Column(length = 255)
    private String landmark;

    @Column(name = "full_address", length = 500)
    private String fullAddress;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;
}
