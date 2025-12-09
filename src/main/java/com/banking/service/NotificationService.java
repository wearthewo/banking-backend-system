package com.banking.service;

import com.banking.dto.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final UserService userService;

    public void sendTransactionNotification(TransactionEvent event) {
        try {
            String recipientEmail = userService.getUserEmail(event.getFromAccount());
            String subject = "Transaction Notification";
            String message = String.format(
                "Transaction Alert!\n" +
                "Type: %s\n" +
                "Amount: %s %s\n" +
                "Account: %s\n" +
                "Status: %s\n" +
                "Description: %s",
                event.getType(),
                event.getAmount(),
                event.getCurrency(),
                maskAccountNumber(event.getFromAccount()),
                event.getStatus(),
                event.getDescription()
            );
            
            emailService.sendEmail(recipientEmail, subject, message);
            log.info("Sent transaction notification for transaction: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Failed to send transaction notification: {}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }

    public void sendTransactionFailureNotification(TransactionEvent event) {
        try {
            String recipientEmail = userService.getUserEmail(event.getFromAccount());
            String subject = "Transaction Failed";
            String message = String.format(
                "Transaction Failed!\n" +
                "Type: %s\n" +
                "Amount: %s %s\n" +
                "Account: %s\n" +
                "Reason: %s",
                event.getType(),
                event.getAmount(),
                event.getCurrency(),
                maskAccountNumber(event.getFromAccount()),
                event.getDescription()
            );
            
            emailService.sendEmail(recipientEmail, subject, message);
            log.warn("Sent transaction failure notification for: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Failed to send failure notification: {}", event.getTransactionId(), e);
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
