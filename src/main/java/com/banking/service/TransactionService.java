package com.banking.service;

import com.banking.dto.transaction.TransactionResponse;
import com.banking.dto.transaction.TransactionRequest;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface TransactionService {
    
    /**
     * Process a new transaction
     */
    TransactionResponse processTransaction(TransactionRequest request, Long userId);
    
    /**
     * Get transaction by reference number
     */
    TransactionResponse getTransaction(String referenceNumber);
    
    /**
     * Get all transactions for an account
     */
    Page<TransactionResponse> getAccountTransactions(Long accountId, Long userId, Pageable pageable);
    
    /**
     * Get all transactions for a user
     */
    Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable);
    
    /**
     * Process a deposit
     */
    TransactionResponse processDeposit(TransactionRequest request, Long userId);
    
    /**
     * Process a withdrawal
     */
    TransactionResponse processWithdrawal(TransactionRequest request, Long userId) 
        throws InsufficientFundsException;
    
    /**
     * Process a transfer between accounts
     */
    TransactionResponse processTransfer(TransactionRequest request, Long userId) 
        throws InsufficientFundsException;
    
    /**
     * Get transaction entity by ID (internal use)
     */
    Transaction getTransactionEntity(String referenceNumber);
}
