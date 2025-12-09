package com.banking.repository;

import com.banking.model.Account;
import com.banking.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionReference(String transactionReference);
    
    List<Transaction> findByFromAccountOrToAccount(Account fromAccount, Account toAccount);
    
    Page<Transaction> findByFromAccountIdOrToAccountId(
        Long fromAccountId, 
        Long toAccountId, 
        Pageable pageable
    );
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.fromAccount.id = :accountId " +
           "AND t.status = 'COMPLETED' " +
           "AND t.transactionType = 'WITHDRAWAL' " +
           "AND t.createdAt >= :startDate AND t.createdAt <= :endDate")
    BigDecimal getTotalWithdrawalsByAccountAndDateRange(
        @Param("accountId") Long accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT t FROM Transaction t " +
           "WHERE (t.fromAccount.id = :accountId OR t.toAccount.id = :accountId) " +
           "AND t.status = 'COMPLETED' " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findRecentTransactionsByAccountId(
        @Param("accountId") Long accountId,
        Pageable pageable
    );
    
    @Query("SELECT t FROM Transaction t " +
           "WHERE (t.fromAccount.user.id = :userId OR t.toAccount.user.id = :userId) " +
           "ORDER BY t.createdAt DESC")
    Page<Transaction> findByUserId(
        @Param("userId") Long userId,
        Pageable pageable
    );
    
    /**
     * Find all recurring transactions where the next payment date is before the given date
     * and the transaction is marked as recurring.
     *
     * @param date The date to compare nextPaymentDate against
     * @return List of transactions that are due for processing
     */
    @Query("SELECT t FROM Transaction t WHERE t.recurring = true AND t.nextPaymentDate <= :date")
    List<Transaction> findRecurringTransactionsDueForProcessing(@Param("date") LocalDateTime date);
}
