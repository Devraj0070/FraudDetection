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
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.client.ApiClientException;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
    @Test
    @DisplayName("TransactionHistoryPanel status filter contains PENDING_REVIEW and executes filtered query (Headless)")
    void transactionHistoryPanel_statusFilter_containsPendingReview_headless() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> capturedStatus = new AtomicReference<>();

        SafePayApiClient testClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public List<TransactionSummaryDto> getTransactionHistory(String type, String status, String search) {
                capturedStatus.set(status);
                latch.countDown();
                return List.of(
                        new TransactionSummaryDto(105L, new BigDecimal("8500.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 10:00:00")
                );
            }
        };

        TransactionHistoryPanel panel = new TransactionHistoryPanel(testClient, () -> {});

        // Verify status filter contains PENDING_REVIEW
        boolean foundPendingReview = false;
        int pendingReviewIndex = -1;
        for (int i = 0; i < panel.getStatusFilter().getItemCount(); i++) {
            if ("PENDING_REVIEW".equals(panel.getStatusFilter().getItemAt(i))) {
                foundPendingReview = true;
                pendingReviewIndex = i;
                break;
            }
        }
        assertTrue(foundPendingReview, "Status filter combo must contain PENDING_REVIEW option");

        panel.getStatusFilter().setSelectedIndex(pendingReviewIndex);
        panel.loadTransactionHistory();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Loading worker should query API");
        assertEquals("PENDING_REVIEW", capturedStatus.get());
    }

    @Test
    @DisplayName("TransactionHistoryPanel StatusBadgeRenderer renders distinct styling for PENDING_REVIEW (Headless)")
    void transactionHistoryPanel_statusBadgeRenderer_rendersPendingReviewBadge_headless() {
        TransactionHistoryPanel.StatusBadgeRenderer renderer = new TransactionHistoryPanel.StatusBadgeRenderer();
        TransactionHistoryPanel panel = new TransactionHistoryPanel(new SafePayApiClient(), () -> {});

        // PENDING_REVIEW badge
        Component compPending = renderer.getTableCellRendererComponent(
                panel.getTransactionsTable(), "PENDING_REVIEW", false, false, 0, 4
        );
        assertNotNull(compPending);
        assertEquals(TransactionHistoryPanel.COLOR_STATUS_PENDING, compPending.getForeground());

        // APPROVED badge
        Component compApproved = renderer.getTableCellRendererComponent(
                panel.getTransactionsTable(), "APPROVED", false, false, 0, 4
        );
        assertEquals(TransactionHistoryPanel.COLOR_STATUS_APPROVED, compApproved.getForeground());

        // DECLINED badge
        Component compDeclined = renderer.getTableCellRendererComponent(
                panel.getTransactionsTable(), "DECLINED", false, false, 0, 4
        );
        assertEquals(TransactionHistoryPanel.COLOR_STATUS_DECLINED, compDeclined.getForeground());

        // BLOCKED badge
        Component compBlocked = renderer.getTableCellRendererComponent(
                panel.getTransactionsTable(), "BLOCKED", false, false, 0, 4
        );
        assertEquals(TransactionHistoryPanel.COLOR_STATUS_BLOCKED, compBlocked.getForeground());
    }

    @Test
    @DisplayName("TransactionHistoryPanel summary metrics count PENDING_REVIEW and volume accurately (Headless)")
    void transactionHistoryPanel_summaryCounts_includesPendingReview_headless() throws Exception {
        TransactionHistoryPanel panel = new TransactionHistoryPanel(new SafePayApiClient(), () -> {});

        List<TransactionSummaryDto> testData = List.of(
                new TransactionSummaryDto(101L, new BigDecimal("1000.00"), "PAYMENT", "APPROVED", "2026-11-18 10:00:00"),
                new TransactionSummaryDto(102L, new BigDecimal("2500.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 11:00:00"),
                new TransactionSummaryDto(103L, new BigDecimal("500.00"), "PAYMENT", "DECLINED", "2026-11-18 12:00:00"),
                new TransactionSummaryDto(104L, new BigDecimal("50000.00"), "TRANSFER", "BLOCKED", "2026-11-18 13:00:00"),
                new TransactionSummaryDto(105L, new BigDecimal("1500.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 14:00:00")
        );

        panel.updateTableData(testData);

        assertEquals("5", panel.getLblTotalCount().getText());
        assertEquals("1", panel.getLblApprovedCount().getText());
        assertEquals("2", panel.getPendingCountText());
        assertEquals("2", panel.getLblPendingCount().getText());
        assertEquals("1", panel.getLblDeclinedCount().getText());
        assertEquals("1", panel.getLblBlockedCount().getText());
        assertTrue(panel.getLblTotalVolume().getText().contains("55,500.00"));
        assertEquals(TransactionHistoryPanel.STATE_CONTENT, panel.getCurrentState());
    }

    @Test
    @DisplayName("TransactionHistoryPanel empty history displays STATE_EMPTY and zeroes counters (Headless)")
    void transactionHistoryPanel_emptyHistory_showsEmptyState_headless() {
        TransactionHistoryPanel panel = new TransactionHistoryPanel(new SafePayApiClient(), () -> {});

        panel.updateTableData(List.of());

        assertEquals(TransactionHistoryPanel.STATE_EMPTY, panel.getCurrentState());
        assertEquals("0", panel.getLblTotalCount().getText());
        assertEquals("0", panel.getLblApprovedCount().getText());
        assertEquals("0", panel.getPendingCountText());
        assertEquals(0, panel.getTransactionsTable().getRowCount());
    }

    @Test
    @DisplayName("TransactionHistoryPanel API failure transitions to STATE_ERROR with message (Headless)")
    void transactionHistoryPanel_apiFailure_showsErrorState_headless() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        SafePayApiClient testClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public List<TransactionSummaryDto> getTransactionHistory(String type, String status, String search)
                    throws ApiClientException {
                latch.countDown();
                throw new ApiClientException("Connection refused to backend API: 503");
            }
        };

        TransactionHistoryPanel panel = new TransactionHistoryPanel(testClient, () -> {});

        panel.loadTransactionHistory();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Worker must attempt loading");
        int retries = 0;
        while (!TransactionHistoryPanel.STATE_ERROR.equals(panel.getCurrentState()) && retries < 20) {
            Thread.sleep(50);
            retries++;
        }

        assertEquals(TransactionHistoryPanel.STATE_ERROR, panel.getCurrentState());
        assertTrue(panel.getErrorMessageLabel().getText().contains("Connection refused"));
    }

    @Test
    @DisplayName("TransactionHistoryPanel detail dialog displays clear security status without exposing raw ML models (Headless)")
    void transactionHistoryPanel_detailDialog_displaysWithoutExposingInternalMl_headless() {
        TransactionHistoryPanel panel = new TransactionHistoryPanel(new SafePayApiClient(), () -> {});

        TransactionResponse pendingResp = new TransactionResponse(
                true,
                "Your transaction is pending review by SafePay Fraud Protection.",
                901L,
                new BigDecimal("12500.00"),
                "TRANSFER",
                "PENDING_REVIEW",
                "2026-11-18 15:30:00",
                "Flagged for manual fraud review",
                "HIGH",
                0.8875,
                new BigDecimal("50000.00")
        );

        JPanel content = panel.buildDetailContent(pendingResp, null);
        assertNotNull(content);

        // Recursively extract all text from labels
        StringBuilder allText = new StringBuilder();
        extractText(content, allText);
        String text = allText.toString();

        // Must display user-facing security info
        assertTrue(text.contains("Under Administrator Review") || text.contains("Security & Verification"),
                "Detail dialog must show security verification status");
        assertTrue(text.contains("12,500.00"), "Detail dialog must show amount");
        assertTrue(text.contains("TRANSFER"), "Detail dialog must show type");

        // Must NOT expose internal ML details to customers
        assertFalse(text.contains("Probability:"), "Customer dialog must not expose 'Probability:'");
        assertFalse(text.contains("88.8%"), "Customer dialog must not expose percentage probability");
        assertFalse(text.contains("0.88"), "Customer dialog must not expose raw probability decimal");
        assertFalse(text.contains("HIGH RISK"), "Customer dialog must not expose raw ML risk assessment tier");
    }

    private void extractText(Component comp, StringBuilder sb) {
        if (comp instanceof JLabel lbl) {
            sb.append(" ").append(lbl.getText());
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                extractText(child, sb);
            }
        }
    }
}
