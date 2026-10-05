package com.frauddetection.frauddetection.ml.training;

import java.io.File;

import org.springframework.stereotype.Component;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;

@Component
public class ModelTrainer {

    private final DatasetLoader datasetLoader;

    public ModelTrainer(DatasetLoader datasetLoader) {
        this.datasetLoader = datasetLoader;
    }

    public RandomForest trainModel(String filePath) throws Exception {

        Instances dataset =
                datasetLoader.loadDataset(filePath);

        RandomForest randomForest =
                new RandomForest();

        randomForest.setNumIterations(100);

        randomForest.buildClassifier(dataset);

        return randomForest;
    }

    public void trainAndSaveModel(
            String datasetPath,
            String modelPath) throws Exception {

        RandomForest model =
                trainModel(datasetPath);

        File modelFile =
                new File(modelPath);

        modelFile.getParentFile().mkdirs();

        SerializationHelper.write(
                modelPath,
                model
        );

        System.out.println(
                "Random Forest model saved to: "
                        + modelPath
        );
    }
}