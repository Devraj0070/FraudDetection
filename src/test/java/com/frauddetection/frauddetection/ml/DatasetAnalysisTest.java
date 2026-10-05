package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.core.Instances;

public class DatasetAnalysisTest {

    @Test
    void shouldAnalyzePaySimTestDataset() throws Exception {

        DatasetLoader datasetLoader = new DatasetLoader();

        String filePath = "ml/dataset/fraud_test.csv";

        Instances dataset = datasetLoader.loadDataset(filePath);

        int legitimate = 0;
        int fraud = 0;

        double legitimateAmount = 0;
        double fraudAmount = 0;

        double legitimateBalance = 0;
        double fraudBalance = 0;

        for (int i = 0; i < dataset.numInstances(); i++) {

            if (dataset.instance(i).classValue() == 0) {

                legitimate++;

                legitimateAmount += dataset.instance(i).value(0);
                legitimateBalance += dataset.instance(i).value(1);

            } else {

                fraud++;

                fraudAmount += dataset.instance(i).value(0);
                fraudBalance += dataset.instance(i).value(1);
            }
        }

        System.out.println("====================================");
        System.out.println("PAYSIM TEST DATASET ANALYSIS");
        System.out.println("File: " + filePath);
        System.out.println("====================================");

        System.out.println("Total instances         : " + dataset.numInstances());
        System.out.println("Legitimate transactions : " + legitimate);
        System.out.println("Fraud transactions      : " + fraud);
        System.out.println("Fraud percentage        : "
                + String.format("%.4f%%", (fraud * 100.0 / dataset.numInstances())));

        System.out.println();

        System.out.println("Average legitimate amount : "
                + String.format("%.2f", legitimateAmount / legitimate));

        System.out.println("Average fraud amount      : "
                + String.format("%.2f", fraudAmount / fraud));

        System.out.println();

        System.out.println("Average legitimate balance : "
                + String.format("%.2f", legitimateBalance / legitimate));

        System.out.println("Average fraud balance      : "
                + String.format("%.2f", fraudBalance / fraud));

        System.out.println("====================================");
    }
}