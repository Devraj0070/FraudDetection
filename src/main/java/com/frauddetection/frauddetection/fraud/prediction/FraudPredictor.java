package com.frauddetection.frauddetection.fraud.prediction;

import com.frauddetection.frauddetection.fraud.feature.FeatureVector;

public interface FraudPredictor {

    String getModelName();

    FraudPredictionResult predict(FeatureVector features);
}
