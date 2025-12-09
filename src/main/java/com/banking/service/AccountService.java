package com.banking.service;

import com.banking.dto.account.AccountResponse;
import com.banking.dto.account.AccountRequest;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.model.Account;
import com.banking.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.banking.dto.transaction.TransactionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    
    /**
     * Create a new account for the specified user
     */
    AccountResponse createAccount(Long userId, AccountRequest request);
    
    /**
     * Get account by ID
     */
    AccountResponse getAccountById(Long accountId);
    
    /**
     * Get account by account number
     */
    AccountResponse getAccountByNumber(String accountNumber);
    
    /**
     * Get all accounts for a user
     */
    List<AccountResponse> getUserAccounts(Long userId);
    
    /**
     * Get paginated transactions for an account with user validation
     * @param accountId the ID of the account
     * @param userId the ID of the user making the request
     * @param pageable pagination information
     * @return page of transaction responses
     * @throws InvalidAccountOperationException if the user doesn't have permission to view the transactions
     */
    Page<TransactionResponse> getAccountTransactions(Long accountId, Long userId, Pageable pageable);
    
    /**
     * Get account balance
     */
    BigDecimal getAccountBalance(Long accountId);
    
    /**
     * Close an account (soft delete)
     */
    void closeAccount(Long accountId, Long userId) 
        throws AccountNotFoundException, InvalidAccountOperationException;
    
    /**
     * Get account entity by ID (internal use)
     */
    Account getAccountEntity(Long accountId) throws AccountNotFoundException;
    
    /**
     * Get account entity by account number (internal use)
     */
    Account getAccountEntity(String accountNumber) throws AccountNotFoundException;
    
    /**
     * Check if an account belongs to a user
     */
    boolean isAccountOwner(Long accountId, Long userId);
    
    /**
     * Check if an account has sufficient balance
     */
    boolean hasSufficientBalance(Long accountId, BigDecimal amount) 
        throws AccountNotFoundException;
    
    /**
     * Update account balance (internal use)
     */
    void updateBalance(Long accountId, BigDecimal amount) 
        throws AccountNotFoundException, InsufficientFundsException;
}
