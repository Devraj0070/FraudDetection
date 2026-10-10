package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ApiDashboardControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ApiDashboardController controller;

    @Test
    @DisplayName("Returns full dashboard metrics and recent transactions for authenticated user")
    void shouldReturnDashboardDataForAuthenticatedUserWithAccount() {
        String username = "alice";
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber("ACC-123456");
        account.setAccountType("SAVINGS");
        account.setStatus("ACTIVE");
        account.setBalance(new BigDecimal("25000.50"));

        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAmount(new BigDecimal("1200.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setStatus("APPROVED");
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 9, 12, 0, 0));

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findTopByUserOrderByIdAsc(user)).thenReturn(Optional.of(account));
        when(transactionRepository.countByAccount(account)).thenReturn(8L);
        when(transactionRepository.countByAccountAndStatus(account, "APPROVED")).thenReturn(6L);
        when(transactionRepository.countByAccountAndStatus(account, "DECLINED")).thenReturn(1L);
        when(transactionRepository.countByAccountAndStatus(account, "BLOCKED")).thenReturn(1L);
        when(transactionRepository.findTop5ByAccountOrderByTransactionTimeDesc(account)).thenReturn(List.of(tx));

        ResponseEntity<DashboardResponse> response = controller.getDashboardData(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.success());
        assertEquals("alice", body.username());
        assertEquals("ACC-123456", body.accountNumber());
        assertEquals("SAVINGS", body.accountType());
        assertEquals("ACTIVE", body.accountStatus());
        assertEquals(new BigDecimal("25000.50"), body.balance());
        assertEquals(8L, body.totalTransactions());
        assertEquals(6L, body.approvedTransactions());
        assertEquals(1L, body.declinedTransactions());
        assertEquals(1L, body.blockedTransactions());
        assertEquals(1, body.recentTransactions().size());
        assertEquals("APPROVED", body.recentTransactions().get(0).status());
        assertEquals(new BigDecimal("1200.00"), body.recentTransactions().get(0).amount());
    }

    @Test
    @DisplayName("Handles user without accounts gracefully with zero metrics")
    void shouldReturnDashboardDataWhenUserHasNoAccount() {
        String username = "bob";
        User user = new User();
        user.setUsername(username);
        user.setRole("USER");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findTopByUserOrderByIdAsc(user)).thenReturn(Optional.empty());
        when(accountRepository.findByUser(user)).thenReturn(Collections.emptyList());

        ResponseEntity<DashboardResponse> response = controller.getDashboardData(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.success());
        assertEquals("bob", body.username());
        assertEquals(BigDecimal.ZERO, body.balance());
        assertEquals(0L, body.totalTransactions());
        assertTrue(body.recentTransactions().isEmpty());
    }

    @Test
    @DisplayName("Returns 401 Unauthorized when authentication is null or unauthenticated")
    void shouldReturnUnauthorizedWhenNotAuthenticated() {
        ResponseEntity<DashboardResponse> response = controller.getDashboardData(null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
    }
}
