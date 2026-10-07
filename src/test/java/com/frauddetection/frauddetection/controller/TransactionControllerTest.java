package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.frauddetection.frauddetection.entity.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.UserRepository;
import com.frauddetection.frauddetection.service.TransactionService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @InjectMocks
    private TransactionController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        when(authentication.getName()).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        org.mockito.Mockito.lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        org.mockito.Mockito.lenient().when(request.getHeader("User-Agent")).thenReturn("test-agent");
    }

    @Test
    void shouldShowApprovedTransactionResult() {
        FraudPrediction fraudPrediction = predictionFor("LEGITIMATE", 0.02, "APPROVED");
        BigDecimal amount = new BigDecimal("100.00");
        when(transactionService.processPayment(
                user,
                amount,
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"))
                .thenReturn(fraudPrediction);

        String view = controller.submitTransaction(amount, TransactionType.PAYMENT, authentication, request, model);

        assertEquals("user/result", view);
        verify(model).addAttribute("prediction", "LEGITIMATE");
        verify(model).addAttribute("fraudProbability", 0.02);
        verify(model).addAttribute("modelName", "Random Forest");
        verify(model).addAttribute("transactionStatus", "APPROVED");
        verify(model).addAttribute("transactionAmount", amount);
        verify(model).addAttribute("transactionType", TransactionType.PAYMENT);
        verify(model).addAttribute("message", "Your transaction was approved.");
        verify(transactionService).processPayment(
                eq(user),
                eq(amount),
                eq(TransactionType.PAYMENT),
                eq("127.0.0.1"),
                eq("test-agent")
        );
    }

    @Test
    void shouldShowInsufficientBalanceResult() {
        FraudPrediction fraudPrediction = predictionFor("LEGITIMATE", 0.02, "DECLINED");
        BigDecimal amount = new BigDecimal("100.00");
        when(transactionService.processPayment(
                user,
                amount,
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"))
                .thenReturn(fraudPrediction);

        String view = controller.submitTransaction(amount, TransactionType.PAYMENT, authentication, request, model);

        assertEquals("user/result", view);
        verify(model).addAttribute("transactionStatus", "DECLINED");
        verify(model).addAttribute(
                "message",
                "Your transaction was declined because the account balance is insufficient."
        );
    }

    @Test
    void shouldShowHistorySortedNewestFirst() {
        Account account = new Account();
        account.setUser(user);

        Transaction older = new Transaction();
        older.setAmount(new BigDecimal("50.00"));
        older.setTransactionTime(LocalDateTime.of(2026, 10, 1, 10, 0));

        Transaction newer = new Transaction();
        newer.setAmount(new BigDecimal("150.00"));
        newer.setTransactionTime(LocalDateTime.of(2026, 10, 5, 15, 30));

        when(accountRepository.findByUser(user)).thenReturn(java.util.List.of(account));
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(java.util.List.of(older, newer));

        String view = controller.showHistory(authentication, model);

        assertEquals("user/history", view);
        verify(model).addAttribute("transactions", java.util.List.of(newer, older));
    }

    @Test
    void shouldShowEmptyHistoryWhenNoTransactions() {
        Account account = new Account();
        account.setUser(user);

        when(accountRepository.findByUser(user)).thenReturn(java.util.List.of(account));
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(java.util.Collections.emptyList());

        String view = controller.showHistory(authentication, model);

        assertEquals("user/history", view);
        verify(model).addAttribute("transactions", java.util.Collections.emptyList());
    }

    private FraudPrediction predictionFor(
            String prediction,
            double probability,
            String transactionStatus) {

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTransactionType(TransactionType.PAYMENT);
        transaction.setStatus(transactionStatus);
        transaction.setTransactionTime(LocalDateTime.of(2026, 10, 5, 12, 0));

        FraudPrediction fraudPrediction = new FraudPrediction();
        fraudPrediction.setPrediction(prediction);
        fraudPrediction.setProbability(probability);
        fraudPrediction.setModelName("Random Forest");
        fraudPrediction.setTransaction(transaction);
        return fraudPrediction;
    }
}
