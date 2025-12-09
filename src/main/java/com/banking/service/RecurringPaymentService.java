package com.banking.service;

import com.banking.dto.transaction.TransactionRequest;
import com.banking.exception.InsufficientFundsException;
import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.Transaction.Frequency;
import com.banking.model.Transaction.TransactionStatus;
import com.banking.model.Transaction.TransactionType;
import com.banking.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringPaymentService {
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void processRecurringPayments() {
        log.info("Processing recurring payments at {}", LocalDateTime.now());
        
        var recurringTransactions = transactionRepository
            .findRecurringTransactionsDueForProcessing(LocalDateTime.now());
        
        for (var transaction : recurringTransactions) {
            try {
                processSingleRecurringPayment(transaction);
            } catch (Exception e) {
                log.error("Unexpected error processing recurring payment: {}", transaction.getId(), e);
            }
        }
    }
    
    @Transactional
    protected void processSingleRecurringPayment(Transaction transaction) {
        log.info("Processing recurring payment with ID: {}", transaction.getId());
        
        try {
            // Create transaction request
            TransactionRequest request = createTransactionRequest(transaction);
            Long userId = transaction.getFromAccount().getUser().getId();
            
            // Process the transaction
            transactionService.processTransaction(request, userId);
            
            // Update recurring transaction details
            updateRecurringTransaction(transaction);
            
            // Send success notification
            sendSuccessNotification(transaction);
            
        } catch (InsufficientFundsException e) {
            handleInsufficientFunds(transaction, e);
        } catch (Exception e) {
            handleTransactionFailure(transaction, e);
        }
    }
    
    private TransactionRequest createTransactionRequest(Transaction transaction) {
        String fromAccount = transaction.getFromAccount() != null ? 
            transaction.getFromAccount().getAccountNumber() : null;
        String toAccount = transaction.getToAccount() != null ? 
            transaction.getToAccount().getAccountNumber() : null;
            
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("recurringTransactionId", transaction.getId());
        metadata.put("isRecurringPayment", true);
        
        return new TransactionRequest(
            fromAccount,
            toAccount,
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getTransactionType(),
            "Recurring payment: " + (transaction.getDescription() != null ? 
                transaction.getDescription() : ""),
            metadata
        );
    }
    
    private void updateRecurringTransaction(Transaction transaction) {
        transaction.setLastPaymentDate(LocalDateTime.now());
        transaction.setNextPaymentDate(calculateNextPaymentDate(transaction));
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);
    }
    
    private void sendSuccessNotification(Transaction transaction) {
        try {
            Account fromAccount = transaction.getFromAccount();
            Account toAccount = transaction.getToAccount();
            String userEmail = fromAccount.getUser().getEmail();
            String userName = fromAccount.getUser().getFirstName() + " " + fromAccount.getUser().getLastName();
            
            String emailSubject = String.format("Payment Processed - %s", 
                transaction.getTransactionReference());
                
            String emailBody = String.format(
                "Hello %s,\n\n" +
                "Your recurring payment has been processed successfully.\n\n" +
                "Transaction Details:\n" +
                "- Amount: %s %s\n" +
                "- From Account: %s\n" +
                "- To Account: %s\n" +
                "- Date: %s\n" +
                "- Reference: %s\n" +
                (transaction.getDescription() != null ? "- Description: " + transaction.getDescription() + "\n" : "") +
                "\nThank you for using our banking services.\n\n" +
                "Best regards,\nYour Bank",
                userName,
                transaction.getAmount().toPlainString(),
                transaction.getCurrency(),
                fromAccount.getAccountNumber(),
                toAccount != null ? toAccount.getAccountNumber() : "N/A",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                transaction.getTransactionReference()
            );
            
            emailService.sendEmail(userEmail, emailSubject, emailBody);
        } catch (Exception e) {
            log.error("Failed to send success notification for transaction: {}", 
                    transaction.getId(), e);
        }
    }
    
    private void handleInsufficientFunds(Transaction transaction, InsufficientFundsException e) {
        log.warn("Insufficient funds for recurring payment: {}", transaction.getId(), e);
        updateFailedTransaction(transaction, "Insufficient funds: " + e.getMessage());
    }
    
    private void handleTransactionFailure(Transaction transaction, Exception e) {
        log.error("Failed to process recurring payment: {}", transaction.getId(), e);
        updateFailedTransaction(transaction, e.getMessage());
    }
    
    private void updateFailedTransaction(Transaction transaction, String errorMessage) {
        try {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setLastPaymentDate(LocalDateTime.now());
            transactionRepository.save(transaction);
            sendFailureNotification(transaction, errorMessage);
        } catch (Exception ex) {
            log.error("Failed to update failed transaction status: {}", transaction.getId(), ex);
        }
    }
    
    private void sendFailureNotification(Transaction transaction, String errorMessage) {
        try {
            Account fromAccount = transaction.getFromAccount();
            String userEmail = fromAccount.getUser().getEmail();
            String userName = fromAccount.getUser().getFirstName() + " " + fromAccount.getUser().getLastName();
            
            String emailSubject = String.format("Payment Failed - Transaction #%s", 
                transaction.getId());
                
            String emailBody = String.format(
                "Hello %s,\n\n" +
                "We were unable to process your recurring payment.\n\n" +
                "Transaction Details:\n" +
                "- Amount: %s %s\n" +
                "- From Account: %s\n" +
                "- To Account: %s\n" +
                "- Date: %s\n" +
                (transaction.getDescription() != null ? "- Description: " + transaction.getDescription() + "\n" : "") +
                "- Error: %s\n\n" +
                "Please contact customer support if you need assistance.\n\n" +
                "Best regards,\nYour Bank",
                userName,
                transaction.getAmount().toPlainString(),
                transaction.getCurrency(),
                fromAccount.getAccountNumber(),
                transaction.getToAccount() != null ? 
                    transaction.getToAccount().getAccountNumber() : "N/A",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                errorMessage
            );
            
            emailService.sendEmail(userEmail, emailSubject, emailBody);
        } catch (Exception e) {
            log.error("Failed to send failure notification for transaction: {}", 
                    transaction.getId(), e);
        }
    }
    
    private LocalDateTime calculateNextPaymentDate(Transaction transaction) {
        return switch (transaction.getFrequency()) {
            case DAILY -> LocalDateTime.now().plusDays(1);
            case WEEKLY -> LocalDateTime.now().plusWeeks(1);
            case MONTHLY -> LocalDateTime.now().plusMonths(1);
            case QUARTERLY -> LocalDateTime.now().plusMonths(3);
            case YEARLY -> LocalDateTime.now().plusYears(1);
        };
    }
}
