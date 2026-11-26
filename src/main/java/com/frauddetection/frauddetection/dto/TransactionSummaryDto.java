package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Lightweight transaction summary for dashboard tables, audit logs, and admin surveillance.
 */
public record TransactionSummaryDto(
        Long id,
        BigDecimal amount,
        String transactionType,
        String status,
        String transactionTime,
        String accountNumber,
        String customerUsername
) {
    public TransactionSummaryDto(Long id, BigDecimal amount, String transactionType, String status, String transactionTime) {
        this(id, amount, transactionType, status, transactionTime, null, null);
    }
}
