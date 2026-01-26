package com.example.jhapcham.user.model;

import com.example.jhapcham.seller.SellerProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@ToString(exclude = { "password", "sellerProfile" })
public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @NotBlank
        @Size(max = 100)
        @Column(nullable = false, unique = true, length = 100)
        private String username;

        @Size(max = 150)
        @Column(length = 150)
        private String fullName;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank
        @Size(max = 255)
        @Column(nullable = false, length = 255)
        private String password;

        @NotBlank
        @Email
        @Size(max = 150)
        @Column(nullable = false, unique = true, length = 150)
        private String email;

        @Size(max = 30)
        @Column(length = 30)
        private String contactNumber;

        @Column(length = 500)
        private String address;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Role role;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Status status;

        @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
        @JsonIgnore
        private SellerProfile sellerProfile;

        @Column(name = "profile_image_path", length = 255)
        private String profileImagePath;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

}