package com.frauddetection.frauddetection.dto;

import java.time.LocalDateTime;

import com.frauddetection.frauddetection.entity.FraudAlert;

/**
 * Dedicated administrative response payload for fraud alerts.
 * Decouples JPA entity models from REST responses and ensures no password hashes can be serialized.
 */
public record AdminAlertResponse(
        Long id,
        String alertType,
        String severity,
        String status,
        LocalDateTime createdAt,
        AdminPendingTransactionDto transaction,
        AdminUserDetailDto reviewingAdmin,
        LocalDateTime reviewedAt,
        String decision,
        String reviewNotes
) {
    public static AdminAlertResponse fromEntity(FraudAlert alert) {
        if (alert == null) {
            return null;
        }
        return new AdminAlertResponse(
                alert.getId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getCreatedAt(),
                AdminPendingTransactionDto.fromEntity(alert.getTransaction()),
                AdminUserDetailDto.fromEntity(alert.getReviewingAdmin()),
                alert.getReviewedAt(),
                alert.getDecision(),
                alert.getReviewNotes()
        );
    }
}
