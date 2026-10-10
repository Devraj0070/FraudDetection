package com.frauddetection.frauddetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.frauddetection.frauddetection.entity.Account;
import com.frauddetection.frauddetection.entity.FraudPrediction;
import com.frauddetection.frauddetection.entity.Transaction;
import com.frauddetection.frauddetection.entity.TransactionType;
import com.frauddetection.frauddetection.entity.User;
import com.frauddetection.frauddetection.fraud.FraudRuleEngine;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionService;
import com.frauddetection.frauddetection.repository.AccountRepository;
import com.frauddetection.frauddetection.repository.FraudAlertRepository;
import com.frauddetection.frauddetection.repository.FraudPredictionRepository;
import com.frauddetection.frauddetection.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionConcurrencyTest {

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

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account sharedAccount;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        sharedAccount = new Account();
        sharedAccount.setId(1L);
        sharedAccount.setAccountNumber("100000000001");
        sharedAccount.setBalance(new BigDecimal("100.00")); // Initial balance: 100.00
    }

    @Test
    void shouldPreventOverspendingWhenTwoConcurrentWithdrawalsAttemptToDebitSameAccount() throws Exception {
        // Models database pessimistic row-lock (SELECT ... FOR UPDATE) serialization
        ReentrantLock pessimisticRowLock = new ReentrantLock();

        lenient().when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));
        lenient().when(fraudRuleEngine.isSuspicious(any(Transaction.class)))
                .thenReturn(false);

        lenient().when(accountRepository.findFirstByUserOrderByIdAsc(user))
                .thenAnswer(invocation -> {
                    pessimisticRowLock.lock();
                    return Optional.of(sharedAccount);
                });

        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {
                    Account acc = invocation.getArgument(0);
                    try {
                        return acc;
                    } finally {
                        if (pessimisticRowLock.isHeldByCurrentThread()) {
                            pessimisticRowLock.unlock();
                        }
                    }
                });

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    // Ensure lock is released if transaction declines without account save
                    if (pessimisticRowLock.isHeldByCurrentThread()) {
                        pessimisticRowLock.unlock();
                    }
                    return tx;
                });

        lenient().when(fraudPredictionRepository.save(any(FraudPrediction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);

        BigDecimal withdrawalAmount = new BigDecimal("80.00");

        Future<FraudPrediction> future1 = executor.submit(() -> {
            startGate.await();
            return transactionService.processPayment(
                    user, withdrawalAmount, TransactionType.PAYMENT, "127.0.0.1", "agent-1"
            );
        });

        Future<FraudPrediction> future2 = executor.submit(() -> {
            startGate.await();
            return transactionService.processPayment(
                    user, withdrawalAmount, TransactionType.PAYMENT, "127.0.0.1", "agent-2"
            );
        });

        // Trigger both threads simultaneously
        startGate.countDown();

        FraudPrediction pred1 = future1.get(5, TimeUnit.SECONDS);
        FraudPrediction pred2 = future2.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        String status1 = pred1.getTransaction().getStatus();
        String status2 = pred2.getTransaction().getStatus();

        // One must be APPROVED and one must be DECLINED
        boolean oneApproved = ("APPROVED".equals(status1) && "DECLINED".equals(status2))
                || ("DECLINED".equals(status1) && "APPROVED".equals(status2));

        assertTrue(oneApproved, "Exactly one transaction should succeed and one should fail due to insufficient funds");

        // The final account balance must be exactly 20.00 (100.00 - 80.00)
        assertEquals(new BigDecimal("20.00"), sharedAccount.getBalance());
        assertTrue(sharedAccount.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "Account balance must never become negative");
    }
}
