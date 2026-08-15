package com.shiksha.erp.config;

import com.shiksha.erp.dto.FeeGenerateDto;
import com.shiksha.erp.service.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeeScheduler {

    private final FeeService feeService;

    @Value("${app.fee.default-amount:1500}")
    private BigDecimal defaultAmount;

    // Har mahine ki 1 tarikh ko subah 8 AM sabhi enrolled students ke liye billing generate karo
    @Scheduled(cron = "0 0 8 1 * ?")
    public void autoGenerateMonthlyFees() {
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.of(today.getYear(), today.getMonthValue());
        LocalDate dueDate = ym.atEndOfMonth();

        log.info("Starting automated monthly fee generation for: {}/{}", today.getMonthValue(), today.getYear());

        FeeGenerateDto dto = FeeGenerateDto.builder()
                .month(today.getMonthValue())
                .year(today.getYear())
                .amountDue(defaultAmount)
                .dueDate(dueDate)
                .classBatchId(null) // all students
                .build();

        int generated = feeService.generateMonthlyFees(dto);
        log.info("Automated fee generation completed. Generated: {} records", generated);
    }

    // Roz subah 9 AM overdue check karo
    @Scheduled(cron = "0 0 9 * * ?")
    public void markOverdue() {
        log.info("Running daily overdue fee check...");
        int count = feeService.markOverdueFees();
        log.info("Overdue fee check completed. Updated: {} records", count);
    }
}
