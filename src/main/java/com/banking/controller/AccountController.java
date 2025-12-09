package com.banking.controller;

import com.banking.dto.account.AccountRequest;
import com.banking.dto.account.AccountResponse;
import com.banking.dto.transaction.TransactionResponse;
import com.banking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {
        Long userId = getCurrentUserId();
        AccountResponse response = accountService.createAccount(userId, request);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
            
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all accounts for the current user")
    public ResponseEntity<List<AccountResponse>> getUserAccounts() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(accountService.getUserAccounts(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        AccountResponse response = accountService.getAccountById(id);
        
        // Verify the account belongs to the user
        if (!accountService.isAccountOwner(id, userId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get transactions for an account")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        
        // Verify the account belongs to the user
        if (!accountService.isAccountOwner(id, userId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(accountService.getAccountTransactions(id, userId, pageable));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<BigDecimal> getAccountBalance(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        
        // Verify the account belongs to the user
        if (!accountService.isAccountOwner(id, userId)) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(accountService.getAccountBalance(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Close an account")
    public ResponseEntity<Void> closeAccount(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        accountService.closeAccount(id, userId);
        return ResponseEntity.noContent().build();
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }
}
