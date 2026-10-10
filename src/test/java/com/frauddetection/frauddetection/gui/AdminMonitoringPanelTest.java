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
import com.frauddetection.frauddetection.dto.AdminAlertDto;

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
}
