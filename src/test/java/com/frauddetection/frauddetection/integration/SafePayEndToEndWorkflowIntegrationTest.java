package com.frauddetection.frauddetection.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.frauddetection.frauddetection.dto.CsrfResponse;
import com.frauddetection.frauddetection.dto.LoginRequest;
import com.frauddetection.frauddetection.dto.RegisterRequest;
import com.frauddetection.frauddetection.dto.ReviewRequest;
import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.FundsReservationRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Full End-to-End Banking and Fraud Review Lifecycle Integration Test.
 *
 * Verifies the 12-step complete workflow:
 *  1. Customer logs in and establishes an authenticated session with CSRF protection.
 *  2. Customer submits a normal payment -> approved and debited.
 *  3. Customer submits a valid transfer to another account -> approved, sender debited and receiver credited atomically.
 *  4. Transaction history reflects the correct results in chronological order.
 *  5. High-risk transaction triggers AI fraud evaluation and enters PENDING_REVIEW.
 *  6. Customer receives pending-review status and balances remain completely unchanged.
 *  7. Administrator logs in and establishes an authenticated administrative session.
 *  8. Administrator views pending transactions awaiting manual investigation.
 *  9. Administrator approves the pending transaction with review notes.
 * 10. Sender and receiver balances are updated and verified in database persistence.
 * 11. Transaction history reflects the final APPROVED status.
 * 12. Repeated approval attempt is rejected with HTTP 400 and balances remain unchanged.
 *
 * Also tests:
 * - Administrator rejection workflow leaves balances untouched and sets transaction to DECLINED.
 * - Customer cannot access administrator fraud surveillance endpoints (HTTP 403 Forbidden).
 */
@SpringBootTest
class SafePayEndToEndWorkflowIntegrationTest {

    private static final String SENDER_USERNAME = "e2e_cust_sender";
    private static final String RECEIVER_USERNAME = "e2e_cust_receiver";
    private static final String ADMIN_USERNAME = "e2e_admin_user";
    private static final String PASSWORD = "TestPassword@123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudAlertRepository fraudAlertRepository;

    @Autowired
    private FraudPredictionRepository fraudPredictionRepository;

    @Autowired
    private FundsReservationRepository fundsReservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User senderUser;
    private Account senderAccount;
    private User receiverUser;
    private Account receiverAccount;
    private User adminUser;
    private Account adminAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        cleanupData();

        // 1. Create Sender User and Account (Balance: 100,000.00)
        senderUser = new User();
        senderUser.setUsername(SENDER_USERNAME);
        senderUser.setEmail("sender@e2e.test");
        senderUser.setPassword(passwordEncoder.encode(PASSWORD));
        senderUser.setRole("USER");
        senderUser = userRepository.save(senderUser);

        senderAccount = new Account();
        senderAccount.setAccountNumber("ACC-E2E-SENDER");
        senderAccount.setAccountType("SAVINGS");
        senderAccount.setBalance(new BigDecimal("100000.00"));
        senderAccount.setStatus("ACTIVE");
        senderAccount.setUser(senderUser);
        senderAccount = accountRepository.save(senderAccount);

        // 2. Create Receiver User and Account (Balance: 5,000.00)
        receiverUser = new User();
        receiverUser.setUsername(RECEIVER_USERNAME);
        receiverUser.setEmail("receiver@e2e.test");
        receiverUser.setPassword(passwordEncoder.encode(PASSWORD));
        receiverUser.setRole("USER");
        receiverUser = userRepository.save(receiverUser);

        receiverAccount = new Account();
        receiverAccount.setAccountNumber("ACC-E2E-RECEIVER");
        receiverAccount.setAccountType("CHECKING");
        receiverAccount.setBalance(new BigDecimal("5000.00"));
        receiverAccount.setStatus("ACTIVE");
        receiverAccount.setUser(receiverUser);
        receiverAccount = accountRepository.save(receiverAccount);

