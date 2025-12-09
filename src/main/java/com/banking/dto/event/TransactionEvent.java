package com.banking.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER
    private String status; // PENDING, COMPLETED, FAILED
    private String description;
    private LocalDateTime timestamp;
}
