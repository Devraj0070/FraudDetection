package com.frauddetection.frauddetection.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountOrderByTransactionTimeDesc(Account account);

    List<Transaction> findTop5ByAccountOrderByTransactionTimeDesc(Account account);

    @Query("SELECT t FROM Transaction t WHERE t.account = :account OR t.receiverAccount = :account ORDER BY t.transactionTime DESC")
    List<Transaction> findAllByAccountOrReceiverAccountOrderByTransactionTimeDesc(@Param("account") Account account);

    long countByAccount(Account account);

    long countByAccountAndStatus(Account account, String status);

    @Query("SELECT COUNT(DISTINCT t) FROM Transaction t WHERE t.account = :account OR t.receiverAccount = :account")
    long countByAccountOrReceiverAccount(@Param("account") Account account);

    @Query("SELECT COUNT(DISTINCT t) FROM Transaction t WHERE (t.account = :account OR t.receiverAccount = :account) AND t.status = :status")
    long countByAccountOrReceiverAccountAndStatus(@Param("account") Account account, @Param("status") String status);

    List<Transaction> findByStatusOrderByTransactionTimeDesc(String status);

    long countByStatus(String status);

    List<Transaction> findTop5ByOrderByTransactionTimeDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdWithLock(@Param("id") Long id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t WHERE t.account = :account AND t.amount = :amount AND t.transactionType = :type AND ((:receiverAccount IS NULL AND t.receiverAccount IS NULL) OR t.receiverAccount = :receiverAccount) AND t.transactionTime >= :cutoffTime AND t.status IN ('APPROVED', 'PENDING_REVIEW')")
    List<Transaction> findRecentDuplicates(
            @Param("account") Account account,
            @Param("amount") BigDecimal amount,
            @Param("type") TransactionType type,
            @Param("receiverAccount") Account receiverAccount,
            @Param("cutoffTime") LocalDateTime cutoffTime);
}
