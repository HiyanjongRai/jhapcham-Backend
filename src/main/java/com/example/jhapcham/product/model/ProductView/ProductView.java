package com.example.jhapcham.product.model.ProductView;

import com.example.jhapcham.product.model.Product;
import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_view",
        indexes = {
                @Index(name = "idx_view_user_time", columnList = "user_id, viewed_at"),
                @Index(name = "idx_view_product_time", columnList = "product_id, viewed_at"),
                @Index(name = "idx_view_anon", columnList = "anon_key")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // FIXED PRODUCT RELATION, ONLY ONE
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            foreignKey = @ForeignKey(
                    name = "fk_product_view_product",
                    foreignKeyDefinition =
                            "FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE"
            )
    )
    private Product product;

    @Column(name = "anon_key", length = 64)
    private String anonKey;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
}
