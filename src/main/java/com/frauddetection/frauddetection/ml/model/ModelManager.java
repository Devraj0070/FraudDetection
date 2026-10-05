package com.frauddetection.frauddetection.ml.model;

import java.io.File;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

@Component
public class ModelManager {

    private static final String FINAL_MODEL_PATH =
            "ml/models/fraud_random_forest_final.model";

    private RandomForest model;

    @PostConstruct
    public void loadFinalModel() {
        try {
            loadModel(FINAL_MODEL_PATH);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to load fraud detection model from " + FINAL_MODEL_PATH,
                    exception
            );
        }
    }

    public void loadModel(String modelPath) throws Exception {

        File modelFile = new File(modelPath);

        model = (RandomForest) SerializationHelper.read(modelPath);

        System.out.println(
                "Fraud detection model loaded: "
                        + modelFile.getPath()
        );
    }

    public RandomForest getModel() {

        return model;
    }
}
