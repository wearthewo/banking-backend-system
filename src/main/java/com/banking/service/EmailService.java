package com.banking.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import java.util.Map;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    private static final int MAX_EMAILS_PER_HOUR = 100;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);
    
    private final JavaMailSender mailSender;
    private final Map<String, EmailRateLimit> rateLimitMap = new ConcurrentHashMap<>();

    /**
     * Send a simple text email
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Plain text email content
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        if (!isValidEmail(to)) {
            log.warn("Invalid email address: {}", to);
            return;
        }

        // Check rate limiting
        if (isRateLimited(to)) {
            log.warn("Rate limit exceeded for email: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            log.info("Email sent to {}", to);
            
            // Update rate limit counter
            updateRateLimit(to);
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    /**
     * Check if the email address is valid
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Check if the email is rate limited
     */
    private boolean isRateLimited(String email) {
        EmailRateLimit rateLimit = rateLimitMap.computeIfAbsent(
            email, k -> new EmailRateLimit()
        );
        
        // Reset counter if window has passed
        if (Duration.between(rateLimit.getLastReset(), LocalDateTime.now())
                .compareTo(RATE_LIMIT_WINDOW) > 0) {
            rateLimit.reset();
            return false;
        }
        
        return rateLimit.getCount() >= MAX_EMAILS_PER_HOUR;
    }
    
    /**
     * Update the rate limit counter for an email
     */
    private void updateRateLimit(String email) {
        EmailRateLimit rateLimit = rateLimitMap.get(email);
        if (rateLimit != null) {
            rateLimit.increment();
        }
    }
    
    /**
     * Inner class to track email rate limiting per recipient
     */
    private static class EmailRateLimit {
        private final AtomicInteger counter = new AtomicInteger(0);
        private volatile LocalDateTime lastReset = LocalDateTime.now();
        
        public int getCount() {
            return counter.get();
        }
        
        public LocalDateTime getLastReset() {
            return lastReset;
        }
        
        public void increment() {
            counter.incrementAndGet();
        }
        
        public void reset() {
            counter.set(1);
            lastReset = LocalDateTime.now();
        }
    }
}
