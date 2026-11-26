package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.frauddetection.frauddetection.entity.Transaction;

/**
 * Public administrative DTO for pending review and surveillance transactions.
 * Preserves compatibility with SafePayApiClient, Next.js frontend, and integration tests
 * while sanitizing nested user credentials.
 */
public record AdminPendingTransactionDto(
        Long id,
        BigDecimal amount,
        String transactionType,
        LocalDateTime transactionTime,
        String ipAddress,
        String userAgent,
        String status,
        String idempotencyKey,
        AdminAccountDetailDto account,
        AdminAccountDetailDto receiverAccount
) {
    public static AdminPendingTransactionDto fromEntity(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        String typeStr = transaction.getTransactionType() != null
                ? transaction.getTransactionType().name()
                : "PAYMENT";
        return new AdminPendingTransactionDto(
                transaction.getId(),
                transaction.getAmount(),
                typeStr,
                transaction.getTransactionTime(),
                transaction.getIpAddress(),
                transaction.getUserAgent(),
                transaction.getStatus(),
                transaction.getIdempotencyKey(),
                AdminAccountDetailDto.fromEntity(transaction.getAccount()),
                AdminAccountDetailDto.fromEntity(transaction.getReceiverAccount())
        );
    }
}
