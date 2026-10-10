package com.frauddetection.frauddetection.ml.model;

import java.io.File;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

@Component
public class ModelManager {

    private static final Logger log = LoggerFactory.getLogger(ModelManager.class);

    private static final String FINAL_MODEL_PATH =
            "ml/models/fraud_random_forest_final.model";

    private RandomForest model;

    @PostConstruct
    public void loadFinalModel() {
        try {
            File modelFile = new File(FINAL_MODEL_PATH);
            if (!modelFile.exists()) {
                log.warn("Production fraud detection model not found at path: {}. Operating in heuristic fallback mode.", FINAL_MODEL_PATH);
                return;
            }
            loadModel(FINAL_MODEL_PATH);
        } catch (Exception exception) {
            log.error("Unable to load fraud detection model from {}. Operating in heuristic fallback mode.", FINAL_MODEL_PATH, exception);
        }
    }

    public void loadModel(String modelPath) throws Exception {
        File modelFile = new File(modelPath);
        model = (RandomForest) SerializationHelper.read(modelPath);
        log.info("Fraud detection model loaded: {}", modelFile.getPath());
    }

    public RandomForest getModel() {
        return model;
    }

    public boolean isModelLoaded() {
        return model != null;
    }
}
