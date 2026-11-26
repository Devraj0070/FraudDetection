package com.frauddetection.frauddetection.jdbc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MySqlConnectivityAuditTest {

    private DataSource originalDataSource;

    @BeforeEach
    void setUp() {
        originalDataSource = DatabaseConnection.getSharedDataSource();
        DatabaseConnection.setSharedDataSource(null);
    }

    @AfterEach
    void tearDown() {
        DatabaseConnection.setSharedDataSource(originalDataSource);
    }

    @Test
    @DisplayName("Audit MySQL database configuration handling and missing credential safety")
    void auditMySqlConnectionAndConfiguration() {
        DatabaseConnection.loadConfiguration();
        assertNotNull(DatabaseConnection.getJdbcUrl(), "JDBC URL must be configured");
        assertNotNull(DatabaseConnection.getJdbcUser(), "JDBC User must be configured");
        assertTrue(DatabaseConnection.getJdbcUrl().contains("mysql") || DatabaseConnection.getJdbcUrl().contains("jdbc"));

        // testConnection must execute safely without throwing unhandled exceptions
        assertDoesNotThrow(DatabaseConnection::testConnection);

        String envPwd = System.getenv("DB_PASSWORD");
        if (envPwd == null || envPwd.isBlank()) {
            // Missing credentials must fail safely without using hardcoded fallback passwords
            assertFalse(DatabaseConnection.isPasswordConfigured(),
                    "Password should not be configured when DB_PASSWORD environment variable is absent");
            DatabaseOperationException ex = assertThrows(DatabaseOperationException.class,
                    DatabaseConnection::getConnection,
                    "getConnection must fail clearly when DB_PASSWORD is not supplied");
            assertTrue(ex.getMessage().contains("Database password is not configured"),
                    "Error message should clearly instruct operator to supply DB_PASSWORD");
        } else {
            // When externally supplied DB_PASSWORD is present, test connectivity safely
            assertTrue(DatabaseConnection.isPasswordConfigured(),
                    "Password should be marked configured when DB_PASSWORD is provided");
        }
    }
}