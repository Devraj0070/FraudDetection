package com.frauddetection.frauddetection.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.User;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    Optional<Account> findTopByUserOrderByIdAsc(User user);

    Optional<Account> findFirstByUserOrderByIdAsc(User user);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.user = :user ORDER BY a.id ASC")
    List<Account> findByUserWithLock(@Param("user") User user);

    @Query("SELECT a.id FROM Account a WHERE a.user = :user ORDER BY a.id ASC")
    List<Long> findAccountIdsByUser(@Param("user") User user);

    @Query("SELECT a.id FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Long> findAccountIdByAccountNumber(@Param("accountNumber") String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id IN (:id1, :id2) ORDER BY a.id ASC")
    List<Account> findAccountsWithLockInOrder(@Param("id1") Long id1, @Param("id2") Long id2);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance - :amount WHERE a.id = :id AND a.balance >= :amount")
    int debitAccount(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.id = :id")
    int creditAccount(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
