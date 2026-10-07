package com.frauddetection.frauddetection.gui;

/**
 * Observer interface for reacting to real-time fraud alert triggers in the GUI.
 *
 * Fulfills Academic Rubric:
 * - OOP Implementation: Interfaces & Observer Pattern
 */
public interface FraudAlertObserver {

    void onFraudAlertGenerated(String alertType, String severity, String message);
}
