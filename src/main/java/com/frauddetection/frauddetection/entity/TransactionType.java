package com.frauddetection.frauddetection.entity;

public enum TransactionType {

    PAYMENT,
    TRANSFER,
    CASH_OUT,
    DEBIT,
    CASH_IN;

    /**
     * Returns the canonical string value expected by the ML model.
     */
    public String toModelValue() {
        return this.name();
    }

    /**
     * Parses a string to a TransactionType, case-insensitive.
     * Returns null if the value is not one of the five model categories.
     */
    public static TransactionType fromModelValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TransactionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns all valid model values as an unmodifiable list.
     */
    public static java.util.List<String> getAllModelValues() {
        return java.util.Arrays.stream(values())
                .map(TransactionType::toModelValue)
                .toList();
    }
}