package com.frauddetection.frauddetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.frauddetection.frauddetection.entity.TransactionType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudAlert;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.exception.TransactionException;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionService;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FraudPredictionService fraudPredictionService;

    @Mock
    private FraudPredictionRepository fraudPredictionRepository;

    @Mock
    private FraudAlertRepository fraudAlertRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        account = new Account();
        account.setBalance(new BigDecimal("100.00"));

        Mockito.lenient().when(accountRepository.findFirstByUserOrderByIdAsc(user))
                .thenReturn(Optional.of(account));
        Mockito.lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldDebitApprovedLegitimatePaymentAfterPrediction() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.02, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("25.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("LEGITIMATE", result.getPrediction());
        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("75.00"), account.getBalance());
        assertEquals(TransactionType.PAYMENT, result.getTransaction().getTransactionType());

        InOrder order = inOrder(accountRepository, fraudPredictionService);
        order.verify(accountRepository).findFirstByUserOrderByIdAsc(user);
        order.verify(fraudPredictionService).predict(any(Transaction.class));
        order.verify(accountRepository).save(account);

        verify(transactionRepository).save(result.getTransaction());
        verify(fraudPredictionRepository).save(result);
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldBlockFraudWithoutChangingBalanceAndCreateAlert() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.98, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("25.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("BLOCKED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));

        ArgumentCaptor<FraudAlert> alertCaptor = ArgumentCaptor.forClass(FraudAlert.class);
        verify(fraudAlertRepository).save(alertCaptor.capture());
        assertEquals("FRAUD", alertCaptor.getValue().getAlertType());
        assertEquals("HIGH", alertCaptor.getValue().getSeverity());
        assertEquals("OPEN", alertCaptor.getValue().getStatus());
        assertEquals(result.getTransaction(), alertCaptor.getValue().getTransaction());
    }

    @Test
    void shouldDeclineLegitimatePaymentWhenBalanceIsInsufficient() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.02, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("125.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldRejectZeroAmountBeforeLoadingAccountOrPredicting() {
        assertThrows(
                TransactionException.class,
                () -> transactionService.processPayment(
                        user,
                        BigDecimal.ZERO,
                        TransactionType.PAYMENT,
                        "127.0.0.1",
                        "test-agent"
                )
        );

        verifyNoInteractions(
                accountRepository,
                fraudPredictionService,
                transactionRepository,
                fraudPredictionRepository,
                fraudAlertRepository
        );
    }

    @Test
    void shouldApproveTransferWhenBalanceSufficient() {
        // Set up sufficient balance for this test
        account.setBalance(new BigDecimal("200000.00"));
        Mockito.lenient().when(accountRepository.findFirstByUserOrderByIdAsc(user))
                .thenReturn(Optional.of(account));

        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.02, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("100000.00"),
                TransactionType.TRANSFER,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("LEGITIMATE", result.getPrediction());
        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance()); // 200000.00 - 100000.00
    }

    @Test
    void shouldBlockCashOutWhenModelSaysFraud() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.95, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("500000.00"),
                TransactionType.CASH_OUT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("FRAUD", result.getPrediction());
        assertEquals("BLOCKED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100.00"), account.getBalance()); // balance unchanged
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    @Test
    void shouldRejectUnsupportedTransactionType() {
        assertThrows(
                TransactionException.class,
                () -> transactionService.processPayment(
                        user,
                        new BigDecimal("100.00"),
                        null, // null type
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertThrows(
                TransactionException.class,
                () -> transactionService.processPayment(
                        user,
                        new BigDecimal("100.00"),
                        TransactionType.fromModelValue("UNSUPPORTED"), // returns null
                        "127.0.0.1",
                        "test-agent"
                )
        );
    }

    @Test
    void shouldProcessDebitTransaction() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.15, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50.00"),
                TransactionType.DEBIT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("LEGITIMATE", result.getPrediction());
        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(TransactionType.DEBIT, result.getTransaction().getTransactionType());
        assertEquals(new BigDecimal("50.00"), account.getBalance());
    }

    @Test
    void shouldProcessCashInTransaction() {
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("30.00"),
                TransactionType.CASH_IN,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("LEGITIMATE", result.getPrediction());
        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(TransactionType.CASH_IN, result.getTransaction().getTransactionType());
        assertEquals(new BigDecimal("70.00"), account.getBalance());
    }
}
