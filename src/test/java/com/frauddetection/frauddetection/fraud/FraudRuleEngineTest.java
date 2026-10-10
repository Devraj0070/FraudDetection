package com.frauddetection.frauddetection.fraud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private FraudRuleEngine fraudRuleEngine;

    private Account account;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("100000000001");
        account.setBalance(new BigDecimal("200000.00"));
        baseTime = LocalDateTime.of(2026, 10, 9, 12, 0, 0);
    }

    private Transaction createTxn(Long id, BigDecimal amount, TransactionType type,
                                  LocalDateTime time, String status) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setAccount(account);
        tx.setAmount(amount);
        tx.setTransactionType(type);
        tx.setTransactionTime(time);
        tx.setStatus(status);
        return tx;
    }

    @Test
    void shouldHandleNullTransactionGracefully() {
        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(null);
        assertEquals(0, result.getScore());
        assertFalse(result.isSuspicious());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertTrue(result.isInsufficientHistory());
    }

    @Test
    void shouldNotBlockNewAccountWithSingleLegitimatePayment() {
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(List.of());

        Transaction current = createTxn(99L, new BigDecimal("25000.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertEquals(0, result.getScore());
        assertFalse(result.isSuspicious());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertTrue(result.isInsufficientHistory());
        assertEquals("Normal activity (New account / Insufficient history)", result.getPrimaryReason());
    }

    @Test
    void shouldApproveNormalTransactionForEstablishedAccount() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            history.add(createTxn((long) i, new BigDecimal("10000.00"), TransactionType.PAYMENT,
                    baseTime.minusDays(i), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("8000.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertEquals(0, result.getScore());
        assertFalse(result.isSuspicious());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
        assertFalse(result.isInsufficientHistory());
        assertEquals("Normal transaction pattern", result.getPrimaryReason());
    }

    @Test
    void shouldBlockOnExtremeRapidVelocityInTenMinutes() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            history.add(createTxn((long) i, new BigDecimal("100.00"), TransactionType.PAYMENT,
                    baseTime.minusMinutes(i % 8 + 1), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("200.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertTrue(result.getPrimaryReason().contains("Extreme rapid transaction velocity"));
    }

    @Test
    void shouldBlockOnHighVelocityWithLargeAmount() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            history.add(createTxn((long) i, new BigDecimal("500.00"), TransactionType.PAYMENT,
                    baseTime.minusMinutes(i), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("55000.00"), TransactionType.TRANSFER,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("High velocity"));
    }

    @Test
    void shouldBlockOnRepeatedLargeTransactionsInTenMinutes() {
        List<Transaction> history = List.of(
                createTxn(1L, new BigDecimal("60000.00"), TransactionType.TRANSFER, baseTime.minusMinutes(2), "APPROVED"),
                createTxn(2L, new BigDecimal("70000.00"), TransactionType.TRANSFER, baseTime.minusMinutes(4), "APPROVED")
        );
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("50000.00"), TransactionType.TRANSFER,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Repeated large transactions"));
    }

    @Test
    void shouldBlockOnExtremeSpikeWithPriorRapidActivity() {
        List<Transaction> history = List.of(
                createTxn(1L, new BigDecimal("1000.00"), TransactionType.PAYMENT, baseTime.minusMinutes(2), "APPROVED"),
                createTxn(2L, new BigDecimal("1000.00"), TransactionType.PAYMENT, baseTime.minusMinutes(4), "APPROVED")
        );
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("600000.00"), TransactionType.TRANSFER,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Extreme amount spike"));
    }

    @Test
    void shouldBlockOnExcessiveCumulative24HourVolume() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            history.add(createTxn((long) i, new BigDecimal("55000.00"), TransactionType.PAYMENT,
                    baseTime.minusHours(i * 3), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        // Prior 5 * 55,000 = 275,000; current = 30,000 => cumulative = 305,000 > 300,000
        Transaction current = createTxn(99L, new BigDecimal("30000.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Excessive cumulative 24-hour volume"));
    }

    @Test
    void shouldBlockOnExcessive24HourTransactionFrequency() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            history.add(createTxn((long) i, new BigDecimal("500.00"), TransactionType.PAYMENT,
                    baseTime.minusHours(i % 23 + 1), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("500.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Excessive 24-hour transaction frequency"));
    }

    @Test
    void shouldBlockOnRepeatedRecentDeclinedOrBlockedAttempts() {
        List<Transaction> history = List.of(
                createTxn(1L, new BigDecimal("500.00"), TransactionType.PAYMENT, baseTime.minusMinutes(10), "DECLINED"),
                createTxn(2L, new BigDecimal("500.00"), TransactionType.PAYMENT, baseTime.minusMinutes(20), "DECLINED"),
                createTxn(3L, new BigDecimal("500.00"), TransactionType.PAYMENT, baseTime.minusMinutes(30), "BLOCKED")
        );
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("500.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Repeated declined/blocked attempts"));
    }

    @Test
    void shouldBlockOnExtremeDeviationFromHistoricalSpendingBaseline() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            history.add(createTxn((long) i, new BigDecimal("1000.00"), TransactionType.PAYMENT,
                    baseTime.minusDays(i), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        // Average = 1,000; max = 1,000; current = 60,000 (>10x avg, >= 50k, > 2x max)
        Transaction current = createTxn(99L, new BigDecimal("60000.00"), TransactionType.TRANSFER,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Extreme deviation from account spending baseline"));
    }

    @Test
    void shouldNotBlockLegitimateHighValueTransactionWithinNormalBaseline() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            history.add(createTxn((long) i, new BigDecimal("30000.00"), TransactionType.TRANSFER,
                    baseTime.minusDays(i), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("40000.00"), TransactionType.TRANSFER,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertEquals(0, result.getScore());
        assertFalse(result.isSuspicious());
        assertEquals(RiskLevel.LOW, result.getRiskLevel());
    }

    @Test
    void shouldBlockOnStructuringPatternJustBelowLimit() {
        // CASH_OUT limit is 50,000; 95% threshold is 47,500
        List<Transaction> history = List.of(
                createTxn(1L, new BigDecimal("48000.00"), TransactionType.CASH_OUT, baseTime.minusHours(2), "APPROVED"),
                createTxn(2L, new BigDecimal("49000.00"), TransactionType.CASH_OUT, baseTime.minusHours(5), "APPROVED")
        );
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("49500.00"), TransactionType.CASH_OUT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertTrue(result.getScore() >= 80);
        assertTrue(result.isSuspicious());
        assertTrue(result.getPrimaryReason().contains("Repeated structuring pattern"));
    }

    @Test
    void shouldElevateRiskOnSustained7DayVelocity() {
        List<Transaction> history = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            history.add(createTxn((long) i, new BigDecimal("100.00"), TransactionType.PAYMENT,
                    baseTime.minusDays(i % 6 + 1), "APPROVED"));
        }
        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(history);

        Transaction current = createTxn(99L, new BigDecimal("100.00"), TransactionType.PAYMENT,
                baseTime, "PENDING");

        FraudRuleEngine.RuleEvaluationResult result = fraudRuleEngine.evaluate(current);
        assertEquals(40, result.getScore());
        assertFalse(result.isSuspicious());
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
    }
}
