package com.frauddetection.frauddetection.service;

import org.springframework.stereotype.Service;

import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}