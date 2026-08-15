package com.shiksha.erp.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_TIME_DURATION_MINUTES = 15;

    private static class Attempt {
        int count;
        LocalDateTime lastAttempt;

        Attempt(int count, LocalDateTime lastAttempt) {
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }

    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(key);
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null || attempt.lastAttempt.isBefore(LocalDateTime.now().minusMinutes(LOCK_TIME_DURATION_MINUTES))) {
            attemptsCache.put(key, new Attempt(1, LocalDateTime.now()));
        } else {
            attempt.count++;
            attempt.lastAttempt = LocalDateTime.now();
        }
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        Attempt attempt = attemptsCache.get(key);
        if (attempt == null) return false;
        if (attempt.lastAttempt.isBefore(LocalDateTime.now().minusMinutes(LOCK_TIME_DURATION_MINUTES))) {
            attemptsCache.remove(key);
            return false;
        }
        return attempt.count >= MAX_ATTEMPTS;
    }
}
