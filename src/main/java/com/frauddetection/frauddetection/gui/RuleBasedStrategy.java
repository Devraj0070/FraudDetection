package com.frauddetection.frauddetection.gui;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Deterministic rule-based fraud detection strategy implementation.
 *
 * Demonstrates:
 * - OOP Implementation: Polymorphism (implements {@link FraudDetectionStrategy})
 */
public class RuleBasedStrategy implements FraudDetectionStrategy {

    private static final BigDecimal CRITICAL_AMOUNT_THRESHOLD = new BigDecimal("200000.00");
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("100000.00");

    @Override
    public FraudPredictionResult evaluate(TransactionRecord transaction) throws GuiException {
        if (transaction == null) {
            throw new ValidationException("Transaction", "Cannot evaluate null transaction");
        }

        BigDecimal amount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
        String type = transaction.getTransactionType() != null ? transaction.getTransactionType() : "PAYMENT";
        LocalDateTime time = transaction.getTransactionTime() != null ? transaction.getTransactionTime() : LocalDateTime.now();

        double riskScore = 0.05; // Base normal risk

        // 1. Transaction Type Risk Weight
        if ("TRANSFER".equalsIgnoreCase(type) || "CASH_OUT".equalsIgnoreCase(type)) {
            riskScore += 0.25;
        }

        // 2. Amount Magnitude Risk Weight
        if (amount.compareTo(CRITICAL_AMOUNT_THRESHOLD) >= 0) {
            riskScore += 0.50;
        } else if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            riskScore += 0.30;
        }

        // 3. Off-Peak / High-Risk Hours (Midnight - 4 AM)
        int hour = time.getHour();
        if (hour >= 0 && hour <= 4) {
            riskScore += 0.20;
        }

        double probability = Math.min(0.99, Math.max(0.01, riskScore));
        String prediction = probability >= 0.50 ? "FRAUD" : "LEGITIMATE";

        return new FraudPredictionResult(prediction, probability, "Rule-Based Heuristic Engine");
    }

    @Override
    public String getStrategyName() {
        return "Rule-Based Heuristics";
    }

    @Override
    public String getDescription() {
        return "Evaluates transactions using domain thresholds: high amounts, night hours, and risky types.";
    }

    @Override
    public String toString() {
        return getStrategyName();
    }
}
