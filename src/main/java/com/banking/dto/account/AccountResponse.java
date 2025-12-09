package com.banking.dto.account;

import com.banking.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Account information response")
public record AccountResponse(
    @Schema(description = "Unique identifier of the account", example = "1")
    Long id,
    
    @Schema(description = "Account number", example = "CHK1234567890123")
    String accountNumber,
    
    @Schema(description = "Type of the account", example = "CHECKING")
    Account.AccountType accountType,
    
    @Schema(description = "Current balance", example = "1000.00")
    BigDecimal balance,
    
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    String currency,
    
    @Schema(description = "Account status", example = "ACTIVE")
    Account.AccountStatus status,
    
    @Schema(description = "Date and time when the account was created", example = "2023-01-01T12:00:00")
    LocalDateTime createdAt,
    
    @Schema(description = "Date and time when the account was last updated", example = "2023-01-01T12:00:00")
    LocalDateTime updatedAt
) {
    public static AccountResponse fromEntity(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getAccountType(),
            account.getBalance(),
            account.getCurrency(),
            account.getStatus(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
