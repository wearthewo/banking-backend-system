package com.banking.controller;

import com.banking.dto.transaction.TransactionRequest;
import com.banking.dto.transaction.TransactionResponse;
import com.banking.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for processing financial transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Process a new transaction")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.processTransaction(request, userId);
        
        URI location = URI.create("/api/v1/transactions/" + response.transactionReference());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/deposit")
    @Operation(summary = "Process a deposit transaction")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.processDeposit(request, userId);
        
        URI location = URI.create("/api/v1/transactions/" + response.transactionReference());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Process a withdrawal transaction")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.processWithdrawal(request, userId);
        
        URI location = URI.create("/api/v1/transactions/" + response.transactionReference());
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Process a transfer between accounts")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransactionRequest request) {
        Long userId = getCurrentUserId();
        TransactionResponse response = transactionService.processTransfer(request, userId);
        
        URI location = URI.create("/api/v1/transactions/" + response.transactionReference());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Get transaction by reference number")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String reference) {
        return ResponseEntity.ok(transactionService.getTransaction(reference));
    }

    @GetMapping
    @Operation(summary = "Get all transactions for the current user")
    public ResponseEntity<Page<TransactionResponse>> getUserTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(transactionService.getUserTransactions(userId, pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get all transactions for a specific account")
    public ResponseEntity<Page<TransactionResponse>> getAccountTransactions(
            @PathVariable Long accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountId, userId, pageable));
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }
}
