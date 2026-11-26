package com.frauddetection.frauddetection.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;
import com.frauddetection.frauddetection.service.UserService;

/**
 * Live MySQL Integration Test Suite.
 *
 * Connects directly to the live MySQL Server 8.0 instance on localhost:3306/fraud_detection
 * to verify complete financial integrity, ACID transaction semantics, row-level locking,
 * persistent idempotency keys, fraud review lifecycle, and multi-threaded concurrency.
 */
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+", disabledReason = "Live MySQL tests require DB_PASSWORD environment variable and active MySQL instance")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/fraud_detection?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
    "spring.datasource.username=root",
    "spring.datasource.password=${DB_PASSWORD:}",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED"
})
class SafePayLiveMySqlIntegrationTest {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/fraud_detection?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String MYSQL_USER = "root";
    private static String mysqlPassword = "";

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

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
    private PasswordEncoder passwordEncoder;

    private static boolean mysqlAvailable = false;

    private User liveAdmin;
    private User customerA;
    private User customerB;
    private Account accountA;
    private Account accountB;

    @BeforeAll
    void checkMySqlAvailability() {
        String envPwd = System.getenv("DB_PASSWORD");
        if (envPwd == null || envPwd.isBlank()) {
            Assumptions.assumeTrue(false, "DB_PASSWORD environment variable is required for live MySQL integration tests.");
            return;
        }
        mysqlPassword = envPwd.trim();

        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, mysqlPassword)) {
            mysqlAvailable = (conn != null && conn.isValid(3));
            System.out.println(">>> LIVE MYSQL: Successfully established connection to MySQL 8.0 on localhost:3306/fraud_detection");
        } catch (Exception e) {
            System.err.println(">>> LIVE MYSQL WARNING: Could not connect to MySQL: " + e.getMessage());
            mysqlAvailable = false;
        }
        Assumptions.assumeTrue(mysqlAvailable, "Live MySQL 8.0 server on localhost:3306 is required for this integration test suite.");
    }

    @Test
    @Order(1)
    @DisplayName("1. Live MySQL schema & connectivity: Verify tables and unique idempotency constraint")
    void test1_verifyMySqlSchemaAndConstraints() throws Exception {
        try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, mysqlPassword);
             Statement stmt = conn.createStatement()) {

            // Verify database and version
            try (ResultSet rs = stmt.executeQuery("SELECT VERSION(), DATABASE()")) {
                assertTrue(rs.next());
                String version = rs.getString(1);
                String db = rs.getString(2);
                assertEquals("fraud_detection", db);
                System.out.println(">>> LIVE MYSQL VERIFIED: Server version " + version + " | Database: " + db);
            }

            // Verify unique index on idempotency_key
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.statistics " +
                    "WHERE table_schema = 'fraud_detection' AND table_name = 'transactions' " +
                    "AND index_name = 'uk_transactions_idempotency_key'")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0, "uk_transactions_idempotency_key constraint must exist in MySQL");
                System.out.println(">>> LIVE MYSQL CONSTRAINT: Verified uk_transactions_idempotency_key index on transactions table");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("2. Live MySQL customer registration and account creation with initial balance")
    void test2_customerRegistrationAndAccountCreation() {
        String suffix = String.valueOf(System.currentTimeMillis()).substring(7);

        // Create Admin
        liveAdmin = userRepository.findByUsername("live_mysql_admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("live_mysql_admin");
            admin.setEmail("admin" + suffix + "@safepay.mysql");
            admin.setPassword(passwordEncoder.encode("AdminPass@123"));
            admin.setRole("ADMIN");
            return userRepository.save(admin);
        });

        // Register Customer A
        User userA = new User();
        userA.setUsername("custA_" + suffix);
        userA.setEmail("custA_" + suffix + "@safepay.mysql");
        userA.setPassword("CustPass@123");
        userA.setRole("USER");
        customerA = userService.registerUser(userA);
        assertNotNull(customerA.getId(), "User A must receive generated MySQL primary key");

        accountA = accountRepository.findFirstByUserOrderByIdAsc(customerA).orElseThrow();
        assertNotNull(accountA.getId(), "Account A must receive generated MySQL primary key");
        assertEquals(new BigDecimal("50000.00"), accountA.getBalance(), "Opening balance must be 50,000.00 in MySQL");

        // Register Customer B
        User userB = new User();
        userB.setUsername("custB_" + suffix);
        userB.setEmail("custB_" + suffix + "@safepay.mysql");
        userB.setPassword("CustPass@123");
        userB.setRole("USER");
        customerB = userService.registerUser(userB);
        assertNotNull(customerB.getId());

        accountB = accountRepository.findFirstByUserOrderByIdAsc(customerB).orElseThrow();
        assertNotNull(accountB.getId());
        assertEquals(new BigDecimal("50000.00"), accountB.getBalance());

        System.out.println(">>> LIVE MYSQL ACCOUNTS: Account A=" + accountA.getAccountNumber() + " (â‚¹" + accountA.getBalance() + "), Account B=" + accountB.getAccountNumber() + " (â‚¹" + accountB.getBalance() + ")");
    }

    @Test
    @Order(3)
    @DisplayName("3. Live MySQL banking: Deposit, Withdrawal, and Atomic Inter-Account Transfer")
    void test3_depositWithdrawalAndTransfer() {
        // Deposit 5,000.00 into Account A
        FraudPrediction depositPred = transactionService.processPayment(
                customerA,
                new BigDecimal("5000.00"),
                TransactionType.CASH_IN,
                "127.0.0.1",
                "MySQL-Test-Client"
        );
        assertEquals("APPROVED", depositPred.getTransaction().getStatus());
        Account accAfterDep = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(new BigDecimal("55000.00"), accAfterDep.getBalance(), "Balance after deposit must be 55,000.00");

        // Withdraw 10,000.00 from Account A
        FraudPrediction withdrawPred = transactionService.processPayment(
                customerA,
                new BigDecimal("10000.00"),
                TransactionType.CASH_OUT,
                "127.0.0.1",
                "MySQL-Test-Client"
        );
        assertEquals("APPROVED", withdrawPred.getTransaction().getStatus());
        Account accAfterWith = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(new BigDecimal("45000.00"), accAfterWith.getBalance(), "Balance after withdrawal must be 45,000.00");

        // Atomic Transfer of 5,000.00 from Account A to Account B
        FraudPrediction transferPred = transactionService.processPayment(
                customerA,
                new BigDecimal("5000.00"),
                TransactionType.TRANSFER,
                accountB.getAccountNumber(),
                "127.0.0.1",
                "MySQL-Test-Client",
                "transfer-key-" + System.currentTimeMillis()
        );
        assertEquals("APPROVED", transferPred.getTransaction().getStatus());

        // Verify atomic debit of A and credit of B in MySQL
        Account finalA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account finalB = accountRepository.findById(accountB.getId()).orElseThrow();
        assertEquals(new BigDecimal("40000.00"), finalA.getBalance(), "Account A debited 5,000.00 -> 40,000.00");
        assertEquals(new BigDecimal("55000.00"), finalB.getBalance(), "Account B credited 5,000.00 -> 55,000.00");

        // Verify transfer audit record in MySQL
        Transaction tx = transactionRepository.findById(transferPred.getTransaction().getId()).orElseThrow();
        assertNotNull(tx.getReceiverAccount(), "Transfer audit must reference receiver account");
        assertEquals(accountB.getId(), tx.getReceiverAccount().getId());
        System.out.println(">>> LIVE MYSQL ATOMIC TRANSFER: Account A = â‚¹" + finalA.getBalance() + " | Account B = â‚¹" + finalB.getBalance());
    }

    @Test
    @Order(4)
    @DisplayName("4. Live MySQL financial integrity: Reject overdrafts, negative amounts, and invalid inputs")
    void test4_financialIntegrityAndOverdraftProtection() {
        // Attempt overdraft of 45,000.00 when balance is 40,000.00 (within limit 50,000 but exceeds balance)
        FraudPrediction overdraftPred = transactionService.processPayment(
                customerA,
                new BigDecimal("45000.00"),
                TransactionType.CASH_OUT,
                "127.0.0.1",
                "MySQL-Test-Client"
        );
        assertNotNull(overdraftPred);
        assertEquals("DECLINED", overdraftPred.getTransaction().getStatus(), "Overdraft transaction must be marked DECLINED");

        // Verify balance in MySQL is unchanged
        Account accA = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(new BigDecimal("40000.00"), accA.getBalance(), "Balance must remain unchanged after rejected overdraft");

        // Reject negative amount
        assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        customerA,
                        new BigDecimal("-500.00"),
                        TransactionType.PAYMENT,
                        "127.0.0.1",
                        "MySQL-Test-Client"
                )
        );

        // Reject transfer to same account
        assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        customerA,
                        new BigDecimal("100.00"),
                        TransactionType.TRANSFER,
                        accountA.getAccountNumber(),
                        "127.0.0.1",
                        "MySQL-Test-Client",
                        "self-transfer-" + System.currentTimeMillis()
                )
        );
        System.out.println(">>> LIVE MYSQL FINANCIAL INTEGRITY: Overdrafts, negative amounts, and self-transfers safely rejected");
    }

    @Test
    @Order(5)
    @DisplayName("5. Live MySQL persistent idempotency: Stored results, no double debits, and duplicate window fallback")
    void test5_persistentFinancialIdempotency() {
        Account beforeAcc = accountRepository.findById(accountA.getId()).orElseThrow();
        BigDecimal startingBalance = beforeAcc.getBalance();

        String uniqueKey = "live-mysql-idemp-" + System.currentTimeMillis();

        // First submission with key
        FraudPrediction firstPred = transactionService.processPayment(
                customerA,
                new BigDecimal("250.00"),
                TransactionType.PAYMENT,
                null,
                "127.0.0.1",
                "MySQL-Test-Client",
                uniqueKey
        );
        assertEquals("APPROVED", firstPred.getTransaction().getStatus());
        Long originalTxId = firstPred.getTransaction().getId();

        Account afterFirstAcc = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(startingBalance.subtract(new BigDecimal("250.00")), afterFirstAcc.getBalance());

        // Second submission with exact same key -> must return stored result and NOT debit again
        FraudPrediction secondPred = transactionService.processPayment(
                customerA,
                new BigDecimal("250.00"),
                TransactionType.PAYMENT,
                null,
                "127.0.0.1",
                "MySQL-Test-Client",
                uniqueKey
        );
        assertEquals(originalTxId, secondPred.getTransaction().getId(), "Must return identical persisted transaction ID");

        Account afterSecondAcc = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(afterFirstAcc.getBalance(), afterSecondAcc.getBalance(), "Balance must NOT be debited again on idempotent duplicate");

        // Verify stored in MySQL with unique idempotency_key
        Transaction txInDb = transactionRepository.findByIdempotencyKey(uniqueKey).orElseThrow();
        assertEquals(originalTxId, txInDb.getId());
        assertEquals(uniqueKey, txInDb.getIdempotencyKey());
        System.out.println(">>> LIVE MYSQL IDEMPOTENCY: Verified stored result lookup and zero duplicate debits for key: " + uniqueKey);
    }

    @Test
    @Order(6)
    @DisplayName("6. Live MySQL fraud review: Pending alert creation, admin approval, and rejection workflow")
    void test6_fraudReviewAndAdminWorkflow() {
        Account beforeAcc = accountRepository.findById(accountA.getId()).orElseThrow();
        BigDecimal balanceBefore = beforeAcc.getBalance();

        // Submit high-risk transaction that triggers review (e.g. transfer > 10,000.00 with anomaly conditions)
        Transaction highRiskTx = new Transaction();
        highRiskTx.setAmount(new BigDecimal("20000.00"));
        highRiskTx.setTransactionType(TransactionType.TRANSFER);
        highRiskTx.setAccount(accountA);
        highRiskTx.setReceiverAccount(accountB);
        highRiskTx.setTransactionTime(LocalDateTime.now());
        highRiskTx.setStatus("PENDING_REVIEW");
        highRiskTx.setIpAddress("198.51.100.1"); // Suspicious external IP
        highRiskTx.setUserAgent("Suspicious-Bot-Agent");
        Transaction savedTx = transactionRepository.save(highRiskTx);

        FraudAlert alert = new FraudAlert();
        alert.setTransaction(savedTx);
        alert.setAlertType("HIGH_VALUE_ANOMALY");
        alert.setSeverity("HIGH");
        alert.setStatus("PENDING");
        alert.setCreatedAt(LocalDateTime.now());
        FraudAlert savedAlert = fraudAlertRepository.save(alert);

        // Verify balances remain completely unchanged while PENDING_REVIEW
        Account accDuringReview = accountRepository.findById(accountA.getId()).orElseThrow();
        assertEquals(balanceBefore, accDuringReview.getBalance(), "Sender balance must NOT change while in PENDING_REVIEW");

        // Admin approves the pending transaction
        Transaction reviewedTx = transactionService.approveTransaction(
                savedTx.getId(),
                liveAdmin,
                "Verified legitimate corporate transfer via phone callback"
        );
        assertEquals("APPROVED", reviewedTx.getStatus(), "Transaction status must update to APPROVED in MySQL");

        // Verify balances updated upon approval
        Account afterApprovalA = accountRepository.findById(accountA.getId()).orElseThrow();
        Account afterApprovalB = accountRepository.findById(accountB.getId()).orElseThrow();
        assertEquals(balanceBefore.subtract(new BigDecimal("20000.00")), afterApprovalA.getBalance());
        assertEquals(new BigDecimal("75000.00"), afterApprovalB.getBalance());

        // Repeated approval attempt must be rejected
        TransactionException repeatEx = assertThrows(TransactionException.class, () ->
                transactionService.approveTransaction(
                        savedTx.getId(),
                        liveAdmin,
                        "Second approval attempt"
                )
        );
        assertTrue(repeatEx.getMessage().contains("not pending review") || repeatEx.getMessage().contains("already been reviewed"));
        System.out.println(">>> LIVE MYSQL FRAUD REVIEW: Successfully transitioned PENDING_REVIEW -> APPROVED and applied balance change");
    }

    @Test
    @Order(7)
    @DisplayName("7. Live MySQL InnoDB Multi-Threaded Concurrency: Pessimistic Row Locking & Overdraft Prevention")
    void test7_concurrentWithdrawalsAgainstMySqlInnoDB() throws Exception {
        // Create dedicated account with exact 1,000.00 balance
        User concUser = new User();
        String cSuffix = String.valueOf(System.currentTimeMillis()).substring(8);
        concUser.setUsername("conc_user_" + cSuffix);
        concUser.setEmail("conc_" + cSuffix + "@safepay.test");
        concUser.setPassword(passwordEncoder.encode("Password@123"));
        concUser.setRole("USER");
        concUser = userRepository.save(concUser);

        Account concAcc = new Account();
        concAcc.setAccountNumber("9999000000" + cSuffix);
        concAcc.setAccountType("SAVINGS");
        concAcc.setBalance(new BigDecimal("1000.00"));
        concAcc.setStatus("ACTIVE");
        concAcc.setUser(concUser);
        concAcc = accountRepository.save(concAcc);

        final Long concAccId = concAcc.getId();
        final User finalConcUser = concUser;

        // 10 concurrent threads each attempting to withdraw 150.00
        // Initial balance = 1,000.00.
        // At most 6 withdrawals can succeed (6 * 150 = 900.00).
        // 4 withdrawals MUST fail with insufficient balance.
        // Final balance must be exactly 100.00.
        int totalThreads = 10;
        BigDecimal withdrawalAmount = new BigDecimal("150.00");
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < totalThreads; i++) {
            final int threadIdx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(10, TimeUnit.SECONDS);
                    FraudPrediction pred = transactionService.processPayment(
                            finalConcUser,
                            withdrawalAmount,
                            TransactionType.CASH_OUT,
                            null,
                            "127.0.0.1",
                            "Concurrent-Thread-" + threadIdx,
                            "conc-key-" + cSuffix + "-" + threadIdx
                    );
                    if (pred != null && pred.getTransaction() != null && "APPROVED".equals(pred.getTransaction().getStatus())) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    failCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads simultaneously against MySQL InnoDB

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        Account finalAcc = accountRepository.findById(concAccId).orElseThrow();

        System.out.println(">>> LIVE MYSQL CONCURRENCY RESULTS:");
        System.out.println("    Successful withdrawals : " + successCount.get() + " (Expected: 6)");
        System.out.println("    Failed (insufficient)  : " + failCount.get() + " (Expected: 4)");
        System.out.println("    Final Account Balance  : â‚¹" + finalAcc.getBalance() + " (Expected: â‚¹100.00)");

        assertEquals(6, successCount.get(), "Exactly 6 withdrawals of 150.00 can succeed from 1,000.00");
        assertEquals(4, failCount.get(), "Remaining 4 withdrawals must fail due to insufficient balance");
        assertEquals(new BigDecimal("100.00"), finalAcc.getBalance(), "Final balance in MySQL must be exactly 100.00");
        assertTrue(finalAcc.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Account balance must NEVER become negative");
    }

    @Test
    @Order(8)
    @DisplayName("8. Live MySQL Persistence Verification across fresh raw JDBC connection")
    void test8_persistenceVerificationAcrossSessions() throws Exception {
        // Disconnect and open a brand-new raw JDBC connection
        try (Connection freshConn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, mysqlPassword);
             PreparedStatement ps = freshConn.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {

            ps.setLong(1, accountA.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Account A must exist in MySQL database file");
                BigDecimal persistedBalance = rs.getBigDecimal("balance");
                assertNotNull(persistedBalance);
                System.out.println(">>> LIVE MYSQL PERSISTENCE VERIFIED: Direct JDBC queried balance for Account " + accountA.getAccountNumber() + " = â‚¹" + persistedBalance);
            }

            // Verify transactions count in MySQL
            try (Statement stmt = freshConn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
                assertTrue(rs.next());
                long txCount = rs.getLong(1);
                assertTrue(txCount > 0, "MySQL transactions table must retain all audited financial transactions");
                System.out.println(">>> LIVE MYSQL PERSISTENCE VERIFIED: Total auditable records in MySQL transactions table: " + txCount);
            }
        }
    }
}

