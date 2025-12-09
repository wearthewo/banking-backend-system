package com.banking.service.impl;

import com.banking.dto.event.TransactionEvent;
import com.banking.dto.transaction.TransactionRequest;
import com.banking.dto.transaction.TransactionResponse;
import com.banking.exception.AccountNotFoundException;
import com.banking.exception.InsufficientFundsException;
import com.banking.exception.InvalidAccountOperationException;
import com.banking.model.*;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.AccountService;
import com.banking.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private static final String TRANSACTIONS_TOPIC = "transactions";

    @Override
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request, Long userId) {
        log.info("Processing transaction of type: {} for user ID: {}", request.transactionType(), userId);
        
        TransactionResponse response = switch (request.transactionType()) {
            case DEPOSIT -> processDeposit(request, userId);
            case WITHDRAWAL -> processWithdrawal(request, userId);
            case TRANSFER -> processTransfer(request, userId);
            default -> throw new InvalidAccountOperationException("Unsupported transaction type: " + request.transactionType());
        };
        
        // Publish transaction event
        publishTransactionEvent(request, response, "COMPLETED", "Transaction processed successfully");
        
        return response;
    }
    
    private void publishTransactionEvent(TransactionRequest request, TransactionResponse response, String status, String description) {
        if (request == null || response == null) {
            log.error("Cannot publish transaction event: request or response is null");
            return;
        }
        
        try {
            TransactionEvent.TransactionEventBuilder eventBuilder = TransactionEvent.builder()
                .transactionId(String.valueOf(response.id()))
                .amount(request.amount())
                .type(request.transactionType().name())
                .status(status)
                .description(description)
                .timestamp(LocalDateTime.now());
                
            // Handle potentially null fields
            if (request.fromAccountNumber() != null) {
                eventBuilder.fromAccount(request.fromAccountNumber());
            }
            
            if (request.toAccountNumber() != null) {
                eventBuilder.toAccount(request.toAccountNumber());
            }
            
            if (request.currency() != null) {
                eventBuilder.currency(request.currency());
            }
            
            TransactionEvent event = eventBuilder.build();
                
            // Create a ProducerRecord with headers
            ProducerRecord<String, TransactionEvent> record = 
                new ProducerRecord<>(TRANSACTIONS_TOPIC, event);
            record.headers().add("transactionType", request.transactionType().name().getBytes());
                
            kafkaTemplate.send(record);
            log.info("Published transaction event for transaction ID: {}", response.id());
            
        } catch (Exception e) {
            log.error("Failed to publish transaction event for transaction ID: {}", response.id(), e);
            // Don't fail the transaction if event publishing fails
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String referenceNumber) {
        log.debug("Fetching transaction with reference: {}", referenceNumber);
        return transactionRepository.findByTransactionReference(referenceNumber)
            .map(TransactionResponse::fromEntity)
            .orElseThrow(() -> new AccountNotFoundException("Transaction not found with reference: " + referenceNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(Long accountId, Long userId, Pageable pageable) {
        log.debug("Fetching transactions for account ID: {} and user ID: {}", accountId, userId);
        
        // Verify the account belongs to the user
        if (!accountService.isAccountOwner(accountId, userId)) {
            throw new InvalidAccountOperationException("You don't have permission to view transactions for this account");
        }
        
        return transactionRepository.findByFromAccountIdOrToAccountId(accountId, accountId, pageable)
            .map(TransactionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getUserTransactions(Long userId, Pageable pageable) {
        log.debug("Fetching all transactions for user ID: {}", userId);
        return transactionRepository.findByUserId(userId, pageable)
            .map(TransactionResponse::fromEntity);
    }

    @Override
    @Transactional
    public TransactionResponse processDeposit(TransactionRequest request, Long userId) {
        log.info("Processing deposit request for user ID: {}", userId);
        
        validateTransactionRequest(request, false, true);
        
        Account toAccount = accountService.getAccountEntity(request.toAccountNumber());
        
        // Create and save the transaction
        Transaction transaction = createTransaction(null, toAccount, request);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);
        
        // Update the account balance
        accountService.updateBalance(toAccount.getId(), request.amount());
        
        log.info("Successfully processed deposit. Transaction ID: {}", transaction.getId());
        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse processWithdrawal(TransactionRequest request, Long userId) 
            throws InsufficientFundsException {
        log.info("Processing withdrawal request for user ID: {}", userId);
        
        validateTransactionRequest(request, true, false);
        
        Account fromAccount = accountService.getAccountEntity(request.fromAccountNumber());
        
        // Verify the account belongs to the user
        if (!fromAccount.getUser().getId().equals(userId)) {
            throw new InvalidAccountOperationException("You don't have permission to withdraw from this account");
        }
        
        // Check if account has sufficient balance
        if (!accountService.hasSufficientBalance(fromAccount.getId(), request.amount())) {
            throw new InsufficientFundsException("Insufficient funds in the account");
        }
        
        // Create and save the transaction
        Transaction transaction = createTransaction(fromAccount, null, request);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);
        
        // Update the account balance (withdraw is a negative amount)
        accountService.updateBalance(fromAccount.getId(), request.amount().negate());
        
        log.info("Successfully processed withdrawal. Transaction ID: {}", transaction.getId());
        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    @Transactional
    public TransactionResponse processTransfer(TransactionRequest request, Long userId) 
            throws InsufficientFundsException {
        log.info("Processing transfer request for user ID: {}", userId);
        
        validateTransactionRequest(request, true, true);
        
        Account fromAccount = accountService.getAccountEntity(request.fromAccountNumber());
        Account toAccount = accountService.getAccountEntity(request.toAccountNumber());
        
        // Verify the source account belongs to the user
        if (!fromAccount.getUser().getId().equals(userId)) {
            throw new InvalidAccountOperationException("You don't have permission to transfer from this account");
        }
        
        // Check if source account has sufficient balance
        if (!accountService.hasSufficientBalance(fromAccount.getId(), request.amount())) {
            throw new InsufficientFundsException("Insufficient funds in the source account");
        }
        
        // Create and save the transaction
        Transaction transaction = createTransaction(fromAccount, toAccount, request);
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        transaction = transactionRepository.save(transaction);
        
        // Update the account balances
        accountService.updateBalance(fromAccount.getId(), request.amount().negate()); // Deduct from source
        accountService.updateBalance(toAccount.getId(), request.amount()); // Add to destination
        
        log.info("Successfully processed transfer. Transaction ID: {}", transaction.getId());
        return TransactionResponse.fromEntity(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransactionEntity(String referenceNumber) {
        return transactionRepository.findByTransactionReference(referenceNumber)
            .orElseThrow(() -> new AccountNotFoundException("Transaction not found with reference: " + referenceNumber));
    }
    
    private void validateTransactionRequest(TransactionRequest request, boolean requireFromAccount, boolean requireToAccount) {
        if (request == null) {
            throw new InvalidAccountOperationException("Transaction request cannot be null");
        }
        
        if (requireFromAccount) {
            if (request.fromAccountNumber() == null || request.fromAccountNumber().trim().isEmpty()) {
                throw new InvalidAccountOperationException("Source account number is required for this transaction type");
            }
        }
        
        if (requireToAccount) {
            if (request.toAccountNumber() == null || request.toAccountNumber().trim().isEmpty()) {
                throw new InvalidAccountOperationException("Destination account number is required for this transaction type");
            }
        }
        
        if (request.amount() == null) {
            throw new InvalidAccountOperationException("Transaction amount is required");
        }
        
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAccountOperationException("Transaction amount must be greater than zero");
        }
        
        if (request.currency() == null || request.currency().trim().isEmpty()) {
            throw new InvalidAccountOperationException("Currency is required");
        }
        
        if (request.transactionType() == null) {
            throw new InvalidAccountOperationException("Transaction type is required");
        }
    }
    
    private Transaction createTransaction(Account fromAccount, Account toAccount, TransactionRequest request) {
        return Transaction.builder()
            .transactionReference(UUID.randomUUID().toString())
            .fromAccount(fromAccount)
            .toAccount(toAccount)
            .amount(request.amount())
            .currency(request.currency())
            .transactionType(request.transactionType())
            .status(Transaction.TransactionStatus.PENDING)
            .description(request.description())
            .metadata(request.metadata())
            .build();
    }
}
