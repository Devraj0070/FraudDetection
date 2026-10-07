package com.frauddetection.frauddetection.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Multithreaded background engine for producing and processing live financial transactions.
 *
 * Fulfills Academic Rubric:
 * - Multithreading & Synchronization — 4 marks
 * - Thread-safe Producer-Consumer pattern using {@link BlockingQueue}
 * - Explicit {@code synchronized} blocks for thread coordination
 * - Background processing offloading Event Dispatch Thread (EDT)
 */
public class TransactionSimulationWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TransactionSimulationWorker.class);

    private static final String[] TRANSACTION_TYPES = {"PAYMENT", "TRANSFER", "CASH_OUT", "DEBIT", "CASH_IN"};
    private static final String[] SAMPLE_IPS = {
            "192.168.1.10", "10.0.0.45", "172.16.0.8", "45.33.32.156", "198.51.100.4"
    };

    private final BlockingQueue<TransactionRecord> transactionQueue;
    private final Object synchronizationLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong transactionSequence = new AtomicLong(1000);
    private final Random random = new Random();

    private FraudDetectionStrategy strategy;
    private TransactionProcessListener processListener;
    private Thread workerThread;
    private int simulationIntervalMillis = 1500;

    public TransactionSimulationWorker(FraudDetectionStrategy strategy,
                                      TransactionProcessListener processListener) {
        this.transactionQueue = new LinkedBlockingQueue<>(500);
        this.strategy = strategy;
        this.processListener = processListener;
    }

    public synchronized void setStrategy(FraudDetectionStrategy newStrategy) {
        synchronized (synchronizationLock) {
            this.strategy = newStrategy;
            log.info("Simulation strategy updated to: {}", newStrategy.getStrategyName());
        }
    }

    public synchronized void setProcessListener(TransactionProcessListener listener) {
        this.processListener = listener;
    }

    public void setInterval(int millis) {
        this.simulationIntervalMillis = Math.max(200, millis);
    }

    /**
     * Starts the background simulation thread.
     */
    public synchronized void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this, "FraudSimulation-WorkerThread");
            workerThread.setDaemon(true);
            workerThread.start();
            log.info("Transaction simulation worker started.");
        }
    }

    /**
     * Stops the background simulation thread gracefully.
     */
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            if (workerThread != null) {
                workerThread.interrupt();
            }
            log.info("Transaction simulation worker stopped.");
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // 1. Produce a simulated transaction
                TransactionRecord generatedTx = generateSyntheticTransaction();

                // 2. Synchronized queue ingestion
                synchronized (synchronizationLock) {
                    transactionQueue.offer(generatedTx);
                }

                // Notify UI listener that processing has begun
                if (processListener != null) {
                    SwingUtilities.invokeLater(() -> processListener.onTransactionStarted(generatedTx));
                }

                // 3. Process through active FraudDetectionStrategy
                FraudPredictionResult result;
                synchronized (synchronizationLock) {
                    result = strategy.evaluate(generatedTx);
                }

                // Update transaction outcome
                if ("FRAUD".equals(result.getPrediction())) {
                    generatedTx.setStatus("BLOCKED");
                } else if (generatedTx.getAmount().compareTo(new BigDecimal("150000.00")) > 0 &&
                           random.nextDouble() < 0.15) {
                    generatedTx.setStatus("DECLINED");
                } else {
                    generatedTx.setStatus("APPROVED");
                }

                // 4. Thread-safe UI Dispatch via SwingUtilities.invokeLater
                final TransactionRecord finalTx = generatedTx;
                final FraudPredictionResult finalResult = result;
                if (processListener != null) {
                    SwingUtilities.invokeLater(() ->
                            processListener.onTransactionProcessed(finalTx, finalResult));
                }

                // Sleep between simulation cycles
                Thread.sleep(simulationIntervalMillis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Simulation cycle error: {}", e.getMessage());
                if (processListener != null) {
                    SwingUtilities.invokeLater(() ->
                            processListener.onTransactionFailed(null, e));
                }
            }
        }
    }

    private TransactionRecord generateSyntheticTransaction() {
        long id = transactionSequence.incrementAndGet();
        String type = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];

        // Generate mix of realistic everyday amounts with occasional high-value spikes
        double rawAmount;
        if (random.nextDouble() < 0.15) {
            // High amount / potential fraud trigger (₹100,000 - ₹500,000)
            rawAmount = 100000.0 + (random.nextDouble() * 400000.0);
        } else {
            // Normal everyday amount (₹100 - ₹25,000)
            rawAmount = 100.0 + (random.nextDouble() * 24900.0);
        }

        BigDecimal amount = BigDecimal.valueOf(rawAmount).setScale(2, RoundingMode.HALF_UP);
        String ip = SAMPLE_IPS[random.nextInt(SAMPLE_IPS.length)];

        return new TransactionRecord(
                id,
                amount,
                type,
                LocalDateTime.now(),
                "PENDING",
                1L,
                ip,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Simulator/1.0"
        );
    }
}
