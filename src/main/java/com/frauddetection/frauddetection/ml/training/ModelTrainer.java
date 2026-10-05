package com.frauddetection.frauddetection.ml.training;

import org.springframework.stereotype.Component;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

@Component
public class ModelTrainer {

    private final DatasetLoader datasetLoader;

    public ModelTrainer(DatasetLoader datasetLoader) {
        this.datasetLoader = datasetLoader;
    }

    public RandomForest trainModel(String filePath) throws Exception {

        // Load the training dataset.
        Instances dataset = datasetLoader.loadDataset(filePath);

        // Create the Random Forest model.
        RandomForest randomForest = new RandomForest();

        // Use 100 decision trees.
        randomForest.setNumIterations(100);

        // Train the model using our dataset.
        randomForest.buildClassifier(dataset);

        return randomForest;
    }
}