package com.frauddetection.frauddetection.fraud.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class TransactionFeatureExtractorTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionFeatureExtractor featureExtractor;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1L);
        account.setAccountNumber("100000000001");
        account.setBalance(new BigDecimal("50000.00"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenTransactionIsNull() {
        assertThrows(IllegalArgumentException.class, () -> featureExtractor.extractFeatures(null));
    }

    @Test
    void shouldExtractFeaturesSuccessfullyFromCompleteTransaction() {
        Transaction tx = new Transaction();
        tx.setId(10L);
        tx.setAccount(account);
        tx.setAmount(new BigDecimal("2500.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setTransactionTime(LocalDateTime.of(2026, 10, 9, 14, 30));

        Transaction prev = new Transaction();
        prev.setId(9L);
        prev.setAccount(account);
        prev.setAmount(new BigDecimal("1500.00"));
        prev.setTransactionType(TransactionType.PAYMENT);
        prev.setTransactionTime(LocalDateTime.of(2026, 10, 9, 14, 25));

        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(List.of(prev));

        FeatureVector features = featureExtractor.extractFeatures(tx);

        assertNotNull(features);
        assertEquals(2500.0, features.getAmount());
        assertEquals(50000.0, features.getAccountBalance());
        assertEquals("PAYMENT", features.getTransactionType());
        assertEquals(14, features.getTransactionHour());
        assertEquals(1, features.getRecentTransactionCount());
        assertEquals(1500.0, features.getAverageTransactionAmount());
        assertEquals(1000.0, features.getAmountDifferenceFromAverage());
    }

    @Test
    void shouldHandleNullFieldsGracefullyWithoutNullPointerException() {
        Transaction tx = new Transaction();
        tx.setAmount(null);
        tx.setAccount(null);
        tx.setTransactionTime(null);
        tx.setTransactionType(null);

        FeatureVector features = featureExtractor.extractFeatures(tx);

        assertNotNull(features);
        assertEquals(0.0, features.getAmount());
        assertEquals(0.0, features.getAccountBalance());
        assertEquals("PAYMENT", features.getTransactionType());
        assertEquals(0, features.getRecentTransactionCount());
        assertEquals(0.0, features.getAverageTransactionAmount());
    }

    @Test
    void shouldHandleNullAccountBalanceGracefully() {
        account.setBalance(null);
        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(new BigDecimal("100.00"));
        tx.setTransactionType(TransactionType.PAYMENT);
        tx.setTransactionTime(LocalDateTime.now());

        when(transactionRepository.findByAccountOrderByTransactionTimeDesc(account))
                .thenReturn(List.of());

        FeatureVector features = featureExtractor.extractFeatures(tx);

        assertNotNull(features);
        assertEquals(100.0, features.getAmount());
        assertEquals(0.0, features.getAccountBalance());
    }
}
