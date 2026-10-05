package com.frauddetection.frauddetection.ml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;
import com.frauddetection.frauddetection.ml.training.ModelTrainer;

import weka.classifiers.trees.RandomForest;

public class ModelTrainerTest {

    @Test
    void shouldTrainRandomForestModel() throws Exception {

        DatasetLoader datasetLoader = new DatasetLoader();

        ModelTrainer modelTrainer = new ModelTrainer(datasetLoader);

        String filePath = "ml/dataset/fraud_transactions.csv";

        RandomForest model = modelTrainer.trainModel(filePath);

        assertNotNull(model);
    }
}