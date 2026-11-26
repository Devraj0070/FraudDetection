package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Request payload for creating and evaluating a new financial transaction.
 *
 * @param amount                Transaction amount (must be positive, scale <= 2)
 * @param transactionType       Transaction type string matching {@link com.frauddetection.frauddetection.entity.TransactionType}
 * @param receiverAccountNumber Optional recipient account number (required for TRANSFER transactions)
 * @param idempotencyKey        Optional client-generated unique idempotency token
 */
public record TransactionRequest(
        BigDecimal amount,
        String transactionType,
        String receiverAccountNumber,
        String idempotencyKey
) {
    public TransactionRequest(BigDecimal amount, String transactionType, String receiverAccountNumber) {
        this(amount, transactionType, receiverAccountNumber, null);
    }

    public TransactionRequest(BigDecimal amount, String transactionType) {
        this(amount, transactionType, null, null);
    }
}
