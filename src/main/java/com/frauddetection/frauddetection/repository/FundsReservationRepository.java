package com.frauddetection.frauddetection.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FundsReservation;
import com.frauddetection.frauddetection.entity.FundsReservation.ReservationStatus;
import com.frauddetection.frauddetection.entity.Transaction;

@Repository
public interface FundsReservationRepository extends JpaRepository<FundsReservation, Long> {

    Optional<FundsReservation> findByTransaction(Transaction transaction);

    Optional<FundsReservation> findByTransactionId(Long transactionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FundsReservation r WHERE r.transaction = :transaction")
    Optional<FundsReservation> findByTransactionWithLock(@Param("transaction") Transaction transaction);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FundsReservation r WHERE r.transaction.id = :transactionId")
    Optional<FundsReservation> findByTransactionIdWithLock(@Param("transactionId") Long transactionId);

    @Query("SELECT SUM(r.amount) FROM FundsReservation r WHERE r.account = :account AND r.status = com.frauddetection.frauddetection.entity.FundsReservation.ReservationStatus.ACTIVE")
    BigDecimal sumActiveReservationsByAccount(@Param("account") Account account);

    @Query("SELECT SUM(r.amount) FROM FundsReservation r WHERE r.account.id = :accountId AND r.status = com.frauddetection.frauddetection.entity.FundsReservation.ReservationStatus.ACTIVE")
    BigDecimal sumActiveReservationsByAccountId(@Param("accountId") Long accountId);

    List<FundsReservation> findByAccountAndStatus(Account account, ReservationStatus status);

    List<FundsReservation> findByAccount(Account account);
}
