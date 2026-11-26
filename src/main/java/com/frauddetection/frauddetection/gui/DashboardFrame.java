package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;

/**
 * Top-level Desktop Window Frame for SafePay — AI-Powered Fraud Detection System.
 * Hosts {@link DashboardPanel} inside a 1260x840 native desktop container.
 *
 * Implements the SafePay Obsidian Design System:
 * - Canvas: Deep Carbon Obsidian (#080808)
 * - Elevated Surface: #111111
 * - Cards: #161616 with subtle #242424 borders
 * - Accent: Electric Neon Lime (#D8FF00)
 * - Pure White and Muted Typography
 */
public class DashboardFrame extends JFrame {

    // SafePay Obsidian & Electric Neon Lime Design Tokens (Matching Website)
    public static final Color COLOR_OBSIDIAN_BG = new Color(8, 8, 8);         // #080808
    public static final Color COLOR_SURFACE_DARK = new Color(17, 17, 17);      // #111111
    public static final Color COLOR_CARD_SURFACE = new Color(22, 22, 22);      // #161616
    public static final Color COLOR_CARD_BORDER = new Color(36, 36, 36);       // #242424
    public static final Color COLOR_LIME_PRIMARY = new Color(216, 255, 0);     // #D8FF00
    public static final Color COLOR_LIME_HOVER = new Color(194, 230, 0);       // #C2E600
    public static final Color COLOR_TEXT_WHITE = Color.WHITE;
    public static final Color COLOR_TEXT_MUTED = new Color(156, 163, 175);     // #9CA3AF

    // Compatibility Token Aliases
    public static final Color COLOR_CANVAS_START = COLOR_OBSIDIAN_BG;
    public static final Color COLOR_CANVAS_END = COLOR_SURFACE_DARK;
    public static final Color COLOR_PURPLE_PRIMARY = COLOR_LIME_PRIMARY;
    public static final Color COLOR_PURPLE_PILL = new Color(216, 255, 0, 35);
    public static final Color COLOR_PURPLE_DARK = new Color(12, 12, 12);
    public static final Color COLOR_PURPLE_ACCENT = COLOR_LIME_PRIMARY;
    public static final Color COLOR_MAGENTA = COLOR_LIME_HOVER;

    public static final Color COLOR_WORKSPACE_BG = COLOR_SURFACE_DARK;
    public static final Color COLOR_HERO_BG = COLOR_CARD_SURFACE;
    public static final Color COLOR_CARD_WHITE = COLOR_CARD_SURFACE;
    public static final Color COLOR_CARD_OUTLINE = COLOR_CARD_BORDER;
    public static final Color COLOR_BORDER_SUBTLE = COLOR_CARD_BORDER;

    public static final Color COLOR_TEXT_MAIN = COLOR_TEXT_WHITE;
    public static final Color COLOR_TEXT_SUBTLE = COLOR_TEXT_MUTED;

    public static final Color COLOR_STATUS_APPROVED = new Color(16, 185, 129);   // #10B981 (Green)
    public static final Color COLOR_STATUS_BLOCKED = new Color(239, 68, 68);     // #EF4444 (Red)
    public static final Color COLOR_STATUS_DECLINED = new Color(245, 158, 11);   // #F59E0B (Amber)

    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_TRANSACTION = "CARD_TRANSACTION";
    public static final String CARD_HISTORY = "CARD_HISTORY";
    public static final String CARD_ALERTS = "CARD_ALERTS";
    public static final String CARD_PROFILE = "CARD_PROFILE";

    private final DashboardPanel dashboardPanel;

    public DashboardFrame(SafePayApiClient apiClient, AuthResponse currentUser) {
        super("SafePay — AI-Powered Fraud Detection System");

        this.dashboardPanel = new DashboardPanel(apiClient, currentUser, () -> {
            dispose();
            SwingUtilities.invokeLater(() -> {
                AuthFrame authFrame = new AuthFrame(apiClient, auth -> {
                    DashboardFrame df = new DashboardFrame(apiClient, auth);
                    df.setVisible(true);
                });
                authFrame.setVisible(true);
            });
        });

        setContentPane(dashboardPanel);
        setSize(1260, 840);
        setMinimumSize(new Dimension(1120, 740));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public DashboardFrame() {
        this(new SafePayApiClient(), null);
    }

    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }

    public boolean isAdmin() {
        return dashboardPanel.isAdmin();
    }

    public TransactionHistoryPanel getHistoryPanel() {
        return dashboardPanel.getHistoryPanel();
    }

    public AdminMonitoringPanel getAdminMonitoringPanel() {
        return dashboardPanel.getAdminMonitoringPanel();
    }

    public TransactionPanel getTransactionPanel() {
        return dashboardPanel.getTransactionPanel();
    }

    public void navigateToHistory() {
        dashboardPanel.navigateToHistory();
    }

    public void navigateToAlerts() {
        dashboardPanel.navigateToAlerts();
    }

    public void navigateToPendingReviews() {
        dashboardPanel.navigateToPendingReviews();
    }

    public void navigateToTransaction() {
        dashboardPanel.navigateToTransaction();
    }

    public void navigateToDashboard() {
        dashboardPanel.navigateToDashboard();
    }

    public void refreshDashboardData() {
        dashboardPanel.refreshDashboardData();
    }

    public void updateDashboardUi(DashboardResponse data) {
        dashboardPanel.updateDashboardUi(data);
    }

    public void setTimeframe(String timeframe) {
        dashboardPanel.setTimeframe(timeframe);
    }

    public void setStatusMessage(String message) {
        dashboardPanel.setStatusMessage(message);
    }

    public void setDatabaseStatus(boolean connected, String detail) {
        dashboardPanel.setDatabaseStatus(connected, detail);
    }

    public void performLogout() {
        dashboardPanel.performLogout();
    }

    // =========================================================================
    // STATIC UI UTILITY METHODS
    // =========================================================================

    public static JPanel createStyledCardPanel() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(COLOR_CARD_SURFACE);
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                g2.setColor(COLOR_CARD_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        return card;
    }

    public static JButton createGradientPillButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getModel().isRollover() ? COLOR_LIME_HOVER : COLOR_LIME_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, getHeight() - 2, getHeight() - 2);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.BLACK);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹ 0.00";
        return String.format(Locale.US, "₹ %,.2f", amount);
    }
}
