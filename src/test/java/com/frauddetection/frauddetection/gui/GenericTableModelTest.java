package com.frauddetection.frauddetection.gui;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.jdbc.TransactionRecord;

import static org.junit.jupiter.api.Assertions.*;

class GenericTableModelTest {

    @Test
    void shouldManageGenericsTableDataAndSorting() {
        List<String> columns = List.of("ID", "Amount", "Status");
        List<Function<TransactionRecord, Object>> extractors = List.of(
                TransactionRecord::getId,
                TransactionRecord::getAmount,
                TransactionRecord::getStatus
        );

        GenericTableModel<TransactionRecord> model = new GenericTableModel<>(columns, extractors);

        assertEquals(3, model.getColumnCount());
        assertEquals("ID", model.getColumnName(0));
        assertEquals("Amount", model.getColumnName(1));
        assertEquals("Status", model.getColumnName(2));
        assertEquals(0, model.getRowCount());

        TransactionRecord t1 = new TransactionRecord(1L, new BigDecimal("100.00"), "PAYMENT", LocalDateTime.now(), "APPROVED", 1L, "127.0.0.1", "ua");
        TransactionRecord t2 = new TransactionRecord(2L, new BigDecimal("5000.00"), "TRANSFER", LocalDateTime.now(), "BLOCKED", 1L, "127.0.0.1", "ua");

        model.addRow(t1);
        model.addRow(t2);

        assertEquals(2, model.getRowCount());
        // Since addRow inserts at index 0, t2 is first
        assertEquals(2L, model.getValueAt(0, 0));
        assertEquals(new BigDecimal("5000.00"), model.getValueAt(0, 1));
        assertEquals("BLOCKED", model.getValueAt(0, 2));

        // Test sorting by amount ascending
        model.sort(Comparator.comparing(TransactionRecord::getAmount));
        assertEquals(1L, model.getValueAt(0, 0));
        assertEquals(2L, model.getValueAt(1, 0));

        // Test clear
        model.clear();
        assertEquals(0, model.getRowCount());
    }
}
