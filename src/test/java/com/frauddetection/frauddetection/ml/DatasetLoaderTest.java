package com.frauddetection.frauddetection.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;

import weka.core.Instances;

public class DatasetLoaderTest {

    @Test
    void shouldLoadFraudDataset() throws Exception {

        DatasetLoader datasetLoader = new DatasetLoader();

        String filePath = "ml/dataset/fraud_transactions.csv";

        Instances dataset = datasetLoader.loadDataset(filePath);

        assertNotNull(dataset);

        assertEquals(1000, dataset.numInstances());

        assertEquals(7, dataset.numAttributes());

        assertEquals("fraud", dataset.classAttribute().name());

        assertEquals(2, dataset.classAttribute().numValues());

        assertEquals("0", dataset.classAttribute().value(0));

        assertEquals("1", dataset.classAttribute().value(1));
    }
}