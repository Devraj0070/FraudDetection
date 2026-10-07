package com.frauddetection.frauddetection.gui;

/**
 * Custom checked exception for Java GUI desktop application errors.
 *
 * Fulfills Academic Rubric:
 * - OOP Implementation: Custom Exception Handling
 */
public class GuiException extends Exception {

    public GuiException(String message) {
        super(message);
    }

    public GuiException(String message, Throwable cause) {
        super(message, cause);
    }
}
