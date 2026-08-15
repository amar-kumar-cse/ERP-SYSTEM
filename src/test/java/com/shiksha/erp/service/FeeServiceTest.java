package com.shiksha.erp.service;

import com.shiksha.erp.dto.FeeGenerateDto;
import com.shiksha.erp.dto.FeeUpdateDto;
import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.FeeStatus;
import com.shiksha.erp.repository.FeeRepository;
import com.shiksha.erp.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private FeeService feeService;

    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        User parentUser = User.builder()
                .id(10L)
                .username("parent1")
                .email("parent1@example.com")
                .build();

        sampleStudent = Student.builder()
                .id(1L)
                .name("Aarav Sharma")
                .rollNo("SHK-2026-001")
                .parentUser(parentUser)
                .parentName("Rajesh Sharma")
                .build();
    }

    @Test
    @DisplayName("generateMonthlyFees: should generate fee record for new month")
    void testGenerateMonthlyFees_NewRecord() {
        FeeGenerateDto dto = new FeeGenerateDto();
        dto.setMonth(8);
        dto.setYear(2026);
        dto.setAmountDue(new BigDecimal("1500.00"));
        dto.setDueDate(LocalDate.of(2026, 8, 10));

        when(studentRepository.findAll()).thenReturn(List.of(sampleStudent));
        when(feeRepository.existsByStudentIdAndMonthAndYear(1L, 8, 2026)).thenReturn(false);

        int count = feeService.generateMonthlyFees(dto);

        assertEquals(1, count);
        verify(feeRepository, times(1)).saveAll(anyList());
        verify(emailService, times(1)).sendFeeGeneratedEmail(eq("parent1@example.com"), eq("Aarav Sharma"), anyString(), eq(new BigDecimal("1500.00")), eq(dto.getDueDate()));
    }

    @Test
    @DisplayName("generateMonthlyFees: should skip if fee record already exists for student")
    void testGenerateMonthlyFees_SkipExisting() {
        FeeGenerateDto dto = new FeeGenerateDto();
        dto.setMonth(8);
        dto.setYear(2026);
        dto.setAmountDue(new BigDecimal("1500.00"));

        when(studentRepository.findAll()).thenReturn(List.of(sampleStudent));
        when(feeRepository.existsByStudentIdAndMonthAndYear(1L, 8, 2026)).thenReturn(true);

        int count = feeService.generateMonthlyFees(dto);

        assertEquals(0, count);
        verify(feeRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("updateFee: full payment should auto-compute status to PAID")
    void testUpdateFee_FullPayment_AutoComputePaid() {
        Fee existingFee = Fee.builder()
                .id(100L)
                .student(sampleStudent)
                .month(8)
                .year(2026)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(FeeStatus.DUE)
                .build();

        when(feeRepository.findById(100L)).thenReturn(Optional.of(existingFee));

        FeeUpdateDto updateDto = new FeeUpdateDto();
        updateDto.setAmountPaid(new BigDecimal("1500.00"));
        updateDto.setPaymentMode("UPI");
        updateDto.setRemarks("GPay Txn");

        feeService.updateFee(100L, updateDto);

        assertEquals(FeeStatus.PAID, existingFee.getStatus());
        assertEquals(new BigDecimal("1500.00"), existingFee.getAmountPaid());
        assertNotNull(existingFee.getPaidDate());
        verify(feeRepository).save(existingFee);
    }

    @Test
    @DisplayName("updateFee: partial payment should auto-compute status to PARTIAL")
    void testUpdateFee_PartialPayment_AutoComputePartial() {
        Fee existingFee = Fee.builder()
                .id(101L)
                .student(sampleStudent)
                .month(8)
                .year(2026)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(FeeStatus.DUE)
                .build();

        when(feeRepository.findById(101L)).thenReturn(Optional.of(existingFee));

        FeeUpdateDto updateDto = new FeeUpdateDto();
        updateDto.setAmountPaid(new BigDecimal("500.00"));
        updateDto.setPaymentMode("Cash");

        feeService.updateFee(101L, updateDto);

        assertEquals(FeeStatus.PARTIAL, existingFee.getStatus());
        assertEquals(new BigDecimal("500.00"), existingFee.getAmountPaid());
        verify(feeRepository).save(existingFee);
    }

    @Test
    @DisplayName("markOverdueFees: should update DUE past-date fees to OVERDUE and send emails")
    void testMarkOverdueFees() {
        Fee pastDueFee = Fee.builder()
                .id(102L)
                .student(sampleStudent)
                .month(7)
                .year(2026)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(BigDecimal.ZERO)
                .dueDate(LocalDate.now().minusDays(5))
                .status(FeeStatus.DUE)
                .build();

        when(feeRepository.findByStatusAndDueDateBefore(eq(FeeStatus.DUE), any(LocalDate.class)))
                .thenReturn(List.of(pastDueFee));

        int updatedCount = feeService.markOverdueFees();

        assertEquals(1, updatedCount);
        assertEquals(FeeStatus.OVERDUE, pastDueFee.getStatus());
        verify(feeRepository).saveAll(anyList());
        verify(emailService, times(1)).sendFeeOverdueEmail(eq("parent1@example.com"), eq("Aarav Sharma"), anyString(), any(BigDecimal.class), any(LocalDate.class));
    }
}
