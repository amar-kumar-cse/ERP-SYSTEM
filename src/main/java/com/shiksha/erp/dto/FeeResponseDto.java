package com.shiksha.erp.dto;

import com.shiksha.erp.enums.FeeStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeResponseDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private String rollNo;
    private String batchName;
    private int monthNumber;
    private String monthName;
    private int year;
    private BigDecimal amountDue;
    private BigDecimal amountPaid;
    private BigDecimal balance;
    private FeeStatus status;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String paymentMode;
    private String remarks;
}
