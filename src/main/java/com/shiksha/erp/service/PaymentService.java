package com.shiksha.erp.service;

import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.enums.FeeStatus;
import com.shiksha.erp.repository.FeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final FeeRepository feeRepository;
    private final ParentStudentHelper parentStudentHelper;

    @Value("${razorpay.key.id:rzp_test_shiksha1234}")
    private String keyId;

    @Value("${razorpay.key.secret:rzp_secret_shiksha5678}")
    private String keySecret;

    /**
     * Creates a payment order payload for the given fee ID.
     */
    @Transactional
    public Map<String, Object> createPaymentOrder(Long feeId, String username) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Fee record not found with ID: " + feeId));

        // Verify parent access
        parentStudentHelper.validateParentAccess(username, fee.getStudent().getId());

        if (fee.getStatus() == FeeStatus.PAID) {
            throw new IllegalStateException("This fee invoice has already been fully paid.");
        }

        BigDecimal balanceDue = fee.getAmountDue().subtract(
                fee.getAmountPaid() != null ? fee.getAmountPaid() : BigDecimal.ZERO
        );

        if (balanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No outstanding balance due on this invoice.");
        }

        // Amount in paise (1 INR = 100 Paise)
        long amountInPaise = balanceDue.multiply(new BigDecimal("100")).longValue();
        String generatedOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        fee.setOrderId(generatedOrderId);
        feeRepository.save(fee);

        Map<String, Object> response = new HashMap<>();
        response.put("keyId", keyId);
        response.put("orderId", generatedOrderId);
        response.put("amount", amountInPaise);
        response.put("currency", "INR");
        response.put("feeId", fee.getId());
        response.put("studentName", fee.getStudent().getName());
        response.put("rollNo", fee.getStudent().getRollNo());
        response.put("monthYear", fee.getMonth() + "/" + fee.getYear());
        response.put("parentName", fee.getStudent().getParentName());
        response.put("parentPhone", fee.getStudent().getParentPhone());

        log.info("Created Razorpay payment order {} for fee ID {} (Amount: ₹{})", generatedOrderId, feeId, balanceDue);
        return response;
    }

    /**
     * Verifies payment signature and completes the fee transaction.
     */
    @Transactional
    public Fee verifyAndCompletePayment(Long feeId, String orderId, String paymentId, String signature, String username) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Fee record not found with ID: " + feeId));

        parentStudentHelper.validateParentAccess(username, fee.getStudent().getId());

        // Signature validation: HMAC-SHA256(orderId + "|" + paymentId, secret)
        boolean isSignatureValid = verifySignature(orderId, paymentId, signature, keySecret);
        if (!isSignatureValid) {
            // If in local test sandbox, allow simulated payments
            if (keyId.startsWith("rzp_test_") && (signature == null || signature.isBlank() || signature.equals("sandbox_mock_signature") || signature.startsWith("simulated_"))) {
                log.info("Accepting sandbox simulated payment signature for order {}", orderId);
            } else {
                throw new SecurityException("Invalid Razorpay payment signature! Transaction verification failed.");
            }
        }

        fee.setAmountPaid(fee.getAmountDue());
        fee.setStatus(FeeStatus.PAID);
        fee.setPaidDate(LocalDate.now());
        fee.setPaymentMode("RAZORPAY");
        fee.setOrderId(orderId);
        fee.setTransactionId(paymentId != null && !paymentId.isBlank() ? paymentId : "pay_" + UUID.randomUUID().toString().substring(0, 14));
        fee.setRemarks("Online Payment via Razorpay (" + fee.getTransactionId() + ")");

        Fee savedFee = feeRepository.save(fee);
        log.info("Payment successfully processed and fee marked PAID for Student: {} (Fee ID: {}, Txn: {})",
                fee.getStudent().getName(), feeId, fee.getTransactionId());

        return savedFee;
    }

    public static boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        if (orderId == null || paymentId == null || signature == null || secret == null) {
            return false;
        }
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equalsIgnoreCase(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification algorithm error: {}", e.getMessage());
            return false;
        }
    }
}
