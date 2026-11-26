package com.frauddetection.frauddetection.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class FraudDetectionGuiAppTest {

    @Test
    @DisplayName("FraudDetectionGuiApp launches gracefully without crashing in headless environment")
    void testLaunchHeadless() {
        assertDoesNotThrow(() -> FraudDetectionGuiApp.launch(new String[0]));
    }

    @Test
    @DisplayName("FraudDetectionGuiApp main entrypoint executes safely")
    void testMainMethod() {
        assertDoesNotThrow(() -> FraudDetectionGuiApp.main(new String[0]));
    }
}
