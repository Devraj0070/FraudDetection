package com.frauddetection.frauddetection.fraud.prediction;

import org.springframework.stereotype.Service;

import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.fraud.feature.FeatureExtractor;
import com.frauddetection.frauddetection.fraud.feature.FeatureVector;

@Service
public class FraudPredictionService {

    private final FeatureExtractor featureExtractor;
    private final FraudPredictor fraudPredictor;

    public FraudPredictionService(
            FeatureExtractor featureExtractor,
            FraudPredictor fraudPredictor) {

        this.featureExtractor = featureExtractor;
        this.fraudPredictor = fraudPredictor;
    }

    public FraudPredictionResult predict(Transaction transaction) {

        FeatureVector features =
                featureExtractor.extractFeatures(transaction);

        return fraudPredictor.predict(features);
    }
}
