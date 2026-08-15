package com.shiksha.erp.dto;

import com.shiksha.erp.enums.FeeStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeUpdateDto {

    @NotNull(message = "Paid amount is required")
    @DecimalMin(value = "0.0", message = "Paid amount cannot be negative")
    private BigDecimal amountPaid;

    private FeeStatus status;

    private String paymentMode;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate paidDate;

    private String remarks;
}
