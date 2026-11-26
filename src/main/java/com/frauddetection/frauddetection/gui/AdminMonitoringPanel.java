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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.frauddetection.frauddetection.client.ApiClientException;
import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AdminAlertDto;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Native Java Swing and Java2D Administrator Surveillance & Fraud Review Monitoring screen.
 *
 * Implements:
 * - Real administrator alert monitoring consuming GET /api/admin/alerts
 * - Dedicated Pending Fraud Review view consuming GET /api/admin/pending
 * - Full transaction investigation modal displaying sender/receiver info and linked alert context
 * - Actionable Approve & Reject decision workflows submitting to backend review endpoints
 * - Duplicate submission prevention and background execution off the EDT
 * - Server-side ADMIN role enforcement (handles HTTP 401 & 403 access denial)
 * - Dynamic summary metric cards for pending reviews, total alerts, critical severities, and open investigations
 * - Finora / SafePay purple & lavender design system
 */
public class AdminMonitoringPanel extends JPanel {

    public static final String STATE_LOADING = "STATE_LOADING";
    public static final String STATE_CONTENT = "STATE_CONTENT";
    public static final String STATE_EMPTY = "STATE_EMPTY";
    public static final String STATE_UNAUTHORIZED = "STATE_UNAUTHORIZED";
    public static final String STATE_ERROR = "STATE_ERROR";

    // Design Tokens matching SafePay Dashboard
    public static final Color COLOR_PURPLE_PRIMARY = new Color(216, 255, 0); // #D8FF00 Electric Lime
    public static final Color COLOR_PURPLE_LIGHT = new Color(216, 255, 0, 100);
    public static final Color COLOR_PURPLE_PILL = new Color(216, 255, 0, 35);
    public static final Color COLOR_HERO_BG = new Color(22, 22, 22);
    public static final Color COLOR_BORDER_SUBTLE = new Color(36, 36, 36);
    public static final Color COLOR_TEXT_MAIN = Color.WHITE;
    public static final Color COLOR_TEXT_SUBTLE = new Color(156, 163, 175);
    public static final Color COLOR_INPUT_BG = new Color(17, 17, 17);

    public static final Color COLOR_SEVERITY_HIGH = new Color(220, 38, 38);   // #DC2626
    public static final Color COLOR_SEVERITY_MED = new Color(217, 119, 6);    // #D97706
    public static final Color COLOR_SEVERITY_LOW = new Color(37, 99, 235);    // #2563EB

    public static final Color COLOR_STATUS_OPEN = new Color(147, 51, 234);    // #9333EA
    public static final Color COLOR_STATUS_REVIEWED = new Color(2, 132, 199);  // #0284C7
    public static final Color COLOR_STATUS_RESOLVED = new Color(22, 163, 74); // #16A34A
    public static final Color COLOR_STATUS_PENDING = new Color(124, 58, 237);  // #7C3AED

    private final SafePayApiClient apiClient;
    private final Runnable onNavigateToDashboard;

    private final CardLayout stateCardLayout = new CardLayout();
    private final JPanel stateContainer = new JPanel(stateCardLayout);
    private String currentState = STATE_LOADING;

    // View Switcher: Pending Review vs Surveillance Alerts
    private JButton btnTabPending;
    private JButton btnTabAlerts;
    private boolean isPendingViewActive = false; // alerts active by default for compatibility

    private final CardLayout tableViewsCardLayout = new CardLayout();
    private final JPanel tableViewsContainer = new JPanel(tableViewsCardLayout);

    private final CardLayout filterControlsLayout = new CardLayout();
    private final JPanel filterControlsContainer = new JPanel(filterControlsLayout);

    // Alerts Filter Controls
    private JTextField txtSearch;
    private JComboBox<String> cmbSeverityFilter;
    private JComboBox<String> cmbStatusFilter;
    private JButton btnApplyFilters;
    private JButton btnResetFilters;
    private JButton btnRefresh;

    // Pending Reviews Filter Controls
    private JTextField txtPendingSearch;

    // Metrics Labels
    private JLabel lblPendingReviewCount;
    private JLabel lblTotalAlerts;
    private JLabel lblHighSeverityCount;
    private JLabel lblMedSeverityCount;
    private JLabel lblOpenCount;

    // Alerts Table & Data
    private JTable alertsTable;
    private AdminAlertsTableModel tableModel;
    private final List<AdminAlertDto> rawAlertsList = new ArrayList<>();
    private final List<AdminAlertDto> displayedAlertsList = new ArrayList<>();

    // Pending Reviews Table & Data
    private JTable pendingTable;
    private PendingTransactionsTableModel pendingTableModel;
    private final List<TransactionSummaryDto> rawPendingList = new ArrayList<>();
    private final List<TransactionSummaryDto> displayedPendingList = new ArrayList<>();

    // Error & Unauthorized Labels
    private JLabel lblUnauthorizedMessage;
    private JLabel lblErrorMessage;

    private boolean isLoading = false;
    private boolean autoConfirmForTesting = false;

    public AdminMonitoringPanel(SafePayApiClient apiClient, Runnable onNavigateToDashboard) {
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.onNavigateToDashboard = onNavigateToDashboard;

        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();
    }

    public AdminMonitoringPanel(SafePayApiClient apiClient) {
        this(apiClient, null);
    }

