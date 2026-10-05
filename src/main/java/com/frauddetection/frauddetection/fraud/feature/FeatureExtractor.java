package com.frauddetection.frauddetection.fraud.feature;

import com.frauddetection.frauddetection.entity.Transaction;

public interface FeatureExtractor {

    FeatureVector extractFeatures(Transaction transaction);
}