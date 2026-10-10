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

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Native Java Swing and Java2D Transaction History screen for SafePay desktop client.
 *
 * Implements:
 * - Search by transaction ID or account
 * - Supported filters for transaction type and status
 * - Formatted INR monetary amounts
 * - Distinct Loading, Empty, Error, and Content states
 * - Server-side transaction detail lookup via {@link SafePayApiClient#getTransactionDetails(Long)}
 * - Background execution off the Swing EDT with UI updates on the EDT
 * - Finora / SafePay purple & lavender design system
 */
public class TransactionHistoryPanel extends JPanel {

    public static final String STATE_LOADING = "STATE_LOADING";
    public static final String STATE_CONTENT = "STATE_CONTENT";
    public static final String STATE_EMPTY = "STATE_EMPTY";
    public static final String STATE_ERROR = "STATE_ERROR";

    // Finora / SafePay Design Tokens
    public static final Color COLOR_PURPLE_PRIMARY = new Color(124, 58, 237); // #7C3AED
    public static final Color COLOR_PURPLE_LIGHT = new Color(196, 181, 253);   // #C4B5FD
    public static final Color COLOR_PURPLE_PILL = new Color(243, 232, 255);    // #F3E8FF
    public static final Color COLOR_HERO_BG = new Color(248, 247, 252);       // #F8F7FC
    public static final Color COLOR_CARD_WHITE = Color.WHITE;
    public static final Color COLOR_CARD_OUTLINE = new Color(241, 245, 249);  // #F1F5F9
    public static final Color COLOR_BORDER_SUBTLE = new Color(226, 232, 240); // #E2E8F0
    public static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);        // #0F172A
    public static final Color COLOR_TEXT_SUBTLE = new Color(100, 116, 139);   // #64748B
    public static final Color COLOR_INPUT_BG = new Color(248, 250, 252);      // #F8FAFC

    public static final Color COLOR_STATUS_APPROVED = new Color(22, 163, 74); // #16A34A
    public static final Color COLOR_STATUS_BLOCKED = new Color(220, 38, 38);  // #DC2626
    public static final Color COLOR_STATUS_DECLINED = new Color(217, 119, 6); // #D97706

    private final SafePayApiClient apiClient;
    private final Runnable onNavigateToDashboard;

    private final CardLayout stateCardLayout = new CardLayout();
    private final JPanel stateContainer = new JPanel(stateCardLayout);

    // Filter Controls
    private JTextField txtSearch;
    private JComboBox<String> cmbTypeFilter;
    private JComboBox<String> cmbStatusFilter;
    private JButton btnApplyFilters;
    private JButton btnResetFilters;
    private JButton btnRefresh;

    // Summary Metric Badges
    private JLabel lblTotalCount;
    private JLabel lblTotalVolume;
    private JLabel lblApprovedCount;
    private JLabel lblDeclinedCount;
    private JLabel lblBlockedCount;

    // Table
    private JTable transactionTable;
    private TransactionHistoryTableModel tableModel;
    private final List<TransactionSummaryDto> transactionsList = new ArrayList<>();

    // Error & Empty Labels
    private JLabel lblErrorMessage;
    private JLabel lblEmptyMessage;

    private boolean isLoading = false;

    public TransactionHistoryPanel(SafePayApiClient apiClient, Runnable onNavigateToDashboard) {
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.onNavigateToDashboard = onNavigateToDashboard;

        setLayout(new BorderLayout());
        setOpaque(false);

        initUI();
    }

    public TransactionHistoryPanel(SafePayApiClient apiClient) {
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

        // 3. Filters & Search Control Bar
        mainContent.add(createFilterCard());
        mainContent.add(Box.createVerticalStrut(12));

        // 4. Multi-State Container (Content, Loading, Empty, Error)
        stateContainer.setOpaque(false);
        stateContainer.add(createTableCard(), STATE_CONTENT);
        stateContainer.add(createLoadingCard(), STATE_LOADING);
        stateContainer.add(createEmptyCard(), STATE_EMPTY);
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

        JLabel lblTitle = new JLabel("Transaction History & Audit Ledger");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblSub = new JLabel("Inspect verified account transactions, search records, and view AI risk classifications.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);

        textGroup.add(lblTitle);
        textGroup.add(Box.createVerticalStrut(4));
        textGroup.add(lblSub);

        hero.add(textGroup, BorderLayout.WEST);

        // Right side controls: Back to Dashboard & Refresh
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);

        btnRefresh = new JButton("⟳ Refresh Ledger") {
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
        btnRefresh.addActionListener(e -> loadTransactionHistory());
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

        lblTotalCount = new JLabel("0");
        lblTotalVolume = new JLabel("₹ 0.00");
        lblApprovedCount = new JLabel("0");
        lblDeclinedCount = new JLabel("0");
        lblBlockedCount = new JLabel("0");

        card.add(createMetricItem("Total Transactions", lblTotalCount, COLOR_TEXT_MAIN));
        card.add(createDivider());
        card.add(createMetricItem("Total Volume", lblTotalVolume, COLOR_PURPLE_PRIMARY));
        card.add(createDivider());
        card.add(createMetricItem("Approved", lblApprovedCount, COLOR_STATUS_APPROVED));
        card.add(createDivider());
        card.add(createMetricItem("Declined", lblDeclinedCount, COLOR_STATUS_DECLINED));
        card.add(createDivider());
        card.add(createMetricItem("Blocked by AI", lblBlockedCount, COLOR_STATUS_BLOCKED));

        return card;
    }

    private JPanel createMetricItem(String label, JLabel valueLabel, Color valueColor) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(COLOR_TEXT_SUBTLE);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
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
                    g2.drawString("Search Txn ID, Account...", 8, 20);
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
        txtSearch.setPreferredSize(new Dimension(200, 32));
        txtSearch.addActionListener(e -> loadTransactionHistory());
        filterRow.add(txtSearch);

        // Type Filter Dropdown
        String[] typeOptions = {"All Types", "PAYMENT", "TRANSFER", "DEPOSIT", "WITHDRAWAL"};
        cmbTypeFilter = new JComboBox<>(typeOptions);
        styleComboBox(cmbTypeFilter);
        filterRow.add(cmbTypeFilter);

        // Status Filter Dropdown
        String[] statusOptions = {"All Statuses", "APPROVED", "DECLINED", "BLOCKED"};
        cmbStatusFilter = new JComboBox<>(statusOptions);
        styleComboBox(cmbStatusFilter);
        filterRow.add(cmbStatusFilter);

        // Filter Action Buttons
        btnApplyFilters = new JButton("Apply Filters") {
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
        btnApplyFilters.setPreferredSize(new Dimension(100, 30));
        btnApplyFilters.setContentAreaFilled(false);
        btnApplyFilters.setBorderPainted(false);
        btnApplyFilters.setFocusPainted(false);
        btnApplyFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnApplyFilters.addActionListener(e -> loadTransactionHistory());
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
        btnResetFilters.setPreferredSize(new Dimension(70, 30));
        btnResetFilters.setContentAreaFilled(false);
        btnResetFilters.setBorderPainted(false);
        btnResetFilters.setFocusPainted(false);
        btnResetFilters.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnResetFilters.addActionListener(e -> {
            txtSearch.setText("");
            cmbTypeFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            loadTransactionHistory();
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

        JLabel lblSection = new JLabel("Audit Records");
        lblSection.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSection.setForeground(COLOR_TEXT_MAIN);

        JLabel lblHint = new JLabel("Double-click any transaction or select to inspect AI assessment details");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHint.setForeground(COLOR_TEXT_SUBTLE);

        tableHeader.add(lblSection, BorderLayout.WEST);
        tableHeader.add(lblHint, BorderLayout.EAST);
        card.add(tableHeader, BorderLayout.NORTH);

        tableModel = new TransactionHistoryTableModel();
        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(36);
        transactionTable.setShowGrid(false);
        transactionTable.setIntercellSpacing(new Dimension(0, 0));
        transactionTable.setFillsViewportHeight(true);
        transactionTable.setBackground(Color.WHITE);
        transactionTable.setSelectionBackground(new Color(243, 232, 255));
        transactionTable.setSelectionForeground(COLOR_TEXT_MAIN);
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader th = transactionTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setForeground(COLOR_TEXT_SUBTLE);
        th.setBackground(new Color(248, 250, 252));
        th.setPreferredSize(new Dimension(0, 32));
        th.setReorderingAllowed(false);

        // Column Renderers
        transactionTable.getColumnModel().getColumn(0).setPreferredWidth(70);  // ID
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(160); // Date/Time
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(110); // Type
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(130); // Amount
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(110); // Status
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(90);  // Details

        transactionTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(COLOR_PURPLE_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        transactionTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setForeground(COLOR_TEXT_MAIN);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                return l;
            }
        });

        transactionTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());

        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(COLOR_PURPLE_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                return l;
            }
        });

        transactionTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = transactionTable.getSelectedRow();
                if (row >= 0 && row < transactionsList.size()) {
                    TransactionSummaryDto item = transactionsList.get(row);
                    if (e.getClickCount() == 2 || transactionTable.columnAtPoint(e.getPoint()) == 5) {
                        showTransactionDetailDialog(item.id());
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(transactionTable);
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

        JLabel lbl = new JLabel("Retrieving transaction history from SafePay server...");
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

        JLabel lblIcon = new JLabel("🔍");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblEmptyMessage = new JLabel("No transactions found matching your criteria.");
        lblEmptyMessage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEmptyMessage.setForeground(COLOR_TEXT_MAIN);
        lblEmptyMessage.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Try clearing your search keyword or selecting a different type/status filter.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnClear = new JButton("Clear Filters") {
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
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnClear.setForeground(COLOR_PURPLE_PRIMARY);
        btnClear.setPreferredSize(new Dimension(110, 32));
        btnClear.setMaximumSize(new Dimension(110, 32));
        btnClear.setContentAreaFilled(false);
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbTypeFilter.setSelectedIndex(0);
            cmbStatusFilter.setSelectedIndex(0);
            loadTransactionHistory();
        });

        inner.add(Box.createVerticalGlue());
        inner.add(lblIcon);
        inner.add(Box.createVerticalStrut(10));
        inner.add(lblEmptyMessage);
        inner.add(Box.createVerticalStrut(4));
        inner.add(lblSub);
        inner.add(Box.createVerticalStrut(14));
        inner.add(btnClear);
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

        lblErrorMessage = new JLabel("Failed to retrieve transaction records.");
        lblErrorMessage.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblErrorMessage.setForeground(COLOR_STATUS_BLOCKED);
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
        btnRetry.addActionListener(e -> loadTransactionHistory());

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
     * Executes transaction history loading strictly off the Swing Event Dispatch Thread (EDT).
     */
    public void loadTransactionHistory() {
        if (isLoading) {
            return;
        }
        isLoading = true;
        stateCardLayout.show(stateContainer, STATE_LOADING);

        String typeParam = cmbTypeFilter != null && cmbTypeFilter.getSelectedIndex() > 0
                ? (String) cmbTypeFilter.getSelectedItem() : null;
        String statusParam = cmbStatusFilter != null && cmbStatusFilter.getSelectedIndex() > 0
                ? (String) cmbStatusFilter.getSelectedItem() : null;
        String searchParam = txtSearch != null && !txtSearch.getText().trim().isEmpty()
                ? txtSearch.getText().trim() : null;

        SwingWorker<List<TransactionSummaryDto>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<TransactionSummaryDto> doInBackground() throws Exception {
                return apiClient.getTransactionHistory(typeParam, statusParam, searchParam);
            }

            @Override
            protected void done() {
                isLoading = false;
                try {
                    List<TransactionSummaryDto> result = get();
                    updateTableData(result);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    lblErrorMessage.setText("Error loading transactions: " + cause.getMessage());
                    stateCardLayout.show(stateContainer, STATE_ERROR);
                }
            }
        };
        worker.execute();
    }

    public void updateTableData(List<TransactionSummaryDto> result) {
        transactionsList.clear();
        if (result != null) {
            transactionsList.addAll(result);
        }
        tableModel.fireTableDataChanged();

        if (transactionsList.isEmpty()) {
            stateCardLayout.show(stateContainer, STATE_EMPTY);
        } else {
            stateCardLayout.show(stateContainer, STATE_CONTENT);
        }

        // Calculate and update metrics
        int total = transactionsList.size();
        BigDecimal totalVolume = BigDecimal.ZERO;
        int approved = 0;
        int declined = 0;
        int blocked = 0;

        for (TransactionSummaryDto tx : transactionsList) {
            if (tx.amount() != null) {
                totalVolume = totalVolume.add(tx.amount());
            }
            if ("APPROVED".equalsIgnoreCase(tx.status())) {
                approved++;
            } else if ("DECLINED".equalsIgnoreCase(tx.status())) {
                declined++;
            } else if ("BLOCKED".equalsIgnoreCase(tx.status())) {
                blocked++;
            }
        }

        lblTotalCount.setText(String.valueOf(total));
        lblTotalVolume.setText(String.format(Locale.US, "₹ %,.2f", totalVolume));
        lblApprovedCount.setText(String.valueOf(approved));
        lblDeclinedCount.setText(String.valueOf(declined));
        lblBlockedCount.setText(String.valueOf(blocked));
    }

    // =========================================================================
    // TRANSACTION DETAIL INSPECTION DIALOG
    // =========================================================================

    private void showTransactionDetailDialog(Long transactionId) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Transaction #" + transactionId + " Detail", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);

        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setBackground(Color.WHITE);
        JLabel lblLoading = new JLabel("Fetching transaction details from server...", SwingConstants.CENTER);
        lblLoading.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblLoading.setForeground(COLOR_PURPLE_PRIMARY);
        loadingPanel.add(lblLoading, BorderLayout.CENTER);
        dialog.add(loadingPanel, BorderLayout.CENTER);

        SwingWorker<TransactionResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected TransactionResponse doInBackground() throws Exception {
                return apiClient.getTransactionDetails(transactionId);
            }

            @Override
            protected void done() {
                try {
                    TransactionResponse resp = get();
                    dialog.getContentPane().removeAll();
                    dialog.add(buildDetailContent(resp, dialog), BorderLayout.CENTER);
                    dialog.revalidate();
                    dialog.repaint();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    dialog.getContentPane().removeAll();
                    JPanel errPanel = new JPanel(new BorderLayout());
                    errPanel.setBackground(Color.WHITE);
                    JLabel lblErr = new JLabel("Failed to load details: " + cause.getMessage(), SwingConstants.CENTER);
                    lblErr.setForeground(COLOR_STATUS_BLOCKED);
                    errPanel.add(lblErr, BorderLayout.CENTER);
                    dialog.add(errPanel, BorderLayout.CENTER);
                    dialog.revalidate();
                    dialog.repaint();
                }
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }

    private JPanel buildDetailContent(TransactionResponse resp, JDialog dialog) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Transaction #" + resp.transactionId());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);

        JLabel lblBadge = new JLabel(resp.status(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = "APPROVED".equalsIgnoreCase(resp.status()) ? new Color(220, 252, 231)
                        : "DECLINED".equalsIgnoreCase(resp.status()) ? new Color(254, 243, 199)
                        : new Color(254, 226, 226);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        Color textColor = "APPROVED".equalsIgnoreCase(resp.status()) ? COLOR_STATUS_APPROVED
                : "DECLINED".equalsIgnoreCase(resp.status()) ? COLOR_STATUS_DECLINED
                : COLOR_STATUS_BLOCKED;
        lblBadge.setForeground(textColor);
        lblBadge.setPreferredSize(new Dimension(84, 24));
        lblBadge.setOpaque(false);

        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(lblBadge, BorderLayout.EAST);
        root.add(topRow);
        root.add(Box.createVerticalStrut(14));

        // Big Amount Display
        JLabel lblAmount = new JLabel(String.format(Locale.US, "₹ %,.2f", resp.amount() != null ? resp.amount() : BigDecimal.ZERO));
        lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblAmount.setForeground(COLOR_PURPLE_PRIMARY);
        root.add(lblAmount);
        root.add(Box.createVerticalStrut(18));

        // Key-Value Grid
        root.add(createDetailRow("Transaction Type", resp.transactionType()));
        root.add(createDetailRow("Timestamp", resp.transactionTime()));
        root.add(createDetailRow("System Outcome", resp.message()));
        if (resp.newBalance() != null) {
            root.add(createDetailRow("Account Balance", String.format(Locale.US, "₹ %,.2f", resp.newBalance())));
        }

        root.add(Box.createVerticalStrut(12));

        // AI Risk Classification Card
        JPanel riskCard = new JPanel();
        riskCard.setLayout(new BoxLayout(riskCard, BoxLayout.Y_AXIS));
        riskCard.setBackground(COLOR_INPUT_BG);
        riskCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblAiHeader = new JLabel("AI Fraud Assessment");
        lblAiHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAiHeader.setForeground(COLOR_PURPLE_PRIMARY);

        double score = resp.fraudProbability() != null ? resp.fraudProbability() : 0.0;
        String tier = resp.riskAssessment() != null ? resp.riskAssessment() : (score >= 0.70 ? "HIGH" : score >= 0.40 ? "MEDIUM" : "LOW");
        String scorePercent = String.format(Locale.US, "Probability: %.1f%% (%s RISK)", score * 100, tier);
        JLabel lblScore = new JLabel(scorePercent);
        lblScore.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblScore.setForeground("HIGH".equalsIgnoreCase(tier) || score >= 0.70 ? COLOR_STATUS_BLOCKED
                : "MEDIUM".equalsIgnoreCase(tier) || score >= 0.40 ? COLOR_STATUS_DECLINED
                : COLOR_STATUS_APPROVED);

        JLabel lblReason = new JLabel("Reason: " + (resp.detectionReason() != null ? resp.detectionReason() : "Verified"));
        lblReason.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblReason.setForeground(COLOR_TEXT_SUBTLE);

        riskCard.add(lblAiHeader);
        riskCard.add(Box.createVerticalStrut(4));
        riskCard.add(lblScore);
        riskCard.add(Box.createVerticalStrut(2));
        riskCard.add(lblReason);

        root.add(riskCard);
        root.add(Box.createVerticalStrut(18));

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
        return root;
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
    // TABLE MODEL & RENDERER
    // =========================================================================

    private class TransactionHistoryTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"ID", "Date & Time", "Type", "Amount", "Status", "Action"};

        @Override
        public int getRowCount() {
            return transactionsList.size();
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
            if (rowIndex < 0 || rowIndex >= transactionsList.size()) {
                return null;
            }
            TransactionSummaryDto tx = transactionsList.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> "#" + tx.id();
                case 1 -> tx.transactionTime() != null ? tx.transactionTime() : "—";
                case 2 -> tx.transactionType() != null ? tx.transactionType() : "—";
                case 3 -> tx.amount() != null ? String.format(Locale.US, "₹ %,.2f", tx.amount()) : "₹ 0.00";
                case 4 -> tx.status() != null ? tx.status() : "UNKNOWN";
                case 5 -> "Details →";
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
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
                    Color bg = "APPROVED".equalsIgnoreCase(status) ? new Color(220, 252, 231)
                            : "DECLINED".equalsIgnoreCase(status) ? new Color(254, 243, 199)
                            : new Color(254, 226, 226);
                    g2.setColor(bg);
                    g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 8, 10, 10);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            Color fg = "APPROVED".equalsIgnoreCase(status) ? COLOR_STATUS_APPROVED
                    : "DECLINED".equalsIgnoreCase(status) ? COLOR_STATUS_DECLINED
                    : COLOR_STATUS_BLOCKED;
            lbl.setForeground(fg);
            lbl.setOpaque(false);
            return lbl;
        }
    }

    // Accessors for testing and integration
    public JTable getTransactionTable() {
        return transactionTable;
    }

    public JTable getTransactionsTable() {
        return transactionTable;
    }

    public List<TransactionSummaryDto> getTransactionsList() {
        return Collections.unmodifiableList(transactionsList);
    }

    public List<TransactionSummaryDto> getDisplayedTransactions() {
        return Collections.unmodifiableList(transactionsList);
    }

    public JTextField getSearchField() {
        return txtSearch;
    }

    public JComboBox<String> getTypeFilter() {
        return cmbTypeFilter;
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

    public JButton getResetButton() {
        return btnResetFilters;
    }
}
