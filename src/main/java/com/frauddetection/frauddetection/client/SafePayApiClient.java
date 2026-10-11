package com.frauddetection.frauddetection.client;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frauddetection.frauddetection.dto.AdminAlertDto;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.LoginRequest;
import com.frauddetection.frauddetection.dto.RegisterRequest;
import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Standard Java HTTP client communicating between the Swing GUI and Spring Boot backend.
 *
 * Preserves security:
 * - Session cookies (JSESSIONID) managed automatically via CookieManager.
 * - CSRF token fetched and attached as an HTTP header (X-CSRF-TOKEN) on all state-changing POST requests.
 * - Parses JSON using the existing tools.jackson.databind.ObjectMapper on the classpath.
 */
public class SafePayApiClient {

    private static final Logger log = LoggerFactory.getLogger(SafePayApiClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final CookieManager cookieManager;
    private final ObjectMapper objectMapper;

    private String csrfToken = null;
    private String csrfHeaderName = "X-CSRF-TOKEN";
    private AuthResponse currentUser = null;

    public SafePayApiClient() {
        this("http://localhost:8080");
    }

    public SafePayApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(this.cookieManager)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Explicit constructor for testing with custom HttpClient or CookieManager.
     */
    public SafePayApiClient(String baseUrl, HttpClient httpClient, CookieManager cookieManager) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.cookieManager = cookieManager;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Retrieves and caches the active CSRF token from the backend.
     */
    public CsrfResponse fetchCsrfToken() throws ApiClientException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/csrf"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                CsrfResponse csrfResponse = objectMapper.readValue(response.body(), CsrfResponse.class);
                this.csrfToken = csrfResponse.token();
                if (csrfResponse.headerName() != null && !csrfResponse.headerName().isBlank()) {
                    this.csrfHeaderName = csrfResponse.headerName();
                }
                log.debug("Acquired CSRF token via header {}", csrfHeaderName);
                return csrfResponse;
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Failed to obtain CSRF token from server (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Unable to connect to SafePay server: " + ex.getMessage(), ex);
        }
    }

    /**
     * Authenticates credentials with the Spring Boot server.
     */
    public AuthResponse login(String username, String password) throws ApiClientException {
        if (csrfToken == null) {
            fetchCsrfToken();
        }

        try {
            LoginRequest loginRequest = new LoginRequest(username, password);
            String requestBody = objectMapper.writeValueAsString(loginRequest);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (csrfToken != null) {
                reqBuilder.header(csrfHeaderName, csrfToken);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
                this.currentUser = authResponse;
                return authResponse;
            } else if (response.statusCode() == 401) {
                AuthResponse errorResponse = parseAuthError(response.body(), "Invalid username or password.");
                throw new ApiClientException(401, errorResponse.message());
            } else if (response.statusCode() == 403) {
                // If CSRF expired or failed, re-fetch and retry once
                log.warn("Login received HTTP 403. Refreshing CSRF token and retrying once.");
                fetchCsrfToken();
                return retryLogin(username, password);
            } else {
                AuthResponse errorResponse = parseAuthError(response.body(), "Login failed with status " + response.statusCode());
                throw new ApiClientException(response.statusCode(), errorResponse.message());
            }

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error during login: " + ex.getMessage(), ex);
        }
    }

