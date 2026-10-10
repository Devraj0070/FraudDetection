package com.frauddetection.frauddetection.fraud.feature;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Component
public class TransactionFeatureExtractor implements FeatureExtractor {

    private final TransactionRepository transactionRepository;

    public TransactionFeatureExtractor(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public FeatureVector extractFeatures(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null for feature extraction");
        }

        FeatureVector features = new FeatureVector();

        double amount = transaction.getAmount() != null
                ? transaction.getAmount().doubleValue()
                : 0.0;

        Account account = transaction.getAccount();
        double balance = (account != null && account.getBalance() != null)
                ? account.getBalance().doubleValue()
                : 0.0;

        LocalDateTime transactionTime = transaction.getTransactionTime() != null
                ? transaction.getTransactionTime()
                : LocalDateTime.now();

        String transactionType = transaction.getTransactionTypeValue() != null
                ? transaction.getTransactionTypeValue()
                : "PAYMENT";

        features.setAmount(amount);
        features.setAccountBalance(balance);
        features.setTransactionType(transactionType);
        features.setTransactionHour(transactionTime.getHour());
        features.setTransactionDayOfWeek(transactionTime.getDayOfWeek().getValue());

        List<Transaction> previousTransactions = (account != null && transactionRepository != null)
                ? transactionRepository.findByAccountOrderByTransactionTimeDesc(account)
                : Collections.emptyList();

        int recentTransactionCount = 0;
        double totalAmount = 0.0;
        int transactionCount = 0;

        LocalDateTime tenMinutesAgo = transactionTime.minusMinutes(10);

        for (Transaction previousTransaction : previousTransactions) {
            if (previousTransaction == null) {
                continue;
            }

            if (previousTransaction.getId() != null
                    && transaction.getId() != null
                    && previousTransaction.getId().equals(transaction.getId())) {
                continue;
            }

            LocalDateTime prevTime = previousTransaction.getTransactionTime();
            if (prevTime != null
                    && prevTime.isAfter(tenMinutesAgo)
                    && (prevTime.isBefore(transactionTime) || prevTime.isEqual(transactionTime))) {
                recentTransactionCount++;
            }

            if (previousTransaction.getAmount() != null) {
                totalAmount += previousTransaction.getAmount().doubleValue();
                transactionCount++;
            }
        }

        double averageAmount = 0.0;
        if (transactionCount > 0) {
            averageAmount = totalAmount / transactionCount;
        }

        double amountDifferenceFromAverage = Math.abs(amount - averageAmount);

        features.setRecentTransactionCount(recentTransactionCount);
        features.setAverageTransactionAmount(averageAmount);
        features.setAmountDifferenceFromAverage(amountDifferenceFromAverage);

        return features;
    }
}
