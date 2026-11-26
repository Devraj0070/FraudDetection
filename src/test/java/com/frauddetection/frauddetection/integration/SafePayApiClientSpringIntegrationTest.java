package com.frauddetection.frauddetection.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.frauddetection.frauddetection.client.ApiClientException;
import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AdminAlertDto;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

/**
 * End-to-End wire integration test verifying that the Swing desktop client (SafePayApiClient)
 * communicates reliably with the running Spring Boot server over actual HTTP:
 *
 * 1. CSRF token retrieval from exact endpoint /api/auth/csrf without HTTP 404 errors.
 * 2. Session cookie (JSESSIONID) preservation and persistence across requests.
 * 3. Successful user registration and customer login.
 * 4. Authenticated profile check (/api/auth/me) using session cookies.
 * 5. Authenticated banking transactions transmitting CSRF tokens and idempotency keys.
 * 6. Admin authentication and role-based access control.
 * 7. Session logout and invalidation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SafePayApiClientSpringIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SafePayApiClient client;
    private String serverUrl;

    @BeforeEach
    void setUp() {
        serverUrl = "http://localhost:" + port;
        client = new SafePayApiClient(serverUrl);

        // Ensure admin user exists with ADMIN role
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@safepay.com");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            userRepository.save(admin);
        }
    }

    @Test
    @DisplayName("1. Verify exact CSRF endpoint URL resolves successfully with HTTP 200 (fixes original 404)")
    void verifyCsrfEndpointResolvesSuccessfully() throws Exception {
        String expectedBaseUrl = serverUrl;
        String expectedCsrfUrl = serverUrl + "/api/auth/csrf";

        assertEquals(expectedBaseUrl, client.getBaseUrl(), "Base URL must match configured server URL");

        CsrfResponse csrf = client.fetchCsrfToken();
        assertNotNull(csrf, "CSRF response must not be null");
        assertNotNull(csrf.token(), "CSRF token must not be null");
        assertFalse(csrf.token().isBlank(), "CSRF token must not be blank");
        assertTrue(csrf.headerName() != null && !csrf.headerName().isBlank(), "CSRF header must be populated");
        assertEquals(csrf.token(), client.getCsrfToken(), "Client must cache the CSRF token internally");
        System.out.println(">>> SWING CSRF VERIFICATION: Successfully queried " + expectedCsrfUrl + " -> Token acquired: " + csrf.token().substring(0, 8) + "... Header: " + csrf.headerName());
    }

    @Test
    @DisplayName("2. Verify customer registration, login, and session cookie persistence (/api/auth/me)")
    void verifyRegistrationLoginAndSessionPersistence() throws Exception {
        String testUser = "swing_client_cust_" + System.currentTimeMillis();
        String testEmail = testUser + "@safepay.test";
        String testPassword = "Password@123";

        // Register new customer
        AuthResponse regResp = client.register(testUser, testEmail, testPassword);
        assertNotNull(regResp);
        assertTrue(regResp.success(), "Registration should succeed: " + regResp.message());
        assertEquals(testUser, regResp.username());
        assertTrue(regResp.role().contains("USER") || regResp.role().contains("CUSTOMER"));

        // Login as customer
        AuthResponse loginResp = client.login(testUser, testPassword);
        assertNotNull(loginResp);
        assertTrue(loginResp.success(), "Login should succeed: " + loginResp.message());
        assertTrue(client.isLoggedIn(), "Client state must reflect logged in status");

        // Verify session persistence by calling /api/auth/me
        AuthResponse meResp = client.getCurrentUser();
        assertNotNull(meResp, "Current authenticated user must not be null");
        assertTrue(meResp.success());
        assertEquals(testUser, meResp.username());
        assertTrue(meResp.role().contains("USER") || meResp.role().contains("CUSTOMER"));
        System.out.println(">>> SWING AUTH VERIFICATION: Registered and authenticated as " + testUser + " via session cookie");
    }

    @Test
    @DisplayName("3. Verify Swing client performs banking transaction with CSRF and Idempotency key")
    void verifyBankingTransactionWithCsrfAndIdempotency() throws Exception {
        String testUser = "tx_user_" + System.currentTimeMillis();
        String testEmail = testUser + "@safepay.test";
        String testPassword = "Password@123";

        client.register(testUser, testEmail, testPassword);
        client.login(testUser, testPassword);

        // Verify initial opening dashboard
        DashboardResponse initialDash = client.getDashboardData();
        assertNotNull(initialDash);
        assertNotNull(initialDash.accountNumber());
        assertEquals(new BigDecimal("50000.00"), initialDash.balance());

        // Perform payment with unique idempotency key
        String idempKey = "test-idemp-" + System.currentTimeMillis();
        TransactionRequest txReq = new TransactionRequest(
                new BigDecimal("150.00"),
                "PAYMENT",
                null,
                idempKey
        );

        TransactionResponse txResp = client.createTransaction(txReq);
        assertNotNull(txResp);
        assertTrue(txResp.success(), "Transaction should succeed: " + txResp.message());
        assertEquals("APPROVED", txResp.status());
        assertEquals(new BigDecimal("150.00"), txResp.amount());
        assertEquals(new BigDecimal("49850.00"), txResp.newBalance());

        // Test idempotency: Resubmit identical request with same key
        TransactionResponse duplicateResp = client.createTransaction(txReq);
        assertNotNull(duplicateResp);
        assertTrue(duplicateResp.success(), "Idempotent duplicate must succeed without double debit");
        assertEquals(txResp.transactionId(), duplicateResp.transactionId(), "Duplicate must return stored transaction ID");

        // Verify balance after duplicate is still 49850.00
        DashboardResponse dashAfter = client.getDashboardData();
        assertEquals(new BigDecimal("49850.00"), dashAfter.balance(), "Balance must remain unchanged after duplicate submission");
        System.out.println(">>> SWING BANKING VERIFICATION: Idempotent transaction succeeded and balance preserved at " + dashAfter.balance());
    }

    @Test
    @DisplayName("4. Verify Admin login and access to fraud surveillance endpoints")
    void verifyAdminLoginAndSurveillanceEndpoints() throws Exception {
        // Login as administrator
        AuthResponse adminLogin = client.login("admin", "admin123");
        assertNotNull(adminLogin);
        assertTrue(adminLogin.success());
        assertEquals("ADMIN", adminLogin.role());

        // Admin endpoints must succeed
        List<AdminAlertDto> alerts = client.getAdminAlerts();
        assertNotNull(alerts, "Admin alerts list must not be null");

        List<TransactionSummaryDto> pending = client.getPendingTransactions();
        assertNotNull(pending, "Pending transactions list must not be null");
        System.out.println(">>> SWING ADMIN VERIFICATION: Admin authenticated and fetched alerts (" + alerts.size() + ") and pending (" + pending.size() + ")");
    }

    @Test
    @DisplayName("5. Verify customer cannot access admin endpoints (HTTP 403 Forbidden)")
    void verifyCustomerForbiddenFromAdminEndpoints() throws Exception {
        String customerUser = "cust_role_test_" + System.currentTimeMillis();
        client.register(customerUser, customerUser + "@test.com", "Password@123");
        client.login(customerUser, "Password@123");

        // Customer calling admin endpoint should fail with 403 Forbidden
        ApiClientException exAlerts = assertThrows(ApiClientException.class, () -> client.getAdminAlerts());
        assertEquals(403, exAlerts.getStatusCode(), "Customer must receive HTTP 403 for admin alerts");

        ApiClientException exPending = assertThrows(ApiClientException.class, () -> client.getPendingTransactions());
        assertEquals(403, exPending.getStatusCode(), "Customer must receive HTTP 403 for pending transactions");
        System.out.println(">>> SWING RBAC VERIFICATION: Customer correctly blocked from admin surveillance with HTTP 403");
    }

    @Test
    @DisplayName("6. Verify logout terminates session and invalidates authenticated access")
    void verifyLogoutTerminatesSession() throws Exception {
        String testUser = "logout_test_" + System.currentTimeMillis();
        client.register(testUser, testUser + "@test.com", "Password@123");
        client.login(testUser, "Password@123");
        assertTrue(client.isLoggedIn());

        // Perform logout
        AuthResponse logoutResp = client.logout();
        assertNotNull(logoutResp);
        assertTrue(logoutResp.success());
        assertFalse(client.isLoggedIn(), "Client state must be logged out");

        // Subsequent /api/auth/me must fail with 401 Unauthorized
        ApiClientException ex = assertThrows(ApiClientException.class, () -> client.getCurrentUser());
        assertEquals(401, ex.getStatusCode(), "Unauthenticated user profile check must throw 401");
        System.out.println(">>> SWING LOGOUT VERIFICATION: Session successfully invalidated after logout (HTTP 401 received)");
    }
}
