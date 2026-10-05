package com.frauddetection.frauddetection.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

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
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("test-agent");
    }

    @Test
    void shouldShowApprovedTransactionResult() {
        FraudPrediction fraudPrediction = predictionFor("LEGITIMATE", 0.02, "APPROVED");
        BigDecimal amount = new BigDecimal("100.00");
        when(transactionService.processPayment(
                user,
                amount,
                "127.0.0.1",
                "test-agent"))
                .thenReturn(fraudPrediction);

        String view = controller.submitTransaction(amount, authentication, request, model);

        assertEquals("user/result", view);
        verify(model).addAttribute("prediction", "LEGITIMATE");
        verify(model).addAttribute("fraudProbability", 0.02);
        verify(model).addAttribute("modelName", "Random Forest");
        verify(model).addAttribute("transactionStatus", "APPROVED");
        verify(model).addAttribute("message", "Your transaction was approved.");
        verify(transactionService).processPayment(
                eq(user),
                eq(amount),
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
                "127.0.0.1",
                "test-agent"))
                .thenReturn(fraudPrediction);

        String view = controller.submitTransaction(amount, authentication, request, model);

        assertEquals("user/result", view);
        verify(model).addAttribute("transactionStatus", "DECLINED");
        verify(model).addAttribute(
                "message",
                "Your transaction was declined because the account balance is insufficient."
        );
    }

    private FraudPrediction predictionFor(
            String prediction,
            double probability,
            String transactionStatus) {

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
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
