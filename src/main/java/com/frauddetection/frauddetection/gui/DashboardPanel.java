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
import java.awt.GraphicsEnvironment;
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
import javax.swing.JDialog;
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
import javax.swing.Timer;

import com.frauddetection.frauddetection.client.ApiClientException;
import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AuthResponse;
import com.frauddetection.frauddetection.dto.DashboardResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Native Java Swing & Java 2D dashboard panel for SafePay — AI-Powered Fraud Detection System.
 *
 * Implements:
 * - Role-aware navigation (Customer vs Administrator)
 * - SafePay AI fraud protection messaging (replaces placeholder conversion banner)
 * - Single-instance panel reuse (prevents duplicate panel creation)
 * - Dynamic pending review count tracking for administrators
 * - Active notification bell popup showing pending review and surveillance status
 * - Contextual SafePay Help & Support dialog
 * - Off-EDT background synchronization and 401 session expiration handling
 * - Clean session cleanup and logout workflow
 */
public class DashboardPanel extends JPanel {

    public static final String CARD_DASHBOARD = "CARD_DASHBOARD";
    public static final String CARD_TRANSACTION = "CARD_TRANSACTION";
    public static final String CARD_HISTORY = "CARD_HISTORY";
    public static final String CARD_ALERTS = "CARD_ALERTS";
    public static final String CARD_PROFILE = "CARD_PROFILE";

    private final SafePayApiClient apiClient;
    private final AuthResponse currentUser;
    private final Runnable onLogout;
    private Runnable onSessionExpired;

    private final boolean isAdmin;
    private final CardLayout mainCardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(mainCardLayout);
    private String currentCard = CARD_DASHBOARD;

    // Sidebar navigation buttons
    private final List<FinoraNavButton> navButtons = new ArrayList<>();
    private FinoraNavButton btnNavDashboard;
    private FinoraNavButton btnNavTransaction;
    private FinoraNavButton btnNavHistory;
    private FinoraNavButton btnNavAlerts;
    private FinoraNavButton btnNavPendingReviews;
    private FinoraNavButton btnNavProfile;
    private FinoraNavButton btnNavSupport;
    private JButton btnLogout;

    // Header Components
    private JLabel lblHeaderTitle;
    private JLabel lblUserInitialBadge;
    private JLabel lblUserNameTag;
    private JButton btnRefresh;
    private JButton btnBell;
    private JLabel lblBellBadge;

    // Status bar labels
    private JLabel lblStatusMessage;
    private JLabel lblDbStatus;

    // Dashboard Dynamic Widgets
    private JLabel lblBalanceAmount;
    private JLabel lblAccountStatusPill;
    private TransactionAmountChart transactionChart;
    private JTable recentTransactionsTable;
    private RecentTransactionsTableModel tableModel;
    private JPanel quickPaymentsListPanel;

    // Sub-panels (Created ONCE and reused across transitions)
    private TransactionPanel transactionPanel;
    private TransactionHistoryPanel historyPanel;
    private AdminMonitoringPanel adminMonitoringPanel;
    private JPanel profileScreen;

    // Metric Summary Labels
    private final JLabel lblMetricTotalVal = new JLabel("0");
    private final JLabel lblMetricApprovedVal = new JLabel("0");
    private final JLabel lblMetricDeclinedVal = new JLabel("0");
    private final JLabel lblMetricBlockedVal = new JLabel("0");
    private JLabel lblAdminPendingCount;

    // Timeframe filter state
    private String selectedTimeframe = "1 Month";
    private final List<TimeframePill> timeframePills = new ArrayList<>();
    private DashboardResponse currentDashboardData;

    private int pendingReviewCount = 0;
    private boolean sessionExpired = false;
    private boolean autoConfirmLogoutForTesting = false;

    public DashboardPanel(SafePayApiClient apiClient, AuthResponse currentUser, Runnable onLogout) {
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.currentUser = currentUser;
        this.onLogout = onLogout;
        this.isAdmin = currentUser != null && currentUser.role() != null
                && (currentUser.role().contains("ADMIN") || "ROLE_ADMIN".equalsIgnoreCase(currentUser.role()));

        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();
        refreshDashboardData();
    }

    public DashboardPanel(SafePayApiClient apiClient, AuthResponse currentUser) {
        this(apiClient, currentUser, null);
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    private void initUI() {
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
                GradientPaint gp = new GradientPaint(0, 0, DashboardFrame.COLOR_OBSIDIAN_BG, 0, h, DashboardFrame.COLOR_SURFACE_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // 2. Soft Ambient Waves
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

        // Floating White Workspace Container (26px corner radius)
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
                g2.setColor(DashboardFrame.COLOR_SURFACE_DARK);
                g2.fillRoundRect(0, 0, w, h, 24, 24);
                g2.setColor(DashboardFrame.COLOR_CARD_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);
                g2.dispose();
            }
        };
        workspaceContainer.setOpaque(false);

        // Sidebar (West)
        workspaceContainer.add(createSidebar(), BorderLayout.WEST);

        // Main Center Area (North Header + Center Cards)
        JPanel mainWorkspace = new JPanel(new BorderLayout());
        mainWorkspace.setOpaque(false);
        mainWorkspace.add(createHeaderPanel(), BorderLayout.NORTH);

        // Instantiate sub-panels ONCE to prevent duplicate panel allocations
        cardsPanel.setOpaque(false);
        cardsPanel.add(createDashboardScreen(), CARD_DASHBOARD);

        transactionPanel = new TransactionPanel(apiClient, this::refreshDashboardData, () -> navigateToDashboard());
        cardsPanel.add(transactionPanel, CARD_TRANSACTION);

        historyPanel = new TransactionHistoryPanel(apiClient, () -> navigateToDashboard());
        cardsPanel.add(historyPanel, CARD_HISTORY);

        adminMonitoringPanel = new AdminMonitoringPanel(apiClient, () -> navigateToDashboard());
        cardsPanel.add(adminMonitoringPanel, CARD_ALERTS);

        profileScreen = createProfileScreen();
        cardsPanel.add(profileScreen, CARD_PROFILE);

        mainWorkspace.add(cardsPanel, BorderLayout.CENTER);
        workspaceContainer.add(mainWorkspace, BorderLayout.CENTER);

        outerCanvas.add(workspaceContainer, BorderLayout.CENTER);
        add(outerCanvas, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setStatusMessage("SafePay Dashboard Ready • Connected to " + apiClient.getBaseUrl());
        setDatabaseStatus(true, "REST Session Active");
    }

    // =========================================================================
    // SIDEBAR NAVIGATION (Role-Aware)
    // =========================================================================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 14));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(215, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(22, 16, 18, 12));

        // 1. Top Logo: Ribbon badge + "SafePay"
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        logoRow.setOpaque(false);
        logoRow.add(new FoldedRibbonLogoBadge(28, 24));

        JLabel lblBrand = new JLabel("SafePay");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBrand.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        logoRow.add(lblBrand);

        sidebar.add(logoRow, BorderLayout.NORTH);

        // 2. Navigation Items Stack
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);

