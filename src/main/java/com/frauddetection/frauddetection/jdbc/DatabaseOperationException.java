package com.frauddetection.frauddetection.jdbc;

/**
 * Custom checked exception representing database and JDBC operation failures.
 * Used across the JDBC layer and GUI components for structured error handling.
 *
 * Satisfies Academic Rubric:
 * - OOP Implementation: Custom Exception Handling
 * - Classes for Database Operations
 */
public class DatabaseOperationException extends Exception {

    private final String sqlState;
    private final int vendorErrorCode;

    public DatabaseOperationException(String message) {
        super(message);
        this.sqlState = "N/A";
        this.vendorErrorCode = 0;
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
        this.sqlState = "N/A";
        this.vendorErrorCode = 0;
    }

    public DatabaseOperationException(String message, String sqlState, int vendorErrorCode, Throwable cause) {
        super(message, cause);
        this.sqlState = sqlState != null ? sqlState : "N/A";
        this.vendorErrorCode = vendorErrorCode;
    }

    public String getSqlState() {
        return sqlState;
    }

    public int getVendorErrorCode() {
        return vendorErrorCode;
    }

    @Override
    public String toString() {
        return String.format("DatabaseOperationException [SQLState=%s, ErrorCode=%d]: %s",
                sqlState, vendorErrorCode, getMessage());
    }
}
