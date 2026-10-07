package com.frauddetection.frauddetection.gui;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

import static org.junit.jupiter.api.Assertions.*;

class FraudDetectionStrategyTest {

    @Test
    void shouldEvaluatePolymorphicStrategies() throws Exception {
        TransactionRecord safeTx = new TransactionRecord(
                1L,
                new BigDecimal("250.00"),
                "PAYMENT",
                LocalDateTime.of(2026, 10, 7, 14, 30),
                "PENDING",
                1L,
                "192.168.1.1",
                "Chrome/120.0"
        );

        TransactionRecord suspiciousTx = new TransactionRecord(
                2L,
                new BigDecimal("450000.00"),
                "TRANSFER",
                LocalDateTime.of(2026, 10, 7, 2, 15), // 2 AM night transfer
                "PENDING",
                1L,
                "45.33.32.156",
                "Python-urllib"
        );

        // Demonstrating Runtime Polymorphism through Interface reference
        FraudDetectionStrategy[] strategies = new FraudDetectionStrategy[]{
                new RuleBasedStrategy(),
                new MachineLearningStrategy(),
                new HybridStrategy()
        };

        for (FraudDetectionStrategy strategy : strategies) {
            assertNotNull(strategy.getStrategyName());
            assertNotNull(strategy.getDescription());

            // Safe transaction evaluation
            FraudPredictionResult safeResult = strategy.evaluate(safeTx);
            assertNotNull(safeResult);
            assertTrue(safeResult.getProbability() >= 0.0 && safeResult.getProbability() <= 1.0);

            // Suspicious transaction evaluation
            FraudPredictionResult suspResult = strategy.evaluate(suspiciousTx);
            assertNotNull(suspResult);
            assertTrue(suspResult.getProbability() >= 0.0 && suspResult.getProbability() <= 1.0);
            // Suspicious transaction should have higher risk probability than safe transaction
            assertTrue(suspResult.getProbability() >= safeResult.getProbability(),
                    "Suspicious txn should have higher or equal risk in " + strategy.getStrategyName());
        }
    }

    @Test
    void shouldTriggerFraudOnCriticalRuleThresholds() throws Exception {
        RuleBasedStrategy ruleStrategy = new RuleBasedStrategy();
        TransactionRecord extremeTx = new TransactionRecord(
                99L,
                new BigDecimal("1000000.00"),
                "CASH_OUT",
                LocalDateTime.of(2026, 10, 7, 3, 0),
                "PENDING",
                1L,
                "127.0.0.1",
                "Agent"
        );

        FraudPredictionResult result = ruleStrategy.evaluate(extremeTx);
        assertEquals("FRAUD", result.getPrediction());
        assertTrue(result.getProbability() >= 0.70);
    }
}
