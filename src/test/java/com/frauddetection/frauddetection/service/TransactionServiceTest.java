package com.frauddetection.frauddetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

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
import com.frauddetection.frauddetection.fraud.FraudRuleEngine;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionService;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;
import com.frauddetection.frauddetection.repository.FundsReservationRepository;
import com.frauddetection.frauddetection.entity.FundsReservation;
import com.frauddetection.frauddetection.entity.FundsReservation.ReservationStatus;

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

    @Mock
    private FraudRuleEngine fraudRuleEngine;

    @Mock
    private FundsReservationRepository fundsReservationRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = new User();
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");
        account.setBalance(new BigDecimal("100.00"));

        Mockito.lenient().when(accountRepository.findByIdWithLock(1L))
                .thenReturn(Optional.of(account));
        Mockito.lenient().when(fundsReservationRepository.sumActiveReservationsByAccount(any()))
                .thenReturn(BigDecimal.ZERO);
        Mockito.lenient().when(fundsReservationRepository.save(any(FundsReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Mockito.lenient().when(accountRepository.findFirstByUserOrderByIdAsc(user))
                .thenReturn(Optional.of(account));
        Mockito.lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.lenient().when(fraudRuleEngine.isSuspicious(any(Transaction.class)))
                .thenReturn(false);
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
        assertEquals("Normal / Legitimate", result.getDetectionReason());
        assertFalse(result.isRuleAnomaly());

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

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals("FRAUD", result.getPrediction());
        assertEquals("Machine Learning Fraud Detection", result.getDetectionReason());
        assertFalse(result.isRuleAnomaly());
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
        assertEquals("Balance Insufficient", result.getDetectionReason());
        assertFalse(result.isRuleAnomaly());
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
        account.setBalance(new BigDecimal("200000.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-200");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50000.00"));

        when(accountRepository.findByAccountNumber("ACC-200")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.02, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("100000.00"),
                TransactionType.TRANSFER,
                "ACC-200",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("LEGITIMATE", result.getPrediction());
        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance()); // 200000.00 - 100000.00
        assertEquals(new BigDecimal("150000.00"), receiver.getBalance()); // 50000.00 + 100000.00
    }

    @Test
    void shouldBlockCashOutWhenModelSaysFraud() {
        account.setBalance(new BigDecimal("600000.00"));
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
        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("600000.00"), account.getBalance()); // balance unchanged
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
        assertEquals(new BigDecimal("130.00"), account.getBalance());
    }

    // ==========================================
    // PAYMENT Tests (Limit: â‚¹100,000)
    // ==========================================

    @Test
    void shouldApprovePaymentWithinLimitWhenBalanceSufficient() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("50000.00"), account.getBalance());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldDeclinePaymentExceedingLimitWithoutChangingBalanceOrCreatingAlert() {
        account.setBalance(new BigDecimal("200000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("150000.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals("Limit Exceeded", result.getDetectionReason());
        assertFalse(result.isRuleAnomaly());
        assertEquals(new BigDecimal("200000.00"), account.getBalance()); // balance unchanged
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldBlockPaymentWhenFraudDetected() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.95, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    // ==========================================
    // TRANSFER Tests (Limit: â‚¹200,000)
    // ==========================================

    @Test
    void shouldApproveTransferWithinLimit() {
        account.setBalance(new BigDecimal("200000.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-REC-1");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50000.00"));

        when(accountRepository.findByAccountNumber("ACC-REC-1")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("150000.00"),
                TransactionType.TRANSFER,
                "ACC-REC-1",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("50000.00"), account.getBalance());
        assertEquals(new BigDecimal("200000.00"), receiver.getBalance());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldDeclineTransferExceedingLimit() {
        account.setBalance(new BigDecimal("300000.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-REC-2");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50000.00"));

        when(accountRepository.findByAccountNumber("ACC-REC-2")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("250000.00"),
                TransactionType.TRANSFER,
                "ACC-REC-2",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("300000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldBlockTransferWhenFraudDetected() {
        account.setBalance(new BigDecimal("200000.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-REC-3");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50000.00"));

        when(accountRepository.findByAccountNumber("ACC-REC-3")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.92, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("100000.00"),
                TransactionType.TRANSFER,
                "ACC-REC-3",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("200000.00"), account.getBalance());
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    // ==========================================
    // CASH_OUT Tests (Limit: â‚¹50,000)
    // ==========================================

    @Test
    void shouldApproveCashOutWithinLimit() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("30000.00"),
                TransactionType.CASH_OUT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("70000.00"), account.getBalance());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldDeclineCashOutExceedingLimit() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("60000.00"),
                TransactionType.CASH_OUT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    // ==========================================
    // DEBIT Tests (Limit: â‚¹100,000)
    // ==========================================

    @Test
    void shouldApproveDebitWithinLimit() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.DEBIT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("50000.00"), account.getBalance());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldDeclineDebitExceedingLimit() {
        account.setBalance(new BigDecimal("200000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("150000.00"),
                TransactionType.DEBIT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("200000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldBlockDebitWhenFraudDetected() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.94, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.DEBIT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    // ==========================================
    // CASH_IN Tests (Credit, Limit: â‚¹50,000)
    // ==========================================

    @Test
    void shouldApproveCashInWithinLimitAndIncreaseBalance() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("40000.00"),
                TransactionType.CASH_IN,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("140000.00"), account.getBalance());
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldDeclineCashInExceedingLimitWithoutIncreasingBalance() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.05, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("60000.00"),
                TransactionType.CASH_IN,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance()); // balance unchanged
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository, never()).save(any(FraudAlert.class));
    }

    @Test
    void shouldBlockCashInWhenFraudDetected() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.90, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("30000.00"),
                TransactionType.CASH_IN,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    // ==========================================
    // Deterministic Anomaly & Huge Balance Tests
    // ==========================================

    @Test
    void shouldBlockTransactionWhenDeterministicAnomalyRuleTriggersEvenIfMLSaysLegitimate() {
        account.setBalance(new BigDecimal("100000.00"));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.12, "Random Forest"));
        when(fraudRuleEngine.isSuspicious(any(Transaction.class))).thenReturn(true);

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        // Stored ML prediction remains the actual model prediction ("LEGITIMATE")
        assertEquals("LEGITIMATE", result.getPrediction());
        assertTrue(result.isRuleAnomaly());
        assertEquals("Behavioral Anomaly / Suspicious Activity", result.getDetectionReason());
        assertEquals(new BigDecimal("100000.00"), account.getBalance());
        verify(fraudAlertRepository).save(any(FraudAlert.class));
    }

    @Test
    void shouldSafelyHandleHugeBigDecimalBalanceWithoutOverflowOrArithmeticError() {
        BigDecimal hugeBalance = new BigDecimal("6767676767676767676.00");
        account.setBalance(hugeBalance);

        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50000.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        BigDecimal expectedBalance = hugeBalance.subtract(new BigDecimal("50000.00"));
        assertEquals(expectedBalance, account.getBalance());
    }

    // ==========================================
    // Actual Transfer & Concurrency Workflows
    // ==========================================

    @Test
    void shouldSuccessfullyTransferFundsBetweenTwoAccounts() {
        account.setBalance(new BigDecimal("100.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-200");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByAccountNumber("ACC-200")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("40.00"),
                TransactionType.TRANSFER,
                "ACC-200",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("APPROVED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("60.00"), account.getBalance()); // 100 - 40
        assertEquals(new BigDecimal("90.00"), receiver.getBalance()); // 50 + 40
        verify(accountRepository).findAccountsWithLockInOrder(1L, 2L);
        verify(accountRepository).save(account);
        verify(accountRepository).save(receiver);
    }

    @Test
    void shouldRejectTransferWhenSenderBalanceInsufficient() {
        account.setBalance(new BigDecimal("30.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-200");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByAccountNumber("ACC-200")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("40.00"),
                TransactionType.TRANSFER,
                "ACC-200",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("30.00"), account.getBalance()); // sender balance unchanged
        assertEquals(new BigDecimal("50.00"), receiver.getBalance()); // receiver balance unchanged
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldRejectTransferWhenReceiverAccountInactive() {
        account.setBalance(new BigDecimal("100.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        Account inactiveReceiver = new Account();
        inactiveReceiver.setId(2L);
        inactiveReceiver.setAccountNumber("ACC-200");
        inactiveReceiver.setStatus("SUSPENDED");
        inactiveReceiver.setBalance(new BigDecimal("50.00"));

        when(accountRepository.findByAccountNumber("ACC-200")).thenReturn(Optional.of(inactiveReceiver));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("40.00"),
                        TransactionType.TRANSFER,
                        "ACC-200",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertTrue(ex.getMessage().contains("Account is not active"));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals(new BigDecimal("50.00"), inactiveReceiver.getBalance());
    }

    @Test
    void shouldRejectTransferWhenSenderAccountInactive() {
        account.setBalance(new BigDecimal("100.00"));
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("FROZEN");

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("40.00"),
                        TransactionType.TRANSFER,
                        "ACC-200",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertTrue(ex.getMessage().contains("Account is not active"));
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        when(accountRepository.findByAccountNumber("ACC-100")).thenReturn(Optional.of(account));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("40.00"),
                        TransactionType.TRANSFER,
                        "ACC-100",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertTrue(ex.getMessage().contains("same account"));
    }

    @Test
    void shouldRejectTransferWhenReceiverAccountNotFound() {
        account.setId(1L);
        account.setAccountNumber("ACC-100");
        account.setStatus("ACTIVE");

        when(accountRepository.findByAccountNumber("NON_EXISTENT")).thenReturn(Optional.empty());

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("40.00"),
                        TransactionType.TRANSFER,
                        "NON_EXISTENT",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertTrue(ex.getMessage().contains("Receiver account not found"));
    }

    // ==========================================
    // Admin Fraud Review Lifecycle Tests
    // ==========================================

    @Test
    void shouldAllowAdminToApprovePendingSingleAccountTransaction() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");

        account.setBalance(new BigDecimal("100.00"));
        account.setId(1L);
        account.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(501L);
        pendingTx.setAmount(new BigDecimal("35.00"));
        pendingTx.setTransactionType(TransactionType.PAYMENT);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);

        FraudAlert alert = new FraudAlert();
        alert.setId(10L);
        alert.setTransaction(pendingTx);
        alert.setStatus("OPEN");

        FundsReservation reservation = new FundsReservation(account, pendingTx, new BigDecimal("35.00"));
        when(transactionRepository.findByIdWithLock(501L)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(reservation));
        when(fraudAlertRepository.findByTransaction(pendingTx)).thenReturn(List.of(alert));

        Transaction approved = transactionService.approveTransaction(501L, admin, "Customer verified by phone");

        assertEquals("APPROVED", approved.getStatus());
        assertEquals(new BigDecimal("65.00"), account.getBalance()); // 100 - 35
        assertEquals("APPROVED", alert.getDecision());
        assertEquals("RESOLVED", alert.getStatus());
        assertEquals(admin, alert.getReviewingAdmin());
        assertNotNull(alert.getReviewedAt());
        assertEquals("Customer verified by phone", alert.getReviewNotes());
        verify(accountRepository).save(account);
        verify(fraudAlertRepository).save(alert);
    }

    @Test
    void shouldAllowAdminToApprovePendingTransferTransaction() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");

        account.setBalance(new BigDecimal("200.00"));
        account.setId(1L);
        account.setStatus("ACTIVE");

        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("50.00"));
        receiver.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(502L);
        pendingTx.setAmount(new BigDecimal("50.00"));
        pendingTx.setTransactionType(TransactionType.TRANSFER);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);
        pendingTx.setReceiverAccount(receiver);

        FraudAlert alert = new FraudAlert();
        alert.setId(11L);
        alert.setTransaction(pendingTx);
        alert.setStatus("OPEN");

        FundsReservation reservation = new FundsReservation(account, pendingTx, new BigDecimal("50.00"));
        when(transactionRepository.findByIdWithLock(502L)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(reservation));
        when(fraudAlertRepository.findByTransaction(pendingTx)).thenReturn(List.of(alert));

        Transaction approved = transactionService.approveTransaction(502L, admin, "Transfer approved");

        assertEquals("APPROVED", approved.getStatus());
        assertEquals(new BigDecimal("150.00"), account.getBalance()); // 200 - 50
        assertEquals(new BigDecimal("100.00"), receiver.getBalance()); // 50 + 50
        assertEquals("APPROVED", alert.getDecision());
        assertEquals("RESOLVED", alert.getStatus());
        verify(accountRepository).save(account);
        verify(accountRepository).save(receiver);
        verify(fraudAlertRepository).save(alert);
    }

    @Test
    void shouldRejectDuplicateApprovalWhenTransactionNotPendingReview() {
        User admin = new User();
        admin.setId(99L);

        Transaction approvedTx = new Transaction();
        approvedTx.setId(503L);
        approvedTx.setStatus("APPROVED");

        when(transactionRepository.findByIdWithLock(503L)).thenReturn(Optional.of(approvedTx));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.approveTransaction(503L, admin, "duplicate approval attempt")
        );

        assertTrue(ex.getMessage().contains("not pending review"));
    }

    @Test
    void shouldAllowAdminToRejectPendingTransactionLeavingBalancesUnchanged() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin");

        account.setBalance(new BigDecimal("100.00"));
        account.setId(1L);
        account.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(504L);
        pendingTx.setAmount(new BigDecimal("50.00"));
        pendingTx.setTransactionType(TransactionType.PAYMENT);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);

        FraudAlert alert = new FraudAlert();
        alert.setId(12L);
        alert.setTransaction(pendingTx);
        alert.setStatus("OPEN");

        FundsReservation reservation = new FundsReservation(account, pendingTx, new BigDecimal("50.00"));
        when(transactionRepository.findByIdWithLock(504L)).thenReturn(Optional.of(pendingTx));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(reservation));
        when(fraudAlertRepository.findByTransaction(pendingTx)).thenReturn(List.of(alert));

        Transaction rejected = transactionService.rejectTransaction(504L, admin, "Known fraud pattern");

        assertEquals("DECLINED", rejected.getStatus());
        assertEquals(new BigDecimal("100.00"), account.getBalance()); // balance unchanged
        assertEquals("REJECTED", alert.getDecision());
        assertEquals("CLOSED", alert.getStatus());
        assertEquals(admin, alert.getReviewingAdmin());
        assertNotNull(alert.getReviewedAt());
        verify(accountRepository, never()).save(any(Account.class));
        verify(fraudAlertRepository).save(alert);
    }

    @Test
    void shouldRejectDuplicateRejectionWhenTransactionNotPendingReview() {
        User admin = new User();
        admin.setId(99L);

        Transaction declinedTx = new Transaction();
        declinedTx.setId(505L);
        declinedTx.setStatus("DECLINED");

        when(transactionRepository.findByIdWithLock(505L)).thenReturn(Optional.of(declinedTx));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.rejectTransaction(505L, admin, "duplicate rejection attempt")
        );

        assertTrue(ex.getMessage().contains("not pending review"));
    }

    @Test
    void shouldPreventApprovalIfSenderBalanceBecomesInsufficientBeforeAdminReview() {
        User admin = new User();
        admin.setId(99L);

        account.setBalance(new BigDecimal("10.00")); // balance dropped below transaction amount
        account.setId(1L);
        account.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(506L);
        pendingTx.setAmount(new BigDecimal("50.00"));
        pendingTx.setTransactionType(TransactionType.PAYMENT);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);

        FundsReservation reservation = new FundsReservation(account, pendingTx, new BigDecimal("50.00"));
        when(transactionRepository.findByIdWithLock(506L)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(reservation));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.approveTransaction(506L, admin, "Approve attempt with insufficient balance")
        );

        assertTrue(ex.getMessage().contains("Insufficient funds"));
        assertEquals(new BigDecimal("10.00"), account.getBalance()); // balance unchanged
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldReturnPendingTransactionsForAdminReview() {
        Transaction tx1 = new Transaction();
        tx1.setId(601L);
        tx1.setStatus("PENDING_REVIEW");

        when(transactionRepository.findByStatusOrderByTransactionTimeDesc("PENDING_REVIEW"))
                .thenReturn(List.of(tx1));

        List<Transaction> pending = transactionService.getPendingTransactions();

        assertEquals(1, pending.size());
        assertEquals(601L, pending.get(0).getId());
        verify(transactionRepository).findByStatusOrderByTransactionTimeDesc("PENDING_REVIEW");
    }

    // ==========================================
    // Finding 1: Mandatory Receiver Validation Tests
    // ==========================================

    @Test
    void shouldRejectTransferWhenReceiverAccountNumberIsNull() {
        account.setBalance(new BigDecimal("100.00"));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("25.00"),
                        TransactionType.TRANSFER,
                        null,
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertEquals("Receiver account number is mandatory for TRANSFER operations.", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verifyNoInteractions(fraudPredictionService, transactionRepository, fraudAlertRepository);
    }

    @Test
    void shouldRejectTransferWhenReceiverAccountNumberIsEmpty() {
        account.setBalance(new BigDecimal("100.00"));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("25.00"),
                        TransactionType.TRANSFER,
                        "",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertEquals("Receiver account number is mandatory for TRANSFER operations.", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verifyNoInteractions(fraudPredictionService, transactionRepository, fraudAlertRepository);
    }

    @Test
    void shouldRejectTransferWhenReceiverAccountNumberIsWhitespace() {
        account.setBalance(new BigDecimal("100.00"));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.processPayment(
                        user,
                        new BigDecimal("25.00"),
                        TransactionType.TRANSFER,
                        "     ",
                        "127.0.0.1",
                        "test-agent"
                )
        );

        assertEquals("Receiver account number is mandatory for TRANSFER operations.", ex.getMessage());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verifyNoInteractions(fraudPredictionService, transactionRepository, fraudAlertRepository);
    }

    // ==========================================
    // Finding 2: Funds Reservation Lifecycle & Financial Invariants Tests
    // ==========================================

    @Test
    void shouldCreateActiveReservationWhenTransferFlaggedForFraudReview() {
        account.setBalance(new BigDecimal("1000.00"));
        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setAccountNumber("ACC-REC-REV");
        receiver.setStatus("ACTIVE");
        receiver.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByAccountNumber("ACC-REC-REV")).thenReturn(Optional.of(receiver));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("FRAUD", 0.95, "Random Forest"));

        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("400.00"),
                TransactionType.TRANSFER,
                "ACC-REC-REV",
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("PENDING_REVIEW", result.getTransaction().getStatus());
        assertEquals(new BigDecimal("1000.00"), account.getBalance()); // Ledger balance untouched
        assertEquals(new BigDecimal("200.00"), receiver.getBalance());

        ArgumentCaptor<FundsReservation> reservationCaptor = ArgumentCaptor.forClass(FundsReservation.class);
        verify(fundsReservationRepository).save(reservationCaptor.capture());

        FundsReservation savedRes = reservationCaptor.getValue();
        assertEquals(account, savedRes.getAccount());
        assertEquals(result.getTransaction(), savedRes.getTransaction());
        assertEquals(new BigDecimal("400.00"), savedRes.getAmount());
        assertEquals(ReservationStatus.ACTIVE, savedRes.getStatus());
        assertNotNull(savedRes.getCreatedAt());
    }

    @Test
    void shouldCalculateAvailableBalanceDecreasedByActiveReservations() {
        account.setBalance(new BigDecimal("1000.00"));
        when(fundsReservationRepository.sumActiveReservationsByAccount(account))
                .thenReturn(new BigDecimal("350.00"));

        BigDecimal ledger = transactionService.getLedgerBalance(account);
        BigDecimal reserved = transactionService.getReservedFunds(account);
        BigDecimal available = transactionService.getAvailableBalance(account);

        assertEquals(new BigDecimal("1000.00"), ledger);
        assertEquals(new BigDecimal("350.00"), reserved);
        assertEquals(new BigDecimal("650.00"), available); // Invariant: 1000 - 350 = 650
    }

    @Test
    void shouldPreventSpendingReservedFundsWhenSubsequentTransactionExceedsAvailableBalance() {
        account.setBalance(new BigDecimal("100.00"));
        // Active hold of 60.00 leaves available balance of 40.00
        when(fundsReservationRepository.sumActiveReservationsByAccount(account))
                .thenReturn(new BigDecimal("60.00"));

        when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));

        // User attempts to spend 50.00 (which exceeds available balance of 40.00, even though ledger has 100.00)
        FraudPrediction result = transactionService.processPayment(
                user,
                new BigDecimal("50.00"),
                TransactionType.PAYMENT,
                "127.0.0.1",
                "test-agent"
        );

        assertEquals("DECLINED", result.getTransaction().getStatus());
        assertEquals("Balance Insufficient", result.getDetectionReason());
        assertEquals(new BigDecimal("100.00"), account.getBalance()); // balance untouched
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldConsumeReservationExactlyOnceAndCreditReceiverUponApproval() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin_auditor");

        account.setBalance(new BigDecimal("1000.00"));
        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("200.00"));
        receiver.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(701L);
        pendingTx.setAmount(new BigDecimal("300.00"));
        pendingTx.setTransactionType(TransactionType.TRANSFER);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);
        pendingTx.setReceiverAccount(receiver);

        FundsReservation activeReservation = new FundsReservation(account, pendingTx, new BigDecimal("300.00"));
        activeReservation.setStatus(ReservationStatus.ACTIVE);

        when(transactionRepository.findByIdWithLock(701L)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findAccountsWithLockInOrder(1L, 2L)).thenReturn(List.of(account, receiver));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(activeReservation));

        Transaction approved = transactionService.approveTransaction(701L, admin, "Audit passed");

        assertEquals("APPROVED", approved.getStatus());
        assertEquals(ReservationStatus.CONSUMED, activeReservation.getStatus());
        assertEquals(new BigDecimal("700.00"), account.getBalance()); // 1000 - 300
        assertEquals(new BigDecimal("500.00"), receiver.getBalance()); // 200 + 300
        verify(fundsReservationRepository).save(activeReservation);
        verify(accountRepository).save(account);
        verify(accountRepository).save(receiver);
    }

    @Test
    void shouldReleaseReservationExactlyOnceAndPreserveAvailableBalanceUponRejection() {
        User admin = new User();
        admin.setId(99L);
        admin.setUsername("admin_auditor");

        account.setBalance(new BigDecimal("1000.00"));
        Account receiver = new Account();
        receiver.setId(2L);
        receiver.setBalance(new BigDecimal("200.00"));
        receiver.setStatus("ACTIVE");

        Transaction pendingTx = new Transaction();
        pendingTx.setId(702L);
        pendingTx.setAmount(new BigDecimal("300.00"));
        pendingTx.setTransactionType(TransactionType.TRANSFER);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);
        pendingTx.setReceiverAccount(receiver);

        FundsReservation activeReservation = new FundsReservation(account, pendingTx, new BigDecimal("300.00"));
        activeReservation.setStatus(ReservationStatus.ACTIVE);

        when(transactionRepository.findByIdWithLock(702L)).thenReturn(Optional.of(pendingTx));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(activeReservation));

        Transaction rejected = transactionService.rejectTransaction(702L, admin, "Suspected identity theft");

        assertEquals("DECLINED", rejected.getStatus());
        assertEquals(ReservationStatus.RELEASED, activeReservation.getStatus());
        assertEquals(new BigDecimal("1000.00"), account.getBalance()); // Ledger balance untouched
        assertEquals(new BigDecimal("200.00"), receiver.getBalance()); // Receiver balance untouched
        verify(fundsReservationRepository).save(activeReservation);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void shouldPreventApprovalAfterRejection() {
        User admin = new User();
        admin.setId(99L);

        Transaction rejectedTx = new Transaction();
        rejectedTx.setId(703L);
        rejectedTx.setStatus("DECLINED");

        when(transactionRepository.findByIdWithLock(703L)).thenReturn(Optional.of(rejectedTx));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.approveTransaction(703L, admin, "Attempting approval after rejection")
        );

        assertTrue(ex.getMessage().contains("not pending review"));
    }

    @Test
    void shouldPreventRejectionAfterApproval() {
        User admin = new User();
        admin.setId(99L);

        Transaction approvedTx = new Transaction();
        approvedTx.setId(704L);
        approvedTx.setStatus("APPROVED");

        when(transactionRepository.findByIdWithLock(704L)).thenReturn(Optional.of(approvedTx));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.rejectTransaction(704L, admin, "Attempting rejection after approval")
        );

        assertTrue(ex.getMessage().contains("not pending review"));
    }

    @Test
    void shouldFailApprovalIfReservationIsNotActive() {
        User admin = new User();
        admin.setId(99L);

        Transaction pendingTx = new Transaction();
        pendingTx.setId(705L);
        pendingTx.setAmount(new BigDecimal("50.00"));
        pendingTx.setTransactionType(TransactionType.PAYMENT);
        pendingTx.setStatus("PENDING_REVIEW");
        pendingTx.setAccount(account);

        FundsReservation alreadyConsumed = new FundsReservation(account, pendingTx, new BigDecimal("50.00"));
        alreadyConsumed.setStatus(ReservationStatus.CONSUMED);

        when(transactionRepository.findByIdWithLock(705L)).thenReturn(Optional.of(pendingTx));
        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(fundsReservationRepository.findByTransactionWithLock(pendingTx)).thenReturn(Optional.of(alreadyConsumed));

        TransactionException ex = assertThrows(TransactionException.class, () ->
                transactionService.approveTransaction(705L, admin, "Approval with consumed hold")
        );

        assertTrue(ex.getMessage().contains("not active"));
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        verify(accountRepository, never()).save(any(Account.class));
    }

}