    private AuthResponse retryLogin(String username, String password) throws ApiClientException {
        try {
            LoginRequest loginRequest = new LoginRequest(username, password);
            String requestBody = objectMapper.writeValueAsString(loginRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(csrfHeaderName, csrfToken != null ? csrfToken : "")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
                this.currentUser = authResponse;
                return authResponse;
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Invalid username or password.");
            } else {
                throw new ApiClientException(response.statusCode(), "Login failed (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error during login retry: " + ex.getMessage(), ex);
        }
    }

    /**
     * Registers a new user account with the Spring Boot server.
     */
    public AuthResponse register(String username, String email, String password) throws ApiClientException {
        if (csrfToken == null) {
            fetchCsrfToken();
        }

        try {
            RegisterRequest registerRequest = new RegisterRequest(username, email, password);
            String requestBody = objectMapper.writeValueAsString(registerRequest);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (csrfToken != null) {
                reqBuilder.header(csrfHeaderName, csrfToken);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), AuthResponse.class);
            } else if (response.statusCode() == 400) {
                AuthResponse errorResponse = parseAuthError(response.body(), "Registration validation failed.");
                throw new ApiClientException(400, errorResponse.message());
            } else if (response.statusCode() == 403) {
                fetchCsrfToken();
                return retryRegister(username, email, password);
            } else {
                throw new ApiClientException(response.statusCode(), "Registration failed with status " + response.statusCode());
            }

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error during registration: " + ex.getMessage(), ex);
        }
    }

    private AuthResponse retryRegister(String username, String email, String password) throws ApiClientException {
        try {
            RegisterRequest registerRequest = new RegisterRequest(username, email, password);
            String requestBody = objectMapper.writeValueAsString(registerRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(csrfHeaderName, csrfToken != null ? csrfToken : "")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return objectMapper.readValue(response.body(), AuthResponse.class);
            } else {
                AuthResponse errorResponse = parseAuthError(response.body(), "Registration failed.");
                throw new ApiClientException(response.statusCode(), errorResponse.message());
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error during registration retry: " + ex.getMessage(), ex);
        }
    }

    /**
     * Terminates the current session.
     */
    public AuthResponse logout() throws ApiClientException {
        if (csrfToken == null) {
            fetchCsrfToken();
        }

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/logout"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody());

            if (csrfToken != null) {
                reqBuilder.header(csrfHeaderName, csrfToken);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            this.currentUser = null;
            if (cookieManager != null && cookieManager.getCookieStore() != null) {
                cookieManager.getCookieStore().removeAll();
            }

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), AuthResponse.class);
            }
            return new AuthResponse(true, "Logged out", null, null, null, null);

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            this.currentUser = null;
            throw new ApiClientException(0, "Network error during logout: " + ex.getMessage(), ex);
        }
    }

    /**
     * Checks currently authenticated user status.
     */
    public AuthResponse getCurrentUser() throws ApiClientException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/me"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                AuthResponse auth = objectMapper.readValue(response.body(), AuthResponse.class);
                this.currentUser = auth;
                return auth;
            } else {
                this.currentUser = null;
                throw new ApiClientException(response.statusCode(), "Not authenticated.");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error checking session: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fetches real dashboard statistics, balances, and recent transactions.
     */
    public DashboardResponse getDashboardData() throws ApiClientException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/dashboard"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), DashboardResponse.class);
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Session expired or authentication required. Please log in again.");
            } else if (response.statusCode() == 403) {
                throw new ApiClientException(403, "Access forbidden to dashboard metrics.");
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Failed to load dashboard metrics (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error fetching dashboard: " + ex.getMessage(), ex);
        }
    }

    /**
     * Submits a financial transaction for AI fraud analysis and backend ledger processing.
     *
     * @param request transaction parameters (amount, transactionType)
     * @return transaction response containing outcome (APPROVED, DECLINED, BLOCKED) and details
     * @throws ApiClientException if validation fails, network error occurs, or unauthorized
     */
    public TransactionResponse createTransaction(TransactionRequest request) throws ApiClientException {
        if (request == null) {
            throw new ApiClientException(400, "Transaction request cannot be null.");
        }

        if (csrfToken == null) {
            fetchCsrfToken();
        }

        try {
            String requestBody = objectMapper.writeValueAsString(request);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/transactions"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (csrfToken != null) {
                reqBuilder.header(csrfHeaderName, csrfToken);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            // Handle CSRF expiration retry on 403
            if (response.statusCode() == 403) {
                log.info("Received 403 during transaction execution; refreshing CSRF token and retrying...");
                return retryCreateTransaction(request);
            }

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TransactionResponse.class);
            } else if (response.statusCode() == 400) {
                return parseTransactionError(response.body(), "Transaction validation failed.");
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Session expired or authentication required. Please log in again.");
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Transaction failed with HTTP status " + response.statusCode() + ".");
            }

        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error submitting transaction: " + ex.getMessage(), ex);
        }
    }

    private TransactionResponse retryCreateTransaction(TransactionRequest txRequest) throws ApiClientException {
        fetchCsrfToken();
        try {
            String requestBody = objectMapper.writeValueAsString(txRequest);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/transactions"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header(csrfHeaderName, csrfToken != null ? csrfToken : "")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TransactionResponse.class);
            } else if (response.statusCode() == 400) {
                return parseTransactionError(response.body(), "Transaction validation failed.");
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Transaction retry failed with HTTP status " + response.statusCode() + ".");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error during transaction retry: " + ex.getMessage(), ex);
        }
    }

    private TransactionResponse parseTransactionError(String responseBody, String defaultMessage) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                return objectMapper.readValue(responseBody, TransactionResponse.class);
            } catch (Exception ignored) {
                // Ignore parse failure and fall back
            }
        }
        return new TransactionResponse(false, defaultMessage, null, null, null, "DECLINED", null, defaultMessage, "HIGH", null, null);
    }

    private AuthResponse parseAuthError(String responseBody, String defaultMessage) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                return objectMapper.readValue(responseBody, AuthResponse.class);
            } catch (Exception ignored) {
                // Ignore parse failure and fall back
            }
        }
        return new AuthResponse(false, defaultMessage, null, null, null, null);
    }

    /**
     * Retrieves all fraud alerts for administrator surveillance monitoring.
     * Enforces ROLE_ADMIN authorization on the server side.
     *
     * @return List of fraud alert DTOs ordered newest first
     * @throws ApiClientException if unauthorized (401), forbidden (403), or network fails
     */
    public List<AdminAlertDto> getAdminAlerts() throws ApiClientException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/admin/alerts"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<AdminAlertDto> alerts = new ArrayList<>();
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (JsonNode item : root) {
                        Long id = item.has("id") && !item.get("id").isNull() ? item.get("id").asLong() : null;
                        String alertType = item.has("alertType") ? item.get("alertType").asString() : "";
                        String severity = item.has("severity") ? item.get("severity").asString() : "";
                        String status = item.has("status") ? item.get("status").asString() : "";
                        String createdAt = item.has("createdAt") ? item.get("createdAt").asString() : "";

                        Long txId = null;
                        BigDecimal txAmount = null;
                        String txType = "";
                        String txStatus = "";

                        if (item.has("transaction") && !item.get("transaction").isNull()) {
                            JsonNode tx = item.get("transaction");
                            txId = tx.has("id") && !tx.get("id").isNull() ? tx.get("id").asLong() : null;
                            txAmount = tx.has("amount") && !tx.get("amount").isNull()
                                    ? new BigDecimal(tx.get("amount").asString()) : null;
                            txType = tx.has("transactionType") ? tx.get("transactionType").asString() : "";
                            txStatus = tx.has("status") ? tx.get("status").asString() : "";
                        }

                        alerts.add(new AdminAlertDto(
                                id, alertType, severity, status, createdAt,
                                txId, txAmount, txType, txStatus
                        ));
                    }
                }
                return alerts;
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Session expired or authentication required. Please log in again.");
            } else if (response.statusCode() == 403) {
                throw new ApiClientException(403, "Access forbidden: Administrator privileges required.");
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Failed to retrieve fraud alerts (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error retrieving admin alerts: " + ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves the authenticated user's transaction history with optional filters.
     *
     * @param type   optional transaction type filter (PAYMENT, TRANSFER, etc.)
     * @param status optional status filter (APPROVED, DECLINED, BLOCKED)
     * @param search optional search string matching ID or amount
     * @return List of transaction summary DTOs
     * @throws ApiClientException if validation fails, unauthorized, or network fails
     */
    public List<TransactionSummaryDto> getTransactionHistory(String type, String status, String search)
            throws ApiClientException {
        try {
            StringBuilder url = new StringBuilder(baseUrl).append("/api/transactions");
            List<String> queryParams = new ArrayList<>();
            if (type != null && !type.isBlank()) {
                queryParams.add("type=" + URLEncoder.encode(type.trim(), StandardCharsets.UTF_8));
            }
            if (status != null && !status.isBlank()) {
                queryParams.add("status=" + URLEncoder.encode(status.trim(), StandardCharsets.UTF_8));
            }
            if (search != null && !search.isBlank()) {
                queryParams.add("search=" + URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
            }
            if (!queryParams.isEmpty()) {
                url.append("?").append(String.join("&", queryParams));
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                List<TransactionSummaryDto> list = new ArrayList<>();
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Long id = node.has("id") && !node.get("id").isNull() ? node.get("id").asLong() : null;
                        JsonNode amountNode = node.get("amount");
                        BigDecimal amt = amountNode != null && !amountNode.isNull() ? amountNode.decimalValue() : BigDecimal.ZERO;
                        JsonNode transactionTypeNode = node.get("transactionType");
                        String tType = transactionTypeNode != null && !transactionTypeNode.isNull()
                                ? transactionTypeNode.asString()
                                : "PAYMENT";
                        JsonNode statusNode = node.get("status");
                        String tStatus = statusNode != null && !statusNode.isNull()
                                ? statusNode.stringValue()
                                : "UNKNOWN";
                        JsonNode transactionTimeNode = node.get("transactionTime");
                        String time = transactionTimeNode != null && !transactionTimeNode.isNull()
                                ? transactionTimeNode.stringValue()
                                : "";
                        list.add(new TransactionSummaryDto(id, amt, tType, tStatus, time));
                    }
                }
                return list;
            } else if (response.statusCode() == 400) {
                String errorMsg = parseErrorMessage(response.body(), "Invalid filter parameters.");
                throw new ApiClientException(400, errorMsg);
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Session expired or authentication required. Please log in again.");
            } else if (response.statusCode() == 403) {
                throw new ApiClientException(403, "Access forbidden to transaction history.");
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Failed to retrieve transaction history (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error retrieving transaction history: " + ex.getMessage(), ex);
        }
    }

    /**
     * Retrieves full transaction details and fraud evaluation for a specific transaction ID.
     * Enforces ownership validation on the server side.
     *
     * @param id Persisted transaction identifier
     * @return Full transaction response containing fraud assessment and details
     * @throws ApiClientException if unauthorized (401), cross-user forbidden (403), not found (404), or network fails
     */
    public TransactionResponse getTransactionDetails(Long id) throws ApiClientException {
        if (id == null || id <= 0) {
            throw new ApiClientException(400, "Invalid transaction ID.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/transactions/" + id))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), TransactionResponse.class);
            } else if (response.statusCode() == 400) {
                throw new ApiClientException(400, parseErrorMessage(response.body(), "Invalid transaction ID."));
            } else if (response.statusCode() == 401) {
                throw new ApiClientException(401, "Session expired or authentication required. Please log in again.");
            } else if (response.statusCode() == 403) {
                throw new ApiClientException(403, "Access denied: Transaction does not belong to your account.");
            } else if (response.statusCode() == 404) {
                throw new ApiClientException(404, "Transaction not found with ID: " + id);
            } else {
                throw new ApiClientException(response.statusCode(),
                        "Failed to retrieve transaction details (HTTP " + response.statusCode() + ").");
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiClientException(0, "Network error retrieving transaction details: " + ex.getMessage(), ex);
        }
    }

    private String parseErrorMessage(String responseBody, String defaultMsg) {
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("error")) {
                    return node.get("error").asString();
                }
                if (node.has("message")) {
                    return node.get("message").asString();
                }
            } catch (Exception ignored) {
            }
            return responseBody.length() < 200 ? responseBody : defaultMsg;
        }
        return defaultMsg;
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentUser.username() != null;
    }

    public AuthResponse getCachedUser() {
        return currentUser;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}