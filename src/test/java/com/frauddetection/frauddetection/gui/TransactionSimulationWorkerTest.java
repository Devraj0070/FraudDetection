package com.frauddetection.frauddetection.gui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSimulationWorkerTest {

    @Test
    void shouldExecuteMultithreadedSimulationWithSynchronization() throws Exception {
        RuleBasedStrategy strategy = new RuleBasedStrategy();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger processedCounter = new AtomicInteger(0);

        TransactionProcessListener testListener = new TransactionProcessListener() {
            @Override
            public void onTransactionStarted(TransactionRecord transaction) {
            }

            @Override
            public void onTransactionProcessed(TransactionRecord transaction, FraudPredictionResult result) {
                processedCounter.incrementAndGet();
                latch.countDown();
            }

            @Override
            public void onTransactionFailed(TransactionRecord transaction, Exception error) {
            }
        };

        TransactionSimulationWorker worker = new TransactionSimulationWorker(strategy, testListener);
        worker.setInterval(50); // fast simulation for unit test

        assertFalse(worker.isRunning());
        worker.start();
        assertTrue(worker.isRunning());

        // Wait up to 3 seconds for at least 2 transactions to be processed asynchronously
        boolean completed = latch.await(3, TimeUnit.SECONDS);
        worker.stop();
        assertFalse(worker.isRunning());

        assertTrue(completed, "Worker should process simulated transactions concurrently");
        assertTrue(processedCounter.get() >= 2);
    }
}
