package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class ModelEvaluationTest {

    @Test
    void shouldEvaluateProductionModelOnTestSet() throws Exception {

        String productionModelPath = "ml/models/fraud_random_forest_final.model";
        String testingFile = "ml/dataset/fraud_test.csv";

        // Load the existing production model
        RandomForest model = (RandomForest) SerializationHelper.read(productionModelPath);

        DatasetLoader datasetLoader = new DatasetLoader();
        Instances testDataset = datasetLoader.loadDataset(testingFile);

        Evaluation evaluation = new Evaluation(testDataset);
        evaluation.evaluateModel(model, testDataset);

        System.out.println("====================================");
        System.out.println("PRODUCTION MODEL EVALUATION");
        System.out.println("Model: " + productionModelPath);
        System.out.println("Test Set: " + testingFile);
        System.out.println("====================================");

        System.out.println("Accuracy  : " + evaluation.pctCorrect() / 100.0);
        System.out.println("Precision : " + evaluation.precision(1));
        System.out.println("Recall    : " + evaluation.recall(1));
        System.out.println("F1 Score  : " + evaluation.fMeasure(1));

        // Calculate PR-AUC (Area Under Precision-Recall Curve)
        double prAuc = evaluation.areaUnderPRC(1);
        System.out.println("PR-AUC    : " + prAuc);

        System.out.println();
        System.out.println("CONFUSION MATRIX");
        System.out.println(evaluation.toMatrixString());

        System.out.println("====================================");
    }
}