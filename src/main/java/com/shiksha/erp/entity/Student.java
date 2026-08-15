package com.shiksha.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "students",
    indexes = {
        @Index(name = "idx_stu_roll", columnList = "rollNo"),
        @Index(name = "idx_stu_batch", columnList = "class_batch_id"),
        @Index(name = "idx_stu_parent", columnList = "parent_user_id")
    }
)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id")
    private User parentUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_batch_id")
    private ClassBatch classBatch;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
