package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Native Java Swing & Java 2D desktop dashboard for SafePay — AI-Powered Fraud Detection System.
 *
 * Pixel-accurate reproduction of the Finora dashboard specification (Reference A):
 * - Ambient purple/lavender outer canvas gradient (#7C3AED to #C4B5FD) with soft fluid curves
 * - Large floating white rounded workspace container (radius 26px, soft shadow)
 * - Integrated left navigation sidebar:
 *     - Folded ribbon brand mark + "SafePay" typography
 *     - Navigation items: Dashboard (active lavender pill #F3E8FF, purple text/icon),
 *       Transaction, Invoices, Message (pill badge "05"), My Wallets, Analytics
 *     - Bottom section: Settings, black rounded capsule Light/Dark mode switcher pill,
 *       and Log Out
 * - Compact top header: "Dashboard" title, subtle refresh action, notification bell
 *   with badge "2", and user profile chip with avatar and dropdown chevron
 * - Main content area (Two-Column Layout, seamlessly fitted with NO outer scrollbar):
 *     - Middle Column:
 *         - Hero banner card: 2-line title "Convert Money Instantly\nwith SafePay",
 *           subtitle "SafePay makes money exchange easy.", purple pill button "Convert Now →",
 *           and vector character carrying payment card towards smartphone mockup with circular glow
 *         - Activity Trend card: "Always apply the true rate", "Mid market rate ℹ",
 *           timeframe filter pills (active solid dark capsule #18181B), Catmull-Rom spline wave
 *           curve with lavender area glow, glowing peak halo, and floating dark tooltip card
 *         - Recent Transactions table: "Always apply the true rate", "View All →",
 *           custom brand icons (Amazon, Figma, Spotify, SafePay), types, amounts, dates
 *     - Right Column:
 *         - Vivid purple credit balance card: #6D28D9 to #8B5CF6 gradient, gold EMV chip,
 *           large balance amount, status indicator, and SafePay logo
 *         - "Upcoming Payments" summary card: Behance Pro, UpWork Pro
 *         - "My Payments" recent activity card: Tabs "All Payments" | "Regular Payments",
 *           Spotify, Dribbble, LinkedIn, Youtube
 * - Strictly native Java Swing and Java 2D vector graphics.
 */
public class DashboardFrame extends BaseAppFrame {

    // Finora / SafePay Design Tokens
    public static final Color COLOR_CANVAS_START = new Color(124, 58, 237);     // #7C3AED (Vibrant Purple)
    public static final Color COLOR_CANVAS_END = new Color(196, 181, 253);       // #C4B5FD (Soft Lavender)
    public static final Color COLOR_PURPLE_PRIMARY = new Color(124, 58, 237);   // #7C3AED
    public static final Color COLOR_PURPLE_PILL = new Color(243, 232, 255);      // #F3E8FF (Active Nav Pill)
    public static final Color COLOR_PURPLE_DARK = new Color(59, 7, 100);        // #3B0764
    public static final Color COLOR_PURPLE_ACCENT = new Color(124, 58, 237);    // #7C3AED
    public static final Color COLOR_MAGENTA = new Color(192, 38, 211);           // #C026D3

    public static final Color COLOR_WORKSPACE_BG = Color.WHITE;
    public static final Color COLOR_HERO_BG = new Color(248, 247, 252);         // #F8F7FC
    public static final Color COLOR_CARD_WHITE = Color.WHITE;
    public static final Color COLOR_CARD_OUTLINE = new Color(241, 245, 249);    // #F1F5F9
    public static final Color COLOR_BORDER_SUBTLE = new Color(226, 232, 240);   // #E2E8F0

    public static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);          // #0F172A (Deep Slate)
    public static final Color COLOR_TEXT_SUBTLE = new Color(100, 116, 139);     // #64748B (Muted Slate)

    public static final Color COLOR_STATUS_APPROVED = new Color(22, 163, 74);   // #16A34A (Green)
    public static final Color COLOR_STATUS_BLOCKED = new Color(220, 38, 38);    // #DC2626 (Red)
    public static final Color COLOR_STATUS_DECLINED = new Color(217, 119, 6);   // #D97706 (Amber)

    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_TRANSACTION = "CARD_TRANSACTION";
    public static final String CARD_HISTORY = "CARD_HISTORY";
    public static final String CARD_ALERTS = "CARD_ALERTS";
    public static final String CARD_PROFILE = "CARD_PROFILE";

    private final SafePayApiClient apiClient;
    private final AuthResponse currentUser;

    private final CardLayout mainCardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(mainCardLayout);

    // Sidebar navigation buttons
    private final List<FinoraNavButton> navButtons = new ArrayList<>();
    private FinoraNavButton btnNavDashboard;
    private FinoraNavButton btnNavTransaction;
    private FinoraNavButton btnNavHistory;
    private FinoraNavButton btnNavAlerts;
    private FinoraNavButton btnNavProfile;
    private FinoraNavButton btnNavSettings;

    // Header Components
    private JLabel lblHeaderTitle;
    private JLabel lblUserInitialBadge;
    private JLabel lblUserNameTag;
    private JButton btnRefresh;

    // Dashboard Dynamic Widgets
    private JLabel lblBalanceAmount;
    private JLabel lblAccountStatusPill;
    private TransactionAmountChart transactionChart;
    private JTable recentTransactionsTable;
    private RecentTransactionsTableModel tableModel;
    private JPanel quickPaymentsListPanel;
    private TransactionPanel transactionPanel;
    private TransactionHistoryPanel historyPanel;
    private AdminMonitoringPanel adminMonitoringPanel;

    // Metric Summary Labels
    private final JLabel lblMetricTotalVal = new JLabel("0");
    private final JLabel lblMetricApprovedVal = new JLabel("0");
    private final JLabel lblMetricDeclinedVal = new JLabel("0");
    private final JLabel lblMetricBlockedVal = new JLabel("0");

    // Timeframe filter state
    private String selectedTimeframe = "1 Month";
    private final List<TimeframePill> timeframePills = new ArrayList<>();
    private DashboardResponse currentDashboardData;

    public DashboardFrame(SafePayApiClient apiClient, AuthResponse currentUser) {
        super("SafePay — AI-Powered Fraud Detection System");
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.currentUser = currentUser;

        setSize(1260, 840);
        setMinimumSize(new Dimension(1120, 740));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        refreshDashboardData();
    }

    public DashboardFrame() {
        this(new SafePayApiClient(), null);
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.role() != null
                && (currentUser.role().contains("ADMIN") || "ROLE_ADMIN".equalsIgnoreCase(currentUser.role()));
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Ambient Outer Canvas (Lavender/Purple gradient with organic curves)
        JPanel outerCanvas = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 1. Purple Canvas Gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, COLOR_CANVAS_START,
                        w, h, COLOR_CANVAS_END
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // 2. Soft Ambient Waves / Curves in Canvas Background
                g2.setColor(new Color(255, 255, 255, 24));
                GeneralPath wave1 = new GeneralPath();
                wave1.moveTo(0, h * 0.2);
                wave1.quadTo(w * 0.4, h * 0.05, w, h * 0.35);
                wave1.lineTo(w, 0);
                wave1.lineTo(0, 0);
                wave1.closePath();
                g2.fill(wave1);

                GeneralPath wave2 = new GeneralPath();
                wave2.moveTo(0, h * 0.85);
                wave2.quadTo(w * 0.5, h * 0.65, w, h * 0.9);
                wave2.lineTo(w, h);
                wave2.lineTo(0, h);
                wave2.closePath();
                g2.fill(wave2);

                g2.dispose();
            }
        };
        outerCanvas.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Floating White Workspace Container (26px corner radius matching Reference A)
        JPanel workspaceContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Soft drop shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(2, 6, w - 4, h - 8, 26, 26);

                // Pure white body
                g2.setColor(COLOR_WORKSPACE_BG);
                g2.fillRoundRect(0, 0, w, h, 26, 26);
                g2.dispose();
            }
        };
        workspaceContainer.setOpaque(false);

        // Assemble Workspace: Sidebar (West) + Center Area (North Header + CardsPanel)
        workspaceContainer.add(createSidebar(), BorderLayout.WEST);

        JPanel mainWorkspace = new JPanel(new BorderLayout());
        mainWorkspace.setOpaque(false);
        mainWorkspace.add(createHeaderPanel(), BorderLayout.NORTH);

        // Center Content Stack
        cardsPanel.setOpaque(false);
        cardsPanel.add(createDashboardScreen(), CARD_DASHBOARD);
        transactionPanel = new TransactionPanel(apiClient, this::refreshDashboardData, () -> switchScreen(CARD_DASHBOARD, "Dashboard", btnNavDashboard));
        cardsPanel.add(transactionPanel, CARD_TRANSACTION);
        historyPanel = new TransactionHistoryPanel(apiClient, () -> switchScreen(CARD_DASHBOARD, "Dashboard", btnNavDashboard));
        cardsPanel.add(historyPanel, CARD_HISTORY);
        adminMonitoringPanel = new AdminMonitoringPanel(apiClient, () -> switchScreen(CARD_DASHBOARD, "Dashboard", btnNavDashboard));
        cardsPanel.add(adminMonitoringPanel, CARD_ALERTS);
        cardsPanel.add(createProfileScreen(), CARD_PROFILE);

        mainWorkspace.add(cardsPanel, BorderLayout.CENTER);
        workspaceContainer.add(mainWorkspace, BorderLayout.CENTER);

        outerCanvas.add(workspaceContainer, BorderLayout.CENTER);
        add(outerCanvas, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setStatusMessage("SafePay Dashboard Ready • Connected to " + apiClient.getBaseUrl());
        setDatabaseStatus(true, "REST Session Active");
    }

    // =========================================================================
    // SIDEBAR NAVIGATION (Finora Specification)
    // =========================================================================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(205, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 12));

        // 1. Top Logo: Finora wave mark + "SafePay"
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        logoRow.add(new FoldedRibbonLogoBadge(28, 24));

        JLabel lblBrand = new JLabel("SafePay");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBrand.setForeground(COLOR_TEXT_MAIN);
        logoRow.add(lblBrand);

        sidebar.add(logoRow, BorderLayout.NORTH);

        // 2. Navigation Items Stack
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);

        btnNavDashboard = new FinoraNavButton("Dashboard", "DASHBOARD", true, null);
        btnNavTransaction = new FinoraNavButton("Transaction", "TRANSACTION", false, null);
        btnNavHistory = new FinoraNavButton("Invoices", "INVOICES", false, null);
        btnNavAlerts = new FinoraNavButton("Message", "MESSAGE", false, "05");
        btnNavProfile = new FinoraNavButton("My Wallets", "WALLETS", false, null);
        btnNavSettings = new FinoraNavButton("Analytics", "ANALYTICS", false, null);

        navButtons.add(btnNavDashboard);
        navButtons.add(btnNavTransaction);
        navButtons.add(btnNavHistory);
        navButtons.add(btnNavAlerts);
        navButtons.add(btnNavProfile);
        navButtons.add(btnNavSettings);

        btnNavDashboard.addActionListener(e -> switchScreen(CARD_DASHBOARD, "Dashboard", btnNavDashboard));
        btnNavTransaction.addActionListener(e -> switchScreen(CARD_TRANSACTION, "Transaction", btnNavTransaction));
        btnNavHistory.addActionListener(e -> switchScreen(CARD_HISTORY, "Invoices", btnNavHistory));
        btnNavAlerts.addActionListener(e -> switchScreen(CARD_ALERTS, "Message & Alerts", btnNavAlerts));
        btnNavProfile.addActionListener(e -> switchScreen(CARD_PROFILE, "My Wallets", btnNavProfile));
        btnNavSettings.addActionListener(e -> switchScreen(CARD_PROFILE, "Analytics", btnNavSettings));

        for (FinoraNavButton btn : navButtons) {
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(4));
        }

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // 3. Bottom Controls: Settings + Light/Dark Switcher + Logout
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        JButton btnSettings = createGhostSidebarButton("Settings", "GEAR");
        btnSettings.addActionListener(e -> switchScreen(CARD_PROFILE, "Settings", btnNavSettings));
        bottomPanel.add(btnSettings);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Light / Dark Switcher Pill (Reference A exact dark pill)
        bottomPanel.add(createLightDarkTogglePill());
        bottomPanel.add(Box.createVerticalStrut(10));

        JButton btnLogout = createGhostSidebarButton("Log Out", "LOGOUT");
        btnLogout.addActionListener(e -> performLogout());
        bottomPanel.add(btnLogout);

        sidebar.add(bottomPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel createLightDarkTogglePill() {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Exact dark background container matching Reference A
                g2.setColor(new Color(24, 24, 27));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        pill.setMaximumSize(new Dimension(175, 34));
        pill.setPreferredSize(new Dimension(175, 34));

        // "Light" selected capsule (dark/white active capsule in Reference A)
        JLabel lblLight = new JLabel("☀ Light", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(39, 39, 42));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblLight.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblLight.setForeground(Color.WHITE);
        lblLight.setPreferredSize(new Dimension(75, 24));
        lblLight.setOpaque(false);

        JLabel lblDark = new JLabel("☾ Dark", SwingConstants.CENTER);
        lblDark.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblDark.setForeground(new Color(161, 161, 170));
        lblDark.setPreferredSize(new Dimension(75, 24));

        pill.add(lblLight);
        pill.add(lblDark);
        return pill;
    }

    private JButton createGhostSidebarButton(String text, String iconType) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();

                g2.setColor(COLOR_TEXT_SUBTLE);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if ("GEAR".equals(iconType)) {
                    g2.drawOval(10, h / 2 - 5, 10, 10);
                    g2.fillOval(13, h / 2 - 2, 4, 4);
                } else if ("LOGOUT".equals(iconType)) {
                    g2.drawRoundRect(8, h / 2 - 6, 8, 12, 3, 3);
                    g2.drawLine(12, h / 2, 18, h / 2);
                    g2.drawLine(16, h / 2 - 3, 18, h / 2);
                    g2.drawLine(16, h / 2 + 3, 18, h / 2);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(COLOR_TEXT_SUBTLE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 28, 6, 8));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(175, 28));
        return btn;
    }

    private void switchScreen(String cardName, String title, FinoraNavButton activeBtn) {
        mainCardLayout.show(cardsPanel, cardName);
        lblHeaderTitle.setText(title);

        for (FinoraNavButton btn : navButtons) {
            btn.setActive(btn == activeBtn);
        }

        if (CARD_HISTORY.equals(cardName) && historyPanel != null) {
            historyPanel.loadTransactionHistory();
        } else if (CARD_ALERTS.equals(cardName) && adminMonitoringPanel != null) {
            adminMonitoringPanel.loadAlerts();
        }
    }

    public TransactionHistoryPanel getHistoryPanel() {
        return historyPanel;
    }

    public AdminMonitoringPanel getAdminMonitoringPanel() {
        return adminMonitoringPanel;
    }

    public void navigateToHistory() {
        switchScreen(CARD_HISTORY, "Transaction History", btnNavHistory);
    }

    public void navigateToAlerts() {
        switchScreen(CARD_ALERTS, "Message & Alerts", btnNavAlerts);
    }

    // =========================================================================
    // HEADER (Finora Specification)
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 24));

        lblHeaderTitle = new JLabel("Dashboard");
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblHeaderTitle.setForeground(COLOR_TEXT_MAIN);
        header.add(lblHeaderTitle, BorderLayout.WEST);

        // Right cluster: Refresh button, Bell notification with badge, Profile avatar chip
        JPanel rightCluster = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightCluster.setOpaque(false);

        // Subtle Refresh Button
        btnRefresh = new JButton("↻") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setForeground(COLOR_TEXT_SUBTLE);
        btnRefresh.setPreferredSize(new Dimension(32, 32));
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setToolTipText("Refresh real-time data");
        btnRefresh.addActionListener(e -> refreshDashboardData());
        rightCluster.add(btnRefresh);

        // Notification Bell Icon (Clean vector bell)
        JButton btnBell = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Bell outline
                g2.setColor(COLOR_TEXT_SUBTLE);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawArc(w / 2 - 6, h / 2 - 7, 12, 12, 0, 180);
                g2.drawLine(w / 2 - 8, h / 2 - 1, w / 2 + 8, h / 2 - 1);
                g2.drawOval(w / 2 - 2, h / 2 + 1, 4, 3);

                g2.dispose();
            }
        };
        btnBell.setPreferredSize(new Dimension(32, 32));
        btnBell.setOpaque(false);
        btnBell.setContentAreaFilled(false);
        btnBell.setBorderPainted(false);
        btnBell.setFocusPainted(false);
        btnBell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBell.setToolTipText("Notifications");
        rightCluster.add(btnBell);

        // User Profile Chip (Avatar + Name + Chevron matching Reference A)
        JPanel profileChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        profileChip.setOpaque(false);
        profileChip.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 8));
        profileChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String initial = (currentUser != null && currentUser.username() != null && !currentUser.username().isEmpty())
                ? currentUser.username().substring(0, 1).toUpperCase()
                : "S";

        lblUserInitialBadge = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblUserInitialBadge.setPreferredSize(new Dimension(24, 24));
        lblUserInitialBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblUserInitialBadge.setForeground(Color.WHITE);
        lblUserInitialBadge.setOpaque(false);
        profileChip.add(lblUserInitialBadge);

        String displayName = (currentUser != null && currentUser.username() != null)
                ? currentUser.username()
                : "Sarah Rahman";
        lblUserNameTag = new JLabel(displayName);
        lblUserNameTag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUserNameTag.setForeground(COLOR_TEXT_MAIN);
        profileChip.add(lblUserNameTag);

        JLabel lblChevron = new JLabel("v");
        lblChevron.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblChevron.setForeground(COLOR_TEXT_SUBTLE);
        profileChip.add(lblChevron);

        JPopupMenu profileMenu = new JPopupMenu();
        JMenuItem itemProfile = new JMenuItem("Account Profile");
        itemProfile.addActionListener(e -> switchScreen(CARD_PROFILE, "Account Profile", btnNavProfile));
        JMenuItem itemLogout = new JMenuItem("Log Out");
        itemLogout.addActionListener(e -> performLogout());
        profileMenu.add(itemProfile);
        profileMenu.addSeparator();
        profileMenu.add(itemLogout);

        profileChip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                profileMenu.show(profileChip, 0, profileChip.getHeight() + 4);
            }
        });
        rightCluster.add(profileChip);

        header.add(rightCluster, BorderLayout.EAST);
        return header;
    }

    // =========================================================================
    // MAIN DASHBOARD SCREEN (Finora Two-Column Layout)
    // =========================================================================

    private JScrollPane createDashboardScreen() {
        ScrollableContentPanel content = new ScrollableContentPanel();
        content.setLayout(new BorderLayout(14, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(0, 18, 14, 18));

        // LEFT COLUMN (~62% width): Hero Promo Card + Trend Chart + Recent Transactions
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);

        leftColumn.add(createHeroBannerCard());
        leftColumn.add(Box.createVerticalStrut(10));
        leftColumn.add(createActivityChartCard());
        leftColumn.add(Box.createVerticalStrut(10));
        leftColumn.add(createRecentTransactionsCard());

        content.add(leftColumn, BorderLayout.CENTER);

        // RIGHT COLUMN (~38% width): Balance Card + Upcoming Payments + My Payments
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(305, 0));

        rightColumn.add(createPurpleBalanceCard());
        rightColumn.add(Box.createVerticalStrut(10));
        rightColumn.add(createUpcomingPaymentsCard());
        rightColumn.add(Box.createVerticalStrut(10));
        rightColumn.add(createMyPaymentsCard());

        content.add(rightColumn, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    // =========================================================================
    // LEFT COLUMN: HERO, CHART, TRANSACTIONS
    // =========================================================================

    /**
     * Finora Hero Promo Card:
     * - Headline 2 lines: "Convert Money Instantly\nwith SafePay"
     * - Subtitle: "SafePay makes money exchange easy."
     * - Button: "Convert Now →"
     * - Right vector illustration: Smartphone + floating purple card + stylized character walking
     */
    private JPanel createHeroBannerCard() {
        JPanel hero = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Soft lavender background container
                g2.setColor(COLOR_HERO_BG);
                g2.fillRoundRect(0, 0, w, h, 20, 20);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);

                // Circular ambient glow behind the right illustration (Finora style)
                g2.setColor(new Color(243, 232, 255));
                g2.fillOval(w - 180, 2, h + 30, h + 30);

                // Right Phone + Card + Character Vector Illustration
                paintHeroIllustration(g2, w - 170, 6, h - 12);

                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 126));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        hero.setBorder(BorderFactory.createEmptyBorder(12, 22, 10, 175));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel lblHeroTitle1 = new JLabel("Secure Instant Payments");
        lblHeroTitle1.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeroTitle1.setForeground(COLOR_TEXT_MAIN);

        JLabel lblHeroTitle2 = new JLabel("with SafePay");
        lblHeroTitle2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblHeroTitle2.setForeground(COLOR_TEXT_MAIN);

        JLabel lblHeroSub = new JLabel("AI-powered fraud detection protects every transaction in real time.");
        lblHeroSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHeroSub.setForeground(COLOR_TEXT_SUBTLE);

        JButton btnConvert = createGradientPillButton("New Transaction →", 150, 28);
        btnConvert.addActionListener(e -> switchScreen(CARD_TRANSACTION, "New Transaction", btnNavTransaction));

        textGroup.add(lblHeroTitle1);
        textGroup.add(lblHeroTitle2);
        textGroup.add(Box.createVerticalStrut(2));
        textGroup.add(lblHeroSub);
        textGroup.add(Box.createVerticalStrut(8));
        textGroup.add(btnConvert);

        hero.add(textGroup, BorderLayout.CENTER);
        return hero;
    }

    private void paintHeroIllustration(Graphics2D g2, int px, int py, int ph) {
        // 1. Stylized Character carrying the big purple card (Reference A)
        int charX = px + 10;
        int charY = py + ph - 6;

        // Head and bob hair
        g2.setColor(new Color(30, 27, 75));
        g2.fillOval(charX + 6, charY - 60, 10, 10);

        // Sleeveless White Shirt
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(charX + 4, charY - 50, 14, 18, 4, 4);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(charX + 4, charY - 50, 14, 18, 4, 4);

        // Dark Trousers
        g2.setColor(new Color(30, 27, 75));
        Polygon pants = new Polygon();
        pants.addPoint(charX + 4, charY - 32);
        pants.addPoint(charX + 18, charY - 32);
        pants.addPoint(charX + 22, charY - 16);
        pants.addPoint(charX, charY - 16);
        g2.fill(pants);

        // Legs and shoes
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(charX + 6, charY - 16, charX + 5, charY);
        g2.drawLine(charX + 16, charY - 16, charX + 18, charY);
        g2.fillOval(charX + 3, charY - 2, 5, 3);
        g2.fillOval(charX + 16, charY - 2, 5, 3);

        // Big Purple Payment Card carried by character
        int cardX = charX + 4;
        int cardY = charY - 42;
        int cardW = 52;
        int cardH = 32;

        GradientPaint cardGrad = new GradientPaint(
                cardX, cardY, new Color(147, 51, 234),
                cardX + cardW, cardY + cardH, new Color(192, 38, 211)
        );
        g2.setPaint(cardGrad);
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 6, 6);

        // Gold chip on card
        g2.setColor(new Color(254, 240, 138));
        g2.fillRoundRect(cardX + 6, cardY + 5, 8, 6, 2, 2);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.fillRect(cardX + 6, cardY + 20, 22, 2);

        // 2. Modern Smartphone Mockup on the right (Finora Specification)
        int phoneX = px + 75;
        int phoneW = 58;
        int phoneH = ph - 2;
        int phoneY = py + 1;

        g2.setColor(new Color(15, 23, 42));
        g2.fillRoundRect(phoneX, phoneY, phoneW, phoneH, 12, 12);

        // Screen
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(phoneX + 3, phoneY + 3, phoneW - 6, phoneH - 6, 9, 9);

        // Screen items: purple dollar badge at top
        g2.setColor(new Color(124, 58, 237));
        g2.fillOval(phoneX + phoneW / 2 - 8, phoneY + 7, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("$", phoneX + phoneW / 2 - 3, phoneY + 19);

        // Purple slider line
        g2.setColor(new Color(124, 58, 237));
        g2.fillRoundRect(phoneX + 10, phoneY + 26, phoneW - 20, 3, 2, 2);

        // Mini transaction row placeholders
        g2.setColor(new Color(241, 245, 249));
        for (int ry = phoneY + 34; ry < phoneY + phoneH - 18; ry += 9) {
            g2.fillRoundRect(phoneX + 7, ry, phoneW - 14, 5, 2, 2);
        }

        // Mini bottom vertical chart bars on phone
        g2.setColor(new Color(226, 232, 240));
        int barStartY = phoneY + phoneH - 14;
        g2.fillRect(phoneX + 10, barStartY - 6, 4, 8);
        g2.fillRect(phoneX + 18, barStartY - 10, 4, 12);
        g2.fillRect(phoneX + 26, barStartY - 4, 4, 6);
        g2.fillRect(phoneX + 34, barStartY - 8, 4, 10);
        g2.fillRect(phoneX + 42, barStartY - 12, 4, 14);
    }

    /**
     * Finora Activity Trend Chart Card:
     * - Header: "Always apply the true rate", "Mid market rate ℹ"
     * - Timeframe filter pills (24 Hours, 1 Week, 1 Month, 6 Month, 1 Year) with active solid dark pill
     * - Spline curve trend chart with area glow and floating dark tooltip card
     */
    private JPanel createActivityChartCard() {
        JPanel card = createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        // Left title block
        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Transaction Activity & Trends");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Real-time volume and transaction velocity");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);

        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSub);
        headerRow.add(titleBlock, BorderLayout.WEST);

        // Right side: Info tag + Timeframe Pills
        JPanel rightPillBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPillBlock.setOpaque(false);

        JLabel lblInfo = new JLabel("AI Monitored 🛡");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblInfo.setForeground(COLOR_TEXT_SUBTLE);
        rightPillBlock.add(lblInfo);
        rightPillBlock.add(Box.createHorizontalStrut(6));

        timeframePills.clear();
        String[] tfLabels = {"24 Hours", "1 Week", "1 Month", "6 Month", "1 Year"};
        for (String tf : tfLabels) {
            TimeframePill pill = new TimeframePill(tf, tf.equals(selectedTimeframe));
            timeframePills.add(pill);
            rightPillBlock.add(pill);
        }

        headerRow.add(rightPillBlock, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        // Smooth Spline Vector Chart
        transactionChart = new TransactionAmountChart();
        transactionChart.setPreferredSize(new Dimension(540, 205));
        card.add(transactionChart, BorderLayout.CENTER);

        return card;
    }

    /**
     * Finora Recent Transactions Table Card:
     * - Header: "Always apply the true rate", "View All →"
     * - Columns: Name / Business (with brand avatars), Type, Amount, Date
     */
    private JPanel createRecentTransactionsCard() {
        JPanel card = createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 6));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Recent Activity & Transactions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JButton btnViewAll = new JButton("View All →");
        btnViewAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnViewAll.setForeground(COLOR_PURPLE_PRIMARY);
        btnViewAll.setBorderPainted(false);
        btnViewAll.setContentAreaFilled(false);
        btnViewAll.setFocusPainted(false);
        btnViewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnViewAll.addActionListener(e -> switchScreen(CARD_HISTORY, "Transaction History", btnNavHistory));

        headerRow.add(lblTitle, BorderLayout.WEST);
        headerRow.add(btnViewAll, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        tableModel = new RecentTransactionsTableModel();
        recentTransactionsTable = new JTable(tableModel);
        recentTransactionsTable.setRowHeight(32);
        recentTransactionsTable.setShowGrid(false);
        recentTransactionsTable.setIntercellSpacing(new Dimension(0, 0));
        recentTransactionsTable.setFillsViewportHeight(true);
        recentTransactionsTable.setBackground(Color.WHITE);
        recentTransactionsTable.setSelectionBackground(new Color(243, 232, 255));
        recentTransactionsTable.setSelectionForeground(COLOR_TEXT_MAIN);
        recentTransactionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Header Styling
        JTableHeader th = recentTransactionsTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setForeground(COLOR_TEXT_SUBTLE);
        th.setBackground(Color.WHITE);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_OUTLINE));
        th.setPreferredSize(new Dimension(0, 24));

        // Column widths
        recentTransactionsTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        recentTransactionsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        recentTransactionsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        recentTransactionsTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        // Custom Cell Renderers
        recentTransactionsTable.getColumnModel().getColumn(0).setCellRenderer(new BrandBusinessCellRenderer());
        recentTransactionsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setForeground(COLOR_TEXT_SUBTLE);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });
        recentTransactionsTable.getColumnModel().getColumn(2).setCellRenderer(new AmountStatusCellRenderer());
        recentTransactionsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setForeground(COLOR_TEXT_SUBTLE);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });

        JScrollPane tableScroll = new JScrollPane(recentTransactionsTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        tableScroll.setPreferredSize(new Dimension(0, 155));
        card.add(tableScroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // RIGHT COLUMN: BALANCE CARD, UPCOMING PAYMENTS, MY PAYMENTS
    // =========================================================================

    /**
     * Finora Vivid Purple Credit Card:
     * - Gradient: #6D28D9 to #8B5CF6
     * - Top: "Balance", Options icon "•••"
     * - Big balance: ₹ 125,000.50 INR / $34,355.00 USD
     * - Indicator: ↗ 2.04% • July 24, 2025
     * - Bottom: Gold EMV Chip vector & "SafePay" logo
     */
    private JPanel createPurpleBalanceCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Purple Gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(109, 40, 217),
                        w, h, new Color(139, 92, 246)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                // Subtle watermark arc
                g2.setColor(new Color(255, 255, 255, 20));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(w - 110, -30, 150, 150, 0, 360);

                // Bottom Left: Gold EMV Chip
                int chipX = 20;
                int chipY = h - 34;
                g2.setColor(new Color(250, 204, 21));
                g2.fillRoundRect(chipX, chipY, 24, 18, 4, 4);
                g2.setColor(new Color(161, 98, 7));
                g2.drawRoundRect(chipX, chipY, 24, 18, 4, 4);
                g2.drawLine(chipX + 8, chipY, chipX + 8, chipY + 18);
                g2.drawLine(chipX + 16, chipY, chipX + 16, chipY + 18);

                // Bottom Right: SafePay text & logo
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                String bName = "SafePay";
                int bnW = g2.getFontMetrics().stringWidth(bName);
                g2.drawString(bName, w - bnW - 20, h - 20);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(0, 145));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 36, 20));

        // Top Row: "Balance" + Options Icon
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblBal = new JLabel("Balance");
        lblBal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBal.setForeground(new Color(233, 213, 255));

        JLabel lblOpts = new JLabel("•••");
        lblOpts.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblOpts.setForeground(Color.WHITE);
        lblOpts.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        topRow.add(lblBal, BorderLayout.WEST);
        topRow.add(lblOpts, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Center: Balance Amount + Indicator
        JPanel centerBlock = new JPanel();
        centerBlock.setLayout(new BoxLayout(centerBlock, BoxLayout.Y_AXIS));
        centerBlock.setOpaque(false);

        lblBalanceAmount = new JLabel("₹ 0.00 INR");
        lblBalanceAmount.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblBalanceAmount.setForeground(Color.WHITE);

        lblAccountStatusPill = new JLabel("Account Status • Active");
        lblAccountStatusPill.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblAccountStatusPill.setForeground(new Color(233, 213, 255));

        centerBlock.add(lblBalanceAmount);
        centerBlock.add(Box.createVerticalStrut(3));
        centerBlock.add(lblAccountStatusPill);

        card.add(centerBlock, BorderLayout.CENTER);
        return card;
    }

    /**
     * Finora Upcoming Payments Card:
     * - Header: "Upcoming Payments", "Next Month"
     * - Rows: Behance Pro ($320.00), UpWork Pro ($230.00)
     */
    private JPanel createUpcomingPaymentsCard() {
        JPanel card = createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(0, 115));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Upcoming Payments");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Next Month");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblSub, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JPanel emptyUpcoming = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 16));
        emptyUpcoming.setOpaque(false);
        JLabel lblEmptyUpcoming = new JLabel("No scheduled payments pending");
        lblEmptyUpcoming.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmptyUpcoming.setForeground(COLOR_TEXT_SUBTLE);
        emptyUpcoming.add(lblEmptyUpcoming);
        listPanel.add(emptyUpcoming);

        card.add(listPanel, BorderLayout.CENTER);
        return card;
    }

    /**
     * Finora My Payments Card:
     * - Header: "My Payments"
     * - Tabs: "All Payments" (underlined) | "Regular Payments"
     * - Subhead: "Today"
     * - Rows: Spotify Premium, Dribbble Pro, LinkedIn Premium, Youtube Premium
     */
    private JPanel createMyPaymentsCard() {
        JPanel card = createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(0, 215));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        JLabel lblTitle = new JLabel("My Payments");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        topSection.add(lblTitle);
        topSection.add(Box.createVerticalStrut(4));

        // Sub-tabs: All Payments (active) | Regular Payments
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        tabs.setOpaque(false);

        JLabel tabAll = new JLabel("All Payments") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(COLOR_PURPLE_PRIMARY);
                g.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
        };
        tabAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tabAll.setForeground(COLOR_TEXT_MAIN);
        tabAll.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JLabel tabRegular = new JLabel("Regular Payments");
        tabRegular.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tabRegular.setForeground(COLOR_TEXT_SUBTLE);

        tabs.add(tabAll);
        tabs.add(tabRegular);
        topSection.add(tabs);
        topSection.add(Box.createVerticalStrut(4));

        JLabel lblToday = new JLabel("Today");
        lblToday.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblToday.setForeground(COLOR_TEXT_SUBTLE);
        topSection.add(lblToday);

        card.add(topSection, BorderLayout.NORTH);

        // Payments List Container (Dynamically updated with real transactions if available)
        quickPaymentsListPanel = new JPanel();
        quickPaymentsListPanel.setLayout(new BoxLayout(quickPaymentsListPanel, BoxLayout.Y_AXIS));
        quickPaymentsListPanel.setOpaque(false);

        JPanel emptyQuick = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 20));
        emptyQuick.setOpaque(false);
        JLabel lblEmptyQuick = new JLabel("No payment activity recorded yet");
        lblEmptyQuick.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmptyQuick.setForeground(COLOR_TEXT_SUBTLE);
        emptyQuick.add(lblEmptyQuick);
        quickPaymentsListPanel.add(emptyQuick);

        card.add(quickPaymentsListPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPaymentRow(String name, String date, String amount, String initial, Color col) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JComponent icon = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col);
                g2.fillOval(0, 2, 22, 22);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                int sw = g2.getFontMetrics().stringWidth(initial);
                g2.drawString(initial, (22 - sw) / 2, 16);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(24, 26));
        row.add(icon, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblName.setForeground(COLOR_TEXT_MAIN);

        JLabel lblDate = new JLabel(date);
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblDate.setForeground(COLOR_TEXT_SUBTLE);

        center.add(lblName);
        center.add(lblDate);
        row.add(center, BorderLayout.CENTER);

        JLabel lblAmt = new JLabel(amount);
        lblAmt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblAmt.setForeground(COLOR_TEXT_MAIN);
        row.add(lblAmt, BorderLayout.EAST);

        return row;
    }

    private void updateQuickPaymentsList(List<TransactionSummaryDto> list) {
        quickPaymentsListPanel.removeAll();
        if (list == null || list.isEmpty()) {
            JPanel emptyQuick = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 20));
            emptyQuick.setOpaque(false);
            JLabel lblEmptyQuick = new JLabel("No payment activity recorded yet");
            lblEmptyQuick.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblEmptyQuick.setForeground(COLOR_TEXT_SUBTLE);
            emptyQuick.add(lblEmptyQuick);
            quickPaymentsListPanel.add(emptyQuick);
            quickPaymentsListPanel.revalidate();
            quickPaymentsListPanel.repaint();
            return;
        }

        int count = Math.min(3, list.size());
        for (int i = 0; i < count; i++) {
            TransactionSummaryDto tx = list.get(i);
            String name = tx.transactionType() != null ? tx.transactionType() : "Transaction";
            String date = tx.transactionTime() != null ? tx.transactionTime() : "Today";
            String amt = formatCurrency(tx.amount());
            String initial = name.length() > 0 ? name.substring(0, 1) : "T";

            Color c = COLOR_PURPLE_PRIMARY;
            if ("APPROVED".equalsIgnoreCase(tx.status())) c = COLOR_STATUS_APPROVED;
            else if ("BLOCKED".equalsIgnoreCase(tx.status())) c = COLOR_STATUS_BLOCKED;
            else if ("DECLINED".equalsIgnoreCase(tx.status())) c = COLOR_STATUS_DECLINED;

            quickPaymentsListPanel.add(createPaymentRow(name, date, amt, initial, c));
            if (i < count - 1) {
                quickPaymentsListPanel.add(Box.createVerticalStrut(5));
            }
        }
        quickPaymentsListPanel.revalidate();
        quickPaymentsListPanel.repaint();
    }

    // =========================================================================
    // DATA REFRESH & SYNCHRONIZATION
    // =========================================================================

    public void refreshDashboardData() {
        if (!btnRefresh.isEnabled()) return;
        btnRefresh.setEnabled(false);
        setStatusMessage("Fetching real-time account data from backend...");

        new SwingWorker<DashboardResponse, Void>() {
            @Override
            protected DashboardResponse doInBackground() throws Exception {
                return apiClient.getDashboardData();
            }

            @Override
            protected void done() {
                try {
                    DashboardResponse response = get();
                    if (response != null && response.success()) {
                        updateDashboardUi(response);
                        setStatusMessage("Account " + response.accountNumber() + " synchronized successfully.");
                        setDatabaseStatus(true, "Active (" + response.recentTransactions().size() + " Tx)");
                    } else {
                        String msg = response != null ? response.message() : "Failed to load dashboard data.";
                        setStatusMessage("Backend response: " + msg);
                    }
                } catch (Exception ex) {
                    setStatusMessage("Dashboard synchronization notice: " + ex.getMessage());
                    setDatabaseStatus(false, "Connection Error");
                } finally {
                    btnRefresh.setEnabled(true);
                }
            }
        }.execute();
    }

    void updateDashboardUi(DashboardResponse data) {
        if (data == null) return;
        this.currentDashboardData = data;

        BigDecimal bal = data.balance() != null ? data.balance() : BigDecimal.ZERO;
        lblBalanceAmount.setText(formatCurrency(bal) + " INR");

        String statusStr = data.accountStatus() != null ? data.accountStatus() : "ACTIVE";
        String accNum = data.accountNumber() != null ? data.accountNumber() : "ACC-NOT-ASSIGNED";
        lblAccountStatusPill.setText(statusStr + " • " + accNum);

        lblMetricTotalVal.setText(String.valueOf(data.totalTransactions()));
        lblMetricApprovedVal.setText(String.valueOf(data.approvedTransactions()));
        lblMetricDeclinedVal.setText(String.valueOf(data.declinedTransactions()));
        lblMetricBlockedVal.setText(String.valueOf(data.blockedTransactions()));

        List<TransactionSummaryDto> filtered = filterTransactionsByTimeframe(data.recentTransactions(), selectedTimeframe);
        transactionChart.setTransactions(filtered);
        tableModel.setTransactions(filtered);
        updateQuickPaymentsList(data.recentTransactions());
    }

    public void setTimeframe(String timeframe) {
        this.selectedTimeframe = timeframe;
        for (TimeframePill pill : timeframePills) {
            pill.setActive(pill.getText().equals(timeframe));
        }
        if (currentDashboardData != null && currentDashboardData.recentTransactions() != null) {
            List<TransactionSummaryDto> filtered = filterTransactionsByTimeframe(
                    currentDashboardData.recentTransactions(), timeframe
            );
            transactionChart.setTransactions(filtered);
            tableModel.setTransactions(filtered);
        }
    }

    private List<TransactionSummaryDto> filterTransactionsByTimeframe(List<TransactionSummaryDto> all, String timeframe) {
        if (all == null || all.isEmpty()) {
            return Collections.emptyList();
        }
        if ("1 Year".equalsIgnoreCase(timeframe)) {
            return new ArrayList<>(all);
        }

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };

        LocalDateTime latest = null;
        for (TransactionSummaryDto tx : all) {
            LocalDateTime dt = parseDateTime(tx.transactionTime(), formatters);
            if (dt != null && (latest == null || dt.isAfter(latest))) {
                latest = dt;
            }
        }

        if (latest == null) {
            return switch (timeframe) {
                case "24 Hours" -> all.subList(0, Math.min(all.size(), 2));
                case "1 Week" -> all.subList(0, Math.min(all.size(), 4));
                case "1 Month" -> all.subList(0, Math.min(all.size(), 8));
                case "6 Month" -> all.subList(0, Math.min(all.size(), 12));
                default -> new ArrayList<>(all);
            };
        }

        final LocalDateTime maxTime = latest;
        long hoursLimit = switch (timeframe) {
            case "24 Hours" -> 24;
            case "1 Week" -> 7 * 24;
            case "1 Month" -> 30 * 24;
            case "6 Month" -> 180 * 24;
            default -> 365 * 24;
        };

        List<TransactionSummaryDto> result = new ArrayList<>();
        for (TransactionSummaryDto tx : all) {
            LocalDateTime dt = parseDateTime(tx.transactionTime(), formatters);
            if (dt != null) {
                long diffHours = ChronoUnit.HOURS.between(dt, maxTime);
                if (diffHours <= hoursLimit) {
                    result.add(tx);
                }
            } else {
                result.add(tx);
            }
        }
        return result.isEmpty() ? new ArrayList<>(all) : result;
    }

    private LocalDateTime parseDateTime(String text, DateTimeFormatter[] formatters) {
        if (text == null || text.isBlank()) return null;
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(text, fmt);
            } catch (Exception ignored) {}
        }
        return null;
    }

    // =========================================================================
    // PLACEHOLDER SCREENS
    // =========================================================================

    private JPanel createPlaceholderScreen(String title, String subtitle, String phaseNote) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = createStyledCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNote = new JLabel(phaseNote);
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNote.setForeground(COLOR_PURPLE_PRIMARY);
        lblNote.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(14));
        card.add(lblNote);

        panel.add(card);
        return panel;
    }

    private JPanel createProfileScreen() {
        if (currentUser == null) {
            return createPlaceholderScreen("User Profile", "Not authenticated", "Please sign in to view profile details.");
        }
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = createStyledCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        JLabel lblTitle = new JLabel("User Profile & Security Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(18));

        card.add(createProfileInfoRow("Username", currentUser.username()));
        card.add(createProfileInfoRow("Email Address", currentUser.email()));
        card.add(createProfileInfoRow("Assigned Role", currentUser.role()));
        card.add(createProfileInfoRow("Account Number", currentUser.accountNumber()));

        panel.add(card);
        return panel;
    }

    private JPanel createProfileInfoRow(String key, String val) {
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(380, 26));

        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        k.setForeground(COLOR_TEXT_SUBTLE);

        JLabel v = new JLabel(val != null ? val : "N/A");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(COLOR_TEXT_MAIN);

        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of SafePay?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        apiClient.logout();
                    } catch (Exception ignored) {}
                    return null;
                }

                @Override
                protected void done() {
                    dispose();
                    SwingUtilities.invokeLater(() -> {
                        AuthFrame authFrame = new AuthFrame(apiClient, authResp -> {
                            DashboardFrame df = new DashboardFrame(apiClient, authResp);
                            df.setVisible(true);
                        });
                        authFrame.setVisible(true);
                    });
                }
            }.execute();
        }
    }

    // =========================================================================
    // UI HELPERS & CUSTOM RENDERERS
    // =========================================================================

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) return "₹ 0.00";
        return String.format(Locale.ENGLISH, "₹ %,.2f", amount);
    }

    public static JPanel createStyledCardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_CARD_OUTLINE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        return panel;
    }

    public static JButton createGradientPillButton(String text, int width, int height) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isEnabled()) {
                    GradientPaint gp = new GradientPaint(
                            0, 0, COLOR_PURPLE_PRIMARY,
                            getWidth(), 0, new Color(147, 51, 234)
                    );
                    g2.setPaint(gp);
                } else {
                    g2.setColor(new Color(203, 213, 225));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int strW = g2.getFontMetrics().stringWidth(getText());
                int strH = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - strW) / 2, (getHeight() + strH) / 2 - 2);

                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(width, height));
        btn.setMaximumSize(new Dimension(width, height));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public TransactionPanel getTransactionPanel() {
        return transactionPanel;
    }

    // =========================================================================
    // INNER CLASSES: SIDEBAR, LOGO, TABLE RENDERERS
    // =========================================================================

    private static class FinoraNavButton extends JButton {
        private boolean active;
        private final String iconType;
        private final String badgeText;

        public FinoraNavButton(String label, String iconType, boolean active, String badgeText) {
            super(label);
            this.iconType = iconType;
            this.active = active;
            this.badgeText = badgeText;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
            setForeground(active ? COLOR_PURPLE_PRIMARY : COLOR_TEXT_SUBTLE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(7, 36, 7, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(185, 34));
            setPreferredSize(new Dimension(185, 34));
        }

        public void setActive(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
            setForeground(active ? COLOR_PURPLE_PRIMARY : COLOR_TEXT_SUBTLE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(COLOR_PURPLE_PILL);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }

            g2.setColor(active ? COLOR_PURPLE_PRIMARY : COLOR_TEXT_SUBTLE);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            paintNavVectorIcon(g2, 12, h / 2 - 8, iconType);

            if (badgeText != null) {
                int bw = 24;
                int bh = 16;
                int bx = w - bw - 8;
                int by = (h - bh) / 2;
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                int tw = g2.getFontMetrics().stringWidth(badgeText);
                g2.drawString(badgeText, bx + (bw - tw) / 2, by + 12);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        private void paintNavVectorIcon(Graphics2D g2, int x, int y, String type) {
            switch (type) {
                case "DASHBOARD" -> {
                    g2.drawRoundRect(x, y, 6, 6, 2, 2);
                    g2.drawRoundRect(x + 8, y, 6, 6, 2, 2);
                    g2.drawRoundRect(x, y + 8, 6, 6, 2, 2);
                    g2.drawRoundRect(x + 8, y + 8, 6, 6, 2, 2);
                }
                case "TRANSACTION" -> {
                    g2.drawLine(x, y + 4, x + 12, y + 4);
                    g2.drawLine(x + 8, y + 1, x + 12, y + 4);
                    g2.drawLine(x + 14, y + 11, x + 2, y + 11);
                    g2.drawLine(x + 6, y + 14, x + 2, y + 11);
                }
                case "INVOICES" -> {
                    g2.drawRoundRect(x + 1, y, 12, 15, 2, 2);
                    g2.drawLine(x + 4, y + 4, x + 10, y + 4);
                    g2.drawLine(x + 4, y + 8, x + 10, y + 8);
                }
                case "MESSAGE" -> {
                    g2.drawRoundRect(x, y + 1, 14, 11, 4, 4);
                    g2.drawLine(x + 4, y + 12, x + 2, y + 15);
                    g2.drawLine(x + 2, y + 15, x + 7, y + 12);
                }
                case "WALLETS" -> {
                    g2.drawRoundRect(x, y + 2, 14, 11, 3, 3);
                    g2.drawArc(x + 9, y + 6, 5, 4, 90, 180);
                }
                case "ANALYTICS" -> {
                    g2.fillRect(x + 1, y + 9, 3, 6);
                    g2.fillRect(x + 6, y + 5, 3, 10);
                    g2.fillRect(x + 11, y + 1, 3, 14);
                }
            }
        }
    }

    private static class FoldedRibbonLogoBadge extends JComponent {
        private final int w;

        public FoldedRibbonLogoBadge(int w, int h) {
            this.w = w;
            setPreferredSize(new Dimension(w, h));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(COLOR_PURPLE_PRIMARY);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.drawRoundRect(2, 4, w - 8, 5, 3, 3);
            g2.drawRoundRect(2, 12, w - 8, 5, 3, 3);
            g2.drawRoundRect(2, 20, w - 8, 5, 3, 3);

            g2.dispose();
        }
    }

    /**
     * Name / Business Cell Renderer with Brand Logo Circle + Title + Subtitle.
     */
    private static class BrandBusinessCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);
            panel.setBackground(isSel ? new Color(243, 232, 255) : Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

            String fullStr = val != null ? val.toString() : "Payment";
            String title = fullStr;
            String sub = "SafePay Transfer";
            String badgeLetter;
            Color badgeCol = COLOR_PURPLE_PRIMARY;

            if (fullStr.contains("Amazon") || r == 0) {
                title = "Amazon Prime Subscription";
                sub = "Amazon";
                badgeLetter = "a";
                badgeCol = new Color(249, 115, 22);
            } else if (fullStr.contains("Figma") || r == 1) {
                title = "Figma Subscription";
                sub = "Figma.Inc";
                badgeLetter = "F";
                badgeCol = new Color(168, 85, 247);
            } else if (fullStr.contains("Spotify") || r == 2) {
                title = "Spotify Premium";
                sub = "Spotify.AB";
                badgeLetter = "S";
                badgeCol = new Color(22, 163, 74);
            } else {
                badgeLetter = fullStr.length() > 0 ? fullStr.substring(0, 1) : "T";
            }

            final String fBadge = badgeLetter;
            final Color fCol = badgeCol;
            JComponent icon = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(fCol);
                    g2.fillOval(0, 3, 20, 20);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                    int sw = g2.getFontMetrics().stringWidth(fBadge);
                    g2.drawString(fBadge, (20 - sw) / 2, 17);
                    g2.dispose();
                }
            };
            icon.setPreferredSize(new Dimension(22, 26));
            panel.add(icon, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);

            JLabel lblT = new JLabel(title);
            lblT.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblT.setForeground(COLOR_TEXT_MAIN);

            JLabel lblS = new JLabel(sub);
            lblS.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblS.setForeground(COLOR_TEXT_SUBTLE);

            text.add(lblT);
            text.add(lblS);
            panel.add(text, BorderLayout.CENTER);

            return panel;
        }
    }

    /**
     * Amount and Status Cell Renderer with color status indicator.
     */
    private static class AmountStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFoc, r, c);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(COLOR_TEXT_MAIN);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return lbl;
        }
    }

    private static class RecentTransactionsTableModel extends AbstractTableModel {
        private final String[] columns = {"Name / Business", "Type", "Amount", "Date"};
        private final List<TransactionSummaryDto> data = new ArrayList<>();

        public void setTransactions(List<TransactionSummaryDto> list) {
            data.clear();
            if (list != null) {
                data.addAll(list);
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return data.size(); }
        @Override
        public int getColumnCount() { return columns.length; }
        @Override
        public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            if (r < 0 || r >= data.size()) return "";
            TransactionSummaryDto tx = data.get(r);
            return switch (c) {
                case 0 -> tx.transactionType() != null ? tx.transactionType() : "Transaction";
                case 1 -> {
                    String type = tx.transactionType() != null ? tx.transactionType() : "Payment";
                    yield switch (type.toUpperCase()) {
                        case "TRANSFER" -> "Software";
                        case "PAYMENT" -> "Entertainment";
                        case "CASH_OUT" -> "Subscription";
                        default -> "Service";
                    };
                }
                case 2 -> formatCurrency(tx.amount());
                case 3 -> {
                    String t = tx.transactionTime();
                    if (t != null && t.length() >= 10) yield t.substring(0, 10);
                    yield "Today";
                }
                default -> "";
            };
        }
    }

    private static class ScrollableContentPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 64; }
        @Override
        public boolean getScrollableTracksViewportWidth() { return true; }
        @Override
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private class TimeframePill extends JLabel {
        private boolean active;

        public TimeframePill(String text, boolean active) {
            super(text, SwingConstants.CENTER);
            this.active = active;
            setOpaque(false);
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 10));
            setForeground(active ? Color.WHITE : COLOR_TEXT_SUBTLE);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    DashboardFrame.this.setTimeframe(getText());
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 10));
            setForeground(active ? Color.WHITE : COLOR_TEXT_SUBTLE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Active timeframe pill: Solid dark capsule (#18181B)
                g2.setColor(new Color(24, 24, 27));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
