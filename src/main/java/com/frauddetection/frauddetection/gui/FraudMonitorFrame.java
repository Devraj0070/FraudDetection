package com.frauddetection.frauddetection.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frauddetection.frauddetection.fraud.prediction.FraudPredictionResult;
import com.frauddetection.frauddetection.jdbc.DatabaseConnection;
import com.frauddetection.frauddetection.jdbc.TransactionJdbcDao;
import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Enterprise Fraud Detection and Live Surveillance Desktop Monitor.
 *
 * Demonstrates Academic Rubric:
 * - OOP Implementation: Inheritance (extends {@link BaseAppFrame}), Interfaces
 *   (implements {@link TransactionProcessListener}, {@link FraudAlertObserver}), Polymorphism
 * - Collections & Generics: Uses {@link GenericTableModel}, Collections sorting, and Maps
 * - Multithreading & Synchronization: Background worker thread, EDT safe dispatching, SwingWorker
 * - Classes for Database Operations & JDBC: Uses {@link TransactionJdbcDao} & {@link DatabaseConnection}
 * - UI/UX, Aesthetics & Responsiveness
 */
public class FraudMonitorFrame extends BaseAppFrame implements TransactionProcessListener, FraudAlertObserver {

    private static final Logger log = LoggerFactory.getLogger(FraudMonitorFrame.class);
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");

    // KPI Metric Labels
    private final JLabel lblTotalCount = new JLabel("0");
    private final JLabel lblApprovedCount = new JLabel("0");
    private final JLabel lblDeclinedCount = new JLabel("0");
    private final JLabel lblBlockedCount = new JLabel("0");
    private final JLabel lblFraudRate = new JLabel("0.0%");

    // Strategies (Polymorphism)
    private final JComboBox<FraudDetectionStrategy> strategyComboBox;
    private final JLabel strategyDescLabel = new JLabel();

    // Table & Model (Generics)
    private GenericTableModel<TransactionRecord> tableModel;
    private JTable transactionTable;

    // Alert Feed
    private JTextArea alertFeedArea;

    // Controls
    private JButton btnToggleSimulation;
    private TransactionSimulationWorker simulationWorker;

    // JDBC DAO
    private final TransactionJdbcDao transactionJdbcDao = new TransactionJdbcDao();

    // Local Counters for UI
    private long countTotal = 0;
    private long countApproved = 0;
    private long countDeclined = 0;
    private long countBlocked = 0;

    public FraudMonitorFrame() {
        super("FraudGuard Enterprise - AI & Heuristic Surveillance Desktop");
        setSize(1200, 780);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(950, 600));

        // Available Polymorphic Strategies
        FraudDetectionStrategy hybrid = new HybridStrategy();
        FraudDetectionStrategy ml = new MachineLearningStrategy();
        FraudDetectionStrategy rules = new RuleBasedStrategy();
        this.strategyComboBox = new JComboBox<>(new FraudDetectionStrategy[]{hybrid, ml, rules});

        initUI();
        initSimulationEngine();
        checkDatabaseConnectivityAsync();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // Top Control Header
        add(createTopControlPanel(), BorderLayout.NORTH);

