package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Request payload for creating and evaluating a new financial transaction.
 *
 * @param amount          Transaction amount (must be positive, scale <= 2)
 * @param transactionType Transaction type string matching {@link com.frauddetection.frauddetection.entity.TransactionType}
 */
public record TransactionRequest(
        BigDecimal amount,
        String transactionType
) {
}
