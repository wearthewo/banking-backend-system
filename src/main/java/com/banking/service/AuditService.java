package com.banking.service;

import com.banking.dto.event.TransactionEvent;
import com.banking.entity.TransactionAudit;
import com.banking.repository.TransactionAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final TransactionAuditRepository auditRepository;

    public void logTransaction(TransactionEvent event) {
        try {
            TransactionAudit audit = TransactionAudit.builder()
                .transactionId(event.getTransactionId())
                .fromAccount(event.getFromAccount())
                .toAccount(event.getToAccount())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .type(event.getType())
                .status(event.getStatus())
                .description(event.getDescription())
                .timestamp(event.getTimestamp())
                .build();

            auditRepository.save(audit);
            log.debug("Audit log created for transaction: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Failed to create audit log for transaction: {}", event.getTransactionId(), e);
            throw new RuntimeException("Failed to create audit log", e);
        }
    }
}
