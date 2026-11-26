package com.frauddetection.frauddetection.ml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import com.frauddetection.frauddetection.ml.model.ModelManager;

import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

/**
 * Verification test suite for ModelManager reliability:
 * 1. Default classpath model loading.
 * 2. External filesystem model loading (via path and file: URL).
 * 3. Diagnostics and error handling for missing/invalid paths.
 * 4. Validation rejecting non-RandomForest serialized objects.
 * 5. Type verification confirming weka.classifiers.trees.RandomForest.
 * 6. Clear diagnostic logging and unavailable-state retention on load failure.
 */
class ModelManagerTest {

    private static final String CLASSPATH_MODEL = "classpath:models/fraud_random_forest_final.model";
    private static final String FILESYSTEM_MODEL = "ml/models/fraud_random_forest_final.model";

    @Test
    @DisplayName("Should successfully load production Random Forest model from classpath")
    void shouldLoadDefaultModelFromClasspath() throws Exception {
        ModelManager manager = new ModelManager(new DefaultResourceLoader(), CLASSPATH_MODEL);
        manager.loadFinalModel();

        assertTrue(manager.isModelLoaded(), "Model should be marked as loaded");
        assertNotNull(manager.getModel(), "Model reference must be non-null");
        assertTrue(manager.getModel() instanceof RandomForest, "Model must be an instance of RandomForest");
        assertEquals("RandomForest", manager.getModel().getClass().getSimpleName());
    }

    @Test
    @DisplayName("Should successfully load model from external filesystem path")
    void shouldLoadExternalModelFromFileSystem() throws Exception {
        File modelFile = new File(FILESYSTEM_MODEL);
        assertTrue(modelFile.exists(), "Original model file must exist on filesystem for this test");

        // Test with raw absolute filesystem path
        ModelManager manager = new ModelManager(new DefaultResourceLoader(), modelFile.getAbsolutePath());
        manager.loadFinalModel();

        assertTrue(manager.isModelLoaded(), "Model should load from absolute filesystem path");
        assertNotNull(manager.getModel(), "Model reference must not be null");
        assertTrue(manager.getModel() instanceof RandomForest);

        // Test explicit loadModel with file: URI
        ModelManager fileUriManager = new ModelManager();
        fileUriManager.loadModel("file:" + modelFile.getAbsolutePath());
        assertTrue(fileUriManager.isModelLoaded(), "Model should load via file: URI");
    }

    @Test
    @DisplayName("Should fail cleanly and report error when model path is invalid or missing")
    void shouldFailWhenModelPathIsMissingOrInvalid() {
        ModelManager manager = new ModelManager(new DefaultResourceLoader(), "classpath:models/non_existent_model.model");

        // Calling loadFinalModel should catch exception, log error, and leave model null
        manager.loadFinalModel();
        assertFalse(manager.isModelLoaded(), "Model must not be marked as loaded on missing resource");
        assertNull(manager.getModel(), "Model reference must remain null");

        // Calling loadModel directly should throw IllegalStateException with detailed message
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.loadModel("classpath:models/non_existent_model.model"));
        assertTrue(ex.getMessage().contains("Model resource not found"),
                "Exception message must identify missing resource: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should strictly validate and reject serialized object if not a RandomForest")
    void shouldRejectInvalidSerializedClassifierType() throws Exception {
        File tempFile = File.createTempFile("invalid_model_", ".model");
        tempFile.deleteOnExit();

        // Write a serialized String instead of a RandomForest classifier
        SerializationHelper.write(tempFile.getAbsolutePath(), "Invalid Model String Payload");

        ModelManager manager = new ModelManager();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> manager.loadModel(tempFile.getAbsolutePath()));

        assertTrue(ex.getMessage().contains("Loaded model object is not a weka.classifiers.trees.RandomForest instance"),
                "Exception message must indicate classifier type mismatch: " + ex.getMessage());
        assertFalse(manager.isModelLoaded(), "ModelManager must not accept invalid classifier object");
        assertNull(manager.getModel(), "Model reference must remain null");
    }

    @Test
    @DisplayName("Should confirm loaded classifier is an instance of weka.classifiers.trees.RandomForest")
    void shouldConfirmClassifierIsWekaRandomForest() throws Exception {
        ModelManager manager = new ModelManager();
        manager.loadModel(CLASSPATH_MODEL);

        RandomForest classifier = manager.getModel();
        assertNotNull(classifier, "Classifier must not be null");
        assertEquals("weka.classifiers.trees.RandomForest", classifier.getClass().getName());
        assertEquals(100, classifier.getNumIterations(), "Production model trained with 100 iterations");
    }

    @Test
    @DisplayName("Should reject blank or null model location")
    void shouldRejectBlankOrNullLocation() {
        ModelManager manager = new ModelManager();
        assertThrows(IllegalArgumentException.class, () -> manager.loadModel(null));
        assertThrows(IllegalArgumentException.class, () -> manager.loadModel("   "));
    }

    @Test
    @DisplayName("Should support external model path override matching ML_MODEL_PATH")
    void shouldSupportExternalModelPathOverride() {
        String customExternalPath = new File(FILESYSTEM_MODEL).getAbsolutePath();
        ModelManager manager = new ModelManager(new DefaultResourceLoader(), customExternalPath);
        assertEquals(customExternalPath, manager.getConfiguredModelPath());
        manager.loadFinalModel();
        assertTrue(manager.isModelLoaded(), "Configured model override should load successfully");
        assertNotNull(manager.getModel());
    }
}
