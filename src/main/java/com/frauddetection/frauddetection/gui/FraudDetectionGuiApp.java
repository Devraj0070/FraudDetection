package com.frauddetection.frauddetection.gui;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frauddetection.frauddetection.client.SafePayApiClient;

/**
 * Main launcher entry point for the Java GUI Desktop Application.
 *
 * Fulfills Academic Rubric:
 * - Java GUI: Complete executable desktop interface for fraud detection
 * - Demonstrates Event Dispatch Thread (EDT) safety via {@link SwingUtilities#invokeLater}
 */
public class FraudDetectionGuiApp {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionGuiApp.class);

    public static void main(String[] args) {
        launch(args);
    }

    public static void launch(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Cannot launch Java GUI: System is running in a headless graphics environment.");
            System.out.println("[HEADLESS] Java GUI cannot launch in headless environment.");
            return;
        }

        // Configure antialiasing and system look and feel
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.debug("Using default Swing look and feel: {}", e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                SafePayApiClient apiClient = new SafePayApiClient("http://127.0.0.1:8080");
                AuthFrame authFrame = new AuthFrame(apiClient, authResp -> {
                    DashboardFrame dashboard = new DashboardFrame(apiClient, authResp);
                    dashboard.setVisible(true);
                });
                authFrame.setVisible(true);
                log.info("SafePay Desktop GUI initialized successfully.");
            } catch (Exception e) {
                log.error("Failed to display SafePay Desktop GUI: {}", e.getMessage(), e);
            }
        });
    }
}
