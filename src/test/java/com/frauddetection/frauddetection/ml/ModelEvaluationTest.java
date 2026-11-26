package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;

public class ModelEvaluationTest {

    @Test
    @DisplayName("Evaluate production model baseline on independent held-out test dataset")
    void shouldEvaluateProductionModelOnTestSet() throws Exception {

        String productionModelPath = "ml/models/fraud_random_forest_final.model";
        String testingFile = "ml/dataset/fraud_test.csv";

        // Load the existing production model
        RandomForest model = (RandomForest) SerializationHelper.read(productionModelPath);

        DatasetLoader datasetLoader = new DatasetLoader();
        Instances testDataset = datasetLoader.loadDataset(testingFile);

        Evaluation evaluation = new Evaluation(testDataset);
        evaluation.evaluateModel(model, testDataset);

        double accuracy = evaluation.pctCorrect() / 100.0;
        double precision = evaluation.precision(1);
        double recall = evaluation.recall(1);
        double f1 = evaluation.fMeasure(1);
        double prAuc = evaluation.areaUnderPRC(1);
        double rocAuc = evaluation.areaUnderROC(1);

        double[][] cm = evaluation.confusionMatrix();
        double tn = cm[0][0];
        double fp = cm[0][1];
        double fn = cm[1][0];
        double tp = cm[1][1];
        double fpr = (fp + tn) > 0 ? fp / (fp + tn) : 0.0;
        double fnr = (fn + tp) > 0 ? fn / (fn + tp) : 0.0;

        System.out.println("==========================================================================");
        System.out.println("PRODUCTION MODEL EVALUATION ON HELD-OUT TEST DATASET (Baseline tau=0.50)");
        System.out.println("Model: " + productionModelPath);
        System.out.println("Test Set: " + testingFile + " (" + testDataset.numInstances() + " instances)");
        System.out.println("==========================================================================");
        System.out.printf("Accuracy  : %.6f (%.2f%%)%n", accuracy, accuracy * 100);
        System.out.printf("Precision : %.6f (%.2f%%)%n", precision, precision * 100);
        System.out.printf("Recall    : %.6f (%.2f%%)%n", recall, recall * 100);
        System.out.printf("F1 Score  : %.6f (%.2f%%)%n", f1, f1 * 100);
        System.out.printf("PR-AUC    : %.6f%n", prAuc);
        System.out.printf("ROC-AUC   : %.6f%n", rocAuc);
        System.out.printf("FPR       : %.6f (%.4f%%)%n", fpr, fpr * 100);
        System.out.printf("FNR       : %.6f (%.4f%%)%n", fnr, fnr * 100);
        System.out.println();
        System.out.println("WEKA EVALUATION CONFUSION MATRIX:");
        System.out.printf("  TN: %-8.0f FP: %-8.0f%n", tn, fp);
        System.out.printf("  FN: %-8.0f TP: %-8.0f%n", fn, tp);
        System.out.println("==========================================================================");
    }

