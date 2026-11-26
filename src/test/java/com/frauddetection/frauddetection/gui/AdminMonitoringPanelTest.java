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
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AdminAlertDto;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;
import com.frauddetection.frauddetection.client.ApiClientException;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Unit and visual rendering tests for {@link AdminMonitoringPanel}.
 * Verifies offscreen Java 2D vector painting, alerts table model mapping,
 * severity/status filter controls, dynamic metric calculations, and empty state transitions.
 */
class AdminMonitoringPanelTest {

    @Test
    @DisplayName("AdminMonitoringPanel initializes and paints all Java 2D vector elements")
    void adminMonitoringPanel_initializesAndRendersOffscreen() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final AdminMonitoringPanel[] panelHolder = new AdminMonitoringPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AdminMonitoringPanel panel = new AdminMonitoringPanel(dummyClient, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();
            panelHolder[0] = panel;
        });

        AdminMonitoringPanel panel = panelHolder[0];
        assertNotNull(panel);
        assertNotNull(panel.getAlertsTable());
        assertNotNull(panel.getSearchField());
        assertNotNull(panel.getSeverityFilter());
        assertNotNull(panel.getStatusFilter());
        assertNotNull(panel.getRefreshButton());
        assertNotNull(panel.getApplyFiltersButton());

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
            ImageIO.write(image, "png", new File(outDir, "admin-monitoring-panel-rendered.png"));
        }
    }

    @Test
    @DisplayName("AdminMonitoringPanel table model and summary metrics compute accurately")
    void adminMonitoringPanel_tableDataAndMetrics_calculateAccurately() throws Exception {
        final AdminMonitoringPanel[] panelHolder = new AdminMonitoringPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AdminMonitoringPanel panel = new AdminMonitoringPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        AdminMonitoringPanel panel = panelHolder[0];

        List<AdminAlertDto> testAlerts = List.of(
                new AdminAlertDto(1L, "SUSPICIOUS_VELOCITY", "HIGH", "OPEN", "2026-10-09 12:00:00", 101L, new BigDecimal("75000.00"), "TRANSFER", "BLOCKED"),
                new AdminAlertDto(2L, "LIMIT_STRUCTURING", "MEDIUM", "OPEN", "2026-10-09 13:00:00", 102L, new BigDecimal("49000.00"), "PAYMENT", "DECLINED"),
                new AdminAlertDto(3L, "BASELINE_DEVIATION", "LOW", "RESOLVED", "2026-10-09 14:00:00", 103L, new BigDecimal("1200.00"), "PAYMENT", "APPROVED")
        );

        SwingUtilities.invokeAndWait(() -> {
            panel.setAlertsData(testAlerts);
        });

        assertEquals(3, panel.getDisplayedAlertsList().size());
        assertEquals(3, panel.getAlertsTable().getRowCount());
        assertEquals(8, panel.getAlertsTable().getColumnCount());

        // Verify column names
        assertEquals("Alert ID", panel.getAlertsTable().getColumnName(0));
        assertEquals("Anomaly Type", panel.getAlertsTable().getColumnName(1));
        assertEquals("Severity", panel.getAlertsTable().getColumnName(2));
        assertEquals("Status", panel.getAlertsTable().getColumnName(3));
        assertEquals("Triggered At", panel.getAlertsTable().getColumnName(4));
        assertEquals("Txn ID", panel.getAlertsTable().getColumnName(5));
        assertEquals("Amount", panel.getAlertsTable().getColumnName(6));
        assertEquals("Txn Status", panel.getAlertsTable().getColumnName(7));

        // Verify values of first row
        assertEquals("#1", panel.getAlertsTable().getValueAt(0, 0));
        assertEquals("SUSPICIOUS_VELOCITY", panel.getAlertsTable().getValueAt(0, 1));
        assertEquals("HIGH", panel.getAlertsTable().getValueAt(0, 2));
        assertEquals("OPEN", panel.getAlertsTable().getValueAt(0, 3));
        assertEquals("2026-10-09 12:00:00", panel.getAlertsTable().getValueAt(0, 4));
        assertEquals("#101", panel.getAlertsTable().getValueAt(0, 5));
        assertTrue(panel.getAlertsTable().getValueAt(0, 6).toString().contains("75,000.00"));
        assertEquals("BLOCKED", panel.getAlertsTable().getValueAt(0, 7));

        assertFalse(panel.getAlertsTable().isCellEditable(0, 0));
        assertFalse(panel.getAlertsTable().isCellEditable(0, 6));

        // Verify metrics summary calculation
        assertEquals("3", panel.getTotalAlertsText());
        assertEquals("1", panel.getHighSeverityCountText());
        assertEquals("1", panel.getMedSeverityCountText());
        assertEquals("2", panel.getOpenCountText());
    }

    @Test
    @DisplayName("AdminMonitoringPanel severity and search filters filter data accurately")
    void adminMonitoringPanel_filtersAndSearch_filterCorrectly() throws Exception {
        final AdminMonitoringPanel[] panelHolder = new AdminMonitoringPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AdminMonitoringPanel panel = new AdminMonitoringPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        AdminMonitoringPanel panel = panelHolder[0];

        List<AdminAlertDto> testAlerts = List.of(
                new AdminAlertDto(1L, "SUSPICIOUS_VELOCITY", "HIGH", "OPEN", "2026-10-09 12:00:00", 101L, new BigDecimal("75000.00"), "TRANSFER", "BLOCKED"),
                new AdminAlertDto(2L, "LIMIT_STRUCTURING", "MEDIUM", "OPEN", "2026-10-09 13:00:00", 102L, new BigDecimal("49000.00"), "PAYMENT", "DECLINED"),
                new AdminAlertDto(3L, "BASELINE_DEVIATION", "LOW", "RESOLVED", "2026-10-09 14:00:00", 103L, new BigDecimal("1200.00"), "PAYMENT", "APPROVED")
        );

        SwingUtilities.invokeAndWait(() -> {
            panel.setAlertsData(testAlerts);
        });

        // 1. Filter by HIGH severity
        SwingUtilities.invokeAndWait(() -> {
            panel.getSeverityFilter().setSelectedItem("HIGH");
            panel.getApplyFiltersButton().doClick();
        });
        assertEquals(1, panel.getDisplayedAlertsList().size());
        assertEquals(1L, panel.getDisplayedAlertsList().get(0).id());

        // 2. Search by Txn ID "102"
        SwingUtilities.invokeAndWait(() -> {
            panel.getSeverityFilter().setSelectedIndex(0); // All
            panel.getSearchField().setText("102");
            panel.getApplyFiltersButton().doClick();
        });
        assertEquals(1, panel.getDisplayedAlertsList().size());
        assertEquals(2L, panel.getDisplayedAlertsList().get(0).id());

        // 3. Reset filters
        SwingUtilities.invokeAndWait(() -> {
            panel.getResetFiltersButton().doClick();
        });
        assertEquals(3, panel.getDisplayedAlertsList().size());
        assertEquals("", panel.getSearchField().getText());
    }

    @Test
    @DisplayName("AdminMonitoringPanel empty state activates when alert list is empty")
    void adminMonitoringPanel_emptyData_showsEmptyState() throws Exception {
        final AdminMonitoringPanel[] panelHolder = new AdminMonitoringPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AdminMonitoringPanel panel = new AdminMonitoringPanel(dummyClient, () -> {});
            panelHolder[0] = panel;
        });

        AdminMonitoringPanel panel = panelHolder[0];

        SwingUtilities.invokeAndWait(() -> {
            panel.setAlertsData(List.of());
        });

        assertEquals(0, panel.getDisplayedAlertsList().size());
        assertEquals(0, panel.getAlertsTable().getRowCount());
        assertEquals("0", panel.getTotalAlertsText());
    }

    @Test
    @DisplayName("AdminMonitoringPanel pending transactions load and populate table (Headless)")
    void adminMonitoringPanel_pendingTransactions_loadsAndPopulatesTable_headless() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        List<TransactionSummaryDto> mockPending = List.of(
                new TransactionSummaryDto(201L, new BigDecimal("15000.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 10:00:00", "ACC-1001", "user1"),
                new TransactionSummaryDto(202L, new BigDecimal("4500.00"), "PAYMENT", "PENDING_REVIEW", "2026-11-18 10:15:00", "ACC-1002", "user2")
        );

        SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public List<TransactionSummaryDto> getPendingTransactions() {
                latch.countDown();
                return mockPending;
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(client, () -> {});
        panel.loadPendingTransactions();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Worker must fetch pending transactions");

        // Wait briefly for EDT update
        int retries = 0;
        while (panel.getPendingTransactionsList().size() < 2 && retries < 20) {
            Thread.sleep(50);
            retries++;
        }

        assertEquals(2, panel.getPendingTransactionsList().size());
        assertEquals(2, panel.getPendingTable().getRowCount());
        assertEquals("#201", panel.getPendingTable().getValueAt(0, 0));
        assertTrue(panel.getPendingTable().getValueAt(0, 1).toString().contains("ACC-1001"));
        assertEquals("TRANSFER", panel.getPendingTable().getValueAt(0, 2));
        assertTrue(panel.getPendingTable().getValueAt(0, 3).toString().contains("15,000.00"));
        assertEquals("PENDING_REVIEW", panel.getPendingTable().getValueAt(0, 5));
        assertEquals("Review & Decide →", panel.getPendingTable().getValueAt(0, 6));
        assertEquals("2", panel.getPendingReviewCountText());
    }

    @Test
    @DisplayName("AdminMonitoringPanel pending-review count updates accurately from dataset (Headless)")
    void adminMonitoringPanel_pendingReviewCount_computesAccurately_headless() {
        AdminMonitoringPanel panel = new AdminMonitoringPanel(new SafePayApiClient(), () -> {});

        List<TransactionSummaryDto> mockPending = List.of(
                new TransactionSummaryDto(301L, new BigDecimal("1000.00"), "PAYMENT", "PENDING_REVIEW", "2026-11-18 11:00:00"),
                new TransactionSummaryDto(302L, new BigDecimal("2000.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 11:05:00"),
                new TransactionSummaryDto(303L, new BigDecimal("3000.00"), "PAYMENT", "PENDING_REVIEW", "2026-11-18 11:10:00")
        );

        panel.setPendingData(mockPending);

        assertEquals("3", panel.getPendingReviewCountText());
        assertEquals("3", panel.getPendingReviewCountLabel().getText());
        assertEquals(3, panel.getPendingTransactionsList().size());
        assertTrue(panel.getViewPendingButton().getText().contains("3"));
    }

    @Test
    @DisplayName("AdminMonitoringPanel empty pending state handles zero records cleanly (Headless)")
    void adminMonitoringPanel_pendingTransactions_emptyState_headless() {
        AdminMonitoringPanel panel = new AdminMonitoringPanel(new SafePayApiClient(), () -> {});

        panel.setPendingData(List.of());

        assertEquals("0", panel.getPendingReviewCountText());
        assertEquals(0, panel.getPendingTransactionsList().size());
        assertEquals(0, panel.getPendingTable().getRowCount());
    }

    @Test
    @DisplayName("AdminMonitoringPanel handles 403 Forbidden with unauthorized state (Headless)")
    void adminMonitoringPanel_unauthorizedState_onForbiddenResponse_headless() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        SafePayApiClient forbiddenClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public List<AdminAlertDto> getAdminAlerts() throws ApiClientException {
                latch.countDown();
                throw new ApiClientException(403, "Access forbidden: Administrator privileges required.");
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(forbiddenClient, () -> {});
        panel.loadAlerts();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Worker must attempt loading");

        int retries = 0;
        while (!AdminMonitoringPanel.STATE_UNAUTHORIZED.equals(panel.getCurrentState()) && retries < 20) {
            Thread.sleep(50);
            retries++;
        }

        assertEquals(AdminMonitoringPanel.STATE_UNAUTHORIZED, panel.getCurrentState());
        assertTrue(panel.getUnauthorizedMessageLabel().getText().contains("Administrator privileges required"));
    }

    @Test
    @DisplayName("AdminMonitoringPanel investigation dialog presents key details without exposing raw ML models (Headless)")
    void adminMonitoringPanel_investigationDetails_displaysWithoutExposingRawMl_headless() {
        AdminMonitoringPanel panel = new AdminMonitoringPanel(new SafePayApiClient(), () -> {});

        // Pre-populate an alert linked to transaction 401
        panel.setAlertsData(List.of(
                new AdminAlertDto(10L, "SUSPICIOUS_VELOCITY", "HIGH", "OPEN", "2026-11-18 12:00:00", 401L, new BigDecimal("35000.00"), "TRANSFER", "PENDING_REVIEW")
        ));

        TransactionSummaryDto txSummary = new TransactionSummaryDto(
                401L, new BigDecimal("35000.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 12:00:00", "ACC-SENDER-01", "alice"
        );

        TransactionResponse txDetails = new TransactionResponse(
                true,
                "Transaction paused for administrator review.",
                401L,
                new BigDecimal("35000.00"),
                "TRANSFER",
                "PENDING_REVIEW",
                "2026-11-18 12:00:00",
                "Machine Learning Velocity Anomaly",
                "HIGH",
                0.892,
                new BigDecimal("120000.00")
        );

        JPanel content = panel.buildInvestigationContent(txSummary, txDetails, null);
        assertNotNull(content);

        // Recursively inspect all text in components
        StringBuilder sb = new StringBuilder();
        extractText(content, sb);
        String text = sb.toString();

        // Must display actionable investigation facts
        assertTrue(text.contains("35,000.00"), "Must show amount");
        assertTrue(text.contains("TRANSFER"), "Must show type");
        assertTrue(text.contains("ACC-SENDER-01"), "Must show sender account");
        assertTrue(text.contains("alice"), "Must show customer username");
        assertTrue(text.contains("SUSPICIOUS_VELOCITY"), "Must show linked alert anomaly");
        assertTrue(text.contains("HIGH"), "Must show alert severity");

        // Must NOT expose internal ML decimals or percentages
        assertFalse(text.contains("0.892"), "Must not expose raw probability float");
        assertFalse(text.contains("89.2%"), "Must not expose probability percentage");
        assertFalse(text.contains("Probability:"), "Must not expose raw model probability label");
    }

    @Test
    @DisplayName("AdminMonitoringPanel approve transaction calls SafePayApiClient and triggers refresh (Headless)")
    void adminMonitoringPanel_approveTransaction_callsApiClientAndRefreshes_headless() throws Exception {
        CountDownLatch approveLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);
        CountDownLatch refreshLatch = new CountDownLatch(1);
        AtomicReference<Long> capturedId = new AtomicReference<>();
        AtomicReference<String> capturedNotes = new AtomicReference<>();
        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        SafePayApiClient testClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public TransactionResponse approveTransaction(Long id, String notes) {
                capturedId.set(id);
                capturedNotes.set(notes);
                approveLatch.countDown();
                return new TransactionResponse(
                        true, "Approved and settled successfully.", id, new BigDecimal("1000.00"),
                        "PAYMENT", "APPROVED", "2026-11-18 12:30:00", "Normal", "LOW", 0.01, new BigDecimal("50000.00")
                );
            }

            @Override
            public List<TransactionSummaryDto> getPendingTransactions() {
                refreshCalled.set(true);
                refreshLatch.countDown();
                return List.of();
            }

            @Override
            public List<AdminAlertDto> getAdminAlerts() {
                refreshCalled.set(true);
                refreshLatch.countDown();
                return List.of();
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(testClient, () -> {});
        JLabel feedbackLabel = new JLabel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JTextField txtNotes = new JTextField("ID confirmed by administrator");

        panel.executeReviewDecision(
                501L,
                true,
                "ID confirmed by administrator",
                null,
                feedbackLabel,
                btnApprove,
                btnReject,
                txtNotes,
                completeLatch::countDown
        );

        assertTrue(approveLatch.await(5, TimeUnit.SECONDS), "Approve API call must be made");
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS), "Worker must finish");
        assertTrue(refreshLatch.await(5, TimeUnit.SECONDS), "Must trigger feed refresh after approval");

        assertEquals(501L, capturedId.get());
        assertEquals("ID confirmed by administrator", capturedNotes.get());
        assertTrue(feedbackLabel.getText().contains("Approved"), "Feedback must confirm approval");
        assertTrue(refreshCalled.get(), "Must trigger feed refresh after approval");
    }

    @Test
    @DisplayName("AdminMonitoringPanel reject transaction calls SafePayApiClient and triggers refresh (Headless)")
    void adminMonitoringPanel_rejectTransaction_callsApiClientAndRefreshes_headless() throws Exception {
        CountDownLatch rejectLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(1);
        CountDownLatch refreshLatch = new CountDownLatch(1);
        AtomicReference<Long> capturedId = new AtomicReference<>();
        AtomicReference<String> capturedNotes = new AtomicReference<>();
        AtomicBoolean refreshCalled = new AtomicBoolean(false);

        SafePayApiClient testClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public TransactionResponse rejectTransaction(Long id, String notes) {
                capturedId.set(id);
                capturedNotes.set(notes);
                rejectLatch.countDown();
                return new TransactionResponse(
                        true, "Transaction rejected.", id, new BigDecimal("5000.00"),
                        "TRANSFER", "DECLINED", "2026-11-18 12:35:00", "Fraud Detected", "HIGH", 0.95, new BigDecimal("50000.00")
                );
            }

            @Override
            public List<TransactionSummaryDto> getPendingTransactions() {
                refreshCalled.set(true);
                refreshLatch.countDown();
                return List.of();
            }

            @Override
            public List<AdminAlertDto> getAdminAlerts() {
                refreshCalled.set(true);
                refreshLatch.countDown();
                return List.of();
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(testClient, () -> {});
        JLabel feedbackLabel = new JLabel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JTextField txtNotes = new JTextField("Flagged as fraudulent by phone verification");

        panel.executeReviewDecision(
                502L,
                false,
                "Flagged as fraudulent by phone verification",
                null,
                feedbackLabel,
                btnApprove,
                btnReject,
                txtNotes,
                completeLatch::countDown
        );

        assertTrue(rejectLatch.await(5, TimeUnit.SECONDS), "Reject API call must be made");
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS), "Worker must finish");
        assertTrue(refreshLatch.await(5, TimeUnit.SECONDS), "Must trigger feed refresh after rejection");

        assertEquals(502L, capturedId.get());
        assertEquals("Flagged as fraudulent by phone verification", capturedNotes.get());
        assertTrue(feedbackLabel.getText().contains("Rejected"), "Feedback must confirm rejection");
        assertTrue(refreshCalled.get(), "Must trigger feed refresh after rejection");
    }

    @Test
    @DisplayName("AdminMonitoringPanel review action handles API failure gracefully (Headless)")
    void adminMonitoringPanel_reviewAction_handlesApiError_headless() throws Exception {
        CountDownLatch completeLatch = new CountDownLatch(1);

        SafePayApiClient failingClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public TransactionResponse approveTransaction(Long id, String notes) throws ApiClientException {
                throw new ApiClientException(400, "Transaction already finalized or settled.");
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(failingClient, () -> {});
        JLabel feedbackLabel = new JLabel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JTextField txtNotes = new JTextField("Notes");

        panel.executeReviewDecision(
                503L,
                true,
                "Notes",
                null,
                feedbackLabel,
                btnApprove,
                btnReject,
                txtNotes,
                completeLatch::countDown
        );

        assertTrue(completeLatch.await(5, TimeUnit.SECONDS), "Worker must finish");

        assertTrue(feedbackLabel.getText().contains("Failed: Transaction already finalized or settled"),
                "Feedback must show error message");
        assertTrue(btnApprove.isEnabled(), "Buttons must be re-enabled on error for retry");
        assertTrue(btnReject.isEnabled(), "Buttons must be re-enabled on error for retry");
        assertTrue(txtNotes.isEnabled(), "Notes must be re-enabled on error for retry");
    }

    @Test
    @DisplayName("AdminMonitoringPanel review action disables controls while executing (Headless)")
    void adminMonitoringPanel_reviewAction_disablesButtonsWhileRunning_headless() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch holdLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);

        SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public TransactionResponse approveTransaction(Long id, String notes) {
                startLatch.countDown();
                try {
                    holdLatch.await(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new TransactionResponse(
                        true, "Success", id, BigDecimal.TEN, "PAYMENT", "APPROVED",
                        "2026-11-18 12:40:00", "Normal", "LOW", 0.01, BigDecimal.TEN
                );
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(client, () -> {});
        JLabel feedbackLabel = new JLabel();
        JButton btnApprove = new JButton("Approve");
        JButton btnReject = new JButton("Reject");
        JTextField txtNotes = new JTextField("Notes");

        panel.executeReviewDecision(
                504L,
                true,
                "Notes",
                null,
                feedbackLabel,
                btnApprove,
                btnReject,
                txtNotes,
                finishLatch::countDown
        );

        // Wait until worker starts running
        assertTrue(startLatch.await(5, TimeUnit.SECONDS), "Worker must start");

        // Verify buttons are disabled to prevent duplicate submissions
        assertFalse(btnApprove.isEnabled(), "Approve button must be disabled while running");
        assertFalse(btnReject.isEnabled(), "Reject button must be disabled while running");
        assertFalse(txtNotes.isEnabled(), "Notes input must be disabled while running");

        // Release worker
        holdLatch.countDown();
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS), "Worker must finish");
    }

    @Test
    @DisplayName("AdminMonitoringPanel view switcher toggles between Alerts and Pending views (Headless)")
    void adminMonitoringPanel_viewSwitching_togglesBetweenAlertsAndPending_headless() {
        AdminMonitoringPanel panel = new AdminMonitoringPanel(new SafePayApiClient(), () -> {});

        assertFalse(panel.isPendingViewActive(), "Alerts view should be active by default");

        panel.showPendingView();
        assertTrue(panel.isPendingViewActive(), "Pending view should become active");

        panel.showAlertsView();
        assertFalse(panel.isPendingViewActive(), "Alerts view should become active again");
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

    @Test
    @DisplayName("AdminMonitoringPanel handles API runtime exception with error state (Headless)")
    void adminMonitoringPanel_errorState_onGenericFailure_headless() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        SafePayApiClient errorClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public List<AdminAlertDto> getAdminAlerts() throws ApiClientException {
                latch.countDown();
                throw new ApiClientException(500, "Internal Server Error: Database unreachable");
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(errorClient, () -> {});
        panel.loadAlerts();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Worker must attempt loading");

        int retries = 0;
        while (!AdminMonitoringPanel.STATE_ERROR.equals(panel.getCurrentState()) && retries < 20) {
            Thread.sleep(50);
            retries++;
        }

        assertEquals(AdminMonitoringPanel.STATE_ERROR, panel.getCurrentState());
        assertTrue(panel.getErrorMessageLabel().getText().contains("Database unreachable"));
    }

    @Test
    @DisplayName("AdminMonitoringPanel dialog Approve button triggers approval API call (Headless)")
    void adminMonitoringPanel_dialogApproveButton_triggersApprovalWorker_headless() throws Exception {
        CountDownLatch approveLatch = new CountDownLatch(1);
        AtomicReference<Long> approvedTxId = new AtomicReference<>();
        AtomicReference<String> approvedNotes = new AtomicReference<>();

        SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public TransactionResponse approveTransaction(Long id, String notes) {
                approvedTxId.set(id);
                approvedNotes.set(notes);
                approveLatch.countDown();
                return new TransactionResponse(
                        true, "Transaction approved.", id, new BigDecimal("1000.00"),
                        "PAYMENT", "APPROVED", "2026-11-18 13:00:00", "Normal", "LOW", 0.01, new BigDecimal("50000.00")
                );
            }

            @Override
            public List<TransactionSummaryDto> getPendingTransactions() {
                return List.of();
            }

            @Override
            public List<AdminAlertDto> getAdminAlerts() {
                return List.of();
            }
        };

        AdminMonitoringPanel panel = new AdminMonitoringPanel(client, () -> {});
        panel.setAutoConfirmForTesting(true);

        TransactionSummaryDto txSummary = new TransactionSummaryDto(
                888L, new BigDecimal("5000.00"), "TRANSFER", "PENDING_REVIEW", "2026-11-18 13:00:00", "ACC-888", "bob"
        );

        JPanel content = panel.buildInvestigationContent(txSummary, null, null);

        JButton btnApprove = null;
        JTextField txtNotes = null;
        for (Component c : findComponentsByName(content, "btnApproveTransaction")) {
            if (c instanceof JButton b) btnApprove = b;
        }
        for (Component c : findComponentsByName(content, "txtReviewNotes")) {
            if (c instanceof JTextField t) txtNotes = t;
        }

        assertNotNull(btnApprove, "Approve button must exist");
        assertNotNull(txtNotes, "Notes field must exist");

        txtNotes.setText("Approved after phone verification with customer");
        btnApprove.doClick();

        assertTrue(approveLatch.await(5, TimeUnit.SECONDS), "Approve API must be triggered by button click");
        assertEquals(888L, approvedTxId.get());
        assertEquals("Approved after phone verification with customer", approvedNotes.get());
    }

    private List<Component> findComponentsByName(Container root, String name) {
        List<Component> matches = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName())) {
                matches.add(c);
            }
            if (c instanceof Container childContainer) {
                matches.addAll(findComponentsByName(childContainer, name));
            }
        }
        return matches;
    }

}
