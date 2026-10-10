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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import com.frauddetection.frauddetection.client.ApiClientException;
import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AdminAlertDto;

/**
 * Native Java Swing and Java2D Administrator Surveillance & Fraud Alert Monitoring screen.
 *
 * Implements:
 * - Real administrator alert monitoring consuming {@code GET /api/admin/alerts}
 * - Server-side ADMIN role enforcement (handles HTTP 401 & 403 access denial)
 * - Off-EDT background refresh via {@link SwingWorker}
 * - Summary metric cards computed dynamically from verified backend alerts
 * - Filters for severity (HIGH, MEDIUM, LOW) and status (OPEN, REVIEWED, RESOLVED)
 * - Quick text search by Alert ID, Transaction ID, or Anomaly Type
 * - Interactive alert detail inspection modal
 * - Distinct Content, Loading, Empty, Unauthorized, and Error states
 * - Finora / SafePay purple & lavender design system
 */
public class AdminMonitoringPanel extends JPanel {

    public static final String STATE_LOADING = "STATE_LOADING";
    public static final String STATE_CONTENT = "STATE_CONTENT";
    public static final String STATE_EMPTY = "STATE_EMPTY";
    public static final String STATE_UNAUTHORIZED = "STATE_UNAUTHORIZED";
    public static final String STATE_ERROR = "STATE_ERROR";

