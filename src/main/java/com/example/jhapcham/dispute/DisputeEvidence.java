package com.example.jhapcham.dispute;

import com.example.jhapcham.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "dispute_id")
    private Dispute dispute;

    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedByUser;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String filePath;

    private String fileType;

    private Long fileSize;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
}
