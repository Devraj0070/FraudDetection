package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consolidated dashboard metrics response for the SafePay desktop client.
 */
public record DashboardResponse(
        boolean success,
        String message,
        String username,
        String role,
        String accountNumber,
        String accountType,
        String accountStatus,
        BigDecimal balance,
        long totalTransactions,
        long approvedTransactions,
        long declinedTransactions,
        long blockedTransactions,
        List<TransactionSummaryDto> recentTransactions
) {
}
