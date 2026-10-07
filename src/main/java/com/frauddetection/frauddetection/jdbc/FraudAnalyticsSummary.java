package com.frauddetection.frauddetection.jdbc;

import java.math.BigDecimal;

/**
 * Aggregated analytics metric record calculated via direct JDBC analytical SQL queries.
 */
public class FraudAnalyticsSummary {

    private long totalTransactions;
    private long approvedCount;
    private long declinedCount;
    private long blockedCount;
    private BigDecimal totalAmountProcessed;
    private BigDecimal totalFraudVolume;
    private double fraudRatePercent;

    public FraudAnalyticsSummary() {
        this.totalAmountProcessed = BigDecimal.ZERO;
        this.totalFraudVolume = BigDecimal.ZERO;
    }

    public FraudAnalyticsSummary(long totalTransactions, long approvedCount,
                                 long declinedCount, long blockedCount,
                                 BigDecimal totalAmountProcessed,
                                 BigDecimal totalFraudVolume,
                                 double fraudRatePercent) {
        this.totalTransactions = totalTransactions;
        this.approvedCount = approvedCount;
        this.declinedCount = declinedCount;
        this.blockedCount = blockedCount;
        this.totalAmountProcessed = totalAmountProcessed != null ? totalAmountProcessed : BigDecimal.ZERO;
        this.totalFraudVolume = totalFraudVolume != null ? totalFraudVolume : BigDecimal.ZERO;
        this.fraudRatePercent = fraudRatePercent;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(long approvedCount) {
        this.approvedCount = approvedCount;
    }

    public long getDeclinedCount() {
        return declinedCount;
    }

    public void setDeclinedCount(long declinedCount) {
        this.declinedCount = declinedCount;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public BigDecimal getTotalAmountProcessed() {
        return totalAmountProcessed;
    }

    public void setTotalAmountProcessed(BigDecimal totalAmountProcessed) {
        this.totalAmountProcessed = totalAmountProcessed;
    }

    public BigDecimal getTotalFraudVolume() {
        return totalFraudVolume;
    }

    public void setTotalFraudVolume(BigDecimal totalFraudVolume) {
        this.totalFraudVolume = totalFraudVolume;
    }

    public double getFraudRatePercent() {
        return fraudRatePercent;
    }

    public void setFraudRatePercent(double fraudRatePercent) {
        this.fraudRatePercent = fraudRatePercent;
    }

    @Override
    public String toString() {
        return String.format("FraudAnalyticsSummary[Total=%d, Approved=%d, Declined=%d, Blocked=%d, FraudRate=%.2f%%]",
                totalTransactions, approvedCount, declinedCount, blockedCount, fraudRatePercent);
    }
}
