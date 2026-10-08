package com.frauddetection.frauddetection.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@Component
public class FraudRuleEngine {

    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("50000.00");
    private static final BigDecimal EXTREME_AMOUNT_THRESHOLD = new BigDecimal("500000.00");

    private final TransactionRepository transactionRepository;

    public FraudRuleEngine(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Evaluates behavioral anomaly patterns for a transaction.
     * Combines velocity, frequency, and transaction amount patterns rather than
     * treating single large amounts as fraud.
     */
    public int calculateRuleScore(Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return 0;
        }

        int score = 0;
        BigDecimal amount = transaction.getAmount();
        Account account = transaction.getAccount();

        if (account != null) {
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            List<Transaction> recentTransactions =
                    transactionRepository.findByAccountOrderByTransactionTimeDesc(account);

            int recentCount = 0;
            int recentLargeCount = 0;

            for (Transaction previousTransaction : recentTransactions) {
                if (previousTransaction.getId() != null
                        && transaction.getId() != null
                        && previousTransaction.getId().equals(transaction.getId())) {
                    continue;
                }

                if (previousTransaction.getTransactionTime() != null
                        && previousTransaction.getTransactionTime().isAfter(tenMinutesAgo)) {
                    recentCount++;

                    if (previousTransaction.getAmount() != null
                            && previousTransaction.getAmount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                        recentLargeCount++;
                    }
                }
            }

            // Pattern 1: Extreme rapid velocity (>= 10 transactions in 10 minutes) -> automated bot attack
            if (recentCount >= 10) {
                score += 80;
            }
            // Pattern 2: High velocity (>= 5 in 10 min) coupled with a large amount (>= ₹50,000)
            else if (recentCount >= 5 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                score += 80;
            }
            // Pattern 3: Repeated large transactions (>= 2 prior large txns in 10 min + current large txn)
            else if (recentLargeCount >= 2 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                score += 80;
            }
            // Pattern 4: Extreme outlier amount spike (>= ₹500,000) with prior rapid activity
            else if (amount.compareTo(EXTREME_AMOUNT_THRESHOLD) >= 0 && recentCount >= 2) {
                score += 80;
            }
            // Moderate indicators
            else if (recentCount >= 5) {
                score += 40;
            } else if (recentLargeCount >= 1 && amount.compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
                score += 30;
            }
        }

        return Math.min(score, 100);
    }

    public boolean isSuspicious(Transaction transaction) {
        return calculateRuleScore(transaction) >= 80;
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