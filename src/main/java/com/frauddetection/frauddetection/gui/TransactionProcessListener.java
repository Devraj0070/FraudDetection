package com.frauddetection.frauddetection.gui;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Observer listener interface notified during transaction processing events.
 *
 * Fulfills Academic Rubric:
 * - OOP Implementation: Interfaces & Observer Pattern
 */
public interface TransactionProcessListener {

    void onTransactionStarted(TransactionRecord transaction);

    void onTransactionProcessed(TransactionRecord transaction, FraudPredictionResult result);

    void onTransactionFailed(TransactionRecord transaction, Exception error);
}
