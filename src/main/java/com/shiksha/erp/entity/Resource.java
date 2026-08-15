package com.shiksha.erp.entity;

import com.shiksha.erp.enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(nullable = false, length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String originalFileName;

    private Long fileSize; // bytes

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_batch_id", nullable = false)
    private ClassBatch classBatch;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_teacher_id", nullable = false)
    private Teacher uploadedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime uploadedAt;
}
