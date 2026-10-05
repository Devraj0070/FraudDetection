package com.frauddetection.frauddetection.fraud.prediction;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.frauddetection.frauddetection.fraud.feature.FeatureVector;
import com.frauddetection.frauddetection.ml.model.ModelManager;

import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;

@Component
public class RandomForestPredictor implements FraudPredictor {

    private static final List<String> TRANSACTION_TYPE_VALUES = List.of(
            "PAYMENT",
            "TRANSFER",
            "CASH_OUT",
            "DEBIT",
            "CASH_IN"
    );

    private static final List<String> FRAUD_CLASS_VALUES = List.of("0", "1");

    private final ModelManager modelManager;

    public RandomForestPredictor(ModelManager modelManager) {
        this.modelManager = modelManager;
    }

    @Override
    public String getModelName() {
        return "Random Forest";
    }

    @Override
    public FraudPredictionResult predict(FeatureVector features) {
        if (features == null) {
            throw new IllegalArgumentException("Feature vector is required");
        }
        if (!Double.isFinite(features.getAmount())
                || !Double.isFinite(features.getAccountBalance())) {
            throw new IllegalArgumentException(
                    "Amount and account balance must be finite numbers"
            );
        }

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("amount"));
        attributes.add(new Attribute("accountBalance"));
        attributes.add(new Attribute(
                "transactionType",
                new ArrayList<>(TRANSACTION_TYPE_VALUES)
        ));
        attributes.add(new Attribute(
                "isFraud",
                new ArrayList<>(FRAUD_CLASS_VALUES)
        ));

        Instances inputHeader = new Instances("FraudPrediction", attributes, 1);
        inputHeader.setClassIndex(3);

        int transactionTypeIndex = inputHeader.attribute(2)
                .indexOfValue(features.getTransactionType());
        if (transactionTypeIndex < 0) {
            throw new IllegalArgumentException(
                    "Unsupported transactionType: " + features.getTransactionType()
            );
        }

        Instance input = new DenseInstance(1.0, new double[] {
                features.getAmount(),
                features.getAccountBalance(),
                transactionTypeIndex,
                Utils.missingValue()
        });
        inputHeader.add(input);

        try {
            RandomForest model = modelManager.getModel();
            if (model == null) {
                throw new IllegalStateException("Fraud detection model is not loaded");
            }

            Instance modelInput = inputHeader.instance(0);
            double[] distribution = model.distributionForInstance(modelInput);
            int fraudClassIndex = inputHeader.classAttribute().indexOfValue("1");
            if (distribution.length <= fraudClassIndex) {
                throw new IllegalStateException(
                        "Model did not return a probability for the fraud class"
                );
            }

            String prediction = model.classifyInstance(modelInput) == fraudClassIndex
                    ? "FRAUD"
                    : "LEGITIMATE";

            return new FraudPredictionResult(
                    prediction,
                    distribution[fraudClassIndex],
                    getModelName()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Random Forest prediction failed",
                    exception
            );
        }
    }
}
