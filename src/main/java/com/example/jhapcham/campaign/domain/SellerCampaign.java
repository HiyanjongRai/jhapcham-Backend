package com.example.jhapcham.campaign.domain;


import com.example.jhapcham.campaign.application.*;
import com.example.jhapcham.campaign.domain.*;
import com.example.jhapcham.campaign.dto.*;
import com.example.jhapcham.campaign.persistence.*;
import com.example.jhapcham.seller.domain.SellerProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private SellerProfile seller;

    @Column(nullable = false)
    private LocalDateTime joinedAt;
}