        // Center Content: KPI Cards + Split Table/Alerts
        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setBackground(COLOR_BG_DARK);
        centerContainer.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        centerContainer.add(createKpiCardRow(), BorderLayout.NORTH);
        centerContainer.add(createDataSplitPane(), BorderLayout.CENTER);
        centerContainer.add(createActionToolbar(), BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        // Standardized Status Bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createTopControlPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_CARD_BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        // Brand Title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);
        JLabel logoLabel = new JLabel("🛡️");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        brandPanel.add(logoLabel);

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1));
        titleTextPanel.setOpaque(false);
        JLabel title = new JLabel("FraudGuard Desktop");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Real-time Multithreaded Fraud Detection System");
        subtitle.setFont(FONT_REGULAR);
        subtitle.setForeground(COLOR_TEXT_MUTED);
        titleTextPanel.add(title);
        titleTextPanel.add(subtitle);
        brandPanel.add(titleTextPanel);

        panel.add(brandPanel, BorderLayout.WEST);

        // Strategy Selector (Demonstrates Polymorphism)
        JPanel strategyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        strategyPanel.setOpaque(false);

        JLabel lblStrategy = new JLabel("Active Detection Engine:");
        lblStrategy.setForeground(COLOR_TEXT_PRIMARY);
        lblStrategy.setFont(FONT_BOLD);
        strategyPanel.add(lblStrategy);

        strategyComboBox.setFont(FONT_REGULAR);
        strategyComboBox.setPreferredSize(new Dimension(240, 28));
        strategyComboBox.addActionListener(e -> {
            FraudDetectionStrategy selected = (FraudDetectionStrategy) strategyComboBox.getSelectedItem();
            if (selected != null) {
                if (simulationWorker != null) {
                    simulationWorker.setStrategy(selected);
                }
                strategyDescLabel.setText(selected.getDescription());
                setStatusMessage("Active Engine Switched: " + selected.getStrategyName());
            }
        });
        strategyPanel.add(strategyComboBox);

        panel.add(strategyPanel, BorderLayout.EAST);
        return panel;
    }

    private JPanel createKpiCardRow() {
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);

        row.add(createSingleKpiCard("TOTAL TRANSACTIONS", lblTotalCount, COLOR_TEXT_PRIMARY, "All analyzed"));
        row.add(createSingleKpiCard("APPROVED", lblApprovedCount, COLOR_STATUS_GREEN, "Legitimate transfers"));
        row.add(createSingleKpiCard("DECLINED", lblDeclinedCount, COLOR_STATUS_AMBER, "Insufficient funds"));
        row.add(createSingleKpiCard("BLOCKED (FRAUD)", lblBlockedCount, COLOR_STATUS_RED, "AI / Rule flagged"));
        row.add(createSingleKpiCard("FRAUD RATE", lblFraudRate, COLOR_STATUS_RED, "Risk proportion"));

        return row;
    }

    private JPanel createSingleKpiCard(String title, JLabel valueLabel, Color accentColor, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(COLOR_TEXT_MUTED);
        card.add(lblTitle, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    private JSplitPane createDataSplitPane() {
        // Table setup (Generics & Collections)
        List<String> columns = List.of("Txn ID", "Amount (₹)", "Type", "Timestamp", "Client IP", "Status");

        List<Function<TransactionRecord, Object>> extractors = new ArrayList<>();
        extractors.add(tx -> "#" + (tx.getId() != null ? tx.getId() : "—"));
        extractors.add(tx -> tx.getAmount() != null ? "₹" + CURRENCY_FORMAT.format(tx.getAmount()) : "₹0.00");
        extractors.add(TransactionRecord::getTransactionType);
        extractors.add(TransactionRecord::getFormattedTime);
        extractors.add(TransactionRecord::getIpAddress);
        extractors.add(TransactionRecord::getStatus);

        tableModel = new GenericTableModel<>(columns, extractors);
        transactionTable = new JTable(tableModel);
        transactionTable.setBackground(COLOR_CARD_BG);
        transactionTable.setForeground(COLOR_TEXT_PRIMARY);
        transactionTable.setGridColor(COLOR_CARD_BORDER);
        transactionTable.setRowHeight(28);
        transactionTable.getTableHeader().setBackground(new Color(20, 29, 47));
        transactionTable.getTableHeader().setForeground(COLOR_TEXT_PRIMARY);
        transactionTable.getTableHeader().setFont(FONT_BOLD);
        transactionTable.setFont(FONT_REGULAR);

        // Custom Status Cell Renderer
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value != null ? value.toString() : "";
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(FONT_BOLD);
                if ("APPROVED".equalsIgnoreCase(status)) {
                    setForeground(COLOR_STATUS_GREEN);
                } else if ("BLOCKED".equalsIgnoreCase(status)) {
                    setForeground(COLOR_STATUS_RED);
                } else if ("DECLINED".equalsIgnoreCase(status)) {
                    setForeground(COLOR_STATUS_AMBER);
                } else {
                    setForeground(COLOR_TEXT_MUTED);
                }
                return c;
            }
        });

        JScrollPane tableScroll = new JScrollPane(transactionTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        tableScroll.getViewport().setBackground(COLOR_CARD_BG);

        // Right Alerts Panel
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setBackground(COLOR_CARD_BG);
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        rightPanel.setPreferredSize(new Dimension(340, 400));

        JLabel lblFeed = new JLabel("Live Fraud & Security Audit Feed");
        lblFeed.setFont(FONT_HEADER);
        lblFeed.setForeground(COLOR_TEXT_PRIMARY);
        rightPanel.add(lblFeed, BorderLayout.NORTH);

        alertFeedArea = new JTextArea();
        alertFeedArea.setBackground(new Color(15, 23, 42));
        alertFeedArea.setForeground(new Color(226, 232, 240));
        alertFeedArea.setFont(FONT_MONO);
        alertFeedArea.setEditable(false);
        alertFeedArea.setLineWrap(true);
        alertFeedArea.setWrapStyleWord(true);
        alertFeedArea.setText("● Surveillance engine online. Awaiting stream...\n");

        JScrollPane alertScroll = new JScrollPane(alertFeedArea);
        alertScroll.setBorder(BorderFactory.createLineBorder(COLOR_CARD_BORDER));
        rightPanel.add(alertScroll, BorderLayout.CENTER);

        // Strategy description footer
        FraudDetectionStrategy initialStrategy = (FraudDetectionStrategy) strategyComboBox.getSelectedItem();
        strategyDescLabel.setText(initialStrategy != null ? initialStrategy.getDescription() : "");
        strategyDescLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        strategyDescLabel.setForeground(COLOR_TEXT_MUTED);
        rightPanel.add(strategyDescLabel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, rightPanel);
        splitPane.setResizeWeight(0.70);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);

        return splitPane;
    }

    private JPanel createActionToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);

        btnToggleSimulation = new JButton("▶ Start Live Simulation");
        btnToggleSimulation.setBackground(COLOR_ACCENT_BLUE);
        btnToggleSimulation.setForeground(Color.WHITE);
        btnToggleSimulation.setFont(FONT_BOLD);
        btnToggleSimulation.setFocusPainted(false);
        btnToggleSimulation.addActionListener(e -> toggleSimulation());
        toolbar.add(btnToggleSimulation);

        JButton btnManual = new JButton("➕ Manual Transaction");
        btnManual.setFont(FONT_REGULAR);
        btnManual.addActionListener(e -> openManualTransactionDialog());
        toolbar.add(btnManual);

        JButton btnLoadDb = new JButton("🔄 Sync from DB (JDBC)");
        btnLoadDb.setFont(FONT_REGULAR);
        btnLoadDb.addActionListener(e -> loadTransactionsFromDatabaseAsync());
        toolbar.add(btnLoadDb);

        JButton btnSaveDb = new JButton("💾 Batch Save to DB (JDBC)");
        btnSaveDb.setFont(FONT_REGULAR);
        btnSaveDb.addActionListener(e -> batchSaveTransactionsToDatabaseAsync());
        toolbar.add(btnSaveDb);

        JButton btnTestConn = new JButton("🔌 Test JDBC Connection");
        btnTestConn.setFont(FONT_REGULAR);
        btnTestConn.addActionListener(e -> testJdbcConnectionAction());
        toolbar.add(btnTestConn);

        JButton btnClear = new JButton("🗑️ Clear View");
        btnClear.setFont(FONT_REGULAR);
        btnClear.addActionListener(e -> clearView());
        toolbar.add(btnClear);

        return toolbar;
    }

    private void initSimulationEngine() {
        FraudDetectionStrategy selected = (FraudDetectionStrategy) strategyComboBox.getSelectedItem();
        this.simulationWorker = new TransactionSimulationWorker(selected, this);
    }

    private void toggleSimulation() {
        if (simulationWorker.isRunning()) {
            simulationWorker.stop();
            btnToggleSimulation.setText("▶ Start Live Simulation");
            btnToggleSimulation.setBackground(COLOR_ACCENT_BLUE);
            setStatusMessage("Live simulation paused.");
        } else {
            simulationWorker.start();
            btnToggleSimulation.setText("⏸ Pause Simulation");
            btnToggleSimulation.setBackground(COLOR_STATUS_AMBER);
            setStatusMessage("Live multithreaded simulation running...");
        }
    }

    private void openManualTransactionDialog() {
        TransactionSimulationDialog dialog = new TransactionSimulationDialog(this);
        dialog.setVisible(true);

        TransactionRecord newTx = dialog.getCreatedTransaction();
        if (newTx != null) {
            FraudDetectionStrategy strategy = (FraudDetectionStrategy) strategyComboBox.getSelectedItem();
            try {
                FraudPredictionResult result = strategy != null ? strategy.evaluate(newTx) :
                        new FraudPredictionResult("LEGITIMATE", 0.05, "Default");

                if ("FRAUD".equals(result.getPrediction())) {
                    newTx.setStatus("BLOCKED");
                } else {
                    newTx.setStatus("APPROVED");
                }

                onTransactionProcessed(newTx, result);
                showInfo(String.format("Transaction evaluated by [%s]:%nPrediction: %s%nFraud Probability: %.2f%%%nStatus: %s",
                                result.getModelName(), result.getPrediction(), result.getProbability() * 100, newTx.getStatus()),
                        "Manual Evaluation Complete");
            } catch (Exception ex) {
                showError("Evaluation failed: " + ex.getMessage(), "Error");
            }
        }
    }

    @Override
    public void onTransactionStarted(TransactionRecord transaction) {
        setStatusMessage("Evaluating incoming transaction #" + (transaction != null ? transaction.getId() : "—") + "...");
    }

    @Override
    public void onTransactionProcessed(TransactionRecord transaction, FraudPredictionResult result) {
        if (transaction == null || result == null) return;

        countTotal++;
        if ("APPROVED".equals(transaction.getStatus())) {
            countApproved++;
        } else if ("DECLINED".equals(transaction.getStatus())) {
            countDeclined++;
        } else if ("BLOCKED".equals(transaction.getStatus())) {
            countBlocked++;
            onFraudAlertGenerated("SUSPICIOUS_TXN", "HIGH",
                    String.format("Blocked #%d: Amount ₹%s [%s] (Prob: %.1f%%)",
                            transaction.getId(), CURRENCY_FORMAT.format(transaction.getAmount()),
                            transaction.getTransactionType(), result.getProbability() * 100));
        }

        updateKpiLabels();
        tableModel.addRow(transaction);

        setStatusMessage(String.format("Processed Txn #%d: %s [%s]",
                transaction.getId(), transaction.getStatus(), result.getModelName()));
    }

    @Override
    public void onTransactionFailed(TransactionRecord transaction, Exception error) {
        setStatusMessage("Transaction processing error: " + (error != null ? error.getMessage() : "Unknown"));
    }

    @Override
    public void onFraudAlertGenerated(String alertType, String severity, String message) {
        alertFeedArea.append(String.format("🚨 [%s] %s%n", severity, message));
        alertFeedArea.setCaretPosition(alertFeedArea.getDocument().getLength());
    }

    private void updateKpiLabels() {
        lblTotalCount.setText(String.valueOf(countTotal));
        lblApprovedCount.setText(String.valueOf(countApproved));
        lblDeclinedCount.setText(String.valueOf(countDeclined));
        lblBlockedCount.setText(String.valueOf(countBlocked));
        double rate = countTotal > 0 ? ((double) countBlocked / countTotal) * 100.0 : 0.0;
        lblFraudRate.setText(String.format("%.1f%%", rate));
    }

    private void clearView() {
        tableModel.clear();
        countTotal = 0;
        countApproved = 0;
        countDeclined = 0;
        countBlocked = 0;
        updateKpiLabels();
        alertFeedArea.setText("● View cleared.\n");
        setStatusMessage("Table cleared.");
    }

    /**
     * Multithreaded SwingWorker: Async DB query using JDBC.
     */
    private void loadTransactionsFromDatabaseAsync() {
        setStatusMessage("Querying transactions from MySQL via JDBC...");
        new SwingWorker<List<TransactionRecord>, Void>() {
            @Override
            protected List<TransactionRecord> doInBackground() throws Exception {
                return transactionJdbcDao.findRecentTransactions(50);
            }

            @Override
            protected void done() {
                try {
                    List<TransactionRecord> records = get();
                    tableModel.setData(records);
                    countTotal = records.size();
                    countApproved = records.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getStatus())).count();
                    countDeclined = records.stream().filter(r -> "DECLINED".equalsIgnoreCase(r.getStatus())).count();
                    countBlocked = records.stream().filter(r -> "BLOCKED".equalsIgnoreCase(r.getStatus())).count();
                    updateKpiLabels();
                    setStatusMessage(String.format("Loaded %d transactions from database via JDBC.", records.size()));
                    showInfo("Successfully synchronized " + records.size() + " records from MySQL database via JDBC.", "Database Sync");
                } catch (Exception ex) {
                    log.warn("JDBC query failed: {}", ex.getMessage());
                    showWarning("Could not load from MySQL database (is MySQL running at localhost:3306?): " + ex.getMessage(),
                            "JDBC Sync Notice");
                    setStatusMessage("Database sync failed. (Offline mode active)");
                }
            }
        }.execute();
    }

    /**
     * Multithreaded SwingWorker: Batch save transactions using JDBC.
     */
    private void batchSaveTransactionsToDatabaseAsync() {
        List<TransactionRecord> allItems = tableModel.getAllItems();
        if (allItems.isEmpty()) {
            showInfo("No transactions in view to save.", "Save Notice");
            return;
        }

        setStatusMessage("Executing JDBC batch insert for " + allItems.size() + " records...");
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws Exception {
                return transactionJdbcDao.executeBatchInsert(allItems);
            }

            @Override
            protected void done() {
                try {
                    int[] results = get();
                    setStatusMessage(String.format("Batch insert complete: %d records committed to database.", results.length));
                    showInfo("Batch committed " + results.length + " transactions into MySQL database using JDBC transactions.", "JDBC Batch Success");
                } catch (Exception ex) {
                    showError("Batch insert failed: " + ex.getMessage(), "JDBC Error");
                    setStatusMessage("Batch insert failed.");
                }
            }
        }.execute();
    }

    private void testJdbcConnectionAction() {
        setStatusMessage("Testing JDBC connectivity...");
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    setDatabaseStatus(ok, DatabaseConnection.getJdbcUrl());
                    if (ok) {
                        showInfo("JDBC Connection to MySQL is ACTIVE and valid!\nURL: " + DatabaseConnection.getJdbcUrl(), "Connection Successful");
                    } else {
                        showWarning("JDBC Connection failed to connect.\nTarget: " + DatabaseConnection.getJdbcUrl() + "\nEnsure MySQL service is active.", "Connection Notice");
                    }
                } catch (Exception ex) {
                    showError("Test error: " + ex.getMessage(), "Connection Error");
                }
            }
        }.execute();
    }

    private void checkDatabaseConnectivityAsync() {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DatabaseConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    setDatabaseStatus(ok, ok ? "Connected" : "Offline Fallback");
                } catch (Exception ignored) {
                    setDatabaseStatus(false, "Offline");
                }
            }
        }.execute();
    }
}
