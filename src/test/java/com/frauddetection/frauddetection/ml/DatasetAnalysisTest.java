package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.core.Instances;

public class DatasetAnalysisTest {

    @Test
    void shouldAnalyzeFraudDataset() throws Exception {

        DatasetLoader datasetLoader = new DatasetLoader();

       String filePath = "ml/dataset/fraud_transactions_generated.csv";

        Instances dataset = datasetLoader.loadDataset(filePath);

        int legitimate = 0;
        int fraud = 0;

        double legitimateAmount = 0;
        double fraudAmount = 0;

        double legitimateRecentCount = 0;
        double fraudRecentCount = 0;

        double legitimateDifference = 0;
        double fraudDifference = 0;

        for (int i = 0; i < dataset.numInstances(); i++) {

            if (dataset.instance(i).classValue() == 0) {

                legitimate++;

                legitimateAmount +=
                        dataset.instance(i).value(0);

                legitimateRecentCount +=
                        dataset.instance(i).value(3);

                legitimateDifference +=
                        dataset.instance(i).value(5);

            } else {

                fraud++;

                fraudAmount +=
                        dataset.instance(i).value(0);

                fraudRecentCount +=
                        dataset.instance(i).value(3);

                fraudDifference +=
                        dataset.instance(i).value(5);
            }
        }

        System.out.println("====================================");
        System.out.println("DATASET ANALYSIS");
        System.out.println("====================================");

        System.out.println("Legitimate transactions : " + legitimate);
        System.out.println("Fraud transactions      : " + fraud);

        System.out.println();

        System.out.println("Average legitimate amount : "
                + (legitimateAmount / legitimate));

        System.out.println("Average fraud amount      : "
                + (fraudAmount / fraud));

        System.out.println();

        System.out.println("Average legitimate recent transactions : "
                + (legitimateRecentCount / legitimate));

        System.out.println("Average fraud recent transactions      : "
                + (fraudRecentCount / fraud));

        System.out.println();

        System.out.println("Average legitimate amount difference : "
                + (legitimateDifference / legitimate));

        System.out.println("Average fraud amount difference      : "
                + (fraudDifference / fraud));

        System.out.println("====================================");
    }
}