    // Design Tokens matching SafePay Dashboard
    public static final Color COLOR_PURPLE_PRIMARY = new Color(124, 58, 237); // #7C3AED
    public static final Color COLOR_PURPLE_LIGHT = new Color(196, 181, 253);   // #C4B5FD
    public static final Color COLOR_PURPLE_PILL = new Color(243, 232, 255);    // #F3E8FF
    public static final Color COLOR_HERO_BG = new Color(248, 247, 252);       // #F8F7FC
    public static final Color COLOR_BORDER_SUBTLE = new Color(226, 232, 240); // #E2E8F0
    public static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);        // #0F172A
    public static final Color COLOR_TEXT_SUBTLE = new Color(100, 116, 139);   // #64748B
    public static final Color COLOR_INPUT_BG = new Color(248, 250, 252);      // #F8FAFC

    public static final Color COLOR_SEVERITY_HIGH = new Color(220, 38, 38);   // #DC2626
    public static final Color COLOR_SEVERITY_MED = new Color(217, 119, 6);    // #D97706
    public static final Color COLOR_SEVERITY_LOW = new Color(37, 99, 235);    // #2563EB

    public static final Color COLOR_STATUS_OPEN = new Color(147, 51, 234);    // #9333EA
    public static final Color COLOR_STATUS_REVIEWED = new Color(2, 132, 199);  // #0284C7
    public static final Color COLOR_STATUS_RESOLVED = new Color(22, 163, 74); // #16A34A

    private final SafePayApiClient apiClient;
    private final Runnable onNavigateToDashboard;

    private final CardLayout stateCardLayout = new CardLayout();
    private final JPanel stateContainer = new JPanel(stateCardLayout);

    // Filter Controls
    private JTextField txtSearch;
    private JComboBox<String> cmbSeverityFilter;
    private JComboBox<String> cmbStatusFilter;
    private JButton btnApplyFilters;
    private JButton btnResetFilters;
    private JButton btnRefresh;

    // Metrics Labels
    private JLabel lblTotalAlerts;
    private JLabel lblHighSeverityCount;
    private JLabel lblMedSeverityCount;
    private JLabel lblOpenCount;

    // Alerts Table
    private JTable alertsTable;
    private AdminAlertsTableModel tableModel;
    private final List<AdminAlertDto> rawAlertsList = new ArrayList<>();
    private final List<AdminAlertDto> displayedAlertsList = new ArrayList<>();

    // Error & Unauthorized Labels
    private JLabel lblUnauthorizedMessage;
    private JLabel lblErrorMessage;

    private boolean isLoading = false;

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

        // 3. Filter & Search Control Bar
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

    // =========================================================================
    // UI CARDS
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

        btnRefresh = new JButton("⟳ Refresh Alerts") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(243, 232, 255));
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
        btnRefresh.addActionListener(e -> loadAlerts());
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
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 28, 6));

        lblTotalAlerts = new JLabel("0");
        lblHighSeverityCount = new JLabel("0");
        lblMedSeverityCount = new JLabel("0");
        lblOpenCount = new JLabel("0");

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

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        // Search Input
        txtSearch = new JTextField(15) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !(isFocusOwner())) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(160, 174, 192));
                    g2.setFont(getFont());
                    g2.drawString("Search Alert ID, Txn...", 8, 20);
                    g2.dispose();
                }
            }
        };
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setBackground(COLOR_INPUT_BG);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        txtSearch.setPreferredSize(new Dimension(190, 32));
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

        card.add(filterRow, BorderLayout.CENTER);
        return card;
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

    private JPanel createTableCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout(0, 10));

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
        card.add(tableHeader, BorderLayout.NORTH);

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

        // Column widths
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
        tableScroll.getViewport().setBackground(Color.WHITE);
        tableScroll.setPreferredSize(new Dimension(0, 380));

        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLoadingCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(0, 380));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        JLabel lbl = new JLabel("Polling fraud alerts from SafePay surveillance backend...");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(COLOR_PURPLE_PRIMARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar pb = new JProgressBar();
        pb.setIndeterminate(true);
        pb.setMaximumSize(new Dimension(280, 8));
        pb.setPreferredSize(new Dimension(280, 8));
        pb.setForeground(COLOR_PURPLE_PRIMARY);
        pb.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(Box.createVerticalGlue());
        inner.add(lbl);
        inner.add(Box.createVerticalStrut(12));
        inner.add(pb);
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

        JLabel lblIcon = new JLabel("🛡");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("No Surveillance Alerts Found");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("All transactions are within normal baseline thresholds or no alerts match filters.");
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
            cmbSeverityFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            applyFilters();
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

        lblErrorMessage = new JLabel("Failed to retrieve administrator alerts.");
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
        btnRetry.addActionListener(e -> loadAlerts());

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
    // BACKGROUND DATA LOADING
    // =========================================================================

    /**
     * Executes fraud alerts retrieval off the Swing Event Dispatch Thread (EDT).
     */
    public void loadAlerts() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        stateCardLayout.show(stateContainer, STATE_LOADING);

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
                            stateCardLayout.show(stateContainer, STATE_UNAUTHORIZED);
                            return;
                        }
                    }
                    lblErrorMessage.setText("Error retrieving alerts: " + cause.getMessage());
                    stateCardLayout.show(stateContainer, STATE_ERROR);
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

        tableModel.fireTableDataChanged();

        if (displayedAlertsList.isEmpty()) {
            stateCardLayout.show(stateContainer, STATE_EMPTY);
        } else {
            stateCardLayout.show(stateContainer, STATE_CONTENT);
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

        lblTotalAlerts.setText(String.valueOf(total));
        lblHighSeverityCount.setText(String.valueOf(high));
        lblMedSeverityCount.setText(String.valueOf(med));
        lblOpenCount.setText(String.valueOf(open));
    }

    // =========================================================================
    // ALERT DETAIL INSPECTION DIALOG
    // =========================================================================

    private void showAlertDetailDialog(AdminAlertDto alert) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Fraud Alert #" + alert.id() + " Inspection", JDialog.ModalityType.APPLICATION_MODAL);
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

        // Close Button
        JButton btnClose = new JButton("Close") {
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
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClose.setForeground(Color.WHITE);
        btnClose.setPreferredSize(new Dimension(100, 32));
        btnClose.setMaximumSize(new Dimension(100, 32));
        btnClose.setContentAreaFilled(false);
        btnClose.setBorderPainted(false);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.addActionListener(e -> dialog.dispose());

        root.add(btnClose);

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
    // TABLE MODEL & RENDERERS
    // =========================================================================

    private class AdminAlertsTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"Alert ID", "Anomaly Type", "Severity", "Status", "Triggered At", "Txn ID", "Amount", "Txn Status"};

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
                case 2 -> alert.severity() != null ? alert.severity() : "LOW";
                case 3 -> alert.status() != null ? alert.status() : "OPEN";
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

    // Accessors for testing
    public JTable getAlertsTable() {
        return alertsTable;
    }

    public List<AdminAlertDto> getDisplayedAlertsList() {
        return Collections.unmodifiableList(displayedAlertsList);
    }

    public JTextField getSearchField() {
        return txtSearch;
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

    public void setAlertsData(List<AdminAlertDto> alerts) {
        rawAlertsList.clear();
        if (alerts != null) {
            rawAlertsList.addAll(alerts);
        }
        applyFilters();
    }

    public List<AdminAlertDto> getRawAlertsList() {
        return Collections.unmodifiableList(rawAlertsList);
    }

    public String getTotalAlertsText() {
        return lblTotalAlerts != null ? lblTotalAlerts.getText() : "0";
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
}
