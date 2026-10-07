package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private Model model;

    @InjectMocks
    private DashboardController controller;

    @Test
    void shouldPopulateDashboardWithAccountMetricsAndRecentTransactions() {
        String username = "alice";
        User user = new User();
        user.setUsername(username);

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber("ACC-123456");
        account.setBalance(new BigDecimal("15000.50"));

        Transaction t1 = new Transaction();
        List<Transaction> recentTransactions = List.of(t1);

        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findTopByUserOrderByIdAsc(user)).thenReturn(Optional.of(account));
        when(transactionRepository.countByAccount(account)).thenReturn(10L);
        when(transactionRepository.countByAccountAndStatus(account, "APPROVED")).thenReturn(7L);
        when(transactionRepository.countByAccountAndStatus(account, "DECLINED")).thenReturn(2L);
        when(transactionRepository.countByAccountAndStatus(account, "BLOCKED")).thenReturn(1L);
        when(transactionRepository.findTop5ByAccountOrderByTransactionTimeDesc(account)).thenReturn(recentTransactions);

        String view = controller.showDashboard(authentication, model);

        assertEquals("user/dashboard", view);
        verify(model).addAttribute("username", username);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("account", account);
        verify(model).addAttribute("balance", new BigDecimal("15000.50"));
        verify(model).addAttribute("totalTransactions", 10L);
        verify(model).addAttribute("approvedTransactions", 7L);
        verify(model).addAttribute("declinedTransactions", 2L);
        verify(model).addAttribute("blockedTransactions", 1L);
        verify(model).addAttribute("recentTransactions", recentTransactions);
    }

    @Test
    void shouldHandleMissingAccountGracefully() {
        String username = "bob";
        User user = new User();
        user.setUsername(username);

        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(accountRepository.findTopByUserOrderByIdAsc(user)).thenReturn(Optional.empty());
        when(accountRepository.findByUser(user)).thenReturn(Collections.emptyList());

        String view = controller.showDashboard(authentication, model);

        assertEquals("user/dashboard", view);
        verify(model).addAttribute("username", username);
        verify(model).addAttribute("user", user);
        verify(model).addAttribute("account", null);
        verify(model).addAttribute("balance", BigDecimal.ZERO);
        verify(model).addAttribute("totalTransactions", 0L);
        verify(model).addAttribute("approvedTransactions", 0L);
        verify(model).addAttribute("declinedTransactions", 0L);
        verify(model).addAttribute("blockedTransactions", 0L);
        verify(model).addAttribute("recentTransactions", Collections.emptyList());
    }

    @Test
    void shouldHandleNullAuthenticationGracefully() {
        String view = controller.showDashboard(null, model);

        assertEquals("user/dashboard", view);
        verify(model).addAttribute("username", null);
        verify(model).addAttribute("user", null);
        verify(model).addAttribute("account", null);
        verify(model).addAttribute("balance", BigDecimal.ZERO);
        verify(model).addAttribute("totalTransactions", 0L);
        verify(model).addAttribute("approvedTransactions", 0L);
        verify(model).addAttribute("declinedTransactions", 0L);
        verify(model).addAttribute("blockedTransactions", 0L);
        verify(model).addAttribute("recentTransactions", Collections.emptyList());
    }
}
