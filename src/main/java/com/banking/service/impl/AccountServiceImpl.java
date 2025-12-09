package com.banking.service.impl;

import com.banking.dto.account.AccountResponse;
import com.banking.dto.account.AccountRequest;
import com.banking.dto.transaction.TransactionResponse;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.exception.UserNotFoundException;
import com.banking.model.Account;
import com.banking.model.User;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.repository.UserRepository;
import com.banking.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(Long userId, AccountRequest request) {
        log.info("Creating new account for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        
        Account account = Account.builder()
            .user(user)
            .accountType(request.accountType())
            .balance(request.initialBalance())
            .currency(request.currency())
            .status(Account.AccountStatus.ACTIVE)
            .build();
        
        // Save the account to generate the ID
        account = accountRepository.save(account);
        
        // Generate account number after saving to get the ID
        String accountNumber = generateAccountNumber(account);
        account.setAccountNumber(accountNumber);
        
        // Save again with the generated account number
        account = accountRepository.save(account);
        
        log.info("Created new account: {}", accountNumber);
        return AccountResponse.fromEntity(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        log.debug("Fetching account with ID: {}", accountId);
        return accountRepository.findById(accountId)
            .map(AccountResponse::fromEntity)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        log.debug("Fetching account with number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
            .map(AccountResponse::fromEntity)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getUserAccounts(Long userId) {
        log.debug("Fetching all accounts for user ID: {}", userId);
        return accountRepository.findByUserId(userId).stream()
            .map(AccountResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(Long accountId, Long userId, Pageable pageable) {
        log.debug("Fetching transactions for account ID: {} for user ID: {}", accountId, userId);
        
        // Verify the account belongs to the user
        if (!isAccountOwner(accountId, userId)) {
            throw new InvalidAccountOperationException("You don't have permission to view transactions for this account");
        }
        
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId, pageable)
            .map(transaction -> TransactionResponse.fromEntity(transaction));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        log.debug("Fetching balance for account ID: {}", accountId);
        return accountRepository.getBalanceById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, Long userId) {
        log.info("Closing account ID: {} for user ID: {}", accountId, userId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new InvalidAccountOperationException("You don't have permission to close this account");
        }
        
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidAccountOperationException("Cannot close account with positive balance");
        }
        
        account.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(account);
        log.info("Successfully closed account: {}", account.getAccountNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountEntity(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountEntity(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with number: " + accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountOwner(Long accountId, Long userId) {
        return accountRepository.existsByIdAndUserId(accountId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long accountId, BigDecimal amount) {
        BigDecimal balance = accountRepository.getBalanceById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));
        return balance.compareTo(amount) >= 0;
    }

    @Override
    @Transactional
    public void updateBalance(Long accountId, BigDecimal amount) {
        log.debug("Updating balance for account ID: {} by amount: {}", accountId, amount);
        
        int updated = accountRepository.updateBalance(accountId, amount);
        if (updated == 0) {
            throw new AccountNotFoundException("Account not found with id: " + accountId);
        }
        
        log.debug("Successfully updated balance for account ID: {}", accountId);
    }
    
    private String generateAccountNumber(Account account) {
        // Simple implementation - in production, you might want a more sophisticated approach
        String prefix = account.getAccountType().name().substring(0, 3);
        return String.format("%s%010d", prefix, account.getId());
    }
}
