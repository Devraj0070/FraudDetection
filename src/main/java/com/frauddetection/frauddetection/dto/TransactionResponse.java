package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

/**
 * Server evaluation and receipt response for a transaction request.
 *
 * @param success          Whether the transaction was accepted and processed by the system
 * @param message          Descriptive outcome message
 * @param transactionId    Persisted transaction identifier (null if rejected at validation)
 * @param amount           Processed amount
 * @param transactionType  Processed transaction type
 * @param status           Evaluation status: APPROVED, DECLINED, or BLOCKED
 * @param transactionTime  Timestamp of transaction execution (yyyy-MM-dd HH:mm:ss)
 * @param detectionReason  Fraud or business rule detection reason
 * @param riskAssessment   Risk tier: LOW, MEDIUM, or HIGH
 * @param fraudProbability Machine learning predicted fraud probability (0.0 to 1.0)
 * @param newBalance       Updated available account balance if applicable
 */
public record TransactionResponse(
        boolean success,
        String message,
        Long transactionId,
        BigDecimal amount,
        String transactionType,
        String status,
        String transactionTime,
        String detectionReason,
        String riskAssessment,
        Double fraudProbability,
        BigDecimal newBalance
) {
}
