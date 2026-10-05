package com.frauddetection.frauddetection.ml.evaluation;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

@Component
public class ModelEvaluator {

    private final DatasetLoader datasetLoader;

    public ModelEvaluator(DatasetLoader datasetLoader) {
        this.datasetLoader = datasetLoader;
    }

    public Evaluation evaluateModel(
            RandomForest model,
            String filePath) throws Exception {

        // Load the dataset.
        Instances dataset = datasetLoader.loadDataset(filePath);

        // Evaluate the trained model using 10-fold cross-validation.
        Evaluation evaluation = new Evaluation(dataset);

        evaluation.crossValidateModel(
                model,
                dataset,
                10,
                new java.util.Random(42)
        );

        return evaluation;
    }
}