        navButtons.clear();

        if (isAdmin) {
            // Administrator Navigation
            btnNavDashboard = new FinoraNavButton("Admin Dashboard", "DASHBOARD", true, null);
            btnNavHistory = new FinoraNavButton("Transaction Monitoring", "INVOICES", false, null);
            btnNavAlerts = new FinoraNavButton("Fraud Alerts", "MESSAGE", false, null);
            btnNavPendingReviews = new FinoraNavButton("Pending Reviews", "ANALYTICS", false, null);
            btnNavProfile = new FinoraNavButton("Admin Profile", "WALLETS", false, null);
            btnNavSupport = new FinoraNavButton("Help & Support", "MESSAGE", false, null);

            navButtons.add(btnNavDashboard);
            navButtons.add(btnNavHistory);
            navButtons.add(btnNavAlerts);
            navButtons.add(btnNavPendingReviews);
            navButtons.add(btnNavProfile);
            navButtons.add(btnNavSupport);

            btnNavDashboard.addActionListener(e -> switchScreen(CARD_DASHBOARD, "Admin Dashboard", btnNavDashboard));
            btnNavHistory.addActionListener(e -> switchScreen(CARD_HISTORY, "Transaction Monitoring", btnNavHistory));
            btnNavAlerts.addActionListener(e -> {
                if (adminMonitoringPanel != null) adminMonitoringPanel.showAlertsView();
                switchScreen(CARD_ALERTS, "Fraud Surveillance Alerts", btnNavAlerts);
            });
            btnNavPendingReviews.addActionListener(e -> {
                if (adminMonitoringPanel != null) adminMonitoringPanel.showPendingView();
                switchScreen(CARD_ALERTS, "Pending Fraud Reviews", btnNavPendingReviews);
            });
            btnNavProfile.addActionListener(e -> switchScreen(CARD_PROFILE, "Admin Profile", btnNavProfile));
            btnNavSupport.addActionListener(e -> showSupportDialog());
        } else {
            // Customer Navigation
            btnNavDashboard = new FinoraNavButton("Dashboard", "DASHBOARD", true, null);
            btnNavTransaction = new FinoraNavButton("Payments & Transfers", "TRANSACTION", false, null);
            btnNavHistory = new FinoraNavButton("Transaction History", "INVOICES", false, null);
            btnNavProfile = new FinoraNavButton("Profile & Security", "WALLETS", false, null);
            btnNavSupport = new FinoraNavButton("Help & Support", "MESSAGE", false, null);

            navButtons.add(btnNavDashboard);
            navButtons.add(btnNavTransaction);
            navButtons.add(btnNavHistory);
            navButtons.add(btnNavProfile);
            navButtons.add(btnNavSupport);

            btnNavDashboard.addActionListener(e -> switchScreen(CARD_DASHBOARD, "Dashboard", btnNavDashboard));
            btnNavTransaction.addActionListener(e -> switchScreen(CARD_TRANSACTION, "Payments & Transfers", btnNavTransaction));
            btnNavHistory.addActionListener(e -> switchScreen(CARD_HISTORY, "Transaction History", btnNavHistory));
            btnNavProfile.addActionListener(e -> switchScreen(CARD_PROFILE, "Profile & Security", btnNavProfile));
            btnNavSupport.addActionListener(e -> showSupportDialog());
        }

        for (FinoraNavButton btn : navButtons) {
            menuPanel.add(btn);
            menuPanel.add(Box.createVerticalStrut(4));
        }

        sidebar.add(menuPanel, BorderLayout.CENTER);

        // 3. Bottom Controls: Logout (misleading theme toggle removed)
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        btnLogout = createGhostSidebarButton("Log Out", "LOGOUT");
        btnLogout.addActionListener(e -> performLogout());
        bottomPanel.add(btnLogout);

