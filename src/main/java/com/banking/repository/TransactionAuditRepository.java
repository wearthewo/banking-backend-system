package com.banking.repository;

import com.banking.entity.TransactionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionAuditRepository extends JpaRepository<TransactionAudit, Long> {
    boolean existsByTransactionId(String transactionId);
}
