package com.shiksha.erp.entity;

import com.shiksha.erp.enums.FeeStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fees",
    uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "fee_month", "fee_year"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "fee_month", nullable = false)
    private int month; // 1-12

    @Column(name = "fee_year", nullable = false)
    private int year;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountDue;

    @Builder.Default
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeeStatus status;

    private LocalDate dueDate;

    private LocalDate paidDate;

    @Column(length = 50)
    private String paymentMode; // Cash / UPI / NetBanking / Cheque / RAZORPAY

    @Column(length = 100)
    private String orderId; // Razorpay Order ID

    @Column(length = 100)
    private String transactionId; // Razorpay Payment ID

    @Column(length = 255)
    private String remarks;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
