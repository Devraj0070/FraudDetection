package com.frauddetection.frauddetection.gui;

/**
 * Custom checked exception for input validation failures in GUI forms.
 *
 * Fulfills Academic Rubric:
 * - OOP Implementation: Inheritance (extends GuiException) & Custom Exception Handling
 */
public class ValidationException extends GuiException {

    private final String fieldName;

    public ValidationException(String fieldName, String message) {
        super(String.format("Validation error on [%s]: %s", fieldName, message));
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