    @Test
    @DisplayName("Calibrate threshold on validation dataset and evaluate on independent test dataset")
    void shouldEvaluateCalibratedThresholdPerformance() throws Exception {
        String productionModelPath = "ml/models/fraud_random_forest_final.model";
        String validationFile = "ml/dataset/fraud_validation.csv";
        String testingFile = "ml/dataset/fraud_test.csv";

        RandomForest model = (RandomForest) SerializationHelper.read(productionModelPath);
        DatasetLoader datasetLoader = new DatasetLoader();

        // Step 1: Validation Dataset (Calibration and Threshold Tuning without Data Leakage)
        System.out.println("=========================================================================================");
        System.out.println("STEP 1: THRESHOLD CALIBRATION ON VALIDATION DATASET (" + validationFile + ")");
        System.out.println("=========================================================================================");
        Instances validationDataset = datasetLoader.loadDataset(validationFile);
        int valInstances = validationDataset.numInstances();
        double[] valProbabilities = new double[valInstances];
        int[] valLabels = new int[valInstances];

        for (int i = 0; i < valInstances; i++) {
            Instance inst = validationDataset.instance(i);
            valLabels[i] = (int) inst.classValue();
            valProbabilities[i] = model.distributionForInstance(inst)[1];
        }

        System.out.printf("%-10s %-8s %-8s %-8s %-8s %-10s %-10s %-10s %-10s %-10s %-10s%n",
                "Threshold", "TP", "FP", "FN", "TN", "Accuracy", "Precision", "Recall", "F1", "FPR", "FNR");
        System.out.println("-----------------------------------------------------------------------------------------");

        double[] thresholds = {0.10, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50};
        double bestValF1 = -1.0;
        double bestValThreshold = 0.50;

        for (double tau : thresholds) {
            int tp = 0, fp = 0, fn = 0, tn = 0;
            for (int i = 0; i < valInstances; i++) {
                boolean predictedFraud = valProbabilities[i] >= tau;
                boolean actualFraud = valLabels[i] == 1;

                if (predictedFraud && actualFraud) tp++;
                else if (predictedFraud && !actualFraud) fp++;
                else if (!predictedFraud && actualFraud) fn++;
                else tn++;
            }

            double accuracy = (double) (tp + tn) / valInstances;
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1 = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0.0;
            double fpr = (fp + tn) > 0 ? (double) fp / (fp + tn) : 0.0;
            double fnr = (fn + tp) > 0 ? (double) fn / (fn + tp) : 0.0;

            if (f1 > bestValF1) {
                bestValF1 = f1;
                bestValThreshold = tau;
            }

            System.out.printf("tau = %4.2f  %-8d %-8d %-8d %-8d %-10.4f %-10.4f %-10.4f %-10.4f %-10.4f %-10.4f%n",
                    tau, tp, fp, fn, tn, accuracy, precision, recall, f1, fpr, fnr);
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("Validation Optimal F1 Threshold: tau = %.2f (F1 = %.4f)%n", bestValThreshold, bestValF1);
        System.out.println("=========================================================================================\n");

        // Step 2: Independent Evaluation on Test Dataset (zero leakage)
        System.out.println("=========================================================================================");
        System.out.println("STEP 2: INDEPENDENT EVALUATION ON TEST DATASET (" + testingFile + ")");
        System.out.println("=========================================================================================");
        Instances testDataset = datasetLoader.loadDataset(testingFile);
        int testInstances = testDataset.numInstances();
        double[] testProbabilities = new double[testInstances];
        int[] testLabels = new int[testInstances];

        for (int i = 0; i < testInstances; i++) {
            Instance inst = testDataset.instance(i);
            testLabels[i] = (int) inst.classValue();
            testProbabilities[i] = model.distributionForInstance(inst)[1];
        }

        System.out.printf("%-10s %-8s %-8s %-8s %-8s %-10s %-10s %-10s %-10s %-10s %-10s%n",
                "Threshold", "TP", "FP", "FN", "TN", "Accuracy", "Precision", "Recall", "F1", "FPR", "FNR");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (double tau : thresholds) {
            int tp = 0, fp = 0, fn = 0, tn = 0;
            for (int i = 0; i < testInstances; i++) {
                boolean predictedFraud = testProbabilities[i] >= tau;
                boolean actualFraud = testLabels[i] == 1;

                if (predictedFraud && actualFraud) tp++;
                else if (predictedFraud && !actualFraud) fp++;
                else if (!predictedFraud && actualFraud) fn++;
                else tn++;
            }

            double accuracy = (double) (tp + tn) / testInstances;
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1 = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0.0;
            double fpr = (fp + tn) > 0 ? (double) fp / (fp + tn) : 0.0;
            double fnr = (fn + tp) > 0 ? (double) fn / (fn + tp) : 0.0;

            String note = "";
            if (tau == 0.25) note = " <-- Production Configured";
            else if (tau == 0.50) note = " <-- Baseline";
            else if (tau == bestValThreshold) note = " <-- Validation Selected";

            System.out.printf("tau = %4.2f  %-8d %-8d %-8d %-8d %-10.4f %-10.4f %-10.4f %-10.4f %-10.4f %-10.4f%s%n",
                    tau, tp, fp, fn, tn, accuracy, precision, recall, f1, fpr, fnr, note);
        }
        System.out.println("=========================================================================================");
    }
}
