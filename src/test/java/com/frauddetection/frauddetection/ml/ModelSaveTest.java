package com.frauddetection.frauddetection.ml;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.ml.training.DatasetLoader;
import com.frauddetection.frauddetection.ml.training.ModelTrainer;

public class ModelSaveTest {

    @Test
    void shouldTrainAndSaveModel() throws Exception {

        String datasetPath =
                "ml/dataset/paysim_train.csv";

        String modelPath =
                "ml/models/fraud_random_forest.model";

        DatasetLoader datasetLoader =
                new DatasetLoader();

        ModelTrainer modelTrainer =
                new ModelTrainer(datasetLoader);

        modelTrainer.trainAndSaveModel(
                datasetPath,
                modelPath
        );
    }
}