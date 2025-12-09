package com.banking.dto.transaction;

import com.banking.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Transaction information response")
public record TransactionResponse(
    @Schema(description = "Unique identifier of the transaction", example = "1")
    Long id,
    
    @Schema(description = "Unique transaction reference number", example = "550e8400-e29b-41d4-a716-446655440000")
    String transactionReference,
    
    @Schema(description = "Source account number", example = "CHK1234567890123", nullable = true)
    String fromAccountNumber,
    
    @Schema(description = "Destination account number", example = "SAV9876543210987", nullable = true)
    String toAccountNumber,
    
    @Schema(description = "Transaction amount", example = "100.00")
    BigDecimal amount,
    
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    String currency,
    
    @Schema(description = "Type of transaction", example = "TRANSFER")
    Transaction.TransactionType transactionType,
    
    @Schema(description = "Transaction status", example = "COMPLETED")
    Transaction.TransactionStatus status,
    
    @Schema(description = "Transaction description", example = "Monthly rent payment")
    String description,
    
    @Schema(description = "Additional metadata for the transaction", 
            example = "{\"invoiceId\":\"INV-1234\",\"category\":\"Rent\"}")
    Map<String, Object> metadata,
    
    @Schema(description = "Date and time when the transaction was created", example = "2023-01-01T12:00:00")
    LocalDateTime createdAt,
    
    @Schema(description = "Date and time when the transaction was last updated", example = "2023-01-01T12:00:00")
    LocalDateTime updatedAt
) {
    public static TransactionResponse fromEntity(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getTransactionReference(),
            transaction.getFromAccount() != null ? transaction.getFromAccount().getAccountNumber() : null,
            transaction.getToAccount() != null ? transaction.getToAccount().getAccountNumber() : null,
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getTransactionType(),
            transaction.getStatus(),
            transaction.getDescription(),
            transaction.getMetadata(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }
}
