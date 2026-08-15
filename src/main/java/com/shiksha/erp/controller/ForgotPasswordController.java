package com.shiksha.erp.controller;

import com.shiksha.erp.service.ForgotPasswordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @RequestParam("identifier") String identifier,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        forgotPasswordService.processForgotPassword(identifier, baseUrl);

        redirectAttributes.addFlashAttribute("infoMsg", "If an account matches your details, a password reset link has been sent to your registered email.");
        return "redirect:/forgot-password?sent=true";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam(value = "token", required = false) String token, Model model) {
        if (token == null || !forgotPasswordService.validateResetToken(token)) {
            model.addAttribute("tokenInvalid", true);
        } else {
            model.addAttribute("token", token);
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("errorMsg", "Passwords do not match!");
            return "reset-password";
        }

        if (password.length() < 6) {
            model.addAttribute("token", token);
            model.addAttribute("errorMsg", "Password must be at least 6 characters long.");
            return "reset-password";
        }

        boolean success = forgotPasswordService.resetPassword(token, password);
        if (success) {
            redirectAttributes.addFlashAttribute("successMsg", "Your password has been reset successfully! Please sign in with your new password.");
            return "redirect:/login";
        } else {
            model.addAttribute("tokenInvalid", true);
            return "reset-password";
        }
    }
}
