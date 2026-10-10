package com.frauddetection.frauddetection.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

/**
 * Isolated unit/component tests for {@link SafePayApiClient}.
 * Uses JDK's built-in lightweight {@link HttpServer} on an ephemeral local port to test
 * actual HTTP wire communications, CSRF header attachment, cookies, and error handling.
 */
class SafePayApiClientTest {

    private HttpServer mockServer;
    private int port;
    private String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = mockServer.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    @DisplayName("fetchCsrfToken queries /api/auth/csrf and caches token")
    void fetchCsrfToken_success() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            String response = objectMapper.writeValueAsString(
                    new CsrfResponse("test-csrf-token-abc", "X-CSRF-TOKEN", "_csrf")
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Set-Cookie", "XSRF-TOKEN=test-csrf-token-abc; Path=/");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        CsrfResponse csrf = client.fetchCsrfToken();

        assertEquals("test-csrf-token-abc", csrf.token());
        assertEquals("X-CSRF-TOKEN", csrf.headerName());
        assertEquals("test-csrf-token-abc", client.getCsrfToken());
    }

    @Test
    @DisplayName("login auto-fetches CSRF token, attaches header, and parses AuthResponse")
    void login_autoFetchesCsrfAndAuthenticates() throws Exception {
        AtomicInteger csrfHits = new AtomicInteger(0);
        AtomicInteger loginHits = new AtomicInteger(0);

        mockServer.createContext("/api/auth/csrf", exchange -> {
            csrfHits.incrementAndGet();
            String response = objectMapper.writeValueAsString(
                    new CsrfResponse("token-999", "X-CSRF-TOKEN", "_csrf")
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/auth/login", exchange -> {
            loginHits.incrementAndGet();
            // Validate that the client attached the CSRF token in the header
            String csrfHeader = exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN");
            assertEquals("token-999", csrfHeader, "Client must attach CSRF token header");

            String response = objectMapper.writeValueAsString(
                    new AuthResponse(true, "Login successful", "alice", "alice@test.com", "USER", "ACC-1001")
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=MOCKSESSION123; Path=/; HttpOnly");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        AuthResponse auth = client.login("alice", "Password@123");

        assertEquals(1, csrfHits.get());
        assertEquals(1, loginHits.get());
        assertTrue(auth.success());
        assertEquals("alice", auth.username());
        assertEquals("USER", auth.role());
        assertEquals("ACC-1001", auth.accountNumber());
        assertTrue(client.isLoggedIn());
        assertEquals("alice", client.getCachedUser().username());
    }

    @Test
    @DisplayName("login with bad credentials throws ApiClientException with status 401")
    void login_badCredentials_throws401() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tok", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/auth/login", exchange -> {
            String response = objectMapper.writeValueAsString(
                    new AuthResponse(false, "Invalid username or password.", null, null, null, null)
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () -> client.login("alice", "wrong"));

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Invalid username or password"));
        assertFalse(client.isLoggedIn());
    }

    @Test
    @DisplayName("register creates user account and parses 201 Created")
    void register_success() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tok-reg", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/auth/register", exchange -> {
            assertEquals("tok-reg", exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"));
            String response = objectMapper.writeValueAsString(
                    new AuthResponse(true, "Registration successful!", "bob", "bob@test.com", "USER", "ACC-2002")
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(201, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        AuthResponse reg = client.register("bob", "bob@test.com", "Password@123");

        assertTrue(reg.success());
        assertEquals("bob", reg.username());
        assertEquals("ACC-2002", reg.accountNumber());
    }

    @Test
    @DisplayName("register validation error returns 400 Bad Request")
    void register_validationFailure_throws400() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tok-reg", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/auth/register", exchange -> {
            String response = objectMapper.writeValueAsString(
                    new AuthResponse(false, "Username is already taken", null, null, null, null)
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () ->
                client.register("existing_user", "email@test.com", "Password@123")
        );

        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Username is already taken"));
    }

    @Test
    @DisplayName("logout invalidates session and clears local cached user")
    void logout_clearsSession() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tok-out", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/auth/logout", exchange -> {
            assertEquals("tok-out", exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"));
            String response = objectMapper.writeValueAsString(
                    new AuthResponse(true, "Logged out successfully.", null, null, null, null)
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        AuthResponse logoutResponse = client.logout();

        assertTrue(logoutResponse.success());
        assertFalse(client.isLoggedIn());
        assertNull(client.getCachedUser());
    }

    @Test
    @DisplayName("network connection failure throws ApiClientException with status 0")
    void networkFailure_throwsStatus0() {
        // Connect to a port where nothing is listening
        SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:1");

        ApiClientException ex = assertThrows(ApiClientException.class, () -> client.login("alice", "pass"));
        assertEquals(0, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Unable to connect"));
    }

    @Test
    @DisplayName("getDashboardData successfully parses backend dashboard metrics")
    void getDashboardData_success() throws Exception {
        mockServer.createContext("/api/dashboard", exchange -> {
            String response = objectMapper.writeValueAsString(
                    new com.frauddetection.frauddetection.dto.DashboardResponse(
                            true,
                            "Success",
                            "alice",
                            "USER",
                            "ACC-9876",
                            "SAVINGS",
                            "ACTIVE",
                            new java.math.BigDecimal("50000.75"),
                            15,
                            12,
                            2,
                            1,
                            java.util.List.of(new com.frauddetection.frauddetection.dto.TransactionSummaryDto(
                                    101L,
                                    new java.math.BigDecimal("1500.00"),
                                    "PAYMENT",
                                    "APPROVED",
                                    "2026-10-09 14:30:00"
                            ))
                    )
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        com.frauddetection.frauddetection.dto.DashboardResponse dash = client.getDashboardData();

        assertTrue(dash.success());
        assertEquals("alice", dash.username());
        assertEquals(new java.math.BigDecimal("50000.75"), dash.balance());
        assertEquals(15, dash.totalTransactions());
        assertEquals(1, dash.recentTransactions().size());
        assertEquals(new java.math.BigDecimal("1500.00"), dash.recentTransactions().get(0).amount());
    }

    @Test
    @DisplayName("createTransaction attaches CSRF header, posts JSON, and parses APPROVED response")
    void createTransaction_success() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tx-csrf-token", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/transactions", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("tx-csrf-token", exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"));
            assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));

            String response = objectMapper.writeValueAsString(
                    new com.frauddetection.frauddetection.dto.TransactionResponse(
                            true,
                            "Your transaction was approved and processed successfully.",
                            205L,
                            new java.math.BigDecimal("2500.00"),
                            "TRANSFER",
                            "APPROVED",
                            "2026-10-09 14:40:00",
                            "Normal / Legitimate",
                            "LOW",
                            0.015,
                            new java.math.BigDecimal("47500.00")
                    )
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        com.frauddetection.frauddetection.dto.TransactionResponse res = client.createTransaction(
                new com.frauddetection.frauddetection.dto.TransactionRequest(
                        new java.math.BigDecimal("2500.00"), "TRANSFER"
                )
        );

        assertNotNull(res);
        assertTrue(res.success());
        assertEquals("APPROVED", res.status());
        assertEquals(205L, res.transactionId());
        assertEquals(new java.math.BigDecimal("2500.00"), res.amount());
        assertEquals("TRANSFER", res.transactionType());
        assertEquals("LOW", res.riskAssessment());
        assertEquals("Normal / Legitimate", res.detectionReason());
        assertEquals(new java.math.BigDecimal("47500.00"), res.newBalance());
    }

    @Test
    @DisplayName("createTransaction parses 400 validation response cleanly")
    void createTransaction_validationFailure_returnsParsedResponse() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tx-csrf-token-2", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/transactions", exchange -> {
            String response = objectMapper.writeValueAsString(
                    new com.frauddetection.frauddetection.dto.TransactionResponse(
                            false,
                            "Amount must be greater than zero.",
                            null,
                            new java.math.BigDecimal("0.00"),
                            "PAYMENT",
                            "DECLINED",
                            null,
                            "Amount must be greater than zero.",
                            "HIGH",
                            null,
                            null
                    )
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        com.frauddetection.frauddetection.dto.TransactionResponse res = client.createTransaction(
                new com.frauddetection.frauddetection.dto.TransactionRequest(
                        new java.math.BigDecimal("0.00"), "PAYMENT"
                )
        );

        assertNotNull(res);
        assertFalse(res.success());
        assertEquals("Amount must be greater than zero.", res.message());
        assertEquals("DECLINED", res.status());
    }

    @Test
    @DisplayName("createTransaction automatically retries on 403 CSRF expiration")
    void createTransaction_csrfExpired_retriesAndSucceeds() throws Exception {
        AtomicInteger csrfFetchCount = new AtomicInteger(0);
        AtomicInteger txCallCount = new AtomicInteger(0);

        mockServer.createContext("/api/auth/csrf", exchange -> {
            int count = csrfFetchCount.incrementAndGet();
            String tok = "csrf-token-v" + count;
            String response = objectMapper.writeValueAsString(new CsrfResponse(tok, "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/transactions", exchange -> {
            int count = txCallCount.incrementAndGet();
            if (count == 1) {
                // Reject initial attempt with 403 Invalid CSRF
                exchange.sendResponseHeaders(403, -1);
            } else {
                // Accept retry attempt with refreshed token
                assertEquals("csrf-token-v2", exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN"));
                String response = objectMapper.writeValueAsString(
                        new com.frauddetection.frauddetection.dto.TransactionResponse(
                                true,
                                "Your transaction was approved and processed successfully.",
                                301L,
                                new java.math.BigDecimal("100.00"),
                                "PAYMENT",
                                "APPROVED",
                                "2026-10-09 14:42:00",
                                "Normal / Legitimate",
                                "LOW",
                                0.01,
                                new java.math.BigDecimal("49900.00")
                        )
                );
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        com.frauddetection.frauddetection.dto.TransactionResponse res = client.createTransaction(
                new com.frauddetection.frauddetection.dto.TransactionRequest(
                        new java.math.BigDecimal("100.00"), "PAYMENT"
                )
        );

        assertNotNull(res);
        assertTrue(res.success());
        assertEquals("APPROVED", res.status());
        assertEquals(301L, res.transactionId());
        assertEquals(2, txCallCount.get());
    }

    @Test
    @DisplayName("createTransaction throws ApiClientException on 401 unauthenticated session")
    void createTransaction_unauthorized_throwsApiClientException() throws Exception {
        mockServer.createContext("/api/auth/csrf", exchange -> {
            String response = objectMapper.writeValueAsString(new CsrfResponse("tok", "X-CSRF-TOKEN", "_csrf"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.createContext("/api/transactions", exchange -> {
            exchange.sendResponseHeaders(401, -1);
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () ->
                client.createTransaction(
                        new com.frauddetection.frauddetection.dto.TransactionRequest(
                                new java.math.BigDecimal("100.00"), "PAYMENT"
                        )
                )
        );

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Session expired or authentication required"));
    }

    @Test
    @DisplayName("getAdminAlerts parses JSON list of alerts with nested transaction fields")
    void getAdminAlerts_success() throws Exception {
        mockServer.createContext("/api/admin/alerts", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            String json = """
                    [
                      {
                        "id": 1,
                        "alertType": "SUSPICIOUS_VELOCITY",
                        "severity": "HIGH",
                        "status": "OPEN",
                        "createdAt": "2026-10-06T16:30:00",
                        "transaction": {
                          "id": 42,
                          "amount": 75000.00,
                          "transactionType": "TRANSFER",
                          "status": "BLOCKED"
                        }
                      }
                    ]
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        java.util.List<com.frauddetection.frauddetection.dto.AdminAlertDto> alerts = client.getAdminAlerts();

        assertNotNull(alerts);
        assertEquals(1, alerts.size());
        com.frauddetection.frauddetection.dto.AdminAlertDto alert = alerts.get(0);
        assertEquals(1L, alert.id());
        assertEquals("SUSPICIOUS_VELOCITY", alert.alertType());
        assertEquals("HIGH", alert.severity());
        assertEquals("OPEN", alert.status());
        assertEquals(42L, alert.transactionId());
        assertEquals(new java.math.BigDecimal("75000.0"), alert.transactionAmount());
        assertEquals("TRANSFER", alert.transactionType());
        assertEquals("BLOCKED", alert.transactionStatus());
    }

    @Test
    @DisplayName("getAdminAlerts throws ApiClientException on 403 Forbidden")
    void getAdminAlerts_forbidden_throws403() throws Exception {
        mockServer.createContext("/api/admin/alerts", exchange -> {
            exchange.sendResponseHeaders(403, -1);
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, client::getAdminAlerts);
        assertEquals(403, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Administrator privileges required"));
    }

    @Test
    @DisplayName("getTransactionHistory formats query parameters and parses transaction summaries")
    void getTransactionHistory_success() throws Exception {
        mockServer.createContext("/api/transactions", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            String query = exchange.getRequestURI().getQuery();
            assertTrue(query.contains("type=TRANSFER"));
            assertTrue(query.contains("status=APPROVED"));
            assertTrue(query.contains("search=101"));

            String json = """
                    [
                      {
                        "id": 101,
                        "amount": 5000.00,
                        "transactionType": "TRANSFER",
                        "status": "APPROVED",
                        "transactionTime": "2026-10-09 15:00:00"
                      }
                    ]
                    """;
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        java.util.List<com.frauddetection.frauddetection.dto.TransactionSummaryDto> history =
                client.getTransactionHistory("TRANSFER", "APPROVED", "101");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(101L, history.get(0).id());
        assertEquals(new java.math.BigDecimal("5000.0"), history.get(0).amount());
        assertEquals("TRANSFER", history.get(0).transactionType());
        assertEquals("APPROVED", history.get(0).status());
    }

    @Test
    @DisplayName("getTransactionHistory throws ApiClientException on 400 Bad Request")
    void getTransactionHistory_badRequest_throws400() throws Exception {
        mockServer.createContext("/api/transactions", exchange -> {
            String json = "{\"error\":\"Invalid transaction type filter\"}";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () ->
                client.getTransactionHistory("INVALID", null, null));
        assertEquals(400, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Invalid transaction type filter"));
    }

    @Test
    @DisplayName("getTransactionDetails parses full transaction receipt and fraud assessment")
    void getTransactionDetails_success() throws Exception {
        mockServer.createContext("/api/transactions/202", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            String response = objectMapper.writeValueAsString(
                    new com.frauddetection.frauddetection.dto.TransactionResponse(
                            true,
                            "Transaction approved and settled.",
                            202L,
                            new java.math.BigDecimal("8500.00"),
                            "PAYMENT",
                            "APPROVED",
                            "2026-10-09 16:00:00",
                            "Normal / Legitimate",
                            "LOW",
                            0.02,
                            new java.math.BigDecimal("41500.00")
                    )
            );
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        com.frauddetection.frauddetection.dto.TransactionResponse details = client.getTransactionDetails(202L);

        assertNotNull(details);
        assertTrue(details.success());
        assertEquals(202L, details.transactionId());
        assertEquals("APPROVED", details.status());
        assertEquals("LOW", details.riskAssessment());
        assertEquals(0.02, details.fraudProbability());
        assertEquals(new java.math.BigDecimal("41500.00"), details.newBalance());
    }

    @Test
    @DisplayName("getTransactionDetails throws ApiClientException on 403 Cross-User Access")
    void getTransactionDetails_forbidden_throws403() throws Exception {
        mockServer.createContext("/api/transactions/505", exchange -> {
            exchange.sendResponseHeaders(403, -1);
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () ->
                client.getTransactionDetails(505L));
        assertEquals(403, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("getTransactionDetails throws ApiClientException on 404 Not Found")
    void getTransactionDetails_notFound_throws404() throws Exception {
        mockServer.createContext("/api/transactions/999", exchange -> {
            exchange.sendResponseHeaders(404, -1);
        });
        mockServer.start();

        SafePayApiClient client = new SafePayApiClient(baseUrl);
        ApiClientException ex = assertThrows(ApiClientException.class, () ->
                client.getTransactionDetails(999L));
        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Transaction not found"));
    }
}
