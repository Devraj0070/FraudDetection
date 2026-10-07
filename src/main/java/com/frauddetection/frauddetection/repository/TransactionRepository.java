package com.frauddetection.frauddetection.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountOrderByTransactionTimeDesc(Account account);

    List<Transaction> findTop5ByAccountOrderByTransactionTimeDesc(Account account);

    long countByAccount(Account account);

    long countByAccountAndStatus(Account account, String status);
}