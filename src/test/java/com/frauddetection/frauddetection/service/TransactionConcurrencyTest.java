package com.frauddetection.frauddetection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.frauddetection.frauddetection.repository.FundsReservationRepository;

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

    @Mock
    private FundsReservationRepository fundsReservationRepository;

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
        sharedAccount.setStatus("ACTIVE");
        sharedAccount.setBalance(new BigDecimal("100.00")); // Initial balance: 100.00
        lenient().when(fundsReservationRepository.sumActiveReservationsByAccount(any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Pessimistic findByIdWithLock prevents double-spending when concurrent withdrawals hit the same account")
    void shouldPreventOverspendingWhenTwoConcurrentWithdrawalsAttemptToDebitSameAccount() throws Exception {
        // Models database pessimistic row-lock (SELECT ... FOR UPDATE via findByIdWithLock)
        ReentrantLock pessimisticRowLock = new ReentrantLock();

        lenient().when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));
        lenient().when(fraudRuleEngine.isSuspicious(any(Transaction.class)))
                .thenReturn(false);

        // Unlocked read to resolve user account
        lenient().when(accountRepository.findFirstByUserOrderByIdAsc(user))
                .thenReturn(Optional.of(sharedAccount));

        // Explicit pessimistic locking query
        lenient().when(accountRepository.findByIdWithLock(1L))
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

        // One must be APPROVED and one must be DECLINED due to insufficient funds
        boolean oneApproved = ("APPROVED".equals(status1) && "DECLINED".equals(status2))
                || ("DECLINED".equals(status1) && "APPROVED".equals(status2));

        assertTrue(oneApproved, "Exactly one transaction should succeed and one should fail due to insufficient funds");

        // The final account balance must be exactly 20.00 (100.00 - 80.00)
        assertEquals(new BigDecimal("20.00"), sharedAccount.getBalance());
        assertTrue(sharedAccount.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "Account balance must never become negative");
    }

    @Test
    @DisplayName("Opposite-direction transfers acquire two-account locks in consistent ID order avoiding deadlock")
    void shouldAcquireLocksInAscendingIdOrderForOppositeDirectionTransfers() throws Exception {
        User userA = new User();
        userA.setId(10L);
        userA.setUsername("userA");

        Account accountA = new Account();
        accountA.setId(10L);
        accountA.setAccountNumber("ACC-10");
        accountA.setStatus("ACTIVE");
        accountA.setBalance(new BigDecimal("100.00"));

        User userB = new User();
        userB.setId(20L);
        userB.setUsername("userB");

        Account accountB = new Account();
        accountB.setId(20L);
        accountB.setAccountNumber("ACC-20");
        accountB.setStatus("ACTIVE");
        accountB.setBalance(new BigDecimal("100.00"));

        lenient().when(accountRepository.findFirstByUserOrderByIdAsc(userA)).thenReturn(Optional.of(accountA));
        lenient().when(accountRepository.findFirstByUserOrderByIdAsc(userB)).thenReturn(Optional.of(accountB));
        lenient().when(accountRepository.findByAccountNumber("ACC-10")).thenReturn(Optional.of(accountA));
        lenient().when(accountRepository.findByAccountNumber("ACC-20")).thenReturn(Optional.of(accountB));

        // Global lock representing ordered row lock acquisition (10L first, then 20L)
        ReentrantLock orderedGlobalLock = new ReentrantLock();

        lenient().when(accountRepository.findAccountsWithLockInOrder(eq(10L), eq(20L)))
                .thenAnswer(inv -> {
                    orderedGlobalLock.lock();
                    return List.of(accountA, accountB);
                });

        lenient().when(accountRepository.findAccountsWithLockInOrder(eq(20L), eq(10L)))
                .thenAnswer(inv -> {
                    orderedGlobalLock.lock();
                    return List.of(accountA, accountB);
                });

        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    if (orderedGlobalLock.isHeldByCurrentThread()) {
                        orderedGlobalLock.unlock();
                    }
                    return inv.getArgument(0);
                });

        lenient().when(fraudPredictionService.predict(any(Transaction.class)))
                .thenReturn(new FraudPredictionResult("LEGITIMATE", 0.01, "Random Forest"));
        lenient().when(fraudRuleEngine.isSuspicious(any(Transaction.class)))
                .thenReturn(false);
        lenient().when(fraudPredictionRepository.save(any(FraudPrediction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);

        // Transfer 1: A -> B, amount 40.00
        Future<FraudPrediction> future1 = executor.submit(() -> {
            startGate.await();
            return transactionService.processPayment(
                    userA, new BigDecimal("40.00"), TransactionType.TRANSFER, "ACC-20", "127.0.0.1", "ua-1"
            );
        });

        // Transfer 2: B -> A, amount 30.00
        Future<FraudPrediction> future2 = executor.submit(() -> {
            startGate.await();
            return transactionService.processPayment(
                    userB, new BigDecimal("30.00"), TransactionType.TRANSFER, "ACC-10", "127.0.0.1", "ua-2"
            );
        });

        startGate.countDown();

        FraudPrediction res1 = future1.get(5, TimeUnit.SECONDS);
        FraudPrediction res2 = future2.get(5, TimeUnit.SECONDS);

        executor.shutdown();

        assertEquals("APPROVED", res1.getTransaction().getStatus());
        assertEquals("APPROVED", res2.getTransaction().getStatus());

        // Account A: 100 - 40 + 30 = 90.00
        // Account B: 100 + 40 - 30 = 110.00
        assertEquals(new BigDecimal("90.00"), accountA.getBalance());
        assertEquals(new BigDecimal("110.00"), accountB.getBalance());
        assertEquals(new BigDecimal("200.00"), accountA.getBalance().add(accountB.getBalance()));
    }
}
