package com.frauddetection.frauddetection.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.frauddetection.frauddetection.config.DataInitializer;
import com.frauddetection.frauddetection.dto.AdminAccountDetailDto;
import com.frauddetection.frauddetection.dto.AdminAlertResponse;
import com.frauddetection.frauddetection.dto.AdminPendingTransactionDto;
import com.frauddetection.frauddetection.dto.AdminUserDetailDto;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Security regression test suite verifying:
 * 1. Admin API endpoints and serialized models NEVER expose password hashes (direct or nested).
 * 2. DataInitializer strictly skips default credential initialization under production profiles.
 * 3. DataInitializer permits default credential seeding under dev, demo, default, and test profiles.
 */
@SpringBootTest
class AdminApiSecurityTest {

    private static final String SECRET_BCRYPT_HASH = "$2a$10$e7K91hQWjT8Qf8.u3mN8leA7.rS1qWp7bN0/XQJk0Z51jKl9L3o2u";

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
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("User entity Jackson serialization must strictly omit password field")
    void userEntitySerializationOmitsPassword() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setUsername("security_target");
        user.setEmail("target@safepay.sec");
        user.setPassword(SECRET_BCRYPT_HASH);
        user.setRole("ADMIN");

        String json = objectMapper.writeValueAsString(user);

