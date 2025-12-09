package com.banking.dto.account;

import com.banking.model.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request object for creating a new account")
public record AccountRequest(
    @Schema(description = "Type of the account", example = "CHECKING", required = true)
    @NotNull(message = "Account type is required")
    Account.AccountType accountType,
    
    @Schema(description = "Initial deposit amount (must be zero or positive)", example = "100.00", required = false, defaultValue = "0.00")
    @DecimalMin(value = "0.0", message = "Initial balance cannot be negative")
    BigDecimal initialBalance,
    
    @Schema(description = "Currency code (ISO 4217)", example = "USD", required = false, defaultValue = "USD")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    String currency
) {
    public AccountRequest {
        if (initialBalance == null) {
            initialBalance = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "USD";
        }
    }
}
