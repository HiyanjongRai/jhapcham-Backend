package com.example.jhapcham.SellerFollow;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "seller_follows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "seller_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SellerFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;
}