        sidebar.add(bottomPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton createGhostSidebarButton(String text, String iconType) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = getHeight();

                g2.setColor(DashboardFrame.COLOR_TEXT_SUBTLE);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if ("LOGOUT".equals(iconType)) {
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
        btn.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 28, 6, 8));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(185, 30));
        return btn;
    }

    public void switchScreen(String cardName, String title, FinoraNavButton activeBtn) {
        // Customer authorization guard: customers cannot switch to alerts
        if (!isAdmin && CARD_ALERTS.equals(cardName)) {
            setStatusMessage("Access Denied: Administrator privileges required.");
            return;
        }

        this.currentCard = cardName;
        mainCardLayout.show(cardsPanel, cardName);
        if (lblHeaderTitle != null) {
            lblHeaderTitle.setText(title);
        }

        for (FinoraNavButton btn : navButtons) {
            btn.setActive(btn == activeBtn);
        }

        if (CARD_HISTORY.equals(cardName) && historyPanel != null) {
            historyPanel.loadTransactionHistory();
        } else if (CARD_ALERTS.equals(cardName) && adminMonitoringPanel != null) {
            adminMonitoringPanel.loadAllData();
        }
    }

    // =========================================================================
    // HEADER (Title, Refresh, Bell Notifications, User Profile Chip)
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 24));

        lblHeaderTitle = new JLabel(isAdmin ? "Admin Dashboard" : "Dashboard");
        lblHeaderTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblHeaderTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        header.add(lblHeaderTitle, BorderLayout.WEST);

        // Right cluster: Refresh button, Bell notification, Profile chip
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
                g2.setColor(DashboardFrame.COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        btnRefresh.setPreferredSize(new Dimension(32, 32));
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.setToolTipText("Refresh real-time data");
        btnRefresh.addActionListener(e -> refreshDashboardData());
        rightCluster.add(btnRefresh);

        // Notification Bell Icon with dynamic badge
        btnBell = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Bell outline
                g2.setColor(DashboardFrame.COLOR_TEXT_SUBTLE);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawArc(w / 2 - 6, h / 2 - 7, 12, 12, 0, 180);
                g2.drawLine(w / 2 - 8, h / 2 - 1, w / 2 + 8, h / 2 - 1);
                g2.drawOval(w / 2 - 2, h / 2 + 1, 4, 3);

                if (pendingReviewCount > 0) {
                    g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
                    g2.fillOval(w - 11, 3, 9, 9);
                }

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
        btnBell.addActionListener(e -> showNotifications());
        rightCluster.add(btnBell);

        // User Profile Chip
        JPanel profileChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(DashboardFrame.COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        profileChip.setOpaque(false);
        profileChip.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 8));
        profileChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String initial = (currentUser != null && currentUser.username() != null && !currentUser.username().isEmpty())
                ? currentUser.username().substring(0, 1).toUpperCase()
                : (isAdmin ? "A" : "U");

        lblUserInitialBadge = new JLabel(initial, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
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
                : (isAdmin ? "Administrator" : "Customer");
        lblUserNameTag = new JLabel(displayName);
        lblUserNameTag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUserNameTag.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        profileChip.add(lblUserNameTag);

        JLabel lblChevron = new JLabel("v");
        lblChevron.setFont(new Font("Segoe UI", Font.BOLD, 9));
        lblChevron.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        profileChip.add(lblChevron);

        JPopupMenu profileMenu = new JPopupMenu();
        JMenuItem itemProfile = new JMenuItem(isAdmin ? "Admin Profile" : "Account Profile");
        itemProfile.addActionListener(e -> navigateToProfile());
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
    // DASHBOARD SCREEN
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

        // RIGHT COLUMN (~38% width): Balance Card / Admin Console + Quick Actions + Activity
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(310, 0));

        if (isAdmin) {
            rightColumn.add(createAdminSurveillanceCard());
            rightColumn.add(Box.createVerticalStrut(10));
            rightColumn.add(createAdminQuickLinksCard());
            rightColumn.add(Box.createVerticalStrut(10));
            rightColumn.add(createAdminMetricsCard());
        } else {
            rightColumn.add(createPurpleBalanceCard());
            rightColumn.add(Box.createVerticalStrut(10));
            rightColumn.add(createCustomerQuickActionsCard());
            rightColumn.add(Box.createVerticalStrut(10));
            rightColumn.add(createMyPaymentsCard());
        }

        content.add(rightColumn, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    // =========================================================================
    // HERO BANNER (SafePay AI Fraud Protection Messaging)
    // =========================================================================

    private JPanel createHeroBannerCard() {
        JPanel hero = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(DashboardFrame.COLOR_HERO_BG);
                g2.fillRoundRect(0, 0, w, h, 20, 20);
                g2.setColor(DashboardFrame.COLOR_BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 20, 20);

                g2.setColor(new Color(243, 232, 255));
                g2.fillOval(w - 180, 2, h + 30, h + 30);

                paintHeroIllustration(g2, w - 170, 6, h - 12);
                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 128));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
        hero.setBorder(BorderFactory.createEmptyBorder(12, 22, 10, 175));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel lblHeroTitle1;
        JLabel lblHeroTitle2;
        JLabel lblHeroSub;
        JButton btnHeroAction;

        if (isAdmin) {
            lblHeroTitle1 = new JLabel("Surveillance & Fraud Defense");
            lblHeroTitle2 = new JLabel("SafePay Security Center");
            lblHeroSub = new JLabel("Active behavioral surveillance, real-time risk scoring, and pending fraud review queue.");
            btnHeroAction = DashboardFrame.createGradientPillButton("Review Pending Queue →", 195, 28);
            btnHeroAction.addActionListener(e -> navigateToPendingReviews());
        } else {
            lblHeroTitle1 = new JLabel("Secure Instant Payments");
            lblHeroTitle2 = new JLabel("with SafePay Protection");
            lblHeroSub = new JLabel("AI-powered machine learning models inspect and protect your transfers in real time.");
            btnHeroAction = DashboardFrame.createGradientPillButton("New Payment / Transfer →", 185, 28);
            btnHeroAction.addActionListener(e -> navigateToTransaction());
        }

        lblHeroTitle1.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblHeroTitle1.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        lblHeroTitle2.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblHeroTitle2.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        lblHeroSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHeroSub.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        textGroup.add(lblHeroTitle1);
        textGroup.add(lblHeroTitle2);
        textGroup.add(Box.createVerticalStrut(2));
        textGroup.add(lblHeroSub);
        textGroup.add(Box.createVerticalStrut(8));
        textGroup.add(btnHeroAction);

        hero.add(textGroup, BorderLayout.CENTER);
        return hero;
    }

    private void paintHeroIllustration(Graphics2D g2, int px, int py, int ph) {
        int charX = px + 10;
        int charY = py + ph - 6;

        g2.setColor(new Color(30, 27, 75));
        g2.fillOval(charX + 6, charY - 60, 10, 10);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(charX + 4, charY - 50, 14, 18, 4, 4);
        g2.setColor(new Color(226, 232, 240));
        g2.drawRoundRect(charX + 4, charY - 50, 14, 18, 4, 4);

        g2.setColor(new Color(30, 27, 75));
        Polygon pants = new Polygon();
        pants.addPoint(charX + 4, charY - 32);
        pants.addPoint(charX + 18, charY - 32);
        pants.addPoint(charX + 22, charY - 16);
        pants.addPoint(charX, charY - 16);
        g2.fill(pants);

        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(charX + 6, charY - 16, charX + 5, charY);
        g2.drawLine(charX + 16, charY - 16, charX + 18, charY);
        g2.fillOval(charX + 3, charY - 2, 5, 3);
        g2.fillOval(charX + 16, charY - 2, 5, 3);

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

        g2.setColor(new Color(254, 240, 138));
        g2.fillRoundRect(cardX + 6, cardY + 5, 8, 6, 2, 2);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.fillRect(cardX + 6, cardY + 20, 22, 2);

        // Modern Smartphone Mockup
        int phoneX = px + 75;
        int phoneW = 58;
        int phoneH = ph - 2;
        int phoneY = py + 1;

        g2.setColor(new Color(15, 23, 42));
        g2.fillRoundRect(phoneX, phoneY, phoneW, phoneH, 12, 12);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(phoneX + 3, phoneY + 3, phoneW - 6, phoneH - 6, 9, 9);

        g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
        g2.fillOval(phoneX + phoneW / 2 - 8, phoneY + 7, 16, 16);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("₹", phoneX + phoneW / 2 - 4, phoneY + 19);

        g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
        g2.fillRoundRect(phoneX + 10, phoneY + 26, phoneW - 20, 3, 2, 2);

        g2.setColor(new Color(241, 245, 249));
        for (int ry = phoneY + 34; ry < phoneY + phoneH - 18; ry += 9) {
            g2.fillRoundRect(phoneX + 7, ry, phoneW - 14, 5, 2, 2);
        }

        g2.setColor(new Color(226, 232, 240));
        int barStartY = phoneY + phoneH - 14;
        g2.fillRect(phoneX + 10, barStartY - 6, 4, 8);
        g2.fillRect(phoneX + 18, barStartY - 10, 4, 12);
        g2.fillRect(phoneX + 26, barStartY - 4, 4, 6);
        g2.fillRect(phoneX + 34, barStartY - 8, 4, 10);
        g2.fillRect(phoneX + 42, barStartY - 12, 4, 14);
    }

    // =========================================================================
    // ACTIVITY CHART CARD
    // =========================================================================

    private JPanel createActivityChartCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel lblTitle = new JLabel("Transaction Activity & Trends");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Real-time volume and transaction velocity");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        titleBlock.add(lblTitle);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(lblSub);
        headerRow.add(titleBlock, BorderLayout.WEST);

        JPanel rightPillBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightPillBlock.setOpaque(false);

        JLabel lblInfo = new JLabel("AI Monitored 🛡");
        lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblInfo.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
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

        transactionChart = new TransactionAmountChart();
        transactionChart.setPreferredSize(new Dimension(540, 205));
        card.add(transactionChart, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // RECENT TRANSACTIONS CARD (Real Data without Placeholder Mocks)
    // =========================================================================

    private JPanel createRecentTransactionsCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 6));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Recent Activity & Transactions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

        JButton btnViewAll = new JButton("View All →");
        btnViewAll.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnViewAll.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        btnViewAll.setBorderPainted(false);
        btnViewAll.setContentAreaFilled(false);
        btnViewAll.setFocusPainted(false);
        btnViewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnViewAll.addActionListener(e -> navigateToHistory());

        headerRow.add(lblTitle, BorderLayout.WEST);
        headerRow.add(btnViewAll, BorderLayout.EAST);
        card.add(headerRow, BorderLayout.NORTH);

        tableModel = new RecentTransactionsTableModel();
        recentTransactionsTable = new JTable(tableModel);
        recentTransactionsTable.setRowHeight(36);
        recentTransactionsTable.setShowGrid(false);
        recentTransactionsTable.setIntercellSpacing(new Dimension(0, 0));
        recentTransactionsTable.setFillsViewportHeight(true);
        recentTransactionsTable.setBackground(DashboardFrame.COLOR_CARD_SURFACE);
        recentTransactionsTable.setSelectionBackground(new Color(243, 232, 255));
        recentTransactionsTable.setSelectionForeground(DashboardFrame.COLOR_TEXT_MAIN);
        recentTransactionsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader th = recentTransactionsTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        th.setBackground(Color.WHITE);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DashboardFrame.COLOR_CARD_OUTLINE));
        th.setPreferredSize(new Dimension(0, 24));

        recentTransactionsTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        recentTransactionsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        recentTransactionsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        recentTransactionsTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        recentTransactionsTable.getColumnModel().getColumn(0).setCellRenderer(new RealTransactionCellRenderer());
        recentTransactionsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });
        recentTransactionsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });
        recentTransactionsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFoc, r, c);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                lbl.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return lbl;
            }
        });

        JScrollPane tableScroll = new JScrollPane(recentTransactionsTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(DashboardFrame.COLOR_CARD_SURFACE);
        tableScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setPreferredSize(new Dimension(0, 155));
        card.add(tableScroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // RIGHT COLUMN: CUSTOMER WIDGETS
    // =========================================================================

    private JPanel createPurpleBalanceCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, DashboardFrame.COLOR_CARD_SURFACE,
                        w, h, new Color(28, 28, 28)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                g2.setColor(new Color(255, 255, 255, 20));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(w - 110, -30, 150, 150, 0, 360);

                int chipX = 20;
                int chipY = h - 34;
                g2.setColor(new Color(250, 204, 21));
                g2.fillRoundRect(chipX, chipY, 24, 18, 4, 4);
                g2.setColor(new Color(161, 98, 7));
                g2.drawRoundRect(chipX, chipY, 24, 18, 4, 4);
                g2.drawLine(chipX + 8, chipY, chipX + 8, chipY + 18);
                g2.drawLine(chipX + 16, chipY, chipX + 16, chipY + 18);

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

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblBal = new JLabel("Available Balance");
        lblBal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBal.setForeground(new Color(233, 213, 255));

        JLabel lblOpts = new JLabel("•••");
        lblOpts.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblOpts.setForeground(Color.WHITE);
        lblOpts.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblOpts.setToolTipText("Balance Options");
        lblOpts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu balMenu = new JPopupMenu();
                JMenuItem miRefresh = new JMenuItem("Refresh Balance");
                miRefresh.addActionListener(ev -> refreshDashboardData());
                JMenuItem miHistory = new JMenuItem("View History");
                miHistory.addActionListener(ev -> navigateToHistory());
                balMenu.add(miRefresh);
                balMenu.add(miHistory);
                balMenu.show(lblOpts, 0, lblOpts.getHeight());
            }
        });

        topRow.add(lblBal, BorderLayout.WEST);
        topRow.add(lblOpts, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

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

    private JPanel createCustomerQuickActionsCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(0, 115));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Quick Actions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        header.add(lblTitle, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnRow.setOpaque(false);

        JButton btnSend = DashboardFrame.createGradientPillButton("Transfer Funds", 130, 30);
        btnSend.addActionListener(e -> navigateToTransaction());

        JButton btnHistory = new JButton("View History");
        btnHistory.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnHistory.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        btnHistory.setBackground(DashboardFrame.COLOR_PURPLE_PILL);
        btnHistory.setContentAreaFilled(false);
        btnHistory.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnHistory.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnHistory.addActionListener(e -> navigateToHistory());

        btnRow.add(btnSend);
        btnRow.add(btnHistory);
        card.add(btnRow, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMyPaymentsCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(0, 215));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        JLabel lblTitle = new JLabel("Recent Payment Activity");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        topSection.add(lblTitle);
        topSection.add(Box.createVerticalStrut(4));

        card.add(topSection, BorderLayout.NORTH);

        quickPaymentsListPanel = new JPanel();
        quickPaymentsListPanel.setLayout(new BoxLayout(quickPaymentsListPanel, BoxLayout.Y_AXIS));
        quickPaymentsListPanel.setOpaque(false);

        JPanel emptyQuick = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 20));
        emptyQuick.setOpaque(false);
        JLabel lblEmptyQuick = new JLabel("No payment activity recorded yet");
        lblEmptyQuick.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmptyQuick.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        emptyQuick.add(lblEmptyQuick);
        quickPaymentsListPanel.add(emptyQuick);

        card.add(quickPaymentsListPanel, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // RIGHT COLUMN: ADMINISTRATOR WIDGETS
    // =========================================================================

    private JPanel createAdminSurveillanceCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, DashboardFrame.COLOR_CARD_SURFACE,
                        w, h, DashboardFrame.COLOR_PURPLE_PRIMARY
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                g2.setColor(new Color(255, 255, 255, 20));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawArc(w - 110, -30, 150, 150, 0, 360);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(0, 155));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblConsole = new JLabel("Surveillance Console");
        lblConsole.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConsole.setForeground(new Color(233, 213, 255));

        JLabel lblActive = new JLabel("● AI Engine Active");
        lblActive.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblActive.setForeground(new Color(187, 247, 208));

        topRow.add(lblConsole, BorderLayout.WEST);
        topRow.add(lblActive, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        lblAdminPendingCount = new JLabel(pendingReviewCount + " Pending Reviews");
        lblAdminPendingCount.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAdminPendingCount.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Transactions awaiting manual fraud investigation");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(233, 213, 255));

        center.add(lblAdminPendingCount);
        center.add(Box.createVerticalStrut(3));
        center.add(lblSub);
        center.add(Box.createVerticalStrut(10));

        JButton btnQueue = new JButton("Open Review Queue →");
        btnQueue.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnQueue.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        btnQueue.setBackground(Color.WHITE);
        btnQueue.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnQueue.addActionListener(e -> navigateToPendingReviews());
        center.add(btnQueue);

        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAdminQuickLinksCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 8));
        card.setPreferredSize(new Dimension(0, 115));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("Administrator Actions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        header.add(lblTitle, BorderLayout.WEST);
        card.add(header, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnRow.setOpaque(false);

        JButton btnAlerts = DashboardFrame.createGradientPillButton("Fraud Alerts", 125, 30);
        btnAlerts.addActionListener(e -> navigateToAlerts());

        JButton btnMonitoring = new JButton("Monitoring");
        btnMonitoring.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnMonitoring.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        btnMonitoring.setBackground(DashboardFrame.COLOR_PURPLE_PILL);
        btnMonitoring.setContentAreaFilled(false);
        btnMonitoring.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btnMonitoring.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMonitoring.addActionListener(e -> navigateToHistory());

        btnRow.add(btnAlerts);
        btnRow.add(btnMonitoring);
        card.add(btnRow, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAdminMetricsCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(0, 205));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 205));

        JLabel lblTitle = new JLabel("System Fraud Metrics");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        card.add(lblTitle, BorderLayout.NORTH);

        JPanel grid = new JPanel(new java.awt.GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        grid.add(createMetricBlock("Total Txns", lblMetricTotalVal, DashboardFrame.COLOR_TEXT_MAIN));
        grid.add(createMetricBlock("Approved", lblMetricApprovedVal, DashboardFrame.COLOR_STATUS_APPROVED));
        grid.add(createMetricBlock("Declined", lblMetricDeclinedVal, DashboardFrame.COLOR_STATUS_DECLINED));
        grid.add(createMetricBlock("Blocked", lblMetricBlockedVal, DashboardFrame.COLOR_STATUS_BLOCKED));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMetricBlock(String label, JLabel valLabel, Color col) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardFrame.COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        valLabel.setForeground(col);

        p.add(lbl);
        p.add(Box.createVerticalStrut(2));
        p.add(valLabel);
        return p;
    }

    // =========================================================================
    // PROFILE SCREEN
    // =========================================================================

    private JPanel createProfileScreen() {
        if (currentUser == null) {
            return createPlaceholderScreen("User Profile", "Not authenticated", "Please sign in to view profile details.");
        }
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        JLabel lblTitle = new JLabel(isAdmin ? "Administrator Profile & Security" : "Customer Profile & Security Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(18));

        card.add(createProfileInfoRow("Username", currentUser.username()));
        card.add(createProfileInfoRow("Email Address", currentUser.email()));
        card.add(createProfileInfoRow("Assigned Role", currentUser.role()));
        card.add(createProfileInfoRow("Primary Account", currentUser.accountNumber()));
        card.add(createProfileInfoRow("Account Status", "Active"));
        card.add(createProfileInfoRow("Fraud Protection", "Real-Time AI Guard Active"));
        card.add(createProfileInfoRow("Financial Locking", "Pessimistic Concurrency Active"));

        card.add(Box.createVerticalStrut(20));
        JButton btnBack = DashboardFrame.createGradientPillButton("Return to Dashboard", 200, 32);
        btnBack.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnBack.addActionListener(e -> navigateToDashboard());
        card.add(btnBack);

        panel.add(card);
        return panel;
    }

    private JPanel createPlaceholderScreen(String title, String subtitle, String statusNote) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 44, 36, 44));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblNote = new JLabel(statusNote);
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblNote.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        lblNote.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(8));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(14));
        card.add(lblNote);

        panel.add(card);
        return panel;
    }

    private JPanel createProfileInfoRow(String key, String val) {
        JPanel r = new JPanel(new BorderLayout(14, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(380, 26));

        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        k.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        JLabel v = new JLabel(val != null ? val : "N/A");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    // =========================================================================
    // NOTIFICATIONS & SUPPORT DIALOGS
    // =========================================================================

    public JPanel buildNotificationsContent(JDialog dialog) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel lblTitle = new JLabel("🔔 SafePay Notifications");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        root.add(lblTitle);
        root.add(Box.createVerticalStrut(12));

        if (isAdmin) {
            JPanel alertBox = new JPanel(new BorderLayout(8, 0));
            alertBox.setBackground(new Color(248, 250, 252));
            alertBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DashboardFrame.COLOR_BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            JLabel lblAlert = new JLabel(pendingReviewCount > 0
                    ? "⏳ " + pendingReviewCount + " transactions awaiting manual fraud review."
                    : "✓ No pending fraud reviews. Surveillance active.");
            lblAlert.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblAlert.setForeground(pendingReviewCount > 0 ? DashboardFrame.COLOR_PURPLE_PRIMARY : DashboardFrame.COLOR_STATUS_APPROVED);
            alertBox.add(lblAlert, BorderLayout.CENTER);

            if (pendingReviewCount > 0) {
                JButton btnOpen = new JButton("Open Queue →");
                btnOpen.setFont(new Font("Segoe UI", Font.BOLD, 11));
                btnOpen.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
                btnOpen.setContentAreaFilled(false);
                btnOpen.setBorderPainted(false);
                btnOpen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnOpen.addActionListener(e -> {
                    if (dialog != null) dialog.dispose();
                    navigateToPendingReviews();
                });
                alertBox.add(btnOpen, BorderLayout.EAST);
            }
            root.add(alertBox);
        } else {
            JPanel infoBox = new JPanel(new BorderLayout(8, 0));
            infoBox.setBackground(new Color(248, 250, 252));
            infoBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(DashboardFrame.COLOR_BORDER_SUBTLE, 1),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));

            JLabel lblInfo = new JLabel("🛡 Your account is actively monitored by SafePay AI Fraud Detection.");
            lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblInfo.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
            infoBox.add(lblInfo, BorderLayout.CENTER);

            JButton btnHistory = new JButton("View History →");
            btnHistory.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnHistory.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
            btnHistory.setContentAreaFilled(false);
            btnHistory.setBorderPainted(false);
            btnHistory.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnHistory.addActionListener(e -> {
                if (dialog != null) dialog.dispose();
                navigateToHistory();
            });
            infoBox.add(btnHistory, BorderLayout.EAST);
            root.add(infoBox);
        }

        root.add(Box.createVerticalStrut(14));
        JButton btnClose = new JButton("Dismiss");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClose.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.addActionListener(e -> {
            if (dialog != null) dialog.dispose();
        });
        root.add(btnClose);

        return root;
    }

    public void showNotifications() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Notifications", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 220);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.add(buildNotificationsContent(dialog), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    public JPanel buildSupportContent(JDialog dialog) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblTitle = new JLabel("SafePay Help & Support Center");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        root.add(lblTitle);

        JLabel lblSub = new JLabel("Security guidance and transaction assistance");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        root.add(Box.createVerticalStrut(4));
        root.add(lblSub);
        root.add(Box.createVerticalStrut(16));

        root.add(createSupportSection(
                "🛡 Real-Time Fraud Prevention",
                "Every transaction is evaluated in real time by the SafePay machine learning model and rule engine. Suspicious patterns are automatically held for review."
        ));
        root.add(Box.createVerticalStrut(10));

        root.add(createSupportSection(
                "⏳ Pending Fraud Review",
                "Transactions in PENDING_REVIEW status require administrator verification before funds are settled. Account balances are never altered while a transaction is pending."
        ));
        root.add(Box.createVerticalStrut(10));

        root.add(createSupportSection(
                "🔒 Account & Transfer Safety",
                "Transfers are processed atomically with pessimistic locking to prevent duplicate debiting. If you notice unexpected activity, contact an administrator immediately."
        ));
        root.add(Box.createVerticalStrut(16));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnClose.setForeground(DashboardFrame.COLOR_PURPLE_PRIMARY);
        btnClose.setBackground(DashboardFrame.COLOR_PURPLE_PILL);
        btnClose.setContentAreaFilled(false);
        btnClose.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.addActionListener(e -> {
            if (dialog != null) dialog.dispose();
        });
        root.add(btnClose);

        return root;
    }

    private JPanel createSupportSection(String heading, String description) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DashboardFrame.COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblH = new JLabel(heading);
        lblH.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblH.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

        JLabel lblD = new JLabel("<html><body style='width: 320px;'>" + description + "</body></html>");
        lblD.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblD.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        p.add(lblH);
        p.add(Box.createVerticalStrut(4));
        p.add(lblD);
        return p;
    }

    public void showSupportDialog() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "SafePay Help & Support", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(460, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.add(buildSupportContent(dialog), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // =========================================================================
    // DATA REFRESH & SESSION EXPIRATION
    // =========================================================================

    public void refreshDashboardData() {
        if (btnRefresh != null && !btnRefresh.isEnabled()) return;
        if (btnRefresh != null) btnRefresh.setEnabled(false);
        setStatusMessage("Fetching real-time account data from backend...");

        new SwingWorker<DashboardDataBundle, Void>() {
            @Override
            protected DashboardDataBundle doInBackground() throws Exception {
                DashboardResponse dash = apiClient.getDashboardData();
                List<TransactionSummaryDto> pending = null;
                if (isAdmin) {
                    try {
                        pending = apiClient.getPendingTransactions();
                    } catch (Exception ignored) {}
                }
                return new DashboardDataBundle(dash, pending);
            }

            @Override
            protected void done() {
                try {
                    DashboardDataBundle bundle = get();
                    if (bundle.dash != null && bundle.dash.success()) {
                        updateDashboardUi(bundle.dash);
                        setStatusMessage("Account " + bundle.dash.accountNumber() + " synchronized successfully.");
                        setDatabaseStatus(true, "Active (" + bundle.dash.recentTransactions().size() + " Tx)");
                    } else {
                        String msg = bundle.dash != null ? bundle.dash.message() : "Failed to load dashboard data.";
                        setStatusMessage("Backend response: " + msg);
                    }
                    if (isAdmin && bundle.pending != null) {
                        setPendingReviewCount(bundle.pending.size());
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiClientException apiEx && apiEx.getStatusCode() == 401) {
                        sessionExpired = true;
                        setStatusMessage("Session expired. Please log in again.");
                        setDatabaseStatus(false, "Session Expired (401)");
                        handleSessionExpired();
                        return;
                    }
                    setStatusMessage("Dashboard synchronization notice: " + cause.getMessage());
                    setDatabaseStatus(false, "Connection Error");
                } finally {
                    if (btnRefresh != null) btnRefresh.setEnabled(true);
                }
            }
        }.execute();
    }

    private record DashboardDataBundle(DashboardResponse dash, List<TransactionSummaryDto> pending) {}

    
    private javax.swing.Timer balanceAnimTimer;
    private void animateBalanceCountUp(BigDecimal targetVal) {
        if (lblBalanceAmount == null || targetVal == null) return;
        if (balanceAnimTimer != null && balanceAnimTimer.isRunning()) {
            balanceAnimTimer.stop();
        }
        final double target = targetVal.doubleValue();
        final int steps = 24;
        final int[] step = {0};
        balanceAnimTimer = new javax.swing.Timer(18, ev -> {
            step[0]++;
            double progress = (double) step[0] / steps;
            // Ease-out quad interpolation
            double ease = 1.0 - Math.pow(1.0 - progress, 2);
            double current = target * ease;
            lblBalanceAmount.setText(DashboardFrame.formatCurrency(BigDecimal.valueOf(current)) + " INR");
            if (step[0] >= steps) {
                lblBalanceAmount.setText(DashboardFrame.formatCurrency(targetVal) + " INR");
                balanceAnimTimer.stop();
            }
        });
        balanceAnimTimer.start();
    }

    public void updateDashboardUi(DashboardResponse data) {
        if (data == null) return;
        this.currentDashboardData = data;

        BigDecimal bal = data.balance() != null ? data.balance() : BigDecimal.ZERO;
        if (lblBalanceAmount != null) {
            lblBalanceAmount.setText(DashboardFrame.formatCurrency(bal) + " INR");
        }

        String statusStr = data.accountStatus() != null ? data.accountStatus() : "ACTIVE";
        String accNum = data.accountNumber() != null ? data.accountNumber() : "ACC-NOT-ASSIGNED";
        if (lblAccountStatusPill != null) {
            lblAccountStatusPill.setText(statusStr + " • " + accNum);
        }

        lblMetricTotalVal.setText(String.valueOf(data.totalTransactions()));
        lblMetricApprovedVal.setText(String.valueOf(data.approvedTransactions()));
        lblMetricDeclinedVal.setText(String.valueOf(data.declinedTransactions()));
        lblMetricBlockedVal.setText(String.valueOf(data.blockedTransactions()));

        List<TransactionSummaryDto> filtered = filterTransactionsByTimeframe(data.recentTransactions(), selectedTimeframe);
        if (transactionChart != null) {
            transactionChart.setTransactions(filtered);
        }
        if (tableModel != null) {
            tableModel.setTransactions(filtered);
        }
        updateQuickPaymentsList(data.recentTransactions());
    }

    public void setPendingReviewCount(int count) {
        this.pendingReviewCount = count;
        if (lblAdminPendingCount != null) {
            lblAdminPendingCount.setText(count + (count == 1 ? " Transaction Awaiting Review" : " Transactions Awaiting Review"));
        }
        if (btnNavPendingReviews != null) {
            btnNavPendingReviews.setBadge(count > 0 ? String.format("%02d", count) : null);
        }
        if (btnBell != null) {
            btnBell.repaint();
        }
    }

    public int getPendingReviewCount() {
        return pendingReviewCount;
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
            if (transactionChart != null) {
                transactionChart.setTransactions(filtered);
            }
            if (tableModel != null) {
                tableModel.setTransactions(filtered);
            }
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

    private void updateQuickPaymentsList(List<TransactionSummaryDto> list) {
        if (quickPaymentsListPanel == null) return;
        quickPaymentsListPanel.removeAll();
        if (list == null || list.isEmpty()) {
            JPanel emptyQuick = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 20));
            emptyQuick.setOpaque(false);
            JLabel lblEmptyQuick = new JLabel("No payment activity recorded yet");
            lblEmptyQuick.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblEmptyQuick.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
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
            String amt = DashboardFrame.formatCurrency(tx.amount());
            String initial = name.length() > 0 ? name.substring(0, 1) : "T";

            Color c = DashboardFrame.COLOR_PURPLE_PRIMARY;
            if ("APPROVED".equalsIgnoreCase(tx.status())) c = DashboardFrame.COLOR_STATUS_APPROVED;
            else if ("BLOCKED".equalsIgnoreCase(tx.status())) c = DashboardFrame.COLOR_STATUS_BLOCKED;
            else if ("DECLINED".equalsIgnoreCase(tx.status())) c = DashboardFrame.COLOR_STATUS_DECLINED;

            quickPaymentsListPanel.add(createPaymentRow(name, date, amt, initial, c));
            if (i < count - 1) {
                quickPaymentsListPanel.add(Box.createVerticalStrut(5));
            }
        }
        quickPaymentsListPanel.revalidate();
        quickPaymentsListPanel.repaint();
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

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel lblN = new JLabel(name);
        lblN.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblN.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

        JLabel lblD = new JLabel(date);
        lblD.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblD.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

        text.add(lblN);
        text.add(lblD);
        row.add(text, BorderLayout.CENTER);

        JLabel lblAmt = new JLabel(amount);
        lblAmt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblAmt.setForeground(DashboardFrame.COLOR_TEXT_MAIN);
        row.add(lblAmt, BorderLayout.EAST);

        return row;
    }

    // =========================================================================
    // LOGOUT & SESSION EXPIRATION
    // =========================================================================

    public void performLogout() {
        if (!confirmLogout()) {
            return;
        }
        executeLogout();
    }

    protected boolean confirmLogout() {
        if (autoConfirmLogoutForTesting || GraphicsEnvironment.isHeadless()) {
            return true;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out of SafePay?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return confirm == JOptionPane.YES_OPTION;
    }

    public void executeLogout() {
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
                if (onLogout != null) {
                    onLogout.run();
                }
            }
        }.execute();
    }

    void handleSessionExpired() {
        sessionExpired = true;
        setStatusMessage("Session expired. Please log in again.");
        try {
            apiClient.logout();
        } catch (Exception ignored) {}
        if (onSessionExpired != null) {
            onSessionExpired.run();
        }
    }

    public boolean isSessionExpired() {
        return sessionExpired;
    }

    public void setAutoConfirmLogoutForTesting(boolean autoConfirm) {
        this.autoConfirmLogoutForTesting = autoConfirm;
    }

    public void setOnSessionExpired(Runnable onSessionExpired) {
        this.onSessionExpired = onSessionExpired;
    }

    // =========================================================================
    // NAVIGATION METHODS
    // =========================================================================

    public void navigateToDashboard() {
        switchScreen(CARD_DASHBOARD, isAdmin ? "Admin Dashboard" : "Dashboard", btnNavDashboard);
    }

    public void navigateToTransaction() {
        switchScreen(CARD_TRANSACTION, "Payments & Transfers", btnNavTransaction);
    }

    public void navigateToHistory() {
        switchScreen(CARD_HISTORY, isAdmin ? "Transaction Monitoring" : "Transaction History", btnNavHistory);
    }

    public void navigateToAlerts() {
        if (!isAdmin) {
            setStatusMessage("Access Denied: Administrator privileges required.");
            return;
        }
        if (adminMonitoringPanel != null) {
            adminMonitoringPanel.showAlertsView();
        }
        switchScreen(CARD_ALERTS, "Fraud Surveillance Alerts", btnNavAlerts);
    }

    public void navigateToPendingReviews() {
        if (!isAdmin) {
            setStatusMessage("Access Denied: Administrator privileges required.");
            return;
        }
        if (adminMonitoringPanel != null) {
            adminMonitoringPanel.showPendingView();
        }
        switchScreen(CARD_ALERTS, "Pending Fraud Reviews", btnNavPendingReviews);
    }

    public void navigateToProfile() {
        switchScreen(CARD_PROFILE, isAdmin ? "Admin Profile" : "Profile & Security", btnNavProfile);
    }

    // =========================================================================
    // STATUS BAR
    // =========================================================================

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(DashboardFrame.COLOR_SURFACE_DARK);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, DashboardFrame.COLOR_BORDER_SUBTLE),
                BorderFactory.createEmptyBorder(6, 20, 6, 20)
        ));

        lblStatusMessage = new JLabel("System Ready");
        lblStatusMessage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatusMessage.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);
        bar.add(lblStatusMessage, BorderLayout.WEST);

        lblDbStatus = new JLabel("● Database Active");
        lblDbStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDbStatus.setForeground(DashboardFrame.COLOR_STATUS_APPROVED);
        bar.add(lblDbStatus, BorderLayout.EAST);

        return bar;
    }

    public void setStatusMessage(String message) {
        if (lblStatusMessage != null) {
            lblStatusMessage.setText(message);
        }
    }

    public String getStatusMessage() {
        return lblStatusMessage != null ? lblStatusMessage.getText() : "";
    }

    public void setDatabaseStatus(boolean connected, String detail) {
        if (lblDbStatus != null) {
            lblDbStatus.setText((connected ? "● " : "○ ") + detail);
            lblDbStatus.setForeground(connected ? DashboardFrame.COLOR_STATUS_APPROVED : DashboardFrame.COLOR_STATUS_BLOCKED);
        }
    }

    // =========================================================================
    // ACCESSORS FOR TESTING & INTEGRATION
    // =========================================================================

    public List<FinoraNavButton> getNavButtons() {
        return Collections.unmodifiableList(navButtons);
    }

    public FinoraNavButton getNavButton(String text) {
        for (FinoraNavButton b : navButtons) {
            if (b.getText().equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public FinoraNavButton getBtnNavDashboard() { return btnNavDashboard; }
    public FinoraNavButton getBtnNavTransaction() { return btnNavTransaction; }
    public FinoraNavButton getBtnNavHistory() { return btnNavHistory; }
    public FinoraNavButton getBtnNavAlerts() { return btnNavAlerts; }
    public FinoraNavButton getBtnNavPendingReviews() { return btnNavPendingReviews; }
    public FinoraNavButton getBtnNavProfile() { return btnNavProfile; }
    public FinoraNavButton getBtnNavSupport() { return btnNavSupport; }
    public JButton getBtnLogout() { return btnLogout; }
    public JButton getBtnBell() { return btnBell; }
    public JButton getBtnRefresh() { return btnRefresh; }

    public String getCurrentCard() { return currentCard; }
    public String getHeaderTitleText() { return lblHeaderTitle != null ? lblHeaderTitle.getText() : ""; }

    public TransactionPanel getTransactionPanel() { return transactionPanel; }
    public TransactionHistoryPanel getHistoryPanel() { return historyPanel; }
    public AdminMonitoringPanel getAdminMonitoringPanel() { return adminMonitoringPanel; }
    public JPanel getCardsPanel() { return cardsPanel; }

    public JLabel getBalanceLabel() { return lblBalanceAmount; }
    public JLabel getAccountStatusLabel() { return lblAccountStatusPill; }

    // =========================================================================
    // HELPER CLASSES
    // =========================================================================

    public static class FinoraNavButton extends JButton {
        private final String iconType;
        private boolean active;
        private String badgeText;

        public FinoraNavButton(String text, String iconType, boolean active, String badgeText) {
            super(text);
            this.iconType = iconType;
            this.active = active;
            this.badgeText = badgeText;

            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
            setForeground(active ? DashboardFrame.COLOR_PURPLE_PRIMARY : DashboardFrame.COLOR_TEXT_SUBTLE);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(7, 36, 7, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(195, 34));
            setPreferredSize(new Dimension(195, 34));
        }

        public void setActive(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 12));
            setForeground(active ? DashboardFrame.COLOR_PURPLE_PRIMARY : DashboardFrame.COLOR_TEXT_SUBTLE);
            repaint();
        }

        public void setBadge(String badgeText) {
            this.badgeText = badgeText;
            repaint();
        }

        public String getBadge() {
            return badgeText;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (active) {
                g2.setColor(DashboardFrame.COLOR_PURPLE_PILL);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }

            g2.setColor(active ? DashboardFrame.COLOR_PURPLE_PRIMARY : DashboardFrame.COLOR_TEXT_SUBTLE);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            paintNavVectorIcon(g2, 12, h / 2 - 8, iconType);

            if (badgeText != null) {
                int bw = 24;
                int bh = 16;
                int bx = w - bw - 8;
                int by = (h - bh) / 2;
                g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
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

            g2.setColor(DashboardFrame.COLOR_PURPLE_PRIMARY);
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.drawRoundRect(2, 4, w - 8, 5, 3, 3);
            g2.drawRoundRect(2, 12, w - 8, 5, 3, 3);
            g2.drawRoundRect(2, 20, w - 8, 5, 3, 3);

            g2.dispose();
        }
    }

    private static class RealTransactionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
            JPanel panel = new JPanel(new BorderLayout(8, 0));
            panel.setOpaque(true);
            panel.setBackground(isSel ? new Color(36, 36, 36) : (r % 2 == 0 ? DashboardFrame.COLOR_CARD_SURFACE : new Color(26, 26, 26)));
            panel.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

            String fullStr = val != null ? val.toString() : "Transaction";
            String title = fullStr;
            String sub = "SafePay Transfer";
            String badgeLetter = fullStr.length() > 0 ? fullStr.substring(0, 1).toUpperCase() : "T";
            Color badgeCol = DashboardFrame.COLOR_PURPLE_PRIMARY;

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
            lblT.setForeground(DashboardFrame.COLOR_TEXT_MAIN);

            JLabel lblS = new JLabel(sub);
            lblS.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblS.setForeground(DashboardFrame.COLOR_TEXT_SUBTLE);

            text.add(lblT);
            text.add(lblS);
            panel.add(text, BorderLayout.CENTER);

            return panel;
        }
    }

    private static class RecentTransactionsTableModel extends AbstractTableModel {
        private final String[] columns = {"Account / Customer", "Type", "Amount", "Date"};
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
                case 0 -> {
                    String acc = tx.accountNumber() != null ? tx.accountNumber() : "Main Account";
                    String user = tx.customerUsername() != null ? " (" + tx.customerUsername() + ")" : "";
                    yield acc + user;
                }
                case 1 -> tx.transactionType() != null ? tx.transactionType() : "PAYMENT";
                case 2 -> DashboardFrame.formatCurrency(tx.amount());
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
            setForeground(active ? Color.WHITE : DashboardFrame.COLOR_TEXT_SUBTLE);
            setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    DashboardPanel.this.setTimeframe(getText());
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 10));
            setForeground(active ? Color.WHITE : DashboardFrame.COLOR_TEXT_SUBTLE);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 24, 27));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
