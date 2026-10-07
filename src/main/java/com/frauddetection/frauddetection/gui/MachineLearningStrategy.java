package com.frauddetection.frauddetection.gui;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frauddetection.frauddetection.fraud.feature.FeatureVector;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.RandomForestPredictor;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;
import com.frauddetection.frauddetection.ml.model.ModelManager;

/**
 * Machine Learning strategy using the pre-trained Weka Random Forest classifier.
 *
 * Demonstrates:
 * - OOP Implementation: Polymorphism (implements {@link FraudDetectionStrategy})
 * - Machine Learning Integration into GUI
 */
public class MachineLearningStrategy implements FraudDetectionStrategy {

    private static final Logger log = LoggerFactory.getLogger(MachineLearningStrategy.class);
    private static final String MODEL_PATH_1 = "ml/models/fraud_random_forest_final.model";
    private static final String MODEL_PATH_2 = "ml/models/fraud_random_forest.model";

    private RandomForestPredictor predictor;
    private boolean modelLoaded = false;
    private final RuleBasedStrategy fallbackRuleStrategy = new RuleBasedStrategy();

    public MachineLearningStrategy() {
        initPredictor();
    }

    private void initPredictor() {
        try {
            ModelManager modelManager = new ModelManager();
            if (new File(MODEL_PATH_1).exists()) {
                modelManager.loadModel(MODEL_PATH_1);
                this.predictor = new RandomForestPredictor(modelManager);
                this.modelLoaded = true;
            } else if (new File(MODEL_PATH_2).exists()) {
                modelManager.loadModel(MODEL_PATH_2);
                this.predictor = new RandomForestPredictor(modelManager);
                this.modelLoaded = true;
            } else {
                log.warn("Random Forest model file not found at default paths.");
            }
        } catch (Exception e) {
            log.warn("Failed to initialize ML Predictor in GUI: {}", e.getMessage());
            this.modelLoaded = false;
        }
    }

    @Override
    public FraudPredictionResult evaluate(TransactionRecord transaction) throws GuiException {
        if (transaction == null) {
            throw new ValidationException("Transaction", "Cannot evaluate null transaction");
        }

        if (modelLoaded && predictor != null) {
            try {
                FeatureVector fv = new FeatureVector();
                fv.setAmount(transaction.getAmount() != null ? transaction.getAmount().doubleValue() : 0.0);
                // Standard average balance proxy for standalone evaluation
                fv.setAccountBalance(50000.0);
                fv.setTransactionType(transaction.getTransactionType() != null ?
                        transaction.getTransactionType().toUpperCase() : "PAYMENT");

                return predictor.predict(fv);
            } catch (Exception e) {
                log.warn("ML prediction threw error, using heuristic fallback: {}", e.getMessage());
            }
        }

        // Fallback to Rule-Based Strategy if model is offline
        FraudPredictionResult fallback = fallbackRuleStrategy.evaluate(transaction);
        return new FraudPredictionResult(fallback.getPrediction(), fallback.getProbability(),
                "Random Forest (Heuristic Fallback)");
    }

    @Override
    public String getStrategyName() {
        return "Machine Learning (Random Forest)";
    }

    @Override
    public String getDescription() {
        return "Classifies transactions using a pre-trained Random Forest decision ensemble on transaction features.";
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    @Override
    public String toString() {
        return getStrategyName();
    }
}
