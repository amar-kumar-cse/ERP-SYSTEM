package com.shiksha.erp.service;

import com.shiksha.erp.dto.FeeGenerateDto;
import com.shiksha.erp.dto.FeeResponseDto;
import com.shiksha.erp.dto.FeeUpdateDto;
import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.FeeStatus;
import com.shiksha.erp.repository.FeeRepository;
import com.shiksha.erp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeService {

    private final FeeRepository feeRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public int generateMonthlyFees(FeeGenerateDto dto) {
        List<Student> targetStudents;

        if (dto.getClassBatchId() != null) {
            targetStudents = studentRepository.findByClassBatchId(dto.getClassBatchId());
        } else {
            targetStudents = studentRepository.findAll();
        }

        List<Fee> newFees = new ArrayList<>();

        for (Student student : targetStudents) {
            // agar us month+year ka record already exist karta hai toh skip
            boolean exists = feeRepository.existsByStudentIdAndMonthAndYear(student.getId(), dto.getMonth(), dto.getYear());
            if (exists) {
                log.debug("Fee record already exists for student: {} ({}/{})", student.getName(), dto.getMonth(), dto.getYear());
                continue;
            }

            Fee fee = Fee.builder()
                    .student(student)
                    .month(dto.getMonth())
                    .year(dto.getYear())
                    .amountDue(dto.getAmountDue())
                    .amountPaid(BigDecimal.ZERO)
                    .status(FeeStatus.DUE)
                    .dueDate(dto.getDueDate())
                    .build();

            newFees.add(fee);
        }

        if (!newFees.isEmpty()) {
            feeRepository.saveAll(newFees);
        }

        log.info("Generated {} fee records for month: {}/{}", newFees.size(), dto.getMonth(), dto.getYear());
        return newFees.size();
    }

    @Transactional
    public void updateFee(Long feeId, FeeUpdateDto dto) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new RuntimeException("Fee record not found with id: " + feeId));

        BigDecimal paid = dto.getAmountPaid() != null ? dto.getAmountPaid() : BigDecimal.ZERO;
        fee.setAmountPaid(paid);

        // Status auto-computation
        if (dto.getStatus() != null) {
            fee.setStatus(dto.getStatus());
        } else {
            if (paid.compareTo(fee.getAmountDue()) >= 0) {
                fee.setStatus(FeeStatus.PAID);
                if (fee.getPaidDate() == null) {
                    fee.setPaidDate(dto.getPaidDate() != null ? dto.getPaidDate() : LocalDate.now());
                }
            } else if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(fee.getAmountDue()) < 0) {
                fee.setStatus(FeeStatus.PARTIAL);
            } else {
                // 0 payment
                if (fee.getDueDate() != null && fee.getDueDate().isBefore(LocalDate.now())) {
                    fee.setStatus(FeeStatus.OVERDUE);
                } else {
                    fee.setStatus(FeeStatus.DUE);
                }
            }
        }

        if (dto.getPaidDate() != null) {
            fee.setPaidDate(dto.getPaidDate());
        }
        if (dto.getPaymentMode() != null && !dto.getPaymentMode().isBlank()) {
            fee.setPaymentMode(dto.getPaymentMode().trim());
        }
        if (dto.getRemarks() != null) {
            fee.setRemarks(dto.getRemarks().trim());
        }

        feeRepository.save(fee);
    }

    @Transactional
    public int markOverdueFees() {
        LocalDate today = LocalDate.now();
        List<Fee> duePastFees = feeRepository.findByStatusAndDueDateBefore(FeeStatus.DUE, today);

        for (Fee fee : duePastFees) {
            fee.setStatus(FeeStatus.OVERDUE);
        }

        if (!duePastFees.isEmpty()) {
            feeRepository.saveAll(duePastFees);
        }

        log.info("Overdue fee records updated: {}", duePastFees.size());
        return duePastFees.size();
    }

    @Transactional(readOnly = true)
    public List<FeeResponseDto> getFeesByStudent(Long studentId) {
        return feeRepository.findByStudentIdOrderByYearDescMonthDesc(studentId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FeeResponseDto> getFeesByBatchAndMonth(Long batchId, int month, int year) {
        List<Fee> fees;
        if (batchId != null) {
            fees = feeRepository.findByClassBatchAndMonthAndYear(batchId, month, year);
        } else {
            fees = feeRepository.findByMonthAndYear(month, year);
        }

        return fees.stream().map(this::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<FeeResponseDto> getCurrentMonthFee(Long studentId) {
        LocalDate now = LocalDate.now();
        return feeRepository.findByStudentIdAndMonthAndYear(studentId, now.getMonthValue(), now.getYear())
                .map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public long getOverdueCount() {
        return feeRepository.countByStatus(FeeStatus.OVERDUE);
    }

    @Transactional(readOnly = true)
    public long getDueCountForMonth(int month, int year) {
        return feeRepository.countByMonthAndYearAndStatus(month, year, FeeStatus.DUE);
    }

    @Transactional(readOnly = true)
    public long getPaidCountForMonth(int month, int year) {
        return feeRepository.countByMonthAndYearAndStatus(month, year, FeeStatus.PAID);
    }

    public FeeResponseDto toResponseDto(Fee f) {
        BigDecimal due = f.getAmountDue() != null ? f.getAmountDue() : BigDecimal.ZERO;
        BigDecimal paid = f.getAmountPaid() != null ? f.getAmountPaid() : BigDecimal.ZERO;
        BigDecimal balance = due.subtract(paid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        String monthName = (f.getMonth() >= 1 && f.getMonth() <= 12)
                ? Month.of(f.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                : String.valueOf(f.getMonth());

        return FeeResponseDto.builder()
                .id(f.getId())
                .studentId(f.getStudent().getId())
                .studentName(f.getStudent().getName())
                .rollNo(f.getStudent().getRollNo())
                .batchName(f.getStudent().getClassBatch() != null ? f.getStudent().getClassBatch().getBatchName() : "No Batch")
                .monthNumber(f.getMonth())
                .monthName(monthName)
                .year(f.getYear())
                .amountDue(due)
                .amountPaid(paid)
                .balance(balance)
                .status(f.getStatus())
                .dueDate(f.getDueDate())
                .paidDate(f.getPaidDate())
                .paymentMode(f.getPaymentMode())
                .remarks(f.getRemarks())
                .build();
    }
}
