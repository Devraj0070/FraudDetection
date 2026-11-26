package com.frauddetection.frauddetection.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPanel;
import javax.swing.JLabel;
import com.frauddetection.frauddetection.client.ApiClientException;


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
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

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

    // =========================================================================
    // HEADLESS-COMPATIBLE MILESTONE 4 TESTS
    // =========================================================================

    @Test
    @DisplayName("DashboardPanel customer navigation shows only customer options (Headless)")
    void dashboardPanel_customerNavigation_visibility_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse customerUser = new AuthResponse(
                true, "Success", "customer1", "customer@safepay.com", "ROLE_USER", "ACC-1001"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, customerUser);

        assertFalse(panel.isAdmin(), "Must not be admin");
        assertNotNull(panel.getBtnNavDashboard(), "Dashboard nav button must be present");
        assertEquals("Dashboard", panel.getBtnNavDashboard().getText());

        assertNotNull(panel.getBtnNavTransaction(), "Transaction nav button must be present for customer");
        assertEquals("Payments & Transfers", panel.getBtnNavTransaction().getText());

        assertNotNull(panel.getBtnNavHistory(), "History nav button must be present");
        assertEquals("Transaction History", panel.getBtnNavHistory().getText());

        assertNotNull(panel.getBtnNavProfile(), "Profile nav button must be present");
        assertEquals("Profile & Security", panel.getBtnNavProfile().getText());

        assertNotNull(panel.getBtnNavSupport(), "Support nav button must be present");
        assertEquals("Help & Support", panel.getBtnNavSupport().getText());

        assertNotNull(panel.getBtnLogout(), "Logout button must be present");

        // Admin-only buttons must not be visible or added for customer
        assertNull(panel.getBtnNavAlerts(), "Alerts button must not exist for customer");
        assertNull(panel.getBtnNavPendingReviews(), "Pending Reviews button must not exist for customer");

        // Customer cannot access alerts screen directly
        panel.navigateToAlerts();
        assertEquals(DashboardPanel.CARD_DASHBOARD, panel.getCurrentCard(), "Customer must not switch to alerts card");
        assertTrue(panel.getStatusMessage().contains("Access Denied"));
    }

    @Test
    @DisplayName("DashboardPanel administrator navigation shows admin-only options (Headless)")
    void dashboardPanel_adminNavigation_visibility_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse adminUser = new AuthResponse(
                true, "Success", "admin1", "admin@safepay.com", "ROLE_ADMIN", "ACC-ADMIN-01"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, adminUser);

        assertTrue(panel.isAdmin(), "Must be admin");
        assertNotNull(panel.getBtnNavDashboard());
        assertEquals("Admin Dashboard", panel.getBtnNavDashboard().getText());

        assertNotNull(panel.getBtnNavHistory());
        assertEquals("Transaction Monitoring", panel.getBtnNavHistory().getText());

        assertNotNull(panel.getBtnNavAlerts(), "Fraud Alerts button must be present for admin");
        assertEquals("Fraud Alerts", panel.getBtnNavAlerts().getText());

        assertNotNull(panel.getBtnNavPendingReviews(), "Pending Reviews button must be present for admin");
        assertEquals("Pending Reviews", panel.getBtnNavPendingReviews().getText());

        assertNotNull(panel.getBtnNavProfile());
        assertEquals("Admin Profile", panel.getBtnNavProfile().getText());

        assertNotNull(panel.getBtnNavSupport());
        assertEquals("Help & Support", panel.getBtnNavSupport().getText());

        assertNull(panel.getBtnNavTransaction(), "Customer transfer button must not be in admin sidebar");

        // Admin can navigate directly to alerts and pending reviews
        panel.navigateToAlerts();
        assertEquals(DashboardPanel.CARD_ALERTS, panel.getCurrentCard(), "Admin must switch to alerts card");
        assertEquals("Fraud Surveillance Alerts", panel.getHeaderTitleText());

        panel.navigateToPendingReviews();
        assertEquals(DashboardPanel.CARD_ALERTS, panel.getCurrentCard(), "Admin must switch to pending reviews card");
        assertEquals("Pending Fraud Reviews", panel.getHeaderTitleText());
    }

    @Test
    @DisplayName("DashboardPanel switches panels correctly and updates header title (Headless)")
    void dashboardPanel_panelSwitching_updatesStateAndHeader_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse user = new AuthResponse(
                true, "Success", "customer2", "customer2@safepay.com", "ROLE_USER", "ACC-1002"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, user);

        assertEquals(DashboardPanel.CARD_DASHBOARD, panel.getCurrentCard());

        panel.navigateToTransaction();
        assertEquals(DashboardPanel.CARD_TRANSACTION, panel.getCurrentCard());
        assertEquals("Payments & Transfers", panel.getHeaderTitleText());

        panel.navigateToHistory();
        assertEquals(DashboardPanel.CARD_HISTORY, panel.getCurrentCard());
        assertEquals("Transaction History", panel.getHeaderTitleText());

        panel.navigateToProfile();
        assertEquals(DashboardPanel.CARD_PROFILE, panel.getCurrentCard());
        assertEquals("Profile & Security", panel.getHeaderTitleText());

        panel.navigateToDashboard();
        assertEquals(DashboardPanel.CARD_DASHBOARD, panel.getCurrentCard());
        assertEquals("Dashboard", panel.getHeaderTitleText());
    }

    @Test
    @DisplayName("DashboardPanel reuses panel instances and prevents duplicate allocation (Headless)")
    void dashboardPanel_noDuplicatePanelCreation_preservesInstances_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse adminUser = new AuthResponse(
                true, "Success", "admin2", "admin2@safepay.com", "ROLE_ADMIN", "ACC-ADMIN-02"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, adminUser);

        TransactionPanel txPanelInitial = panel.getTransactionPanel();
        TransactionHistoryPanel historyPanelInitial = panel.getHistoryPanel();
        AdminMonitoringPanel adminPanelInitial = panel.getAdminMonitoringPanel();

        assertNotNull(txPanelInitial);
        assertNotNull(historyPanelInitial);
        assertNotNull(adminPanelInitial);

        // Perform repeated navigation between panels
        for (int i = 0; i < 5; i++) {
            panel.navigateToHistory();
            panel.navigateToAlerts();
            panel.navigateToPendingReviews();
            panel.navigateToDashboard();
        }

        assertSame(txPanelInitial, panel.getTransactionPanel(), "TransactionPanel instance must be preserved");
        assertSame(historyPanelInitial, panel.getHistoryPanel(), "TransactionHistoryPanel instance must be preserved");
        assertSame(adminPanelInitial, panel.getAdminMonitoringPanel(), "AdminMonitoringPanel instance must be preserved");
        assertEquals(5, panel.getCardsPanel().getComponentCount(), "CardsPanel must contain exactly 5 pre-allocated cards");
    }

    @Test
    @DisplayName("DashboardPanel pending-review count updates nav badge and metrics (Headless)")
    void dashboardPanel_pendingReviewCount_updatesBadgeAndLabel_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse adminUser = new AuthResponse(
                true, "Success", "admin3", "admin3@safepay.com", "ROLE_ADMIN", "ACC-ADMIN-03"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, adminUser);

        panel.setPendingReviewCount(4);
        assertEquals(4, panel.getPendingReviewCount());
        assertEquals("04", panel.getBtnNavPendingReviews().getBadge());

        panel.setPendingReviewCount(0);
        assertEquals(0, panel.getPendingReviewCount());
        assertNull(panel.getBtnNavPendingReviews().getBadge());
    }

    @Test
    @DisplayName("DashboardPanel logout clears session and triggers onLogout callback (Headless)")
    void dashboardPanel_logoutAndSessionCleanup_headless() throws Exception {
        CountDownLatch logoutLatch = new CountDownLatch(1);
        CountDownLatch callbackLatch = new CountDownLatch(1);

        SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public AuthResponse logout() {
                logoutLatch.countDown();
                return new AuthResponse(true, "Logged out", null, null, null, null);
            }
        };

        AuthResponse user = new AuthResponse(
                true, "Success", "alice_logout", "alice@safepay.com", "ROLE_USER", "ACC-LOGOUT"
        );

        DashboardPanel panel = new DashboardPanel(client, user, callbackLatch::countDown);
        panel.setAutoConfirmLogoutForTesting(true);

        panel.performLogout();

        assertTrue(logoutLatch.await(5, TimeUnit.SECONDS), "API client logout must be invoked");
        assertTrue(callbackLatch.await(5, TimeUnit.SECONDS), "onLogout callback must be triggered");
    }

    @Test
    @DisplayName("DashboardPanel notification and support content render safely without external contacts (Headless)")
    void dashboardPanel_notificationAndSupportActions_headless() {
        SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
        AuthResponse adminUser = new AuthResponse(
                true, "Success", "admin_notify", "admin@safepay.com", "ROLE_ADMIN", "ACC-ADMIN-99"
        );
        DashboardPanel panel = new DashboardPanel(dummyClient, adminUser);
        panel.setPendingReviewCount(3);

        // 1. Verify Notifications content
        JPanel notifPanel = panel.buildNotificationsContent(null);
        assertNotNull(notifPanel);
        StringBuilder notifText = new StringBuilder();
        extractText(notifPanel, notifText);
        assertTrue(notifText.toString().contains("3 transactions awaiting manual fraud review"));

        // 2. Verify Support content
        JPanel supportPanel = panel.buildSupportContent(null);
        assertNotNull(supportPanel);
        StringBuilder supportText = new StringBuilder();
        extractText(supportPanel, supportText);
        String s = supportText.toString();
        assertTrue(s.contains("SafePay Help & Support Center"));
        assertTrue(s.contains("Real-Time Fraud Prevention"));
        assertTrue(s.contains("Pending Fraud Review"));
        assertTrue(s.contains("Account & Transfer Safety"));
        // Confirm no fake external numbers or addresses
        assertFalse(s.contains("1-800"));
        assertFalse(s.contains("@external.com"));
        assertFalse(s.contains("www.fakebank.com"));
    }

    @Test
    @DisplayName("DashboardPanel 401 response flags expired session and triggers cleanup (Headless)")
    void dashboardPanel_expiredSessionHandling_on401_headless() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        CountDownLatch expiredCallbackLatch = new CountDownLatch(1);

        SafePayApiClient expiredClient = new SafePayApiClient("http://127.0.0.1:8080") {
            @Override
            public DashboardResponse getDashboardData() throws ApiClientException {
                errorLatch.countDown();
                throw new ApiClientException(401, "Full authentication is required to access this resource");
            }

            @Override
            public AuthResponse logout() {
                return new AuthResponse(true, "Logged out", null, null, null, null);
            }
        };

        AuthResponse user = new AuthResponse(
                true, "Success", "bob_expired", "bob@safepay.com", "ROLE_USER", "ACC-EXPIRED"
        );

        DashboardPanel panel = new DashboardPanel(expiredClient, user);
        panel.setOnSessionExpired(expiredCallbackLatch::countDown);

        panel.refreshDashboardData();

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "getDashboardData must be called");
        assertTrue(expiredCallbackLatch.await(5, TimeUnit.SECONDS), "onSessionExpired callback must be invoked");

        assertTrue(panel.isSessionExpired(), "Session must be marked expired");
        assertTrue(panel.getStatusMessage().contains("Session expired"));
    }

    @Test
    @DisplayName("DashboardFrame static helpers and constants remain functional")
    void dashboardFrame_staticHelpersAndConstants() {
        assertEquals("₹ 1,250.50", DashboardFrame.formatCurrency(new BigDecimal("1250.50")));
        assertEquals("₹ 0.00", DashboardFrame.formatCurrency(null));
        assertNotNull(DashboardFrame.createStyledCardPanel());
        assertNotNull(DashboardFrame.createGradientPillButton("Action", 100, 30));
        assertEquals(DashboardPanel.CARD_DASHBOARD, DashboardFrame.CARD_DASHBOARD);
        assertEquals(DashboardPanel.CARD_TRANSACTION, DashboardFrame.CARD_TRANSACTION);
        assertEquals(DashboardPanel.CARD_HISTORY, DashboardFrame.CARD_HISTORY);
        assertEquals(DashboardPanel.CARD_ALERTS, DashboardFrame.CARD_ALERTS);
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
