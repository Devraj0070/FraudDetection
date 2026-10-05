package com.frauddetection.frauddetection.ml.training;

import java.io.File;

import org.springframework.stereotype.Component;

import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

@Component
public class DatasetLoader {

    public Instances loadDataset(String filePath) throws Exception {

        CSVLoader loader = new CSVLoader();

        loader.setSource(new File(filePath));

        Instances dataset = loader.getDataSet();

        // The last column is the fraud column.
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Convert fraud from numeric 0/1 to nominal classes.
        NumericToNominal filter = new NumericToNominal();

       filter.setAttributeIndices(
        Integer.toString(dataset.classIndex() + 1)
        );

        filter.setInputFormat(dataset);

        dataset = Filter.useFilter(dataset, filter);

        // Set the class again after filtering.
        dataset.setClassIndex(dataset.numAttributes() - 1);

        return dataset;
    }
}