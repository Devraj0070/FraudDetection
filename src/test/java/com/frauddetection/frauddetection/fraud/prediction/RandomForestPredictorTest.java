package com.frauddetection.frauddetection.fraud.prediction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.fraud.feature.FeatureVector;
import com.frauddetection.frauddetection.ml.model.ModelManager;

class RandomForestPredictorTest {

    private static final String MODEL_FILE =
            "ml/models/fraud_random_forest_final.model";

    @Test
    void shouldReturnPredictionFraudProbabilityAndModelName() throws Exception {
        ModelManager modelManager = new ModelManager();
        modelManager.loadModel(MODEL_FILE);
        RandomForestPredictor predictor = new RandomForestPredictor(modelManager);

        FeatureVector features = new FeatureVector();
        features.setAmount(181.0);
        features.setAccountBalance(181.0);
        features.setTransactionType("TRANSFER");

        FraudPredictionResult result = predictor.predict(features);

        assertTrue("FRAUD".equals(result.getPrediction())
                || "LEGITIMATE".equals(result.getPrediction()));
        assertTrue(Double.isFinite(result.getProbability()));
        assertTrue(result.getProbability() >= 0.0 && result.getProbability() <= 1.0);
        assertEquals("Random Forest", result.getModelName());
    }

    @Test
    void inspectModelPredictionsAcrossScenarios() throws Exception {
        ModelManager modelManager = new ModelManager();
        modelManager.loadModel(MODEL_FILE);
        RandomForestPredictor predictor = new RandomForestPredictor(modelManager);

        double[][] testCases = {
                {50.0, 5000.0, 0},     // PAYMENT 50, bal 5000
                {1000.0, 10000.0, 0},  // PAYMENT 1000, bal 10000
                {500.0, 2000.0, 1},    // TRANSFER 500, bal 2000
                {50000.0, 50000.0, 1}, // TRANSFER 50k, bal 50k
                {100000.0, 100000.0, 1}, // TRANSFER 100k, bal 100k
                {250000.0, 250000.0, 1}, // TRANSFER 250k, bal 250k
                {100000.0, 100000.0, 2}, // CASH_OUT 100k, bal 100k
                {500000.0, 500000.0, 2}, // CASH_OUT 500k, bal 500k
                {1000000.0, 1000000.0, 2}, // CASH_OUT 1M, bal 1M
                {100.0, 500.0, 3},     // DEBIT 100, bal 500
                {500.0, 1000.0, 4},    // CASH_IN 500, bal 1000
        };

        String[] types = {"PAYMENT", "TRANSFER", "CASH_OUT", "DEBIT", "CASH_IN"};

        System.out.println("=== MODEL PREDICTION EXPLORATION ===");
        for (double[] tc : testCases) {
            FeatureVector f = new FeatureVector();
            f.setAmount(tc[0]);
            f.setAccountBalance(tc[1]);
            f.setTransactionType(types[(int) tc[2]]);
            FraudPredictionResult res = predictor.predict(f);
            System.out.printf("Type: %-10s | Amount: %10.2f | Balance: %10.2f => Prediction: %-10s | Fraud Prob: %.4f%n",
                    types[(int) tc[2]], tc[0], tc[1], res.getPrediction(), res.getProbability());
        }
        System.out.println("====================================");
    }

    @Test
    void shouldRejectTransactionTypeOutsideTrainingCategories() throws Exception {
        ModelManager modelManager = new ModelManager();
        modelManager.loadModel(MODEL_FILE);
        RandomForestPredictor predictor = new RandomForestPredictor(modelManager);

        FeatureVector features = new FeatureVector();
        features.setAmount(100.0);
        features.setAccountBalance(1000.0);
        features.setTransactionType("UNSUPPORTED");

        assertThrows(IllegalArgumentException.class, () -> predictor.predict(features));
    }
}
