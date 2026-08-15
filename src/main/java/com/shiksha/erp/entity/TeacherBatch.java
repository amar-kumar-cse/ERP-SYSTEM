package com.shiksha.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "teacher_batches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "class_batch_id"}),
    indexes = {
        @Index(name = "idx_tb_teacher", columnList = "teacher_id"),
        @Index(name = "idx_tb_batch", columnList = "class_batch_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_batch_id", nullable = false)
    private ClassBatch classBatch;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime assignedAt;
}
