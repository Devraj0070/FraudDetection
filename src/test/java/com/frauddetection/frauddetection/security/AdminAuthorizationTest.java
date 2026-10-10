package com.frauddetection.frauddetection.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@SpringBootTest
class AdminAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @MockitoBean
    private FraudAlertRepository fraudAlertRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userRepository.findAll().stream()
                .filter(u -> "dash_user".equals(u.getUsername()) || "dash_admin".equals(u.getUsername()) || "dash_other".equals(u.getUsername()))
                .forEach(u -> {
                    accountRepository.findByUser(u).forEach(acc -> {
                        transactionRepository.findByAccountOrderByTransactionTimeDesc(acc).forEach(transactionRepository::delete);
                        accountRepository.delete(acc);
                    });
                    userRepository.delete(u);
                });
    }

    @Test
    void unauthenticatedUserCannotAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unauthenticatedUserCannotAccessApiAdminAlerts() throws Exception {
        mockMvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void userCannotAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void userCannotAccessApiAdminAlerts() throws Exception {
        mockMvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessAdminAlerts() throws Exception {
        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessApiAdminAlerts() throws Exception {
        Mockito.when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/admin/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotAccessApiTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dash_user", roles = "USER")
    void authenticatedUserCanAccessApiTransactions() throws Exception {
        User user = new User();
        user.setUsername("dash_user");
        user.setEmail("dash_api@test.com");
        user.setPassword("password");
        user.setRole("USER");
        userRepository.save(user);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotAccessApiTransactionDetails() throws Exception {
        mockMvc.perform(get("/api/transactions/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "dash_user", roles = "USER")
    void transactionDetailsReturnsNotFoundForMissingId() throws Exception {
        User user = new User();
        user.setUsername("dash_user");
        user.setEmail("dash_api_missing@test.com");
        user.setPassword("password");
        user.setRole("USER");
        userRepository.save(user);

        mockMvc.perform(get("/api/transactions/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "dash_user", roles = "USER")
    void userCannotAccessOtherUsersTransactionDetails() throws Exception {
        User user = new User();
        user.setUsername("dash_user");
        user.setEmail("dash_api_owner@test.com");
        user.setPassword("password");
        user.setRole("USER");
        user = userRepository.save(user);

        User other = new User();
        other.setUsername("dash_other");
        other.setEmail("dash_api_other@test.com");
        other.setPassword("password");
        other.setRole("USER");
        other = userRepository.save(other);

        Account otherAcc = new Account();
        otherAcc.setAccountNumber("100000000088");
        otherAcc.setAccountType("SAVINGS");
        otherAcc.setBalance(new BigDecimal("1000.00"));
        otherAcc.setStatus("ACTIVE");
        otherAcc.setUser(other);
        otherAcc = accountRepository.save(otherAcc);

        Transaction otherTx = new Transaction();
        otherTx.setAccount(otherAcc);
        otherTx.setAmount(new BigDecimal("250.00"));
        otherTx.setTransactionType(TransactionType.PAYMENT);
        otherTx.setStatus("APPROVED");
        otherTx.setTransactionTime(LocalDateTime.of(2026, 10, 8, 12, 0));
        otherTx = transactionRepository.save(otherTx);

        mockMvc.perform(get("/api/transactions/" + otherTx.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dash_user", roles = "USER")
    void userCanAccessOwnTransactionDetails() throws Exception {
        User user = new User();
        user.setUsername("dash_user");
        user.setEmail("dash_api_self@test.com");
        user.setPassword("password");
        user.setRole("USER");
        user = userRepository.save(user);

        Account acc = new Account();
        acc.setAccountNumber("100000000087");
        acc.setAccountType("SAVINGS");
        acc.setBalance(new BigDecimal("1000.00"));
        acc.setStatus("ACTIVE");
        acc.setUser(user);
        acc = accountRepository.save(acc);

        Transaction ownTx = new Transaction();
        ownTx.setAccount(acc);
        ownTx.setAmount(new BigDecimal("150.00"));
        ownTx.setTransactionType(TransactionType.PAYMENT);
        ownTx.setStatus("APPROVED");
        ownTx.setTransactionTime(LocalDateTime.of(2026, 10, 8, 12, 0));
        ownTx = transactionRepository.save(ownTx);

        mockMvc.perform(get("/api/transactions/" + ownTx.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "dash_admin", roles = "ADMIN")
    void adminCanAccessAnyUserTransactionDetails() throws Exception {
        User admin = new User();
        admin.setUsername("dash_admin");
        admin.setEmail("dash_admin_audit@test.com");
        admin.setPassword("password");
        admin.setRole("ADMIN");
        userRepository.save(admin);

        User other = new User();
        other.setUsername("dash_other");
        other.setEmail("dash_other_for_admin@test.com");
        other.setPassword("password");
        other.setRole("USER");
        other = userRepository.save(other);

        Account otherAcc = new Account();
        otherAcc.setAccountNumber("100000000086");
        otherAcc.setAccountType("SAVINGS");
        otherAcc.setBalance(new BigDecimal("2000.00"));
        otherAcc.setStatus("ACTIVE");
        otherAcc.setUser(other);
        otherAcc = accountRepository.save(otherAcc);

        Transaction otherTx = new Transaction();
        otherTx.setAccount(otherAcc);
        otherTx.setAmount(new BigDecimal("999.00"));
        otherTx.setTransactionType(TransactionType.TRANSFER);
        otherTx.setStatus("BLOCKED");
        otherTx.setTransactionTime(LocalDateTime.of(2026, 10, 8, 12, 0));
        otherTx = transactionRepository.save(otherTx);

        mockMvc.perform(get("/api/transactions/" + otherTx.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAlertsPageRendersExistingAlerts() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("250.00"));
        transaction.setTransactionTime(LocalDateTime.of(2026, 10, 6, 12, 0));

        FraudAlert alert = new FraudAlert();
        alert.setAlertType("VELOCITY");
        alert.setSeverity("HIGH");
        alert.setStatus("OPEN");
        alert.setCreatedAt(LocalDateTime.of(2026, 10, 6, 12, 5));
        alert.setTransaction(transaction);

        Mockito.when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(alert));

        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"))
                .andExpect(model().attribute("alerts", List.of(alert)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAlertsPageRendersWhenNoAlertsExist() throws Exception {
        Mockito.when(fraudAlertRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/alerts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/alerts"))
                .andExpect(model().attribute("alerts", List.of()));
    }

    @Test
    void loginStaysPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void registerStaysPublic() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void existingAuthenticatedUserAccessStillWorks() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void csrfProtectionIsStillEnabled() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dash_user", roles = "USER")
    void dashboardRendersSuccessfullyForUserWithAccount() throws Exception {
        User user = new User();
        user.setUsername("dash_user");
        user.setEmail("dash@test.com");
        user.setPassword("password");
        user.setRole("USER");
        user = userRepository.save(user);

        Account account = new Account();
        account.setAccountNumber("100000000099");
        account.setAccountType("SAVINGS");
        account.setBalance(new BigDecimal("5000.00"));
        account.setStatus("ACTIVE");
        account.setUser(user);
        accountRepository.save(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attribute("username", "dash_user"))
                .andExpect(model().attribute("balance", new BigDecimal("5000.00")));
    }

    @Test
    @WithMockUser(username = "dash_admin", roles = "ADMIN")
    void dashboardRendersSuccessfullyForAdminWithAccount() throws Exception {
        User admin = new User();
        admin.setUsername("dash_admin");
        admin.setEmail("admin@test.com");
        admin.setPassword("password");
        admin.setRole("ADMIN");
        admin = userRepository.save(admin);

        Account account = new Account();
        account.setAccountNumber("100000000098");
        account.setAccountType("SAVINGS");
        account.setBalance(new BigDecimal("10000.00"));
        account.setStatus("ACTIVE");
        account.setUser(admin);
        accountRepository.save(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("user/dashboard"))
                .andExpect(model().attribute("username", "dash_admin"))
                .andExpect(model().attribute("balance", new BigDecimal("10000.00")));
    }
}
