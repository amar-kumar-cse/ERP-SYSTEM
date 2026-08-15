package com.shiksha.erp.service;

import com.shiksha.erp.entity.PasswordResetToken;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.repository.PasswordResetTokenRepository;
import com.shiksha.erp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordService forgotPasswordService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("parent1")
                .email("parent1@example.com")
                .password("old_hashed_pass")
                .build();
    }

    @Test
    @DisplayName("processForgotPassword: creates token and sends email")
    void testProcessForgotPassword_Success() {
        when(userRepository.findByUsername("parent1")).thenReturn(Optional.of(sampleUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean processed = forgotPasswordService.processForgotPassword("parent1", "http://localhost:8080");

        assertTrue(processed);
        verify(tokenRepository).deleteByUser(sampleUser);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("parent1@example.com"), anyString());
    }

    @Test
    @DisplayName("resetPassword: valid token updates password and deletes token")
    void testResetPassword_Success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("valid-uuid-token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .build();

        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new_hashed_pass");

        boolean resetSuccess = forgotPasswordService.resetPassword("valid-uuid-token", "newPassword123");

        assertTrue(resetSuccess);
        assertEquals("new_hashed_pass", sampleUser.getPassword());
        verify(userRepository).save(sampleUser);
        verify(tokenRepository).delete(token);
    }

    @Test
    @DisplayName("resetPassword: expired token returns false")
    void testResetPassword_ExpiredToken() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .token("expired-token")
                .user(sampleUser)
                .expiryDate(LocalDateTime.now().minusMinutes(5))
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        boolean resetSuccess = forgotPasswordService.resetPassword("expired-token", "newPassword123");

        assertFalse(resetSuccess);
        verify(userRepository, never()).save(any(User.class));
    }
}
