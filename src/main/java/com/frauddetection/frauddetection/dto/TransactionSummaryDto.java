package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Lightweight transaction summary for dashboard tables and charts.
 */
public record TransactionSummaryDto(
        Long id,
        BigDecimal amount,
        String transactionType,
        String status,
        String transactionTime
) {
}