        assertNotNull(json);
        assertFalse(json.contains("password"), "Serialized User JSON must never contain 'password'");
        assertFalse(json.contains(SECRET_BCRYPT_HASH), "Serialized User JSON must never contain the BCrypt hash value");
        assertTrue(json.contains("security_target"), "Username should remain present");
        assertTrue(json.contains("target@safepay.sec"), "Email should remain present");
    }

    @Test
    @DisplayName("AdminUserDetailDto strictly excludes password field")
    void adminUserDetailDtoExcludesPassword() throws Exception {
        User user = new User();
        user.setId(77L);
        user.setUsername("admin_auditor");
        user.setEmail("auditor@safepay.sec");
        user.setPassword(SECRET_BCRYPT_HASH);
        user.setRole("ADMIN");

        AdminUserDetailDto dto = AdminUserDetailDto.fromEntity(user);
        assertNotNull(dto);

        String json = objectMapper.writeValueAsString(dto);
        assertFalse(json.contains("password"), "AdminUserDetailDto JSON must not contain 'password'");
        assertFalse(json.contains(SECRET_BCRYPT_HASH), "AdminUserDetailDto JSON must not contain the hash");
        assertEquals("admin_auditor", dto.username());
    }

    @Test
    @DisplayName("AdminAlertResponse serializes full hierarchy without exposing password hashes")
    void adminAlertResponseSerializesCleanlyWithoutPasswords() throws Exception {
        User customer = new User();
        customer.setId(10L);
        customer.setUsername("cust_alice");
        customer.setEmail("alice@test.sec");
        customer.setPassword(SECRET_BCRYPT_HASH);
        customer.setRole("USER");

        Account account = new Account();
        account.setId(100L);
        account.setAccountNumber("ACCT-10001");
        account.setAccountType("SAVINGS");
        account.setBalance(new BigDecimal("25000.00"));
        account.setStatus("ACTIVE");
        account.setUser(customer);

        Transaction tx = new Transaction();
        tx.setId(500L);
        tx.setAmount(new BigDecimal("12000.00"));
        tx.setTransactionType(TransactionType.TRANSFER);
        tx.setStatus("PENDING_REVIEW");
        tx.setTransactionTime(LocalDateTime.of(2026, 11, 22, 10, 0));
        tx.setAccount(account);

        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin_reviewer");
        admin.setEmail("reviewer@safepay.sec");
        admin.setPassword(SECRET_BCRYPT_HASH);
        admin.setRole("ADMIN");

        FraudAlert alert = new FraudAlert();
        alert.setId(99L);
        alert.setAlertType("SUSPICIOUS_VELOCITY");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.of(2026, 11, 22, 10, 1));
        alert.setTransaction(tx);
        alert.setReviewingAdmin(admin);

        AdminAlertResponse response = AdminAlertResponse.fromEntity(alert);
        assertNotNull(response);

        String json = objectMapper.writeValueAsString(response);

        assertFalse(json.contains("password"), "AdminAlertResponse must not contain 'password'");
        assertFalse(json.contains(SECRET_BCRYPT_HASH), "AdminAlertResponse must not contain the password hash");
        assertTrue(json.contains("ACCT-10001"), "Account number must be preserved");
        assertTrue(json.contains("cust_alice"), "Customer username must be preserved");
        assertTrue(json.contains("admin_reviewer"), "Admin username must be preserved");
        assertTrue(json.contains("SUSPICIOUS_VELOCITY"), "Anomaly type must be preserved");
    }

    @Test
    @DisplayName("GET /api/admin/alerts over HTTP does not leak passwords in serialized payload")
    @WithMockUser(username = "admin_sec_auditor", roles = "ADMIN")
    void getAdminAlertsOverHttpOmitsPasswordHashes() throws Exception {
        User cust = new User();
        cust.setUsername("leak_test_cust");
        cust.setEmail("cust@leaktest.sec");
        cust.setPassword(SECRET_BCRYPT_HASH);
        cust.setRole("USER");
        cust = userRepository.save(cust);

        Account acc = new Account();
        acc.setAccountNumber("ACCT-SEC-99");
        acc.setAccountType("SAVINGS");
        acc.setBalance(new BigDecimal("10000.00"));
        acc.setStatus("ACTIVE");
        acc.setUser(cust);
        acc = accountRepository.save(acc);

        Transaction tx = new Transaction();
        tx.setAccount(acc);
        tx.setAmount(new BigDecimal("3500.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setStatus("PENDING_REVIEW");
        tx.setTransactionTime(LocalDateTime.now());
        tx = transactionRepository.save(tx);

        FraudAlert alert = new FraudAlert();
        alert.setAlertType("ANOMALY_TEST");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setTransaction(tx);
        fraudAlertRepository.save(alert);

        MvcResult result = mockMvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertNotNull(body);
        assertFalse(body.contains("password"), "GET /api/admin/alerts response must not expose 'password' field");
        assertFalse(body.contains(SECRET_BCRYPT_HASH), "GET /api/admin/alerts response must not contain any password hash");
        assertTrue(body.contains("ACCT-SEC-99"), "Account number must be present");
        assertTrue(body.contains("leak_test_cust"), "Username must be present");
    }

    @Test
    @DisplayName("GET /api/admin/pending over HTTP does not leak passwords in serialized payload")
    @WithMockUser(username = "admin_sec_auditor", roles = "ADMIN")
    void getAdminPendingOverHttpOmitsPasswordHashes() throws Exception {
        User cust = new User();
        cust.setUsername("pending_leak_cust");
        cust.setEmail("pending@leaktest.sec");
        cust.setPassword(SECRET_BCRYPT_HASH);
        cust.setRole("USER");
        cust = userRepository.save(cust);

        Account acc = new Account();
        acc.setAccountNumber("ACCT-PEND-88");
        acc.setAccountType("SAVINGS");
        acc.setBalance(new BigDecimal("5000.00"));
        acc.setStatus("ACTIVE");
        acc.setUser(cust);
        acc = accountRepository.save(acc);

        Transaction tx = new Transaction();
        tx.setAccount(acc);
        tx.setAmount(new BigDecimal("2000.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setStatus("PENDING_REVIEW");
        tx.setTransactionTime(LocalDateTime.now());
        transactionRepository.save(tx);

        MvcResult result = mockMvc.perform(get("/api/admin/pending"))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertNotNull(body);
        assertFalse(body.contains("password"), "GET /api/admin/pending response must not expose 'password' field");
        assertFalse(body.contains(SECRET_BCRYPT_HASH), "GET /api/admin/pending response must not contain password hashes");
        assertTrue(body.contains("ACCT-PEND-88"), "Account number must be present");
        assertTrue(body.contains("pending_leak_cust"), "Username must be present");
    }

    @Test
    @DisplayName("DataInitializer strictly skips seeding when 'prod' profile is active")
    void dataInitializerSkipsSeedingUnderProdProfile() {
        UserRepository mockUserRepo = mock(UserRepository.class);
        AccountRepository mockAccRepo = mock(AccountRepository.class);
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        DataInitializer initializer = new DataInitializer(mockUserRepo, mockAccRepo, mockEncoder, prodEnv);
        initializer.run();

        verify(mockUserRepo, never()).findByUsername(any());
        verify(mockUserRepo, never()).save(any());
        verify(mockAccRepo, never()).save(any());
    }

    @Test
    @DisplayName("DataInitializer strictly skips seeding when 'production' profile is active")
    void dataInitializerSkipsSeedingUnderProductionProfile() {
        UserRepository mockUserRepo = mock(UserRepository.class);
        AccountRepository mockAccRepo = mock(AccountRepository.class);
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        DataInitializer initializer = new DataInitializer(mockUserRepo, mockAccRepo, mockEncoder, prodEnv);
        initializer.run();

        verify(mockUserRepo, never()).findByUsername(any());
        verify(mockUserRepo, never()).save(any());
        verify(mockAccRepo, never()).save(any());
    }

    @Test
    @DisplayName("DataInitializer proceeds with seeding under 'dev' profile when users missing")
    void dataInitializerSeedsUnderDevProfile() {
        UserRepository mockUserRepo = mock(UserRepository.class);
        AccountRepository mockAccRepo = mock(AccountRepository.class);
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        MockEnvironment devEnv = new MockEnvironment();
        devEnv.setActiveProfiles("dev");

        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.empty());
        when(mockUserRepo.findByUsername("user")).thenReturn(Optional.empty());
        when(mockEncoder.encode(any())).thenReturn("$2a$10$encodedMockPassword");
        when(mockUserRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        DataInitializer initializer = new DataInitializer(mockUserRepo, mockAccRepo, mockEncoder, devEnv);
        initializer.run();

        verify(mockUserRepo).findByUsername("admin");
        verify(mockUserRepo).findByUsername("user");
        org.mockito.Mockito.verify(mockUserRepo, org.mockito.Mockito.times(2)).save(any(User.class));
        org.mockito.Mockito.verify(mockAccRepo, org.mockito.Mockito.times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("DataInitializer proceeds with seeding under 'default' profile when users missing")
    void dataInitializerSeedsUnderDefaultProfile() {
        UserRepository mockUserRepo = mock(UserRepository.class);
        AccountRepository mockAccRepo = mock(AccountRepository.class);
        PasswordEncoder mockEncoder = mock(PasswordEncoder.class);
        MockEnvironment defaultEnv = new MockEnvironment();
        defaultEnv.setActiveProfiles("default");

        when(mockUserRepo.findByUsername("admin")).thenReturn(Optional.empty());
        when(mockUserRepo.findByUsername("user")).thenReturn(Optional.empty());
        when(mockEncoder.encode(any())).thenReturn("$2a$10$encodedMockPassword");
        when(mockUserRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        DataInitializer initializer = new DataInitializer(mockUserRepo, mockAccRepo, mockEncoder, defaultEnv);
        initializer.run();

        verify(mockUserRepo).findByUsername("admin");
        verify(mockUserRepo).findByUsername("user");
        org.mockito.Mockito.verify(mockUserRepo, org.mockito.Mockito.times(2)).save(any(User.class));
        org.mockito.Mockito.verify(mockAccRepo, org.mockito.Mockito.times(2)).save(any(Account.class));
    }
}
