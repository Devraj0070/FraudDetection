package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.evaluation.ModelEvaluator;
import com.frauddetection.frauddetection.ml.training.DatasetLoader;
import com.frauddetection.frauddetection.ml.training.ModelTrainer;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;

public class ModelEvaluationTest {

    @Test
    void shouldPrintModelEvaluationResults() throws Exception {

        String filePath = "ml/dataset/fraud_transactions_generated.csv";

        DatasetLoader datasetLoader = new DatasetLoader();

        ModelTrainer modelTrainer = new ModelTrainer(datasetLoader);

        ModelEvaluator modelEvaluator = new ModelEvaluator(datasetLoader);

        RandomForest model = modelTrainer.trainModel(filePath);

        Evaluation evaluation =
                modelEvaluator.evaluateModel(model, filePath);

        System.out.println("====================================");
        System.out.println("FRAUD DETECTION MODEL EVALUATION");
        System.out.println("====================================");

        System.out.println("Accuracy  : "
                + (evaluation.pctCorrect() / 100.0));

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