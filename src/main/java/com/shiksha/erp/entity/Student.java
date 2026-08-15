package com.shiksha.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true, length = 30)
    private String rollNo;

    @Column(length = 100)
    private String parentName;

    @Column(length = 15)
    private String parentPhone;

    // parent ka login account — ek parent ke multiple bachhe ho sakte hain
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id")
    private User parentUser;

    // Phase 2: student class assign hone se pehle bhi exist kar sakta hai (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_batch_id")
    private ClassBatch classBatch;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
