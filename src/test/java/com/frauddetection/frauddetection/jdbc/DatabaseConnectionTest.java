package com.frauddetection.frauddetection.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    void shouldLoadConfigurationSuccessfully() {
        DatabaseConnection.loadConfiguration();
        assertNotNull(DatabaseConnection.getJdbcUrl());
        assertNotNull(DatabaseConnection.getJdbcUser());
        assertTrue(DatabaseConnection.getJdbcUrl().contains("mysql") ||
                   DatabaseConnection.getJdbcUrl().contains("jdbc"));
    }

    @Test
    void shouldCloseQuietlyWithoutExceptions() {
        assertDoesNotThrow(() -> DatabaseConnection.closeQuietly((AutoCloseable[]) null));
        assertDoesNotThrow(() -> DatabaseConnection.closeQuietly((AutoCloseable) null));
    }

    @Test
    void shouldHandleTestConnectionSafely() {
        // testConnection should return true or false but NEVER throw an unhandled exception
        assertDoesNotThrow(DatabaseConnection::testConnection);
    }
}
