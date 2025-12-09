package com.banking.repository;

import com.banking.model.Account;
import com.banking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Optional<Account> findByAccountNumber(String accountNumber);
    
    List<Account> findByUser(User user);
    
    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.id = :accountId")
    int updateBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);
    
    List<Account> findByUserId(Long userId);
    
    boolean existsByAccountNumber(String accountNumber);
    
    @Query("SELECT a FROM Account a WHERE a.user.id = :userId AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a.balance FROM Account a WHERE a.id = :accountId")
    Optional<BigDecimal> getBalanceById(@Param("accountId") Long accountId);
    
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.user.id = :userId")
    Optional<Account> findByAccountNumberAndUserId(
        @Param("accountNumber") String accountNumber, 
        @Param("userId") Long userId
    );
    
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE a.id = :id AND a.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
