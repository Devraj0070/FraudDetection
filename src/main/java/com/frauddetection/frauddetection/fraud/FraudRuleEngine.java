package com.frauddetection.frauddetection.fraud;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Component
public class FraudRuleEngine {

    private final TransactionRepository transactionRepository;

    public FraudRuleEngine(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public int calculateRuleScore(Transaction transaction) {

        int score = 0;

        /*
         * Temporary compatibility logic.
         *
         * This is NOT the final fraud detection system.
         * The Random Forest model will replace this later.
         */

        double amount = transaction.getAmount().doubleValue();

        if (amount >= 100000) {
            score += 80;
        } else if (amount >= 50000) {
            score += 40;
        } else if (amount >= 10000) {
            score += 20;
        }

        Account account = transaction.getAccount();

        if (account != null) {

            LocalDateTime tenMinutesAgo =
                    LocalDateTime.now().minusMinutes(10);

            List<Transaction> recentTransactions =
                    transactionRepository
                            .findByAccountOrderByTransactionTimeDesc(account);

            int recentCount = 0;

            for (Transaction previousTransaction : recentTransactions) {

                if (previousTransaction.getTransactionTime() != null
                        && previousTransaction.getTransactionTime().isAfter(tenMinutesAgo)) {

                    recentCount++;
                }
            }

            if (recentCount >= 10) {
                score += 40;
            } else if (recentCount >= 5) {
                score += 25;
            }
        }

        return Math.min(score, 100);
    }

    public RiskLevel getRiskLevel(int score) {

        if (score <= 30) {
            return RiskLevel.LOW;
        } else if (score <= 60) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.HIGH;
        }
    }
}