package com.shiksha.erp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reports",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "subject", "exam_date"}),
    indexes = {
        @Index(name = "idx_rep_student_date", columnList = "student_id, exam_date"),
        @Index(name = "idx_rep_batch_subject", columnList = "class_batch_id, subject"),
        @Index(name = "idx_rep_subject", columnList = "subject")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_batch_id", nullable = false)
    private ClassBatch classBatch;

    @Column(nullable = false, length = 80)
    private String subject;

    @Column(nullable = false)
    private LocalDate examDate;

    @Column(nullable = false)
    private Integer marks;

    @Column(nullable = false)
    private Integer maxMarks;

    @Column(length = 255)
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_teacher_id", nullable = false)
    private Teacher uploadedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public double getPercentage() {
        if (maxMarks != null && maxMarks > 0 && marks != null) {
            return Math.round(((double) marks / maxMarks * 100.0) * 10.0) / 10.0;
        }
        return 0.0;
    }
}
