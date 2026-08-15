package com.shiksha.erp.controller;

import com.shiksha.erp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/student/fee")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Endpoint to create a Razorpay order before opening checkout modal
     */
    @PostMapping("/pay/{feeId}")
    @ResponseBody
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<?> initiatePayment(@PathVariable Long feeId, Authentication authentication) {
        try {
            Map<String, Object> orderDetails = paymentService.createPaymentOrder(feeId, authentication.getName());
            return ResponseEntity.ok(orderDetails);
        } catch (Exception e) {
            log.error("Failed to initiate fee payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint to verify payment and complete transaction
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('PARENT')")
    public String verifyPayment(
            @RequestParam("feeId") Long feeId,
            @RequestParam(value = "razorpay_order_id", required = false) String orderId,
            @RequestParam(value = "razorpay_payment_id", required = false) String paymentId,
            @RequestParam(value = "razorpay_signature", required = false) String signature,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            paymentService.verifyAndCompletePayment(feeId, orderId, paymentId, signature, authentication.getName());
            redirectAttributes.addFlashAttribute("successMsg", "Payment successful! Your fee invoice has been marked as PAID.");
        } catch (Exception e) {
            log.error("Payment verification failed for Fee ID {}: {}", feeId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMsg", "Payment verification failed: " + e.getMessage());
        }
        return "redirect:/student/fee";
    }
}
