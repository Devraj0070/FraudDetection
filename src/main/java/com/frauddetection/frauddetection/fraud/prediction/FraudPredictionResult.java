package com.frauddetection.frauddetection.fraud.prediction;

public class FraudPredictionResult {

    private String prediction;
    private double probability;
    private String modelName;

    public FraudPredictionResult(
            String prediction,
            double probability,
            String modelName) {

        this.prediction = prediction;
        this.probability = probability;
        this.modelName = modelName;
    }

    public String getPrediction() {
        return prediction;
    }

    public double getProbability() {
        return probability;
    }

    public String getModelName() {
        return modelName;
    }
}