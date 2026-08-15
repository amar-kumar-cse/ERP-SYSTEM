package com.shiksha.erp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_batches")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String batchName;

    @Column(length = 60)
    private String timing; // e.g. "4:00 PM - 6:00 PM"

    @Column(length = 60)
    private String days; // e.g. "Mon, Wed, Fri"

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
