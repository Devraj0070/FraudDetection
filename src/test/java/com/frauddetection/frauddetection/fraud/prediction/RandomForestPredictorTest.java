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
