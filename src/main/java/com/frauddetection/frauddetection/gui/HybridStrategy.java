package com.frauddetection.frauddetection.gui;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Hybrid ensemble strategy combining both Rule-Based and Machine Learning heuristics.
 *
 * Demonstrates:
 * - OOP Implementation: Polymorphism & Strategy pattern
 */
public class HybridStrategy implements FraudDetectionStrategy {

    private final RuleBasedStrategy ruleStrategy;
    private final MachineLearningStrategy mlStrategy;

    public HybridStrategy() {
        this.ruleStrategy = new RuleBasedStrategy();
        this.mlStrategy = new MachineLearningStrategy();
    }

    @Override
    public FraudPredictionResult evaluate(TransactionRecord transaction) throws GuiException {
        if (transaction == null) {
            throw new ValidationException("Transaction", "Cannot evaluate null transaction");
        }

        FraudPredictionResult ruleResult = ruleStrategy.evaluate(transaction);
        FraudPredictionResult mlResult = mlStrategy.evaluate(transaction);

        double ruleProb = ruleResult.getProbability();
        double mlProb = mlResult.getProbability();

        // 40% Weight Rule Engine, 60% Weight ML Model
        double ensembleProbability = (ruleProb * 0.40) + (mlProb * 0.60);

        // Circuit breaker: If either flagged severe fraud with > 0.85 confidence
        if (ruleProb >= 0.85 || mlProb >= 0.85) {
            ensembleProbability = Math.max(ensembleProbability, 0.90);
        }

        String prediction = ensembleProbability >= 0.50 ? "FRAUD" : "LEGITIMATE";

        return new FraudPredictionResult(
                prediction,
                ensembleProbability,
                "Hybrid AI + Heuristic Ensemble"
        );
    }

    @Override
    public String getStrategyName() {
        return "Hybrid Ensemble (Rules + AI)";
    }

    @Override
    public String getDescription() {
        return "Combines rule-based behavioral boundaries with Random Forest ML predictions for maximum recall.";
    }

    @Override
    public String toString() {
        return getStrategyName();
    }
}
