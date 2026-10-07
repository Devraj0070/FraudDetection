package com.frauddetection.frauddetection.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Immutable/POJO record representing a financial transaction in the JDBC and GUI layers.
 * Implements {@link Comparable} to demonstrate Collections sorting.
 *
 * Demonstrates:
 * - OOP Implementation: Encapsulation & Comparable Interface
 * - Collections & Generics: Type-safe elements for lists, sets, and queues
 */
public class TransactionRecord implements Comparable<TransactionRecord> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private BigDecimal amount;
    private String transactionType;
    private LocalDateTime transactionTime;
    private String status;
    private Long accountId;
    private String ipAddress;
    private String userAgent;

    public TransactionRecord() {
    }

    public TransactionRecord(Long id, BigDecimal amount, String transactionType,
                             LocalDateTime transactionTime, String status,
                             Long accountId, String ipAddress, String userAgent) {
        this.id = id;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionTime = transactionTime;
        this.status = status;
        this.accountId = accountId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getFormattedTime() {
        return transactionTime != null ? transactionTime.format(FORMATTER) : "N/A";
    }

    @Override
    public int compareTo(TransactionRecord o) {
        if (o == null) return 1;
        if (this.transactionTime == null && o.transactionTime == null) return 0;
        if (this.transactionTime == null) return -1;
        if (o.transactionTime == null) return 1;
        // Descending order (newest first)
        return o.transactionTime.compareTo(this.transactionTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("TransactionRecord[id=%d, amount=%.2f, type=%s, status=%s, time=%s]",
                id, amount != null ? amount.doubleValue() : 0.0, transactionType, status, getFormattedTime());
    }
}
