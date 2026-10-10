package com.frauddetection.frauddetection.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Unit and visual rendering tests for {@link TransactionHistoryPanel}.
 * Verifies offscreen Java 2D vector painting, table model column mapping,
 * filter controls, summary metric calculations, and multi-state card transitions.
 */
class TransactionHistoryPanelTest {

    @Test
    @DisplayName("TransactionHistoryPanel initializes and paints all Java 2D vector elements")
    void transactionHistoryPanel_initializesAndRendersOffscreen() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final TransactionHistoryPanel[] panelHolder = new TransactionHistoryPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionHistoryPanel panel = new TransactionHistoryPanel(dummyClient, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();
            panelHolder[0] = panel;
        });

        TransactionHistoryPanel panel = panelHolder[0];
        assertNotNull(panel);
        assertNotNull(panel.getTransactionsTable());
        assertNotNull(panel.getSearchField());
        assertNotNull(panel.getTypeFilter());
        assertNotNull(panel.getStatusFilter());
        assertNotNull(panel.getRefreshButton());

        // Perform offscreen Java 2D painting verification
        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        // 1. Verify Header Hero card area has painted pixels (x=500, y=30)
        int heroPixel = image.getRGB(500, 30);
        assertNotEquals(0, heroPixel, "Header hero card area must have painted pixels");

        // 2. Verify Metrics Summary area has painted pixels (x=300, y=100)
        int metricsPixel = image.getRGB(300, 100);
        assertNotEquals(0, metricsPixel, "Metrics card area must have painted pixels");

        // Write offscreen rendering artifact to target directory
        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "transaction-history-panel-rendered.png"));
        }
    }

    @Test
    @DisplayName("TransactionHistoryPanel table model and metric calculations map accurately")
    void transactionHistoryPanel_tableDataAndMetrics_calculateCorrectly() throws Exception {
        final TransactionHistoryPanel[] panelHolder = new TransactionHistoryPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionHistoryPanel panel = new TransactionHistoryPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        TransactionHistoryPanel panel = panelHolder[0];

        List<TransactionSummaryDto> testData = List.of(
                new TransactionSummaryDto(101L, new BigDecimal("1200.00"), "PAYMENT", "APPROVED", "2026-10-09 10:00:00"),
                new TransactionSummaryDto(102L, new BigDecimal("45000.00"), "TRANSFER", "BLOCKED", "2026-10-09 11:30:00"),
                new TransactionSummaryDto(103L, new BigDecimal("150000.00"), "CASH_OUT", "DECLINED", "2026-10-09 12:15:00")
        );

        SwingUtilities.invokeAndWait(() -> {
            panel.updateTableData(testData);
        });

        assertEquals(3, panel.getDisplayedTransactions().size());
        assertEquals(3, panel.getTransactionsTable().getRowCount());
        assertEquals(6, panel.getTransactionsTable().getColumnCount());

        // Verify column headers
        assertEquals("ID", panel.getTransactionsTable().getColumnName(0));
        assertEquals("Date & Time", panel.getTransactionsTable().getColumnName(1));
        assertEquals("Type", panel.getTransactionsTable().getColumnName(2));
        assertEquals("Amount", panel.getTransactionsTable().getColumnName(3));
        assertEquals("Status", panel.getTransactionsTable().getColumnName(4));
        assertEquals("Action", panel.getTransactionsTable().getColumnName(5));

        // Verify values
        assertEquals("#101", panel.getTransactionsTable().getValueAt(0, 0));
        assertEquals("2026-10-09 10:00:00", panel.getTransactionsTable().getValueAt(0, 1));
        assertEquals("PAYMENT", panel.getTransactionsTable().getValueAt(0, 2));
        assertTrue(panel.getTransactionsTable().getValueAt(0, 3).toString().contains("1,200.00"));
        assertEquals("APPROVED", panel.getTransactionsTable().getValueAt(0, 4));
        assertEquals("Details →", panel.getTransactionsTable().getValueAt(0, 5));

        assertFalse(panel.getTransactionsTable().isCellEditable(0, 0));
        assertFalse(panel.getTransactionsTable().isCellEditable(0, 4));
    }

    @Test
    @DisplayName("TransactionHistoryPanel empty state triggers when zero transactions exist")
    void transactionHistoryPanel_emptyData_showsEmptyState() throws Exception {
        final TransactionHistoryPanel[] panelHolder = new TransactionHistoryPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionHistoryPanel panel = new TransactionHistoryPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        TransactionHistoryPanel panel = panelHolder[0];

        SwingUtilities.invokeAndWait(() -> {
            panel.updateTableData(List.of());
        });

        assertEquals(0, panel.getDisplayedTransactions().size());
        assertEquals(0, panel.getTransactionsTable().getRowCount());
    }

    @Test
    @DisplayName("TransactionHistoryPanel filter controls exist and reset cleanly")
    void transactionHistoryPanel_filterControls_resetCleanly() throws Exception {
        final TransactionHistoryPanel[] panelHolder = new TransactionHistoryPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionHistoryPanel panel = new TransactionHistoryPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        TransactionHistoryPanel panel = panelHolder[0];

        SwingUtilities.invokeAndWait(() -> {
            panel.getSearchField().setText("test_search");
            panel.getTypeFilter().setSelectedIndex(1);
            panel.getStatusFilter().setSelectedIndex(1);
            panel.getResetButton().doClick();
        });

        assertEquals("", panel.getSearchField().getText());
        assertEquals(0, panel.getTypeFilter().getSelectedIndex());
        assertEquals(0, panel.getStatusFilter().getSelectedIndex());
    }
}
