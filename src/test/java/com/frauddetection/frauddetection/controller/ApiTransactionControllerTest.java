package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ApiTransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudPredictionRepository fraudPredictionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ApiTransactionController controller;

    private User testUser;
    private Account testAccount;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole("USER");

        testAccount = new Account();
        testAccount.setId(10L);
        testAccount.setUser(testUser);
        testAccount.setAccountNumber("ACC-112233");
        testAccount.setBalance(new BigDecimal("50000.00"));

        mockRequest = mock(HttpServletRequest.class);
    }

    @Test
    @DisplayName("createTransaction returns 401 when authentication is null or unauthenticated")
    void createTransaction_unauthenticated_returns401() {
        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("100.00"), "PAYMENT"),
                null,
                mockRequest
        );

        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().success());
        assertEquals("Authentication required.", res.getBody().message());
    }

    @Test
    @DisplayName("createTransaction returns 400 when payload is null")
    void createTransaction_nullPayload_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> res = controller.createTransaction(null, authentication, mockRequest);

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().success());
        assertEquals("Transaction request payload cannot be empty.", res.getBody().message());
    }

    @Test
    @DisplayName("createTransaction returns 400 when amount is null")
    void createTransaction_nullAmount_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(null, "PAYMENT"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertFalse(res.getBody().success());
        assertEquals("Transaction amount is required.", res.getBody().message());
    }

    @Test
    @DisplayName("createTransaction returns 400 when amount is zero or negative")
    void createTransaction_nonPositiveAmount_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> zeroRes = controller.createTransaction(
                new TransactionRequest(BigDecimal.ZERO, "PAYMENT"),
                authentication,
                mockRequest
        );
        assertEquals(HttpStatus.BAD_REQUEST, zeroRes.getStatusCode());
        assertEquals("Amount must be greater than zero.", zeroRes.getBody().message());

        ResponseEntity<TransactionResponse> negRes = controller.createTransaction(
                new TransactionRequest(new BigDecimal("-50.00"), "PAYMENT"),
                authentication,
                mockRequest
        );
        assertEquals(HttpStatus.BAD_REQUEST, negRes.getStatusCode());
        assertEquals("Amount must be greater than zero.", negRes.getBody().message());
    }

    @Test
    @DisplayName("createTransaction returns 400 when amount scale exceeds 2 decimal places")
    void createTransaction_invalidScale_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("100.555"), "PAYMENT"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertEquals("Amount cannot have more than 2 decimal places.", res.getBody().message());
    }

    @Test
    @DisplayName("createTransaction returns 400 when transaction type is missing or invalid")
    void createTransaction_invalidTransactionType_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> nullType = controller.createTransaction(
                new TransactionRequest(new BigDecimal("100.00"), null),
                authentication,
                mockRequest
        );
        assertEquals(HttpStatus.BAD_REQUEST, nullType.getStatusCode());
        assertEquals("Transaction type is required.", nullType.getBody().message());

        ResponseEntity<TransactionResponse> invalidType = controller.createTransaction(
                new TransactionRequest(new BigDecimal("100.00"), "UNKNOWN_TYPE"),
                authentication,
                mockRequest
        );
        assertEquals(HttpStatus.BAD_REQUEST, invalidType.getStatusCode());
        assertTrue(invalidType.getBody().message().contains("Invalid transaction type"));
    }

    @Test
    @DisplayName("createTransaction successfully returns 200 with APPROVED status")
    void createTransaction_approved_returns200() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findTopByUserOrderByIdAsc(testUser)).thenReturn(Optional.of(testAccount));

        Transaction tx = new Transaction();
        tx.setId(101L);
        tx.setAmount(new BigDecimal("500.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setStatus("APPROVED");
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 9, 14, 0, 0));

        FraudPrediction prediction = new FraudPrediction();
        prediction.setTransaction(tx);
        prediction.setPrediction("LEGITIMATE");
        prediction.setProbability(0.02);
        prediction.setRuleAnomaly(false);
        prediction.setDetectionReason("Normal / Legitimate");

        when(transactionService.processPayment(
                eq(testUser),
                eq(new BigDecimal("500.00")),
                eq(TransactionType.PAYMENT),
                any(),
                any()
        )).thenReturn(prediction);

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("500.00"), "PAYMENT"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().success());
        assertEquals("APPROVED", res.getBody().status());
        assertEquals(101L, res.getBody().transactionId());
        assertEquals(new BigDecimal("500.00"), res.getBody().amount());
        assertEquals("PAYMENT", res.getBody().transactionType());
        assertEquals("LOW", res.getBody().riskAssessment());
        assertEquals("Normal / Legitimate", res.getBody().detectionReason());
    }

    @Test
    @DisplayName("createTransaction returns 200 with DECLINED status when limit exceeded or insufficient funds")
    void createTransaction_declined_returns200() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findTopByUserOrderByIdAsc(testUser)).thenReturn(Optional.of(testAccount));

        Transaction tx = new Transaction();
        tx.setId(102L);
        tx.setAmount(new BigDecimal("150000.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setStatus("DECLINED");
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 9, 14, 5, 0));

        FraudPrediction prediction = new FraudPrediction();
        prediction.setTransaction(tx);
        prediction.setPrediction("LEGITIMATE");
        prediction.setProbability(0.05);
        prediction.setRuleAnomaly(false);
        prediction.setDetectionReason("Limit Exceeded (Max ₹100,000)");

        when(transactionService.processPayment(
                eq(testUser),
                eq(new BigDecimal("150000.00")),
                eq(TransactionType.PAYMENT),
                any(),
                any()
        )).thenReturn(prediction);

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("150000.00"), "PAYMENT"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().success());
        assertEquals("DECLINED", res.getBody().status());
        assertEquals(102L, res.getBody().transactionId());
        assertTrue(res.getBody().detectionReason().contains("Limit Exceeded"));
    }

    @Test
    @DisplayName("createTransaction returns 200 with BLOCKED status when flagged by ML fraud detector")
    void createTransaction_blocked_returns200() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findTopByUserOrderByIdAsc(testUser)).thenReturn(Optional.of(testAccount));

        Transaction tx = new Transaction();
        tx.setId(103L);
        tx.setAmount(new BigDecimal("49000.00"));
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setStatus("BLOCKED");
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 9, 14, 10, 0));

        FraudPrediction prediction = new FraudPrediction();
        prediction.setTransaction(tx);
        prediction.setPrediction("FRAUD");
        prediction.setProbability(0.96);
        prediction.setRuleAnomaly(true);
        prediction.setDetectionReason("Machine Learning & Behavioral Anomaly");

        when(transactionService.processPayment(
                eq(testUser),
                eq(new BigDecimal("49000.00")),
                eq(TransactionType.TRANSFER),
                any(),
                any()
        )).thenReturn(prediction);

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("49000.00"), "TRANSFER"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertTrue(res.getBody().success());
        assertEquals("BLOCKED", res.getBody().status());
        assertEquals("HIGH", res.getBody().riskAssessment());
        assertEquals(0.96, res.getBody().fraudProbability());
        assertEquals("Machine Learning & Behavioral Anomaly", res.getBody().detectionReason());
    }

    @Test
    @DisplayName("createTransaction catches TransactionException and returns 400 Bad Request")
    void createTransaction_serviceException_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        when(transactionService.processPayment(
                eq(testUser),
                eq(new BigDecimal("500.00")),
                eq(TransactionType.PAYMENT),
                any(),
                any()
        )).thenThrow(new TransactionException("Transaction failed: account locked"));

        ResponseEntity<TransactionResponse> res = controller.createTransaction(
                new TransactionRequest(new BigDecimal("500.00"), "PAYMENT"),
                authentication,
                mockRequest
        );

        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertNotNull(res.getBody());
        assertFalse(res.getBody().success());
        assertEquals("Transaction failed: account locked", res.getBody().message());
    }

    // =========================================================================
    // GET /api/transactions Tests
    // =========================================================================

    @Test
    @DisplayName("getTransactions returns 401 when authentication is null or unauthenticated")
    void getTransactions_unauthenticated_returns401() {
        ResponseEntity<?> res = controller.getTransactions(null, null, null, null);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());

        when(authentication.isAuthenticated()).thenReturn(false);
        ResponseEntity<?> unauthRes = controller.getTransactions(null, null, null, authentication);
        assertEquals(HttpStatus.UNAUTHORIZED, unauthRes.getStatusCode());
    }

    @Test
    @DisplayName("getTransactions returns 400 when invalid transaction type filter is provided")
    void getTransactions_invalidType_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> res = controller.getTransactions("INVALID_TYPE", null, null, authentication);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().toString().contains("Invalid transaction type filter"));
    }

    @Test
    @DisplayName("getTransactions returns 400 when invalid status filter is provided")
    void getTransactions_invalidStatus_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> res = controller.getTransactions(null, "INVALID_STATUS", null, authentication);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        assertTrue(res.getBody().toString().contains("Invalid transaction status filter"));
    }

    @Test
    @DisplayName("getTransactions returns user transaction history sorted newest first")
    void getTransactions_returnsHistory_newestFirst() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUser(testUser)).thenReturn(List.of(testAccount));

        Transaction t1 = new Transaction();
        t1.setId(1L);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setTransactionType(TransactionType.PAYMENT);
        t1.setStatus("APPROVED");
        t1.setTransactionTime(LocalDateTime.of(2026, 10, 1, 10, 0));

        Transaction t2 = new Transaction();
        t2.setId(2L);
        t2.setAmount(new BigDecimal("200.00"));
        t2.setTransactionType(TransactionType.TRANSFER);
        t2.setStatus("BLOCKED");
        t2.setTransactionTime(LocalDateTime.of(2026, 10, 2, 12, 0));

        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(testAccount))
                .thenReturn(List.of(t1, t2));

        ResponseEntity<?> res = controller.getTransactions(null, null, null, authentication);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        @SuppressWarnings("unchecked")
        List<TransactionSummaryDto> list = (List<TransactionSummaryDto>) res.getBody();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(2L, list.get(0).id()); // Newest first
        assertEquals(1L, list.get(1).id());
    }

    @Test
    @DisplayName("getTransactions filters by type and status")
    void getTransactions_filtersByTypeAndStatus() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUser(testUser)).thenReturn(List.of(testAccount));

        Transaction t1 = new Transaction();
        t1.setId(1L);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setTransactionType(TransactionType.PAYMENT);
        t1.setStatus("APPROVED");

        Transaction t2 = new Transaction();
        t2.setId(2L);
        t2.setAmount(new BigDecimal("200.00"));
        t2.setTransactionType(TransactionType.TRANSFER);
        t2.setStatus("APPROVED");

        Transaction t3 = new Transaction();
        t3.setId(3L);
        t3.setAmount(new BigDecimal("300.00"));
        t3.setTransactionType(TransactionType.TRANSFER);
        t3.setStatus("BLOCKED");

        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(testAccount))
                .thenReturn(List.of(t1, t2, t3));

        // Filter by TRANSFER and APPROVED -> should match t2 only
        ResponseEntity<?> res = controller.getTransactions("TRANSFER", "APPROVED", null, authentication);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        @SuppressWarnings("unchecked")
        List<TransactionSummaryDto> list = (List<TransactionSummaryDto>) res.getBody();
        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).id());
    }

    @Test
    @DisplayName("getTransactions search filter matches transaction ID or amount")
    void getTransactions_searchFilter_matchesIdOrAmount() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUser(testUser)).thenReturn(List.of(testAccount));

        Transaction t1 = new Transaction();
        t1.setId(101L);
        t1.setAmount(new BigDecimal("4500.00"));
        t1.setTransactionType(TransactionType.PAYMENT);
        t1.setStatus("APPROVED");

        Transaction t2 = new Transaction();
        t2.setId(202L);
        t2.setAmount(new BigDecimal("9900.00"));
        t2.setTransactionType(TransactionType.TRANSFER);
        t2.setStatus("DECLINED");

        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(testAccount))
                .thenReturn(List.of(t1, t2));

        // Search by ID with '#' prefix
        ResponseEntity<?> resId = controller.getTransactions(null, null, "#101", authentication);
        assertEquals(HttpStatus.OK, resId.getStatusCode());
        @SuppressWarnings("unchecked")
        List<TransactionSummaryDto> listId = (List<TransactionSummaryDto>) resId.getBody();
        assertEquals(1, listId.size());
        assertEquals(101L, listId.get(0).id());

        // Search by amount substring
        ResponseEntity<?> resAmt = controller.getTransactions(null, null, "9900", authentication);
        assertEquals(HttpStatus.OK, resAmt.getStatusCode());
        @SuppressWarnings("unchecked")
        List<TransactionSummaryDto> listAmt = (List<TransactionSummaryDto>) resAmt.getBody();
        assertEquals(1, listAmt.size());
        assertEquals(202L, listAmt.get(0).id());
    }

    // =========================================================================
    // GET /api/transactions/{id} Tests
    // =========================================================================

    @Test
    @DisplayName("getTransactionDetails returns 401 when unauthenticated")
    void getTransactionDetails_unauthenticated_returns401() {
        ResponseEntity<TransactionResponse> res = controller.getTransactionDetails(1L, null);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }

    @Test
    @DisplayName("getTransactionDetails returns 400 when ID is invalid (null or <= 0)")
    void getTransactionDetails_invalidId_returns400() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<TransactionResponse> resZero = controller.getTransactionDetails(0L, authentication);
        assertEquals(HttpStatus.BAD_REQUEST, resZero.getStatusCode());

        ResponseEntity<TransactionResponse> resNeg = controller.getTransactionDetails(-5L, authentication);
        assertEquals(HttpStatus.BAD_REQUEST, resNeg.getStatusCode());
    }

    @Test
    @DisplayName("getTransactionDetails returns 404 when transaction is not found")
    void getTransactionDetails_notFound_returns404() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<TransactionResponse> res = controller.getTransactionDetails(999L, authentication);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertTrue(res.getBody().message().contains("not found"));
    }

    @Test
    @DisplayName("getTransactionDetails returns 403 Forbidden on cross-user access attempt")
    void getTransactionDetails_crossUserAccess_returns403() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setUsername("otheruser");

        Account otherAccount = new Account();
        otherAccount.setUser(otherUser);

        Transaction otherTx = new Transaction();
        otherTx.setId(500L);
        otherTx.setAccount(otherAccount);

        when(transactionRepository.findById(500L)).thenReturn(Optional.of(otherTx));

        ResponseEntity<TransactionResponse> res = controller.getTransactionDetails(500L, authentication);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
        assertFalse(res.getBody().success());
        assertTrue(res.getBody().message().contains("Access denied"));
    }

    @Test
    @DisplayName("getTransactionDetails returns 200 with full details when owner requests")
    void getTransactionDetails_owner_returns200WithFullDetails() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Transaction tx = new Transaction();
        tx.setId(700L);
        tx.setAccount(testAccount);
        tx.setAmount(new BigDecimal("12000.00"));
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setStatus("BLOCKED");
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 5, 11, 30));

        FraudPrediction pred = new FraudPrediction();
        pred.setPrediction("FRAUD");
        pred.setProbability(0.92);
        pred.setRuleAnomaly(true);
        pred.setDetectionReason("High velocity spike");

        when(transactionRepository.findById(700L)).thenReturn(Optional.of(tx));
        when(fraudPredictionRepository.findByTransaction(tx)).thenReturn(Optional.of(pred));

        ResponseEntity<TransactionResponse> res = controller.getTransactionDetails(700L, authentication);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().success());
        assertEquals(700L, res.getBody().transactionId());
        assertEquals("BLOCKED", res.getBody().status());
        assertEquals("HIGH", res.getBody().riskAssessment());
        assertEquals(0.92, res.getBody().fraudProbability());
        assertEquals("High velocity spike", res.getBody().detectionReason());
        assertEquals(new BigDecimal("50000.00"), res.getBody().newBalance());
    }

    @Test
    @DisplayName("getTransactionDetails returns 200 when ADMIN requests another user's transaction")
    void getTransactionDetails_admin_returns200() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("adminuser");
        adminUser.setRole("ROLE_ADMIN");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("adminuser");
        when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(adminUser));

        User otherUser = new User();
        otherUser.setId(99L);
        Account otherAccount = new Account();
        otherAccount.setUser(otherUser);
        otherAccount.setBalance(new BigDecimal("1000.00"));

        Transaction otherTx = new Transaction();
        otherTx.setId(800L);
        otherTx.setAccount(otherAccount);
        otherTx.setAmount(new BigDecimal("500.00"));
        otherTx.setTransactionType(TransactionType.PAYMENT);
        otherTx.setStatus("APPROVED");

        when(transactionRepository.findById(800L)).thenReturn(Optional.of(otherTx));
        when(fraudPredictionRepository.findByTransaction(otherTx)).thenReturn(Optional.empty());

        ResponseEntity<TransactionResponse> res = controller.getTransactionDetails(800L, authentication);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertTrue(res.getBody().success());
        assertEquals(800L, res.getBody().transactionId());
        assertEquals("APPROVED", res.getBody().status());
    }
}
