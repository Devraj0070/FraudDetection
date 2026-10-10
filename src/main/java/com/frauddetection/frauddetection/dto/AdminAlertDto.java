package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Data transfer object representing a fraud alert for administrator surveillance monitoring.
 *
 * @param id                Persisted fraud alert identifier
 * @param alertType         Classification anomaly type (e.g. SUSPICIOUS_VELOCITY, LIMIT_STRUCTURING)
 * @param severity          Severity level: HIGH, MEDIUM, LOW
 * @param status            Investigation status: OPEN, REVIEWED, RESOLVED
 * @param createdAt         Timestamp when the alert was triggered
 * @param transactionId     Associated transaction identifier
 * @param transactionAmount Transaction monetary amount in INR
 * @param transactionType   Transaction type (PAYMENT, TRANSFER, etc.)
 * @param transactionStatus Transaction outcome status (APPROVED, DECLINED, BLOCKED)
 */
public record AdminAlertDto(
        Long id,
        String alertType,
        String severity,
        String status,
        String createdAt,
        Long transactionId,
        BigDecimal transactionAmount,
        String transactionType,
        String transactionStatus
) {
}
