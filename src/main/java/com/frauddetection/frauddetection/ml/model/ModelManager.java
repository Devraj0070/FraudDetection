package com.frauddetection.frauddetection.ml.model;

import java.io.File;
import java.io.InputStream;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

@Component
public class ModelManager {

    private static final Logger log = LoggerFactory.getLogger(ModelManager.class);

    private static final String DEFAULT_MODEL_PATH = "classpath:models/fraud_random_forest_final.model";

    private final ResourceLoader resourceLoader;
    private final String configuredModelPath;
    private RandomForest model;

    @Autowired
    public ModelManager(
            ResourceLoader resourceLoader,
            @Value("${safepay.ml.model-path:" + DEFAULT_MODEL_PATH + "}") String configuredModelPath) {
        this.resourceLoader = resourceLoader != null ? resourceLoader : new DefaultResourceLoader();
        this.configuredModelPath = (configuredModelPath != null && !configuredModelPath.isBlank())
                ? configuredModelPath.trim()
                : DEFAULT_MODEL_PATH;
    }

    public ModelManager() {
        this(new DefaultResourceLoader(), DEFAULT_MODEL_PATH);
    }

    public ModelManager(ResourceLoader resourceLoader) {
        this(resourceLoader, DEFAULT_MODEL_PATH);
    }

    @PostConstruct
    public void loadFinalModel() {
        try {
            loadModel(this.configuredModelPath);
        } catch (Exception exception) {
            this.model = null;
            log.error("Production Random Forest model could not be loaded from '{}'. Random Forest ML prediction is UNAVAILABLE.",
                    this.configuredModelPath, exception);
        }
    }

    public void loadModel(String modelLocation) throws Exception {
        if (modelLocation == null || modelLocation.isBlank()) {
            throw new IllegalArgumentException("Model location cannot be null or blank");
        }

        Resource resource = resolveResource(modelLocation.trim());
        if (!resource.exists()) {
            throw new IllegalStateException("Model resource not found at location: " + modelLocation);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            if (inputStream == null) {
                throw new IllegalStateException("Failed to open input stream for model at: " + modelLocation);
            }
            Object deserialized = SerializationHelper.read(inputStream);
            if (deserialized == null) {
                throw new IllegalStateException("Deserialized model is null from: " + modelLocation);
            }
            if (!(deserialized instanceof RandomForest)) {
                throw new IllegalStateException("Loaded model object is not a weka.classifiers.trees.RandomForest instance. Actual type: "
                        + deserialized.getClass().getName());
            }

            this.model = (RandomForest) deserialized;
            log.info("Fraud detection Random Forest model successfully loaded from: {}", modelLocation);
        }
    }

    private Resource resolveResource(String location) {
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            return resourceLoader.getResource(location);
        }
        File file = new File(location);
        if (file.exists()) {
            return new FileSystemResource(file);
        }
        return resourceLoader.getResource(location);
    }

    public RandomForest getModel() {
        return model;
    }

    public boolean isModelLoaded() {
        return model != null;
    }

    public String getConfiguredModelPath() {
        return configuredModelPath;
    }
}
