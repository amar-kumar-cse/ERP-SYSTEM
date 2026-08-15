package com.shiksha.erp.service;

import com.shiksha.erp.entity.PasswordResetToken;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.repository.PasswordResetTokenRepository;
import com.shiksha.erp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public boolean processForgotPassword(String identifier, String baseUrl) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }

        // Find user by username or email
        Optional<User> userOpt = userRepository.findByUsername(identifier.trim());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier.trim());
        }

        if (userOpt.isEmpty()) {
            log.warn("Forgot password requested for non-existent identifier: {}", identifier);
            // Return true anyway to prevent account enumeration attacks
            return true;
        }

        User user = userOpt.get();
        // Remove any old token
        tokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + token;
        log.info("Generated password reset link for user {}: {}", user.getUsername(), resetLink);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        }

        return true;
    }

    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        if (token == null || token.isBlank()) return false;
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        return tokenOpt.isPresent() && !tokenOpt.get().isExpired();
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
        log.info("Password successfully updated via reset token for user: {}", user.getUsername());
        return true;
    }
}
