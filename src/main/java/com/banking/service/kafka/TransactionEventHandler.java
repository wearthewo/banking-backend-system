package com.banking.service.kafka;

import com.banking.dto.event.TransactionEvent;
import com.banking.service.AuditService;
import com.banking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventHandler {

    private final AuditService auditService;
    private final NotificationService notificationService;

    @KafkaListener(
        topics = "transactions",
        groupId = "banking-transaction-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransaction(TransactionEvent event) {
        try {
            log.info("Processing transaction event: {}", event.getTransactionId());
            
            // 1. Log transaction for audit
            auditService.logTransaction(event);
            
            // 2. Send notifications
            if (event.getStatus().equals("COMPLETED")) {
                notificationService.sendTransactionNotification(event);
            } else if (event.getStatus().equals("FAILED")) {
                notificationService.sendTransactionFailureNotification(event);
            }
            
            log.info("Successfully processed transaction: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing transaction event: " + event.getTransactionId(), e);
            // Implement retry or dead-letter queue logic here if needed
        }
    }
}
