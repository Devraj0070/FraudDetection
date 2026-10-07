package com.frauddetection.frauddetection.jdbc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionRecordTest {

    @Test
    void shouldConstructAndProvideGetters() {
        LocalDateTime now = LocalDateTime.now();
        TransactionRecord record = new TransactionRecord(
                101L,
                new BigDecimal("5000.00"),
                "PAYMENT",
                now,
                "APPROVED",
                1L,
                "127.0.0.1",
                "JUnit/5.0"
        );

        assertEquals(101L, record.getId());
        assertEquals(new BigDecimal("5000.00"), record.getAmount());
        assertEquals("PAYMENT", record.getTransactionType());
        assertEquals(now, record.getTransactionTime());
        assertEquals("APPROVED", record.getStatus());
        assertEquals(1L, record.getAccountId());
        assertEquals("127.0.0.1", record.getIpAddress());
        assertEquals("JUnit/5.0", record.getUserAgent());
        assertNotNull(record.getFormattedTime());
        assertTrue(record.toString().contains("101"));
    }

    @Test
    void shouldSortDescendingByTimeUsingComparable() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 5, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 7, 10, 0);

        TransactionRecord r1 = new TransactionRecord(1L, BigDecimal.TEN, "PAYMENT", t1, "APPROVED", 1L, "127.0.0.1", "agent");
        TransactionRecord r2 = new TransactionRecord(2L, BigDecimal.TEN, "PAYMENT", t2, "APPROVED", 1L, "127.0.0.1", "agent");
        TransactionRecord r3 = new TransactionRecord(3L, BigDecimal.TEN, "PAYMENT", t3, "APPROVED", 1L, "127.0.0.1", "agent");

        List<TransactionRecord> list = new ArrayList<>(List.of(r1, r2, r3));
        Collections.sort(list);

        // Descending order (newest first)
        assertEquals(3L, list.get(0).getId());
        assertEquals(2L, list.get(1).getId());
        assertEquals(1L, list.get(2).getId());
    }
}