    private void initUI() {
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 18, 18, 18));

        // 1. Header Hero Card
        mainContent.add(createHeaderHeroCard());
        mainContent.add(Box.createVerticalStrut(12));

        // 2. Metrics Summary Card
        mainContent.add(createMetricsSummaryCard());
        mainContent.add(Box.createVerticalStrut(12));

        // 3. Filter & View Switcher Control Bar
        mainContent.add(createFilterCard());
        mainContent.add(Box.createVerticalStrut(12));

        // 4. Multi-State Container
        stateContainer.setOpaque(false);
        stateContainer.add(createTableCard(), STATE_CONTENT);
        stateContainer.add(createLoadingCard(), STATE_LOADING);
        stateContainer.add(createEmptyCard(), STATE_EMPTY);
        stateContainer.add(createUnauthorizedCard(), STATE_UNAUTHORIZED);
        stateContainer.add(createErrorCard(), STATE_ERROR);

        mainContent.add(stateContainer);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void showState(String state) {
        this.currentState = state;
        stateCardLayout.show(stateContainer, state);
    }

    public String getCurrentState() {
        return currentState;
    }

    // =========================================================================
    // UI CARDS & PANELS
    // =========================================================================

    private JPanel createHeaderHeroCard() {
        JPanel hero = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, COLOR_HERO_BG,
                        w, h, new Color(243, 232, 255, 140)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 18, 18);

                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 18, 18);
                g2.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JPanel textGroup = new JPanel();
        textGroup.setLayout(new BoxLayout(textGroup, BoxLayout.Y_AXIS));
        textGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("Fraud Surveillance & Admin Alert Feed");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Real-time behavioral anomaly detection alerts, velocity triggers, and risk surveillance.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);

        textGroup.add(lblTitle);
        textGroup.add(Box.createVerticalStrut(4));
        textGroup.add(lblSub);

        hero.add(textGroup, BorderLayout.WEST);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        btnRefresh = new JButton("⟳ Refresh Feed") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(22, 22, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRefresh.setForeground(COLOR_PURPLE_PRIMARY);
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadAllData());
        actions.add(btnRefresh);

        if (onNavigateToDashboard != null) {
            JButton btnBack = new JButton("← Dashboard");
            btnBack.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnBack.setForeground(COLOR_TEXT_SUBTLE);
            btnBack.setContentAreaFilled(false);
            btnBack.setBorderPainted(false);
            btnBack.setFocusPainted(false);
            btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnBack.addActionListener(e -> onNavigateToDashboard.run());
            actions.add(btnBack);
        }

        hero.add(actions, BorderLayout.EAST);
        return hero;
    }

    private JPanel createMetricsSummaryCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 6));

        lblPendingReviewCount = new JLabel("0");
        lblTotalAlerts = new JLabel("0");
        lblHighSeverityCount = new JLabel("0");
        lblMedSeverityCount = new JLabel("0");
        lblOpenCount = new JLabel("0");

        card.add(createMetricItem("Pending Reviews", lblPendingReviewCount, COLOR_PURPLE_PRIMARY));
        card.add(createDivider());
        card.add(createMetricItem("Total Triggered Alerts", lblTotalAlerts, COLOR_TEXT_MAIN));
        card.add(createDivider());
        card.add(createMetricItem("High Severity (Critical)", lblHighSeverityCount, COLOR_SEVERITY_HIGH));
        card.add(createDivider());
        card.add(createMetricItem("Medium Severity", lblMedSeverityCount, COLOR_SEVERITY_MED));
        card.add(createDivider());
        card.add(createMetricItem("Open Investigations", lblOpenCount, COLOR_STATUS_OPEN));

        return card;
    }

    private JPanel createMetricItem(String label, JLabel valueLabel, Color valueColor) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(COLOR_TEXT_SUBTLE);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(valueColor);

        p.add(lbl);
        p.add(Box.createVerticalStrut(2));
        p.add(valueLabel);
        return p;
    }

    private Component createDivider() {
        JPanel div = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(COLOR_BORDER_SUBTLE);
                g.drawLine(0, 4, 0, getHeight() - 4);
            }
        };
        div.setPreferredSize(new Dimension(1, 32));
        div.setOpaque(false);
        return div;
    }

    private JPanel createFilterCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(12, 8));

        // Row 1: View Switcher Tabs
        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tabRow.setOpaque(false);

        btnTabAlerts = createViewTabButton("🚨 Surveillance Alerts (0)", true);
        btnTabPending = createViewTabButton("⏳ Pending Reviews (0)", false);

        btnTabAlerts.addActionListener(e -> setActiveView(false));
        btnTabPending.addActionListener(e -> setActiveView(true));

        tabRow.add(btnTabAlerts);
        tabRow.add(btnTabPending);
        card.add(tabRow, BorderLayout.NORTH);

        // Row 2: Filter Controls Container
        filterControlsContainer.setOpaque(false);
        filterControlsContainer.add(createAlertsFilterRow(), "FILTERS_ALERTS");
        filterControlsContainer.add(createPendingFilterRow(), "FILTERS_PENDING");

        card.add(filterControlsContainer, BorderLayout.CENTER);
        return card;
    }

    private JPanel createAlertsFilterRow() {
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtSearch.setBackground(COLOR_INPUT_BG);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtSearch.setPreferredSize(new Dimension(190, 32));
        txtSearch.setToolTipText("Filter by Alert ID, Txn ID, or Anomaly Type");
        txtSearch.addActionListener(e -> applyFilters());
        filterRow.add(txtSearch);

        // Severity Filter
        String[] severityOptions = {"All Severities", "HIGH", "MEDIUM", "LOW"};
        cmbSeverityFilter = new JComboBox<>(severityOptions);
        styleComboBox(cmbSeverityFilter);
        cmbSeverityFilter.addActionListener(e -> applyFilters());
        filterRow.add(cmbSeverityFilter);

        // Status Filter
        String[] statusOptions = {"All Statuses", "OPEN", "REVIEWED", "RESOLVED"};
        cmbStatusFilter = new JComboBox<>(statusOptions);
        styleComboBox(cmbStatusFilter);
        cmbStatusFilter.addActionListener(e -> applyFilters());
        filterRow.add(cmbStatusFilter);

        // Action Buttons
        btnApplyFilters = new JButton("Filter") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnApplyFilters.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnApplyFilters.setForeground(Color.WHITE);
        btnApplyFilters.setPreferredSize(new Dimension(75, 30));
        btnApplyFilters.setContentAreaFilled(false);
        btnApplyFilters.setBorderPainted(false);
        btnApplyFilters.setFocusPainted(false);
        btnApplyFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnApplyFilters.addActionListener(e -> applyFilters());
        filterRow.add(btnApplyFilters);

        btnResetFilters = new JButton("Reset") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(COLOR_TEXT_SUBTLE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnResetFilters.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnResetFilters.setPreferredSize(new Dimension(65, 30));
        btnResetFilters.setContentAreaFilled(false);
        btnResetFilters.setBorderPainted(false);
        btnResetFilters.setFocusPainted(false);
        btnResetFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResetFilters.addActionListener(e -> {
            txtSearch.setText("");
            cmbSeverityFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            applyFilters();
        });
        filterRow.add(btnResetFilters);

        return filterRow;
    }

    private JPanel createPendingFilterRow() {
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        txtPendingSearch = new JTextField();
        txtPendingSearch.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtPendingSearch.setBackground(COLOR_INPUT_BG);
        txtPendingSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtPendingSearch.setPreferredSize(new Dimension(240, 32));
        txtPendingSearch.setToolTipText("Filter by Txn ID, Account, Username, or Type");
        txtPendingSearch.addActionListener(e -> updatePendingTable());
        filterRow.add(txtPendingSearch);

        JButton btnSearchPending = new JButton("Search") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnSearchPending.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSearchPending.setForeground(Color.WHITE);
        btnSearchPending.setPreferredSize(new Dimension(75, 30));
        btnSearchPending.setContentAreaFilled(false);
        btnSearchPending.setBorderPainted(false);
        btnSearchPending.setFocusPainted(false);
        btnSearchPending.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSearchPending.addActionListener(e -> updatePendingTable());
        filterRow.add(btnSearchPending);

        JButton btnResetPending = new JButton("Reset") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(COLOR_TEXT_SUBTLE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnResetPending.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnResetPending.setPreferredSize(new Dimension(65, 30));
        btnResetPending.setContentAreaFilled(false);
        btnResetPending.setBorderPainted(false);
        btnResetPending.setFocusPainted(false);
        btnResetPending.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResetPending.addActionListener(e -> {
            txtPendingSearch.setText("");
            updatePendingTable();
        });
        filterRow.add(btnResetPending);

        return filterRow;
    }

    private JButton createViewTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(active ? Color.WHITE : COLOR_TEXT_SUBTLE);
        btn.setBackground(active ? COLOR_PURPLE_PRIMARY : COLOR_INPUT_BG);
        btn.setPreferredSize(new Dimension(190, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(btn.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                if (btn.getBackground().equals(COLOR_INPUT_BG)) {
                    g2.setColor(COLOR_BORDER_SUBTLE);
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
        return btn;
    }

    public void setActiveView(boolean showPending) {
        this.isPendingViewActive = showPending;
        if (showPending) {
            tableViewsCardLayout.show(tableViewsContainer, "VIEW_PENDING");
            filterControlsLayout.show(filterControlsContainer, "FILTERS_PENDING");
            btnTabPending.setBackground(COLOR_PURPLE_PRIMARY);
            btnTabPending.setForeground(Color.WHITE);
            btnTabAlerts.setBackground(COLOR_INPUT_BG);
            btnTabAlerts.setForeground(COLOR_TEXT_SUBTLE);
            updatePendingTable();
        } else {
            tableViewsCardLayout.show(tableViewsContainer, "VIEW_ALERTS");
            filterControlsLayout.show(filterControlsContainer, "FILTERS_ALERTS");
            btnTabAlerts.setBackground(COLOR_PURPLE_PRIMARY);
            btnTabAlerts.setForeground(Color.WHITE);
            btnTabPending.setBackground(COLOR_INPUT_BG);
            btnTabPending.setForeground(COLOR_TEXT_SUBTLE);
            applyFilters();
        }
        btnTabPending.repaint();
        btnTabAlerts.repaint();
    }

    private JPanel createTableCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        // Center Views (CardLayout container switching between Alerts and Pending Reviews)
        tableViewsContainer.setOpaque(false);
        tableViewsContainer.add(createAlertsViewPanel(), "VIEW_ALERTS");
        tableViewsContainer.add(createPendingViewPanel(), "VIEW_PENDING");

        card.add(tableViewsContainer, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // ALERTS TABLE
    // =========================================================================

    private JPanel createAlertsViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel lblSection = new JLabel("Surveillance Alert Registry");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSection.setForeground(COLOR_TEXT_MAIN);

        JLabel lblHint = new JLabel("Double-click any alert row to view investigation and transaction details");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHint.setForeground(COLOR_TEXT_SUBTLE);

        tableHeader.add(lblSection, BorderLayout.WEST);
        tableHeader.add(lblHint, BorderLayout.EAST);
        panel.add(tableHeader, BorderLayout.NORTH);

        tableModel = new AdminAlertsTableModel();
        alertsTable = new JTable(tableModel);
        alertsTable.setRowHeight(36);
        alertsTable.setShowGrid(false);
        alertsTable.setIntercellSpacing(new Dimension(0, 0));
        alertsTable.setFillsViewportHeight(true);
        alertsTable.setBackground(Color.WHITE);
        alertsTable.setSelectionBackground(new Color(243, 232, 255));
        alertsTable.setSelectionForeground(COLOR_TEXT_MAIN);
        alertsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        alertsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader th = alertsTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setForeground(COLOR_TEXT_SUBTLE);
        th.setBackground(new Color(248, 250, 252));
        th.setPreferredSize(new Dimension(0, 32));
        th.setReorderingAllowed(false);

        alertsTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // ID
        alertsTable.getColumnModel().getColumn(1).setPreferredWidth(170); // Anomaly Type
        alertsTable.getColumnModel().getColumn(2).setPreferredWidth(90);  // Severity
        alertsTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // Status
        alertsTable.getColumnModel().getColumn(4).setPreferredWidth(140); // Created At
        alertsTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // Txn ID
        alertsTable.getColumnModel().getColumn(6).setPreferredWidth(110); // Amount
        alertsTable.getColumnModel().getColumn(7).setPreferredWidth(90);  // Txn Status

        alertsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(COLOR_PURPLE_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        alertsTable.getColumnModel().getColumn(2).setCellRenderer(new SeverityBadgeRenderer());
        alertsTable.getColumnModel().getColumn(3).setCellRenderer(new StatusBadgeRenderer());

        alertsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setForeground(COLOR_TEXT_MAIN);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });

        alertsTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String s = value != null ? value.toString() : "";
                l.setForeground("BLOCKED".equalsIgnoreCase(s) ? COLOR_SEVERITY_HIGH
                        : "DECLINED".equalsIgnoreCase(s) ? COLOR_SEVERITY_MED
                        : "PENDING_REVIEW".equalsIgnoreCase(s) ? COLOR_STATUS_PENDING
                        : new Color(22, 163, 74));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        alertsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = alertsTable.getSelectedRow();
                if (row >= 0 && row < displayedAlertsList.size() && e.getClickCount() == 2) {
                    AdminAlertDto alert = displayedAlertsList.get(row);
                    showAlertDetailDialog(alert);
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(alertsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1));
        tableScroll.setBackground(Color.WHITE);
        tableScroll.getViewport().setBackground(new Color(22, 22, 22));
        alertsTable.setBackground(new Color(22, 22, 22));
        tableScroll.setPreferredSize(new Dimension(0, 380));

        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // PENDING REVIEWS TABLE
    // =========================================================================

    private JPanel createPendingViewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        JPanel tableHeader = new JPanel(new BorderLayout());
        tableHeader.setOpaque(false);

        JLabel lblSection = new JLabel("Pending Fraud Review Queue");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSection.setForeground(COLOR_TEXT_MAIN);

        JLabel lblHint = new JLabel("Double-click row or click 'Review & Decide' to approve or reject transactions");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHint.setForeground(COLOR_TEXT_SUBTLE);

        tableHeader.add(lblSection, BorderLayout.WEST);
        tableHeader.add(lblHint, BorderLayout.EAST);
        panel.add(tableHeader, BorderLayout.NORTH);

        pendingTableModel = new PendingTransactionsTableModel();
        pendingTable = new JTable(pendingTableModel);
        pendingTable.setRowHeight(36);
        pendingTable.setShowGrid(false);
        pendingTable.setIntercellSpacing(new Dimension(0, 0));
        pendingTable.setFillsViewportHeight(true);
        pendingTable.setBackground(Color.WHITE);
        pendingTable.setSelectionBackground(new Color(243, 232, 255));
        pendingTable.setSelectionForeground(COLOR_TEXT_MAIN);
        pendingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader th = pendingTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setForeground(COLOR_TEXT_SUBTLE);
        th.setBackground(new Color(248, 250, 252));
        th.setPreferredSize(new Dimension(0, 32));
        th.setReorderingAllowed(false);

        pendingTable.getColumnModel().getColumn(0).setPreferredWidth(70);  // ID
        pendingTable.getColumnModel().getColumn(1).setPreferredWidth(170); // Account / Customer
        pendingTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Type
        pendingTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Amount
        pendingTable.getColumnModel().getColumn(4).setPreferredWidth(150); // Submission Time
        pendingTable.getColumnModel().getColumn(5).setPreferredWidth(130); // Status
        pendingTable.getColumnModel().getColumn(6).setPreferredWidth(130); // Action

        pendingTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(COLOR_PURPLE_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        pendingTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setForeground(COLOR_TEXT_MAIN);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });

        pendingTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                String status = value != null ? value.toString() : "";
                JLabel lbl = new JLabel(status, SwingConstants.CENTER) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(COLOR_PURPLE_PILL);
                        g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 8, 10, 10);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lbl.setForeground(COLOR_STATUS_PENDING);
                lbl.setOpaque(false);
                return lbl;
            }
        });

        pendingTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(COLOR_PURPLE_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        pendingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = pendingTable.getSelectedRow();
                if (row >= 0 && row < displayedPendingList.size()) {
                    TransactionSummaryDto item = displayedPendingList.get(row);
                    if (e.getClickCount() == 2 || pendingTable.columnAtPoint(e.getPoint()) == 6) {
                        showTransactionInvestigationDialog(item);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(pendingTable);
        scroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(0, 380));

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(130, 32));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                if (isSelected) {
                    setBackground(COLOR_PURPLE_PILL);
                    setForeground(COLOR_PURPLE_PRIMARY);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(COLOR_TEXT_MAIN);
                }
                return c;
            }
        });
    }

    // =========================================================================
    // MULTI-STATE PANELS (Loading, Empty, Unauthorized, Error)
    // =========================================================================

    private JPanel createLoadingCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(0, 380));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setForeground(COLOR_PURPLE_PRIMARY);
        progress.setMaximumSize(new Dimension(180, 8));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Synchronizing surveillance alerts and pending fraud queue...");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(COLOR_TEXT_SUBTLE);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(Box.createVerticalGlue());
        inner.add(progress);
        inner.add(Box.createVerticalStrut(14));
        inner.add(lbl);
        inner.add(Box.createVerticalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel createEmptyCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(0, 380));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel lblIcon = new JLabel("✓");
        lblIcon.setFont(new Font("Segoe UI", Font.BOLD, 42));
        lblIcon.setForeground(COLOR_STATUS_RESOLVED);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("No Surveillance Alerts or Pending Reviews Found");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("All transactions are clear and no active behavioral anomalies match current filters.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnReset = new JButton("Reset Filters") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnReset.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnReset.setForeground(COLOR_PURPLE_PRIMARY);
        btnReset.setPreferredSize(new Dimension(110, 32));
        btnReset.setMaximumSize(new Dimension(110, 32));
        btnReset.setContentAreaFilled(false);
        btnReset.setBorderPainted(false);
        btnReset.setFocusPainted(false);
        btnReset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReset.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            if (txtPendingSearch != null) txtPendingSearch.setText("");
            cmbSeverityFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            applyFilters();
            updatePendingTable();
        });

        inner.add(Box.createVerticalGlue());
        inner.add(lblIcon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(lblTitle);
        inner.add(Box.createVerticalStrut(4));
        inner.add(lblSub);
        inner.add(Box.createVerticalStrut(14));
        inner.add(btnReset);
        inner.add(Box.createVerticalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUnauthorizedCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(0, 380));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel lblIcon = new JLabel("🔒");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("Administrator Privileges Required");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(COLOR_SEVERITY_HIGH);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblUnauthorizedMessage = new JLabel("Access denied: Your account role does not have authorization to view surveillance alerts.");
        lblUnauthorizedMessage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUnauthorizedMessage.setForeground(COLOR_TEXT_SUBTLE);
        lblUnauthorizedMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblReq = new JLabel("This administrative endpoint requires server-side ROLE_ADMIN authorization.");
        lblReq.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblReq.setForeground(COLOR_TEXT_SUBTLE);
        lblReq.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnReturn = new JButton("Return to Dashboard") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnReturn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReturn.setForeground(Color.WHITE);
        btnReturn.setPreferredSize(new Dimension(170, 34));
        btnReturn.setMaximumSize(new Dimension(170, 34));
        btnReturn.setContentAreaFilled(false);
        btnReturn.setBorderPainted(false);
        btnReturn.setFocusPainted(false);
        btnReturn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReturn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnReturn.addActionListener(e -> {
            if (onNavigateToDashboard != null) {
                onNavigateToDashboard.run();
            }
        });

        inner.add(Box.createVerticalGlue());
        inner.add(lblIcon);
        inner.add(Box.createVerticalStrut(12));
        inner.add(lblTitle);
        inner.add(Box.createVerticalStrut(6));
        inner.add(lblUnauthorizedMessage);
        inner.add(Box.createVerticalStrut(4));
        inner.add(lblReq);
        inner.add(Box.createVerticalStrut(18));
        inner.add(btnReturn);
        inner.add(Box.createVerticalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JPanel createErrorCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(0, 380));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel lblIcon = new JLabel("⚠️");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblErrorMessage = new JLabel("Failed to retrieve administrator data.");
        lblErrorMessage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblErrorMessage.setForeground(COLOR_SEVERITY_HIGH);
        lblErrorMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnRetry = new JButton("Retry Connection") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnRetry.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRetry.setForeground(Color.WHITE);
        btnRetry.setPreferredSize(new Dimension(130, 32));
        btnRetry.setMaximumSize(new Dimension(130, 32));
        btnRetry.setContentAreaFilled(false);
        btnRetry.setBorderPainted(false);
        btnRetry.setFocusPainted(false);
        btnRetry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRetry.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRetry.addActionListener(e -> loadAllData());

        inner.add(Box.createVerticalGlue());
        inner.add(lblIcon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(lblErrorMessage);
        inner.add(Box.createVerticalStrut(14));
        inner.add(btnRetry);
        inner.add(Box.createVerticalGlue());

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // BACKGROUND DATA LOADING & REFRESH
    // =========================================================================

    public void loadAllData() {
        loadAlerts();
        loadPendingTransactions();
    }

    /**
     * Executes fraud alerts retrieval off the Swing Event Dispatch Thread (EDT).
     */
    public void loadAlerts() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        showState(STATE_LOADING);

        SwingWorker<List<AdminAlertDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<AdminAlertDto> doInBackground() throws Exception {
                return apiClient.getAdminAlerts();
            }

            @Override
            protected void done() {
                isLoading = false;
                try {
                    List<AdminAlertDto> alerts = get();
                    rawAlertsList.clear();
                    if (alerts != null) {
                        rawAlertsList.addAll(alerts);
                    }
                    applyFilters();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiClientException apiEx) {
                        if (apiEx.getStatusCode() == 403 || apiEx.getStatusCode() == 401) {
                            lblUnauthorizedMessage.setText("Server response: " + apiEx.getMessage());
                            showState(STATE_UNAUTHORIZED);
                            return;
                        }
                    }
                    lblErrorMessage.setText("Error retrieving alerts: " + cause.getMessage());
                    showState(STATE_ERROR);
                }
            }
        };
        worker.execute();
    }

    public void loadPendingTransactions() {
        SwingWorker<List<TransactionSummaryDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TransactionSummaryDto> doInBackground() throws Exception {
                return apiClient.getPendingTransactions();
            }

            @Override
            protected void done() {
                try {
                    List<TransactionSummaryDto> pending = get();
                    rawPendingList.clear();
                    if (pending != null) {
                        rawPendingList.addAll(pending);
                    }
                    updatePendingTable();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiClientException apiEx) {
                        if (apiEx.getStatusCode() == 403 || apiEx.getStatusCode() == 401) {
                            lblUnauthorizedMessage.setText("Server response: " + apiEx.getMessage());
                            showState(STATE_UNAUTHORIZED);
                        }
                    }
                }
            }
        };
        worker.execute();
    }

    private void applyFilters() {
        String severityFilter = cmbSeverityFilter != null && cmbSeverityFilter.getSelectedIndex() > 0
                ? (String) cmbSeverityFilter.getSelectedItem() : null;
        String statusFilter = cmbStatusFilter != null && cmbStatusFilter.getSelectedIndex() > 0
                ? (String) cmbStatusFilter.getSelectedItem() : null;
        String search = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";

        displayedAlertsList.clear();
        for (AdminAlertDto a : rawAlertsList) {
            if (severityFilter != null && !severityFilter.equalsIgnoreCase(a.severity())) {
                continue;
            }
            if (statusFilter != null && !statusFilter.equalsIgnoreCase(a.status())) {
                continue;
            }
            if (!search.isEmpty()) {
                String idStr = String.valueOf(a.id());
                String txnStr = a.transactionId() != null ? String.valueOf(a.transactionId()) : "";
                String typeStr = a.alertType() != null ? a.alertType().toLowerCase() : "";
                if (!idStr.contains(search) && !txnStr.contains(search) && !typeStr.contains(search)) {
                    continue;
                }
            }
            displayedAlertsList.add(a);
        }

        if (tableModel != null) {
            tableModel.fireTableDataChanged();
        }

        if (!isPendingViewActive) {
            if (displayedAlertsList.isEmpty()) {
                showState(STATE_EMPTY);
            } else {
                showState(STATE_CONTENT);
            }
        }

        // Update metric summaries from raw list
        int total = rawAlertsList.size();
        int high = 0;
        int med = 0;
        int open = 0;
        for (AdminAlertDto a : rawAlertsList) {
            if ("HIGH".equalsIgnoreCase(a.severity())) {
                high++;
            } else if ("MEDIUM".equalsIgnoreCase(a.severity())) {
                med++;
            }
            if ("OPEN".equalsIgnoreCase(a.status())) {
                open++;
            }
        }

        if (lblTotalAlerts != null) lblTotalAlerts.setText(String.valueOf(total));
        if (lblHighSeverityCount != null) lblHighSeverityCount.setText(String.valueOf(high));
        if (lblMedSeverityCount != null) lblMedSeverityCount.setText(String.valueOf(med));
        if (lblOpenCount != null) lblOpenCount.setText(String.valueOf(open));
        if (btnTabAlerts != null) btnTabAlerts.setText("🚨 Surveillance Alerts (" + total + ")");
    }

    public void updatePendingTable() {
        String search = txtPendingSearch != null ? txtPendingSearch.getText().trim().toLowerCase() : "";
        displayedPendingList.clear();

        for (TransactionSummaryDto tx : rawPendingList) {
            if (!search.isEmpty()) {
                String idStr = tx.id() != null ? String.valueOf(tx.id()) : "";
                String typeStr = tx.transactionType() != null ? tx.transactionType().toLowerCase() : "";
                String accStr = tx.accountNumber() != null ? tx.accountNumber().toLowerCase() : "";
                String userStr = tx.customerUsername() != null ? tx.customerUsername().toLowerCase() : "";
                if (!idStr.contains(search) && !typeStr.contains(search) && !accStr.contains(search) && !userStr.contains(search)) {
                    continue;
                }
            }
            displayedPendingList.add(tx);
        }

        if (pendingTableModel != null) {
            pendingTableModel.fireTableDataChanged();
        }

        if (lblPendingReviewCount != null) {
            lblPendingReviewCount.setText(String.valueOf(rawPendingList.size()));
        }

        if (btnTabPending != null) {
            btnTabPending.setText("⏳ Pending Reviews (" + rawPendingList.size() + ")");
        }

        if (isPendingViewActive) {
            if (displayedPendingList.isEmpty() && rawAlertsList.isEmpty()) {
                showState(STATE_EMPTY);
            } else {
                showState(STATE_CONTENT);
            }
        }
    }

    // =========================================================================
    // TRANSACTION INVESTIGATION & REVIEW DIALOG
    // =========================================================================

    public void showTransactionInvestigationDialog(TransactionSummaryDto txSummary) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        if (txSummary == null || txSummary.id() == null) return;
        Long txId = txSummary.id();

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Fraud Investigation & Review — Transaction #" + txId, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(520, 640);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(Color.WHITE);
        JLabel lblLoading = new JLabel("Retrieving transaction investigation records...", SwingConstants.CENTER);
        lblLoading.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblLoading.setForeground(COLOR_PURPLE_PRIMARY);
        loadingPanel.add(lblLoading, BorderLayout.CENTER);
        dialog.add(loadingPanel, BorderLayout.CENTER);

        SwingWorker<TransactionResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected TransactionResponse doInBackground() throws Exception {
                return apiClient.getTransactionDetails(txId);
            }

            @Override
            protected void done() {
                try {
                    TransactionResponse details = get();
                    dialog.getContentPane().removeAll();
                    dialog.add(buildInvestigationContent(txSummary, details, dialog), BorderLayout.CENTER);
                    dialog.revalidate();
                    dialog.repaint();
                } catch (Exception ex) {
                    dialog.getContentPane().removeAll();
                    dialog.add(buildInvestigationContent(txSummary, null, dialog), BorderLayout.CENTER);
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }

    JPanel buildInvestigationContent(
            TransactionSummaryDto txSummary,
            TransactionResponse txDetails,
            JDialog dialog) {

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Top Row: Title + Status Pill
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Transaction #" + txSummary.id());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblBadge = new JLabel("PENDING_REVIEW", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setForeground(COLOR_PURPLE_PRIMARY);
        lblBadge.setPreferredSize(new Dimension(120, 24));
        lblBadge.setOpaque(false);

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(lblBadge, BorderLayout.EAST);
        root.add(topRow);
        root.add(Box.createVerticalStrut(10));

        // Large Amount Display
        BigDecimal amt = txSummary.amount() != null ? txSummary.amount()
                : (txDetails != null && txDetails.amount() != null ? txDetails.amount() : BigDecimal.ZERO);
        JLabel lblAmount = new JLabel(String.format(Locale.US, "₹ %,.2f", amt));
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblAmount.setForeground(COLOR_PURPLE_PRIMARY);
        root.add(lblAmount);
        root.add(Box.createVerticalStrut(14));

        // Key Value Grid
        String type = txSummary.transactionType() != null ? txSummary.transactionType()
                : (txDetails != null ? txDetails.transactionType() : "—");
        root.add(createDetailRow("Transaction Type", type));
        root.add(createDetailRow("Submission Time", txSummary.transactionTime() != null ? txSummary.transactionTime() : "—"));

        String senderAcc = txSummary.accountNumber() != null ? txSummary.accountNumber() : "—";
        if (txSummary.customerUsername() != null) {
            senderAcc += " (" + txSummary.customerUsername() + ")";
        }
        root.add(createDetailRow("Sender Account", senderAcc));

        if ("TRANSFER".equalsIgnoreCase(type)) {
            String targetAcc = "Beneficiary Account";
            if (txDetails != null && txDetails.message() != null && txDetails.message().contains("ACC-")) {
                targetAcc = txDetails.message();
            }
            root.add(createDetailRow("Transfer Target", targetAcc));
        }

        if (txDetails != null && txDetails.newBalance() != null) {
            root.add(createDetailRow("Account Balance", String.format(Locale.US, "₹ %,.2f", txDetails.newBalance())));
        }

        root.add(createDetailRow("Current Status", "PAUSED — Awaiting Manual Fraud Review"));

        root.add(Box.createVerticalStrut(12));

        // Related Fraud Alert / Assessment Card
        JPanel alertCard = new JPanel();
        alertCard.setLayout(new BoxLayout(alertCard, BoxLayout.Y_AXIS));
        alertCard.setBackground(COLOR_INPUT_BG);
        alertCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        // Look up linked alert from cache
        AdminAlertDto linkedAlert = null;
        for (AdminAlertDto a : rawAlertsList) {
            if (a.transactionId() != null && a.transactionId().equals(txSummary.id())) {
                linkedAlert = a;
                break;
            }
        }

        if (linkedAlert != null) {
            JLabel lblAlertHead = new JLabel("🚨 Linked Surveillance Alert #" + linkedAlert.id() + " (" + linkedAlert.severity() + " SEVERITY)");
            lblAlertHead.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblAlertHead.setForeground(COLOR_SEVERITY_HIGH);

            JLabel lblAnomaly = new JLabel("Anomaly Trigger: " + linkedAlert.alertType() + " • Status: " + linkedAlert.status());
            lblAnomaly.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblAnomaly.setForeground(COLOR_TEXT_MAIN);

            JLabel lblTime = new JLabel("Triggered: " + linkedAlert.createdAt());
            lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblTime.setForeground(COLOR_TEXT_SUBTLE);

            alertCard.add(lblAlertHead);
            alertCard.add(Box.createVerticalStrut(3));
            alertCard.add(lblAnomaly);
            alertCard.add(Box.createVerticalStrut(2));
            alertCard.add(lblTime);
        } else {
            JLabel lblAssessmentHead = new JLabel("🛡️ Surveillance & Fraud Evaluation");
            lblAssessmentHead.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblAssessmentHead.setForeground(COLOR_PURPLE_PRIMARY);

            String reason = (txDetails != null && txDetails.detectionReason() != null)
                    ? txDetails.detectionReason() : "Flagged for manual fraud review";
            JLabel lblReason = new JLabel("Detection Reason: " + reason);
            lblReason.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblReason.setForeground(COLOR_TEXT_MAIN);

            JLabel lblNote = new JLabel("Funds remain held and untouched until administrator determination.");
            lblNote.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblNote.setForeground(COLOR_TEXT_SUBTLE);

            alertCard.add(lblAssessmentHead);
            alertCard.add(Box.createVerticalStrut(3));
            alertCard.add(lblReason);
            alertCard.add(Box.createVerticalStrut(2));
            alertCard.add(lblNote);
        }

        root.add(alertCard);
        root.add(Box.createVerticalStrut(14));

        // Review Action Panel
        JPanel reviewActionPanel = new JPanel();
        reviewActionPanel.setLayout(new BoxLayout(reviewActionPanel, BoxLayout.Y_AXIS));
        reviewActionPanel.setOpaque(false);

        JLabel lblNotesPrompt = new JLabel("Administrator Review Notes / Remarks:");
        lblNotesPrompt.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblNotesPrompt.setForeground(COLOR_TEXT_MAIN);

        JTextField txtNotes = new JTextField("Approved by administrator");
        txtNotes.setName("txtReviewNotes");
        txtNotes.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtNotes.setBackground(COLOR_INPUT_BG);
        txtNotes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        txtNotes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lblActionFeedback = new JLabel(" ");
        lblActionFeedback.setName("lblActionFeedback");
        lblActionFeedback.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblActionFeedback.setAlignmentX(Component.CENTER_ALIGNMENT);

        reviewActionPanel.add(lblNotesPrompt);
        reviewActionPanel.add(Box.createVerticalStrut(4));
        reviewActionPanel.add(txtNotes);
        reviewActionPanel.add(Box.createVerticalStrut(8));
        reviewActionPanel.add(lblActionFeedback);
        reviewActionPanel.add(Box.createVerticalStrut(8));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);

        JButton btnApprove = new JButton("✓ Approve & Settle");
        btnApprove.setName("btnApproveTransaction");
        styleActionButton(btnApprove, COLOR_STATUS_RESOLVED);

        JButton btnReject = new JButton("✕ Reject Transaction");
        btnReject.setName("btnRejectTransaction");
        styleActionButton(btnReject, COLOR_SEVERITY_HIGH);

        JButton btnClose = new JButton("Close");
        btnClose.setName("btnCloseDialog");
        styleActionButton(btnClose, COLOR_INPUT_BG, COLOR_TEXT_SUBTLE, COLOR_BORDER_SUBTLE);
        btnClose.addActionListener(e -> {
            if (dialog != null) dialog.dispose();
        });

        btnApprove.addActionListener(e -> {
            boolean confirmed = confirmAction(
                    "Are you sure you want to APPROVE and settle Transaction #" + txSummary.id() + "?\n\n"
                    + "This will debit the sender's account and settle the funds.",
                    "Confirm Transaction Approval"
            );
            if (confirmed) {
                executeReviewDecision(
                        txSummary.id(),
                        true,
                        txtNotes.getText().trim(),
                        dialog,
                        lblActionFeedback,
                        btnApprove,
                        btnReject,
                        txtNotes,
                        null
                );
            }
        });

        btnReject.addActionListener(e -> {
            boolean confirmed = confirmAction(
                    "Are you sure you want to REJECT Transaction #" + txSummary.id() + "?\n\n"
                    + "Account balances will remain untouched.",
                    "Confirm Transaction Rejection"
            );
            if (confirmed) {
                executeReviewDecision(
                        txSummary.id(),
                        false,
                        txtNotes.getText().trim(),
                        dialog,
                        lblActionFeedback,
                        btnApprove,
                        btnReject,
                        txtNotes,
                        null
                );
            }
        });

        btnRow.add(btnApprove);
        btnRow.add(btnReject);
        btnRow.add(btnClose);

        reviewActionPanel.add(btnRow);
        root.add(reviewActionPanel);

        return root;
    }

    public void executeReviewDecision(
            Long txId,
            boolean isApprove,
            String notes,
            JDialog dialog,
            JLabel lblFeedback,
            JButton btnApprove,
            JButton btnReject,
            Component inputComponent,
            Runnable onComplete) {

        // Prevent duplicate submissions: disable controls immediately
        if (btnApprove != null) btnApprove.setEnabled(false);
        if (btnReject != null) btnReject.setEnabled(false);
        if (inputComponent != null) inputComponent.setEnabled(false);

        if (lblFeedback != null) {
            lblFeedback.setForeground(COLOR_PURPLE_PRIMARY);
            lblFeedback.setText("Submitting " + (isApprove ? "approval" : "rejection") + " to backend...");
        }

        SwingWorker<TransactionResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected TransactionResponse doInBackground() throws Exception {
                if (isApprove) {
                    return apiClient.approveTransaction(txId, notes);
                } else {
                    return apiClient.rejectTransaction(txId, notes);
                }
            }

            @Override
            protected void done() {
                try {
                    TransactionResponse resp = get();
                    if (lblFeedback != null) {
                        lblFeedback.setForeground(isApprove ? COLOR_STATUS_RESOLVED : COLOR_SEVERITY_MED);
                        lblFeedback.setText((isApprove ? "✓ Approved: " : "✕ Rejected: ")
                                + (resp != null && resp.message() != null ? resp.message() : "Decision recorded."));
                    }
                    // Refresh feeds
                    loadAllData();

                    if (dialog != null) {
                        Timer timer = new Timer(500, e -> dialog.dispose());
                        timer.setRepeats(false);
                        timer.start();
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof ApiClientException apiEx && (apiEx.getStatusCode() == 401 || apiEx.getStatusCode() == 403)) {
                        showState(STATE_UNAUTHORIZED);
                        if (dialog != null) dialog.dispose();
                        return;
                    }
                    if (lblFeedback != null) {
                        lblFeedback.setForeground(COLOR_SEVERITY_HIGH);
                        lblFeedback.setText("Failed: " + cause.getMessage());
                    }
                    if (btnApprove != null) btnApprove.setEnabled(true);
                    if (btnReject != null) btnReject.setEnabled(true);
                    if (inputComponent != null) inputComponent.setEnabled(true);
                } finally {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        };
        worker.execute();
    }

    public void executeReviewDecision(Long txId, boolean isApprove, String notes) {
        executeReviewDecision(txId, isApprove, notes, null, null, null, null, null, null);
    }

    private void styleActionButton(JButton btn, Color bgColor) {
        styleActionButton(btn, bgColor, Color.WHITE, null);
    }

    private void styleActionButton(JButton btn, Color bgColor, Color fgColor, Color borderColor) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(fgColor);
        btn.setPreferredSize(new Dimension(140, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, javax.swing.JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(btn.isEnabled() ? bgColor : COLOR_BORDER_SUBTLE);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 14, 14);
                }
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    protected boolean confirmAction(String message, String title) {
        if (autoConfirmForTesting || GraphicsEnvironment.isHeadless()) {
            return true;
        }
        int opt = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        return opt == JOptionPane.YES_OPTION;
    }

    public void setAutoConfirmForTesting(boolean autoConfirm) {
        this.autoConfirmForTesting = autoConfirm;
    }

    public boolean isAutoConfirmForTesting() {
        return autoConfirmForTesting;
    }

    private void showAlertDetailDialog(AdminAlertDto alert) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Alert #" + alert.id() + " Detail", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Top Row
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Alert #" + alert.id());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblSev = new JLabel(alert.severity(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = "HIGH".equalsIgnoreCase(alert.severity()) ? new Color(254, 226, 226)
                        : "MEDIUM".equalsIgnoreCase(alert.severity()) ? new Color(254, 243, 199)
                        : new Color(219, 234, 254);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblSev.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSev.setForeground("HIGH".equalsIgnoreCase(alert.severity()) ? COLOR_SEVERITY_HIGH
                : "MEDIUM".equalsIgnoreCase(alert.severity()) ? COLOR_SEVERITY_MED : COLOR_SEVERITY_LOW);
        lblSev.setPreferredSize(new Dimension(74, 24));
        lblSev.setOpaque(false);

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(lblSev, BorderLayout.EAST);
        root.add(topRow);
        root.add(Box.createVerticalStrut(14));

        // Anomaly Classification
        JLabel lblType = new JLabel(alert.alertType());
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblType.setForeground(COLOR_PURPLE_PRIMARY);
        root.add(lblType);
        root.add(Box.createVerticalStrut(16));

        // Key Value Grid
        root.add(createDetailRow("Investigation Status", alert.status()));
        root.add(createDetailRow("Triggered Timestamp", alert.createdAt()));
        root.add(createDetailRow("Associated Txn ID", alert.transactionId() != null ? "#" + alert.transactionId() : "N/A"));
        root.add(createDetailRow("Transaction Type", alert.transactionType() != null ? alert.transactionType() : "N/A"));
        root.add(createDetailRow("Transaction Amount", alert.transactionAmount() != null
                ? String.format(Locale.US, "₹ %,.2f", alert.transactionAmount()) : "—"));
        root.add(createDetailRow("Transaction Outcome", alert.transactionStatus() != null ? alert.transactionStatus() : "N/A"));

        root.add(Box.createVerticalStrut(16));

        // Administrative Guidance Card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(COLOR_INPUT_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblGuidanceTitle = new JLabel("Surveillance Protocol");
        lblGuidanceTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblGuidanceTitle.setForeground(COLOR_PURPLE_PRIMARY);

        JLabel lblGuidanceDesc = new JLabel("Verify transaction pattern velocity and historical customer profile baseline.");
        lblGuidanceDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblGuidanceDesc.setForeground(COLOR_TEXT_SUBTLE);

        card.add(lblGuidanceTitle);
        card.add(Box.createVerticalStrut(3));
        card.add(lblGuidanceDesc);

        root.add(card);
        root.add(Box.createVerticalStrut(20));

        // Button Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnRow.setOpaque(false);

        if (alert.transactionId() != null) {
            JButton btnInvestigate = new JButton("Investigate & Review →");
            styleActionButton(btnInvestigate, COLOR_PURPLE_PRIMARY);
            btnInvestigate.addActionListener(e -> {
                dialog.dispose();
                TransactionSummaryDto pendingSummary = null;
                for (TransactionSummaryDto tx : rawPendingList) {
                    if (tx.id() != null && tx.id().equals(alert.transactionId())) {
                        pendingSummary = tx;
                        break;
                    }
                }
                if (pendingSummary == null) {
                    pendingSummary = new TransactionSummaryDto(
                            alert.transactionId(),
                            alert.transactionAmount(),
                            alert.transactionType(),
                            alert.transactionStatus(),
                            alert.createdAt()
                    );
                }
                showTransactionInvestigationDialog(pendingSummary);
            });
            btnRow.add(btnInvestigate);
        }

        JButton btnClose = new JButton("Close") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PURPLE_PILL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(COLOR_PURPLE_PRIMARY);
                g2.setFont(getFont());
                int sw = g2.getFontMetrics().stringWidth(getText());
                int sh = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (getWidth() - sw) / 2, (getHeight() + sh) / 2 - 2);
                g2.dispose();
            }
        };
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(COLOR_PURPLE_PRIMARY);
        btnClose.setPreferredSize(new Dimension(100, 32));
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dialog.dispose());
        btnRow.add(btnClose);

        root.add(btnRow);

        dialog.add(root, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXT_SUBTLE);

        JLabel val = new JLabel(value != null ? value : "—");
        val.setFont(new Font("Segoe UI", Font.BOLD, 12));
        val.setForeground(COLOR_TEXT_MAIN);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // =========================================================================
    // TABLE MODELS & RENDERERS
    // =========================================================================

    private class AdminAlertsTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {
                "Alert ID", "Anomaly Type", "Severity", "Status", "Triggered At", "Txn ID", "Amount", "Txn Status"
        };

        @Override
        public int getRowCount() {
            return displayedAlertsList.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= displayedAlertsList.size()) {
                return null;
            }
            AdminAlertDto alert = displayedAlertsList.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> "#" + alert.id();
                case 1 -> alert.alertType() != null ? alert.alertType() : "—";
                case 2 -> alert.severity() != null ? alert.severity() : "—";
                case 3 -> alert.status() != null ? alert.status() : "—";
                case 4 -> alert.createdAt() != null ? alert.createdAt() : "—";
                case 5 -> alert.transactionId() != null ? "#" + alert.transactionId() : "—";
                case 6 -> alert.transactionAmount() != null ? String.format(Locale.US, "₹ %,.2f", alert.transactionAmount()) : "—";
                case 7 -> alert.transactionStatus() != null ? alert.transactionStatus() : "—";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class PendingTransactionsTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {
                "Txn ID", "Customer / Account", "Type", "Amount", "Submission Time", "Status", "Action"
        };

        @Override
        public int getRowCount() {
            return displayedPendingList.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex < 0 || rowIndex >= displayedPendingList.size()) {
                return null;
            }
            TransactionSummaryDto tx = displayedPendingList.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> "#" + tx.id();
                case 1 -> {
                    String acc = tx.accountNumber() != null ? tx.accountNumber() : "—";
                    String user = tx.customerUsername() != null ? " (" + tx.customerUsername() + ")" : "";
                    yield acc + user;
                }
                case 2 -> tx.transactionType() != null ? tx.transactionType() : "—";
                case 3 -> tx.amount() != null ? String.format(Locale.US, "₹ %,.2f", tx.amount()) : "₹ 0.00";
                case 4 -> tx.transactionTime() != null ? tx.transactionTime() : "—";
                case 5 -> tx.status() != null ? tx.status() : "PENDING_REVIEW";
                case 6 -> "Review & Decide →";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private static class SeverityBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String sev = value != null ? value.toString() : "";
            JLabel lbl = new JLabel(sev, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = "HIGH".equalsIgnoreCase(sev) ? new Color(254, 226, 226)
                            : "MEDIUM".equalsIgnoreCase(sev) ? new Color(254, 243, 199)
                            : new Color(219, 234, 254);
                    g2.setColor(bg);
                    g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 8, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            Color fg = "HIGH".equalsIgnoreCase(sev) ? COLOR_SEVERITY_HIGH
                    : "MEDIUM".equalsIgnoreCase(sev) ? COLOR_SEVERITY_MED : COLOR_SEVERITY_LOW;
            lbl.setForeground(fg);
            lbl.setOpaque(false);
            return lbl;
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            String status = value != null ? value.toString() : "";
            JLabel lbl = new JLabel(status, SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = "RESOLVED".equalsIgnoreCase(status) ? new Color(220, 252, 231)
                            : "REVIEWED".equalsIgnoreCase(status) ? new Color(224, 242, 254)
                            : new Color(243, 232, 255);
                    g2.setColor(bg);
                    g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 8, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            Color fg = "RESOLVED".equalsIgnoreCase(status) ? COLOR_STATUS_RESOLVED
                    : "REVIEWED".equalsIgnoreCase(status) ? COLOR_STATUS_REVIEWED : COLOR_STATUS_OPEN;
            lbl.setForeground(fg);
            lbl.setOpaque(false);
            return lbl;
        }
    }

    // =========================================================================
    // ACCESSORS FOR TESTING & INTEGRATION
    // =========================================================================

    public JTable getAlertsTable() {
        return alertsTable;
    }

    public JTable getPendingTable() {
        return pendingTable;
    }

    public List<AdminAlertDto> getDisplayedAlertsList() {
        return Collections.unmodifiableList(displayedAlertsList);
    }

    public List<AdminAlertDto> getRawAlertsList() {
        return Collections.unmodifiableList(rawAlertsList);
    }

    public List<TransactionSummaryDto> getPendingTransactionsList() {
        return Collections.unmodifiableList(displayedPendingList);
    }

    public List<TransactionSummaryDto> getRawPendingList() {
        return Collections.unmodifiableList(rawPendingList);
    }

    public JTextField getSearchField() {
        return txtSearch;
    }

    public JTextField getPendingSearchField() {
        return txtPendingSearch;
    }

    public JComboBox<String> getSeverityFilter() {
        return cmbSeverityFilter;
    }

    public JComboBox<String> getStatusFilter() {
        return cmbStatusFilter;
    }

    public JButton getRefreshButton() {
        return btnRefresh;
    }

    public JButton getApplyFiltersButton() {
        return btnApplyFilters;
    }

    public JButton getResetFiltersButton() {
        return btnResetFilters;
    }

    public JButton getViewPendingButton() {
        return btnTabPending;
    }

    public JButton getViewAlertsButton() {
        return btnTabAlerts;
    }

    public boolean isPendingViewActive() {
        return isPendingViewActive;
    }

    public void showPendingView() {
        setActiveView(true);
    }

    public void showAlertsView() {
        setActiveView(false);
    }

    public void setAlertsData(List<AdminAlertDto> alerts) {
        rawAlertsList.clear();
        if (alerts != null) {
            rawAlertsList.addAll(alerts);
        }
        applyFilters();
    }

    public void setPendingData(List<TransactionSummaryDto> pending) {
        rawPendingList.clear();
        if (pending != null) {
            rawPendingList.addAll(pending);
        }
        updatePendingTable();
    }

    public String getTotalAlertsText() {
        return lblTotalAlerts != null ? lblTotalAlerts.getText() : "0";
    }

    public String getPendingReviewCountText() {
        return lblPendingReviewCount != null ? lblPendingReviewCount.getText() : "0";
    }

    public JLabel getPendingReviewCountLabel() {
        return lblPendingReviewCount;
    }

    public String getHighSeverityCountText() {
        return lblHighSeverityCount != null ? lblHighSeverityCount.getText() : "0";
    }

    public String getMedSeverityCountText() {
        return lblMedSeverityCount != null ? lblMedSeverityCount.getText() : "0";
    }

    public String getOpenCountText() {
        return lblOpenCount != null ? lblOpenCount.getText() : "0";
    }

    public JLabel getUnauthorizedMessageLabel() {
        return lblUnauthorizedMessage;
    }

    public JLabel getErrorMessageLabel() {
        return lblErrorMessage;
    }
}
