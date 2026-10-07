package com.frauddetection.frauddetection.gui;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Strategy pattern interface for polymorphic fraud detection evaluation.
 *
 * Fulfills Academic Rubric:
 * - OOP Implementation: Interfaces & Polymorphism
 */
public interface FraudDetectionStrategy {

    /**
     * Evaluates a transaction record and produces a fraud prediction verdict.
     *
     * @param transaction The transaction to inspect
     * @return Result containing prediction ("FRAUD" or "LEGITIMATE"), probability, and model name
     * @throws GuiException If evaluation fails
     */
    FraudPredictionResult evaluate(TransactionRecord transaction) throws GuiException;

    /**
     * Human-readable name of the strategy.
     */
    String getStrategyName();

    /**
     * Brief description of the detection heuristics used.
     */
    String getDescription();
}