        // 3. Create Admin User and Account
        adminUser = new User();
        adminUser.setUsername(ADMIN_USERNAME);
        adminUser.setEmail("admin@e2e.test");
        adminUser.setPassword(passwordEncoder.encode(PASSWORD));
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);

        adminAccount = new Account();
        adminAccount.setAccountNumber("ACC-E2E-ADMIN");
        adminAccount.setAccountType("CORPORATE");
        adminAccount.setBalance(new BigDecimal("1000000.00"));
        adminAccount.setStatus("ACTIVE");
        adminAccount.setUser(adminUser);
        adminAccount = accountRepository.save(adminAccount);
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        List<String> usernames = List.of(SENDER_USERNAME, RECEIVER_USERNAME, ADMIN_USERNAME);
        for (String uname : usernames) {
            Optional<User> uOpt = userRepository.findByUsername(uname);
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                List<Account> accs = accountRepository.findByUser(u);
                for (Account acc : accs) {
                    fundsReservationRepository.findByAccount(acc).forEach(fundsReservationRepository::delete);
                    List<Transaction> txs = transactionRepository.findByAccountOrderByTransactionTimeDesc(acc);
                    for (Transaction tx : txs) {
                        fundsReservationRepository.findByTransaction(tx).ifPresent(fundsReservationRepository::delete);
                        fraudAlertRepository.findByTransaction(tx).forEach(fraudAlertRepository::delete);
                        fraudPredictionRepository.findByTransaction(tx).ifPresent(fraudPredictionRepository::delete);
                        transactionRepository.delete(tx);
                    }
                }
            }
        }
        for (String uname : usernames) {
            Optional<User> uOpt = userRepository.findByUsername(uname);
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                List<Account> accs = accountRepository.findByUser(u);
                for (Account acc : accs) {
                    fundsReservationRepository.findByAccount(acc).forEach(fundsReservationRepository::delete);
                    accountRepository.delete(acc);
                }
                userRepository.delete(u);
            }
        }
    }

    private record AuthContext(MockHttpSession session, Cookie cookie, String csrfHeader, String csrfToken) {}

    private AuthContext login(String username, String password) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        CsrfResponse csrf = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), CsrfResponse.class);
        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.username").value(username))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Session must not be null upon successful login");

        return new AuthContext(session, xsrfCookie, csrf.headerName(), csrf.token());
    }

    @Test
    @DisplayName("Complete 12-Step End-to-End Banking, Transfer, Fraud Review and Duplicate Prevention Workflow")
    void testCompleteBankingAndFraudReviewWorkflow() throws Exception {
        // =====================================================================
        // Step 1: Customer logs in
        // =====================================================================
        AuthContext customerAuth = login(SENDER_USERNAME, PASSWORD);

        // =====================================================================
        // Step 2: Customer submits a normal payment
        // =====================================================================
        TransactionRequest paymentRequest = new TransactionRequest(
                new BigDecimal("500.00"),
                "PAYMENT"
        );

        MvcResult paymentResult = mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode paymentNode = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        Long paymentTxId = paymentNode.get("transactionId").asLong();
        assertNotNull(paymentTxId);

        // Sender balance debited: 100,000.00 - 500.00 = 99,500.00
        Account updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("99500.00").compareTo(updatedSender.getBalance()));

        // =====================================================================
        // Step 3: Customer submits a transfer to receiver account
        // =====================================================================
        TransactionRequest transferRequest = new TransactionRequest(
                new BigDecimal("500.00"),
                "TRANSFER",
                receiverAccount.getAccountNumber()
        );

        MvcResult transferResult = mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode transferNode = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        Long transferTxId = transferNode.get("transactionId").asLong();
        assertNotNull(transferTxId);

        // Sender debited: 99,500.00 - 500.00 = 99,000.00
        updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("99000.00").compareTo(updatedSender.getBalance()));

        // Receiver credited: 5,000.00 + 500.00 = 5,500.00
        Account updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5500.00").compareTo(updatedReceiver.getBalance()));

        // =====================================================================
        // Step 4: Transaction history reflects the correct results
        // =====================================================================
        MvcResult historyResult = mockMvc.perform(get("/api/transactions")
                        .session(customerAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyArray = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertTrue(historyArray.isArray());
        assertEquals(2, historyArray.size(), "Should have exactly 2 completed transactions in history");
        assertEquals("TRANSFER", historyArray.get(0).get("transactionType").asText());
        assertEquals("APPROVED", historyArray.get(0).get("status").asText());
        assertEquals("PAYMENT", historyArray.get(1).get("transactionType").asText());
        assertEquals("APPROVED", historyArray.get(1).get("status").asText());

        // =====================================================================
        // Step 5 & 6: Fraudulent transaction enters PENDING_REVIEW and customer sees status
        // =====================================================================
        // Full balance drain transfer of 99,000.00 triggers AI Random Forest fraud detection (Paysim pattern)
        TransactionRequest fraudTxRequest = new TransactionRequest(
                new BigDecimal("99000.00"),
                "TRANSFER",
                receiverAccount.getAccountNumber()
        );

        MvcResult fraudTxResult = mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fraudTxRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn();

        JsonNode fraudNode = objectMapper.readTree(fraudTxResult.getResponse().getContentAsString());
        Long pendingTxId = fraudNode.get("transactionId").asLong();
        assertNotNull(pendingTxId);
        assertTrue(fraudNode.get("message").asText().contains("pending review"));

        // Crucial check: Balances MUST remain untouched while transaction is PENDING_REVIEW!
        updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("99000.00").compareTo(updatedSender.getBalance()),
                "Sender balance must remain unchanged while transaction is pending review");

        updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("5500.00").compareTo(updatedReceiver.getBalance()),
                "Receiver balance must remain unchanged while transaction is pending review");

        // Verify FraudAlert was created in database
        Transaction pendingTx = transactionRepository.findById(pendingTxId).orElseThrow();
        assertEquals("PENDING_REVIEW", pendingTx.getStatus());
        List<FraudAlert> alerts = fraudAlertRepository.findByTransaction(pendingTx);
        assertFalse(alerts.isEmpty(), "FraudAlert must be generated for pending transaction");
        assertEquals("OPEN", alerts.get(0).getStatus());

        // =====================================================================
        // Step 7: Administrator logs in
        // =====================================================================
        AuthContext adminAuth = login(ADMIN_USERNAME, PASSWORD);

        // =====================================================================
        // Step 8: Administrator sees the pending transaction in review queue
        // =====================================================================
        MvcResult pendingListResult = mockMvc.perform(get("/api/admin/pending")
                        .session(adminAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode pendingArray = objectMapper.readTree(pendingListResult.getResponse().getContentAsString());
        assertTrue(pendingArray.isArray());
        boolean foundPending = false;
        for (JsonNode item : pendingArray) {
            if (item.get("id").asLong() == pendingTxId) {
                foundPending = true;
                assertEquals("PENDING_REVIEW", item.get("status").asText());
                break;
            }
        }
        assertTrue(foundPending, "Administrator pending queue must contain the flagged transaction #" + pendingTxId);

        // =====================================================================
        // Step 9: Administrator approves it
        // =====================================================================
        ReviewRequest approveRequest = new ReviewRequest("Verified legitimate transaction by administrator.");
        mockMvc.perform(post("/api/admin/transactions/" + pendingTxId + "/approve")
                        .session(adminAuth.session())
                        .cookie(adminAuth.cookie())
                        .header(adminAuth.csrfHeader(), adminAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // =====================================================================
        // Step 10: Sender and receiver balances are verified
        // =====================================================================
        // Sender debited: 99,000.00 - 99,000.00 = 0.00
        updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(updatedSender.getBalance()),
                "Sender balance must be debited to 0.00 after approval");

        // Receiver credited: 5,500.00 + 99,000.00 = 104,500.00
        updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("104500.00").compareTo(updatedReceiver.getBalance()),
                "Receiver balance must be credited to 104,500.00 after approval");

        // =====================================================================
        // Step 11: Transaction history reflects the final status
        // =====================================================================
        MvcResult finalTxDetailResult = mockMvc.perform(get("/api/transactions/" + pendingTxId)
                        .session(customerAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode finalTxNode = objectMapper.readTree(finalTxDetailResult.getResponse().getContentAsString());
        assertEquals("APPROVED", finalTxNode.get("status").asText());

        Transaction persistedTx = transactionRepository.findById(pendingTxId).orElseThrow();
        assertEquals("APPROVED", persistedTx.getStatus());

        List<FraudAlert> resolvedAlerts = fraudAlertRepository.findByTransaction(persistedTx);
        assertFalse(resolvedAlerts.isEmpty());
        assertEquals("RESOLVED", resolvedAlerts.get(0).getStatus());
        assertEquals("APPROVED", resolvedAlerts.get(0).getDecision());
        assertEquals(ADMIN_USERNAME, resolvedAlerts.get(0).getReviewingAdmin().getUsername());

        // =====================================================================
        // Step 12: Repeated approval is rejected
        // =====================================================================
        mockMvc.perform(post("/api/admin/transactions/" + pendingTxId + "/approve")
                        .session(adminAuth.session())
                        .cookie(adminAuth.cookie())
                        .header(adminAuth.csrfHeader(), adminAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isString());

        // Balances MUST NOT change on duplicate approval attempt!
        updatedSender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("0.00").compareTo(updatedSender.getBalance()));
        updatedReceiver = accountRepository.findById(receiverAccount.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("104500.00").compareTo(updatedReceiver.getBalance()));
    }

    @Test
    @DisplayName("Administrator Fraud Rejection Workflow leaves balances untouched and marks transaction DECLINED")
    void testAdministratorRejectionWorkflow() throws Exception {
        AuthContext customerAuth = login(SENDER_USERNAME, PASSWORD);

        // 1. Submit full-balance drain transfer (100,000.00 of 100,000.00) that triggers PENDING_REVIEW
        TransactionRequest fraudTxRequest = new TransactionRequest(
                new BigDecimal("100000.00"),
                "TRANSFER",
                receiverAccount.getAccountNumber()
        );

        MvcResult fraudTxResult = mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fraudTxRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
                .andReturn();

        JsonNode fraudNode = objectMapper.readTree(fraudTxResult.getResponse().getContentAsString());
        Long pendingTxId = fraudNode.get("transactionId").asLong();

        BigDecimal senderBalBefore = accountRepository.findById(senderAccount.getId()).orElseThrow().getBalance();
        BigDecimal receiverBalBefore = accountRepository.findById(receiverAccount.getId()).orElseThrow().getBalance();

        // 2. Admin logs in and rejects transaction
        AuthContext adminAuth = login(ADMIN_USERNAME, PASSWORD);
        ReviewRequest rejectRequest = new ReviewRequest("Fraud confirmed by security intelligence.");

        mockMvc.perform(post("/api/admin/transactions/" + pendingTxId + "/reject")
                        .session(adminAuth.session())
                        .cookie(adminAuth.cookie())
                        .header(adminAuth.csrfHeader(), adminAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("DECLINED"));

        // 3. Confirm balances remain strictly unchanged!
        BigDecimal senderBalAfter = accountRepository.findById(senderAccount.getId()).orElseThrow().getBalance();
        BigDecimal receiverBalAfter = accountRepository.findById(receiverAccount.getId()).orElseThrow().getBalance();
        assertEquals(0, senderBalBefore.compareTo(senderBalAfter), "Sender balance must not change on rejection");
        assertEquals(0, receiverBalBefore.compareTo(receiverBalAfter), "Receiver balance must not change on rejection");

        // 4. Confirm transaction status is DECLINED and alert is CLOSED
        Transaction rejectedTx = transactionRepository.findById(pendingTxId).orElseThrow();
        assertEquals("DECLINED", rejectedTx.getStatus());

        List<FraudAlert> closedAlerts = fraudAlertRepository.findByTransaction(rejectedTx);
        assertFalse(closedAlerts.isEmpty());
        assertEquals("CLOSED", closedAlerts.get(0).getStatus());
        assertEquals("REJECTED", closedAlerts.get(0).getDecision());

        // 5. Repeated rejection is rejected
        mockMvc.perform(post("/api/admin/transactions/" + pendingTxId + "/reject")
                        .session(adminAuth.session())
                        .cookie(adminAuth.cookie())
                        .header(adminAuth.csrfHeader(), adminAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Customer cannot access administrator surveillance and review endpoints (HTTP 403 Forbidden)")
    void testCustomerForbiddenFromAdminEndpoints() throws Exception {
        AuthContext customerAuth = login(SENDER_USERNAME, PASSWORD);

        // 1. Cannot view admin alerts
        mockMvc.perform(get("/api/admin/alerts")
                        .session(customerAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 2. Cannot view admin pending queue
        mockMvc.perform(get("/api/admin/pending")
                        .session(customerAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // 3. Cannot approve transactions
        ReviewRequest request = new ReviewRequest("Unauthorized attempt");
        mockMvc.perform(post("/api/admin/transactions/1/approve")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Customer registration, login, profile (/api/auth/me), and session invalidation")
    void testCustomerRegistrationLoginAndMe() throws Exception {
        String newUsername = "e2e_fresh_user";
        String newEmail = "fresh@e2e.test";
        String newPass = "FreshPass@123";

        // Clean up beforehand if exists
        userRepository.findByUsername(newUsername).ifPresent(u -> {
            accountRepository.findByUser(u).forEach(a -> {
                transactionRepository.findByAccountOrderByTransactionTimeDesc(a).forEach(transactionRepository::delete);
                accountRepository.delete(a);
            });
            userRepository.delete(u);
        });

        try {
            // 1. Fetch CSRF token for registration
            MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
            CsrfResponse csrf = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), CsrfResponse.class);
            Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

            // 2. Register new customer
            RegisterRequest regReq = new RegisterRequest(newUsername, newEmail, newPass);
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(xsrfCookie)
                            .header(csrf.headerName(), csrf.token())
                            .content(objectMapper.writeValueAsString(regReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.username").value(newUsername));

            // 3. Log in with registered customer
            AuthContext freshAuth = login(newUsername, newPass);

            // 4. Access /api/auth/me
            mockMvc.perform(get("/api/auth/me")
                            .session(freshAuth.session())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.username").value(newUsername))
                    .andExpect(jsonPath("$.email").value(newEmail))
                    .andExpect(jsonPath("$.role").value("USER"));

            // 5. Logout
            mockMvc.perform(post("/api/auth/logout")
                            .session(freshAuth.session())
                            .cookie(freshAuth.cookie())
                            .header(freshAuth.csrfHeader(), freshAuth.csrfToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // 6. Verify subsequent request with invalidated session returns 401
            mockMvc.perform(get("/api/auth/me")
                            .session(freshAuth.session())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

        } finally {
            userRepository.findByUsername(newUsername).ifPresent(u -> {
                accountRepository.findByUser(u).forEach(a -> {
                    transactionRepository.findByAccountOrderByTransactionTimeDesc(a).forEach(transactionRepository::delete);
                    accountRepository.delete(a);
                });
                userRepository.delete(u);
            });
        }
    }

    @Test
    @DisplayName("Authentication rejections: invalid credentials (401) and missing CSRF (403)")
    void testAuthenticationRejections() throws Exception {
        // 1. Invalid credentials -> 401
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        CsrfResponse csrf = objectMapper.readValue(csrfResult.getResponse().getContentAsString(), CsrfResponse.class);
        Cookie xsrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        LoginRequest badLogin = new LoginRequest(SENDER_USERNAME, "WrongPassword@999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(xsrfCookie)
                        .header(csrf.headerName(), csrf.token())
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        // 2. State-changing request without CSRF protection -> 403 Forbidden
        AuthContext auth = login(SENDER_USERNAME, PASSWORD);
        TransactionRequest req = new TransactionRequest(new BigDecimal("100.00"), "PAYMENT");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Banking operations: Deposit (CASH_IN), Withdrawal (CASH_OUT), and balance/amount validations")
    void testBankingDepositsWithdrawalsAndValidations() throws Exception {
        AuthContext auth = login(SENDER_USERNAME, PASSWORD);
        Account sender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        BigDecimal initialBal = sender.getBalance();

        // 1. Deposit (CASH_IN) 10,000.00
        TransactionRequest depositReq = new TransactionRequest(new BigDecimal("10000.00"), "CASH_IN");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .cookie(auth.cookie())
                        .header(auth.csrfHeader(), auth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        Account afterDeposit = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, initialBal.add(new BigDecimal("10000.00")).compareTo(afterDeposit.getBalance()));

        // 2. Withdrawal (CASH_OUT) 2,000.00
        TransactionRequest withdrawReq = new TransactionRequest(new BigDecimal("2000.00"), "CASH_OUT");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .cookie(auth.cookie())
                        .header(auth.csrfHeader(), auth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        Account afterWithdraw = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, afterDeposit.getBalance().subtract(new BigDecimal("2000.00")).compareTo(afterWithdraw.getBalance()));

        // 3. Insufficient balance -> DECLINED, balance unchanged (amount < limit 50,000 but > balance)
        // Temporarily set sender balance to 100.00
        sender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        BigDecimal savedBal = sender.getBalance();
        sender.setBalance(new BigDecimal("100.00"));
        accountRepository.save(sender);

        try {
            TransactionRequest overdraftReq = new TransactionRequest(new BigDecimal("500.00"), "CASH_OUT");
            mockMvc.perform(post("/api/transactions")
                            .session(auth.session())
                            .cookie(auth.cookie())
                            .header(auth.csrfHeader(), auth.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(overdraftReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DECLINED"));
        } finally {
            sender.setBalance(savedBal);
            accountRepository.save(sender);
        }

        Account afterOverdraft = accountRepository.findById(senderAccount.getId()).orElseThrow();
        assertEquals(0, afterWithdraw.getBalance().compareTo(afterOverdraft.getBalance()));

        // 4. Invalid amount validations (negative, zero, scale > 2) -> 400 Bad Request
        TransactionRequest negativeReq = new TransactionRequest(new BigDecimal("-100.00"), "PAYMENT");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .cookie(auth.cookie())
                        .header(auth.csrfHeader(), auth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeReq)))
                .andExpect(status().isBadRequest());

        TransactionRequest zeroReq = new TransactionRequest(BigDecimal.ZERO, "PAYMENT");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .cookie(auth.cookie())
                        .header(auth.csrfHeader(), auth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroReq)))
                .andExpect(status().isBadRequest());

        TransactionRequest badScaleReq = new TransactionRequest(new BigDecimal("10.999"), "PAYMENT");
        mockMvc.perform(post("/api/transactions")
                        .session(auth.session())
                        .cookie(auth.cookie())
                        .header(auth.csrfHeader(), auth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badScaleReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Receiver visibility: Incoming transfer is visible in receiver transaction history and receipt")
    void testReceiverTransferVisibilityAndOwnership() throws Exception {
        AuthContext senderAuth = login(SENDER_USERNAME, PASSWORD);

        // 1. Sender transfers 1,234.00 to Receiver
        TransactionRequest transferReq = new TransactionRequest(
                new BigDecimal("1234.00"),
                "TRANSFER",
                receiverAccount.getAccountNumber()
        );

        MvcResult transferResult = mockMvc.perform(post("/api/transactions")
                        .session(senderAuth.session())
                        .cookie(senderAuth.cookie())
                        .header(senderAuth.csrfHeader(), senderAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andReturn();

        JsonNode txNode = objectMapper.readTree(transferResult.getResponse().getContentAsString());
        Long transferTxId = txNode.get("transactionId").asLong();

        // 2. Receiver logs in
        AuthContext receiverAuth = login(RECEIVER_USERNAME, PASSWORD);

        // 3. Receiver checks transaction history (/api/transactions)
        MvcResult receiverHistory = mockMvc.perform(get("/api/transactions")
                        .session(receiverAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyItems = objectMapper.readTree(receiverHistory.getResponse().getContentAsString());
        boolean receiverFoundTx = false;
        for (JsonNode item : historyItems) {
            if (item.get("id").asLong() == transferTxId) {
                receiverFoundTx = true;
                assertEquals("APPROVED", item.get("status").asText());
                assertEquals("TRANSFER", item.get("transactionType").asText());
                break;
            }
        }
        assertTrue(receiverFoundTx, "Receiver must see the incoming transfer in their transaction history");

        // 4. Receiver views transaction receipt (/api/transactions/{id}) -> 200 OK
        mockMvc.perform(get("/api/transactions/" + transferTxId)
                        .session(receiverAuth.session())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").value(transferTxId));

        // 5. Unrelated third customer attempts cross-user access -> 403 Forbidden
        User thirdUser = new User();
        thirdUser.setUsername("e2e_third_user");
        thirdUser.setEmail("third@e2e.test");
        thirdUser.setPassword(passwordEncoder.encode(PASSWORD));
        thirdUser.setRole("USER");
        thirdUser = userRepository.save(thirdUser);
        try {
            AuthContext thirdAuth = login("e2e_third_user", PASSWORD);
            mockMvc.perform(get("/api/transactions/" + transferTxId)
                            .session(thirdAuth.session())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        } finally {
            userRepository.delete(thirdUser);
        }
    }

    @Test
    @DisplayName("Duplicate transaction submission prevention blocks rapid identical submissions within 2 seconds")
    void testDuplicateTransactionSubmissionPrevention() throws Exception {
        AuthContext customerAuth = login(SENDER_USERNAME, PASSWORD);

        TransactionRequest uniqueReq = new TransactionRequest(
                new BigDecimal("777.77"),
                "PAYMENT"
        );

        // First submission succeeds
        mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Immediate duplicate submission within 2 seconds is blocked
        mockMvc.perform(post("/api/transactions")
                        .session(customerAuth.session())
                        .cookie(customerAuth.cookie())
                        .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Duplicate transaction detected")));
    }

    @Test
    @DisplayName("Inactive or blocked account cannot perform transactions")
    void testBlockedAccountCannotTransact() throws Exception {
        AuthContext customerAuth = login(SENDER_USERNAME, PASSWORD);

        // Temporarily suspend sender account
        Account sender = accountRepository.findById(senderAccount.getId()).orElseThrow();
        sender.setStatus("BLOCKED");
        accountRepository.save(sender);

        try {
            TransactionRequest req = new TransactionRequest(new BigDecimal("100.00"), "PAYMENT");
            mockMvc.perform(post("/api/transactions")
                            .session(customerAuth.session())
                            .cookie(customerAuth.cookie())
                            .header(customerAuth.csrfHeader(), customerAuth.csrfToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Account is not active")));
        } finally {
            // Restore status
            sender.setStatus("ACTIVE");
            accountRepository.save(sender);
        }
    }
}
