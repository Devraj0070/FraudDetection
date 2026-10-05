package com.frauddetection.frauddetection.fraud.feature;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Component
public class TransactionFeatureExtractor {

    private final TransactionRepository transactionRepository;

    public TransactionFeatureExtractor(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public FeatureVector extractFeatures(Transaction transaction) {

        FeatureVector features = new FeatureVector();

        double amount = transaction.getAmount().doubleValue();

        LocalDateTime transactionTime = transaction.getTransactionTime();

        features.setAmount(amount);

        features.setTransactionHour(
                transactionTime.getHour()
        );

        features.setTransactionDayOfWeek(
                transactionTime.getDayOfWeek().getValue()
        );

        List<Transaction> previousTransactions =
                transactionRepository
                        .findByAccountOrderByTransactionTimeDesc(
                                transaction.getAccount()
                        );

        int recentTransactionCount = 0;
        double totalAmount = 0;
        int transactionCount = 0;

        LocalDateTime tenMinutesAgo =
                transactionTime.minusMinutes(10);

        for (Transaction previousTransaction : previousTransactions) {

            if (previousTransaction.getId() != null
                    && transaction.getId() != null
                    && previousTransaction.getId().equals(transaction.getId())) {
                continue;
            }

            if (previousTransaction.getTransactionTime() != null
                    && previousTransaction.getTransactionTime()
                            .isAfter(tenMinutesAgo)
                    && previousTransaction.getTransactionTime()
                            .isBefore(transactionTime)) {

                recentTransactionCount++;
            }

            if (previousTransaction.getAmount() != null) {

                totalAmount +=
                        previousTransaction.getAmount().doubleValue();

                transactionCount++;
            }
        }

        double averageAmount = 0;

        if (transactionCount > 0) {
            averageAmount = totalAmount / transactionCount;
        }

        double amountDifferenceFromAverage =
                Math.abs(amount - averageAmount);

        features.setRecentTransactionCount(
                recentTransactionCount
        );

        features.setAverageTransactionAmount(
                averageAmount
        );

        features.setAmountDifferenceFromAverage(
                amountDifferenceFromAverage
        );

        return features;
    }
}