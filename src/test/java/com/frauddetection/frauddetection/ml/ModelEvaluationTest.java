package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;
import com.frauddetection.frauddetection.ml.training.ModelTrainer;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

public class ModelEvaluationTest {

    @Test
    void shouldPrintModelEvaluationResults() throws Exception {

        String trainingFile = "ml/dataset/paysim_train.csv";
        String testingFile = "ml/dataset/paysim_realistic_test.csv";

        DatasetLoader datasetLoader =
                new DatasetLoader();

        ModelTrainer modelTrainer =
                new ModelTrainer(datasetLoader);

        RandomForest model =
                modelTrainer.trainModel(trainingFile);

        Instances testDataset =
                datasetLoader.loadDataset(testingFile);

        Evaluation evaluation =
                new Evaluation(testDataset);

        evaluation.evaluateModel(
                model,
                testDataset
        );

        System.out.println("====================================");
        System.out.println("PAYSIM FRAUD DETECTION EVALUATION");
        System.out.println("====================================");

        System.out.println("Accuracy  : "
                + evaluation.pctCorrect() / 100.0);

        System.out.println("Precision : "
                + evaluation.precision(1));

        System.out.println("Recall    : "
                + evaluation.recall(1));

        System.out.println("F1 Score  : "
                + evaluation.fMeasure(1));

        System.out.println();
        System.out.println("CONFUSION MATRIX");
        System.out.println(evaluation.toMatrixString());

        System.out.println("====================================");
    }
}