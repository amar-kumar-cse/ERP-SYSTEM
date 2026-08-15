package com.shiksha.erp.service;

import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.enums.FeeStatus;
import com.shiksha.erp.repository.FeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private FeeRepository feeRepository;

    @Mock
    private ParentStudentHelper parentStudentHelper;

    @InjectMocks
    private PaymentService paymentService;

    private Fee sampleFee;
    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "keyId", "rzp_test_123");
        ReflectionTestUtils.setField(paymentService, "keySecret", "secret_abc");

        sampleStudent = Student.builder()
                .id(1L)
                .name("Aarav Sharma")
                .rollNo("SHK-2026-001")
                .build();

        sampleFee = Fee.builder()
                .id(10L)
                .student(sampleStudent)
                .month(8)
                .year(2026)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(FeeStatus.DUE)
                .build();
    }

    @Test
    @DisplayName("createPaymentOrder: creates valid order payload with balance in paise")
    void testCreatePaymentOrder() {
        when(feeRepository.findById(10L)).thenReturn(Optional.of(sampleFee));
        doNothing().when(parentStudentHelper).validateParentAccess("parent1", 1L);
        when(feeRepository.save(any(Fee.class))).thenReturn(sampleFee);

        Map<String, Object> orderDetails = paymentService.createPaymentOrder(10L, "parent1");

        assertNotNull(orderDetails);
        assertEquals(150000L, orderDetails.get("amount")); // 1500 INR = 150000 Paise
        assertEquals("INR", orderDetails.get("currency"));
        assertEquals("Aarav Sharma", orderDetails.get("studentName"));
        assertNotNull(orderDetails.get("orderId"));
    }

    @Test
    @DisplayName("verifyAndCompletePayment: updates fee status to PAID with transaction details")
    void testVerifyAndCompletePayment() {
        when(feeRepository.findById(10L)).thenReturn(Optional.of(sampleFee));
        doNothing().when(parentStudentHelper).validateParentAccess("parent1", 1L);
        when(feeRepository.save(any(Fee.class))).thenAnswer(inv -> inv.getArgument(0));

        Fee paidFee = paymentService.verifyAndCompletePayment(10L, "order_123", "pay_456", "sandbox_mock_signature", "parent1");

        assertEquals(FeeStatus.PAID, paidFee.getStatus());
        assertEquals(new BigDecimal("1500.00"), paidFee.getAmountPaid());
        assertEquals("RAZORPAY", paidFee.getPaymentMode());
        assertEquals("pay_456", paidFee.getTransactionId());
        assertNotNull(paidFee.getPaidDate());
    }
}
