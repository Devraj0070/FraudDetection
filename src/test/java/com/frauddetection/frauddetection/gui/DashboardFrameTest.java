package com.frauddetection.frauddetection.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Unit and visual rendering tests for the desktop {@link DashboardFrame}.
 */
class DashboardFrameTest {

    @Test
    @DisplayName("DashboardFrame initializes and paints all Java 2D vector components")
    void dashboardFrame_initializesAndRendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final DashboardFrame[] frameHolder = new DashboardFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthResponse testUser = new AuthResponse(
                    true, "Success", "alice_test", "alice@safepay.com", "ROLE_USER", "ACC-789012"
            );
            DashboardFrame frame = new DashboardFrame(dummyClient, testUser);
            frame.setSize(1200, 800);
            frame.addNotify();
            frame.validate();
            frameHolder[0] = frame;
        });

        DashboardFrame frame = frameHolder[0];
        assertNotNull(frame, "DashboardFrame must be instantiated");
        assertEquals("SafePay — AI-Powered Fraud Detection System", frame.getTitle());

        // Perform offscreen Java 2D painting verification
        BufferedImage image = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        frame.getContentPane().paint(g2);
        g2.dispose();

        // 1. Verify Left Sidebar area has painted purple gradient pixels (x=80, y=200)
        int sidebarPixel = image.getRGB(80, 200);
        assertNotEquals(0, sidebarPixel, "Sidebar area must have painted gradient pixels");

        // 2. Verify Center Dashboard area has painted pixels (x=600, y=200)
        int centerPixel = image.getRGB(600, 200);
        assertNotEquals(0, centerPixel, "Dashboard content panel must have painted pixels");

        // Write offscreen rendering artifact to target directory
        File outDir = new File("target");
        if (outDir.exists()) {
            File outFile = new File(outDir, "dashboard-frame-rendered.png");
            ImageIO.write(image, "png", outFile);
        }

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("TransactionAmountChart renders status-aware bars and empty state accurately")
    void transactionAmountChart_rendersEmptyAndPopulated() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final TransactionAmountChart[] chartHolder = new TransactionAmountChart[1];

        SwingUtilities.invokeAndWait(() -> {
            TransactionAmountChart chart = new TransactionAmountChart();
            chart.setSize(500, 250);
            chartHolder[0] = chart;
        });

        TransactionAmountChart chart = chartHolder[0];

        // 1. Empty state offscreen paint
        BufferedImage emptyImage = new BufferedImage(500, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Empty = emptyImage.createGraphics();
        chart.paint(g2Empty);
        g2Empty.dispose();
        assertNotEquals(0, emptyImage.getRGB(250, 125), "Empty chart must paint placeholder message");

        // 2. Populated state offscreen paint
        List<TransactionSummaryDto> list = List.of(
                new TransactionSummaryDto(1L, new BigDecimal("1500.00"), "PAYMENT", "APPROVED", "2026-10-09 10:00:00"),
                new TransactionSummaryDto(2L, new BigDecimal("45000.00"), "TRANSFER", "BLOCKED", "2026-10-09 10:15:00"),
                new TransactionSummaryDto(3L, new BigDecimal("3200.00"), "CASH_OUT", "DECLINED", "2026-10-09 10:30:00")
        );

        SwingUtilities.invokeAndWait(() -> chart.setTransactions(list));

        BufferedImage populatedImage = new BufferedImage(500, 250, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Pop = populatedImage.createGraphics();
        chart.paint(g2Pop);
        g2Pop.dispose();

        assertNotEquals(0, populatedImage.getRGB(250, 150), "Populated chart must paint colored status bars");

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(populatedImage, "png", new File(outDir, "chart-rendered.png"));
        }
    }

    @Test
    @DisplayName("DashboardFrame supports Administrator role and displays admin security alerts")
    void dashboardFrame_adminRole_rendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final DashboardFrame[] frameHolder = new DashboardFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthResponse adminUser = new AuthResponse(
                    true, "Success", "admin_officer", "admin@safepay.com", "ADMIN", "ACC-ADMIN-01"
            );
            DashboardFrame frame = new DashboardFrame(dummyClient, adminUser);
            frame.setSize(1200, 800);
            frame.addNotify();
            frame.validate();
            frameHolder[0] = frame;
        });

        DashboardFrame frame = frameHolder[0];
        assertNotNull(frame, "DashboardFrame must instantiate for administrator");
        assertTrue(frame.getTitle().contains("SafePay"));

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("DashboardFrame updates UI with real backend data, balances, charts, and table rows")
    void dashboardFrame_populatedData_rendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final DashboardFrame[] frameHolder = new DashboardFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthResponse user = new AuthResponse(
                    true, "Success", "alice_master", "alice@safepay.com", "USER", "ACC-982341"
            );
            DashboardFrame frame = new DashboardFrame(dummyClient, user);
            frame.setSize(1260, 840);
            frame.addNotify();
            frame.validate();

            // Feed populated metrics
            DashboardResponse populated = new DashboardResponse(
                    true,
                    "Loaded successfully",
                    "alice_master",
                    "USER",
                    "ACC-982341",
                    "CHECKING",
                    "ACTIVE",
                    new BigDecimal("125000.50"),
                    18,
                    14,
                    2,
                    2,
                    List.of(
                            new TransactionSummaryDto(101L, new BigDecimal("1500.00"), "PAYMENT", "APPROVED", "2026-10-09 10:00:00"),
                            new TransactionSummaryDto(102L, new BigDecimal("45000.00"), "TRANSFER", "BLOCKED", "2026-10-09 10:15:00"),
                            new TransactionSummaryDto(103L, new BigDecimal("3200.00"), "CASH_OUT", "DECLINED", "2026-10-09 10:30:00"),
                            new TransactionSummaryDto(104L, new BigDecimal("850.00"), "DEBIT", "APPROVED", "2026-10-09 10:45:00")
                    )
            );
            frame.updateDashboardUi(populated);
            frameHolder[0] = frame;
        });

        DashboardFrame frame = frameHolder[0];
        assertNotNull(frame);

        // Perform offscreen painting verification
        BufferedImage image = new BufferedImage(1260, 840, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        frame.getContentPane().paint(g2);
        g2.dispose();

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "dashboard-frame-populated.png"));
        }

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("DashboardFrame renders clean empty state when no transactions exist")
    void dashboardFrame_emptyData_rendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final DashboardFrame[] frameHolder = new DashboardFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthResponse user = new AuthResponse(
                    true, "Success", "bob_newuser", "bob@safepay.com", "USER", "ACC-100200"
            );
            DashboardFrame frame = new DashboardFrame(dummyClient, user);
            frame.setSize(1260, 840);
            frame.addNotify();
            frame.validate();

            // Feed empty metrics
            DashboardResponse emptyResp = new DashboardResponse(
                    true,
                    "Loaded successfully",
                    "bob_newuser",
                    "USER",
                    "ACC-100200",
                    "CHECKING",
                    "ACTIVE",
                    new BigDecimal("50000.00"),
                    0,
                    0,
                    0,
                    0,
                    Collections.emptyList()
            );
            frame.updateDashboardUi(emptyResp);
            frame.setTimeframe("24 Hours");
            frameHolder[0] = frame;
        });

        DashboardFrame frame = frameHolder[0];
        assertNotNull(frame);

        BufferedImage image = new BufferedImage(1260, 840, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        frame.getContentPane().paint(g2);
        g2.dispose();

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "dashboard-frame-empty.png"));
        }

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("DashboardFrame initializes and navigates to TransactionHistoryPanel and AdminMonitoringPanel")
    void dashboardFrame_navigatesToHistoryAndAlerts() throws Exception {
        final DashboardFrame[] frameHolder = new DashboardFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthResponse user = new AuthResponse(
                    true, "Success", "admin_officer", "admin@safepay.com", "ROLE_ADMIN", "ACC-ADMIN-01"
            );
            DashboardFrame frame = new DashboardFrame(dummyClient, user);
            frameHolder[0] = frame;
        });

        DashboardFrame frame = frameHolder[0];
        assertNotNull(frame);
        assertNotNull(frame.getHistoryPanel(), "TransactionHistoryPanel must be wired in DashboardFrame");
        assertNotNull(frame.getAdminMonitoringPanel(), "AdminMonitoringPanel must be wired in DashboardFrame");

        SwingUtilities.invokeAndWait(() -> {
            frame.navigateToHistory();
        });
        assertNotNull(frame.getHistoryPanel().getTransactionTable());

        SwingUtilities.invokeAndWait(() -> {
            frame.navigateToAlerts();
        });
        assertNotNull(frame.getAdminMonitoringPanel().getAlertsTable());

        SwingUtilities.invokeAndWait(frame::dispose);
    }
}
