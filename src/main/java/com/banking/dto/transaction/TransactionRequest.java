package com.banking.dto.transaction;

import com.banking.model.Transaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Request object for creating a new transaction")
public record TransactionRequest(
    @Schema(description = "Source account number (required for WITHDRAWAL, TRANSFER, PAYMENT)", 
            example = "CHK1234567890123", required = false)
    String fromAccountNumber,
    
    @Schema(description = "Destination account number (required for DEPOSIT, TRANSFER, PAYMENT, REFUND)", 
            example = "SAV9876543210987", required = false)
    String toAccountNumber,
    
    @Schema(description = "Transaction amount (must be positive)", 
            example = "100.00", required = true)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,
    
    @Schema(description = "Currency code (ISO 4217)", 
            example = "USD", required = false, defaultValue = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    String currency,
    
    @Schema(description = "Type of transaction", 
            example = "TRANSFER", required = true)
    @NotNull(message = "Transaction type is required")
    Transaction.TransactionType transactionType,
    
    @Schema(description = "Transaction description", 
            example = "Monthly rent payment", required = false)
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description,
    
    @Schema(description = "Additional metadata for the transaction", 
            example = "{\"invoiceId\":\"INV-1234\",\"category\":\"Rent\"}", 
            required = false)
    Map<String, Object> metadata
) {
    public TransactionRequest {
        if (currency == null) {
            currency = "USD";
        }
    }
}
