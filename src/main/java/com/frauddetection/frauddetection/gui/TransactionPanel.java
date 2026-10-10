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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.TransactionRequest;
import com.frauddetection.frauddetection.dto.TransactionResponse;
import com.frauddetection.frauddetection.entity.TransactionType;

/**
 * Pure Java Swing and Java 2D New Transaction screen for SafePay desktop client.
 * Follows the Finora / SafePay purple/lavender design system:
 * - Rounded white cards with subtle borders and drop shadows
 * - Strict client-side validation (positive amount, scale <= 2)
 * - Anti-duplicate submission protection
 * - Comprehensive result receipt displaying APPROVED, DECLINED, or BLOCKED outcomes
 * - Automatic dashboard ledger synchronization on completion
 */
public class TransactionPanel extends JPanel {

    public static final String VIEW_FORM = "VIEW_FORM";
    public static final String VIEW_RESULT = "VIEW_RESULT";

    // Design Tokens matching DashboardFrame
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
    private final Runnable onTransactionSuccess;
    private final Runnable onNavigateToDashboard;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);

    // Form Controls
    private JComboBox<TransactionTypeItem> cmbType;
    private JTextField txtAmount;
    private JLabel lblLimitNotice;
    private JLabel lblValidationError;
    private JLabel lblProcessingNotice;
    private JButton btnSubmit;
    private JButton[] presetChips;

    // Result Controls
    private JPanel resultBannerCard;
    private JLabel lblResultTitle;
    private JLabel lblResultSubtitle;
    private JLabel lblReceiptId;
    private JLabel lblReceiptAmount;
    private JLabel lblReceiptType;
    private JLabel lblReceiptStatus;
    private JLabel lblReceiptTime;
    private JLabel lblReceiptRisk;
    private JLabel lblReceiptReason;
    private JLabel lblReceiptBalance;

    private boolean isSubmitting = false;

    public TransactionPanel(SafePayApiClient apiClient, Runnable onTransactionSuccess, Runnable onNavigateToDashboard) {
        this.apiClient = apiClient;
        this.onTransactionSuccess = onTransactionSuccess;
        this.onNavigateToDashboard = onNavigateToDashboard;

        setLayout(new BorderLayout());
        setOpaque(false);

        containerPanel.setOpaque(false);
        containerPanel.add(createFormScreen(), VIEW_FORM);
        containerPanel.add(createResultScreen(), VIEW_RESULT);

        add(containerPanel, BorderLayout.CENTER);
    }

    // =========================================================================
    // 1. TRANSACTION FORM SCREEN
    // =========================================================================

    private JScrollPane createFormScreen() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(6, 18, 18, 18));

        // 1. Header Hero Card
        content.add(createHeaderHeroCard());
        content.add(Box.createVerticalStrut(14));

        // 2. Two-Column Layout (Form Card on Left + Info & Security on Right)
        JPanel columnsPanel = new JPanel(new BorderLayout(14, 0));
        columnsPanel.setOpaque(false);

        // Left Column: Interactive Input Card
        JPanel leftColumn = createInteractiveFormCard();
        columnsPanel.add(leftColumn, BorderLayout.CENTER);

        // Right Column: Limits and Security Info
        JPanel rightColumn = createSecurityAndLimitsCard();
        columnsPanel.add(rightColumn, BorderLayout.EAST);

        content.add(columnsPanel);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createHeaderHeroCard() {
        JPanel hero = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Soft subtle hero card background with light purple gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(248, 247, 252),
                        w, h, new Color(243, 232, 255, 120)
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
        hero.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel textCluster = new JPanel();
        textCluster.setLayout(new BoxLayout(textCluster, BoxLayout.Y_AXIS));
        textCluster.setOpaque(false);

        JLabel lblTitle = new JLabel("Initiate New Transaction");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        textCluster.add(lblTitle);
        textCluster.add(Box.createVerticalStrut(4));

        JLabel lblSub = new JLabel("Fast, secure funds processing protected by real-time Machine Learning and behavioral anomaly filters.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_SUBTLE);
        textCluster.add(lblSub);

        hero.add(textCluster, BorderLayout.CENTER);

        // Security Shield Badge on the right
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(243, 232, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(196, 181, 253));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JLabel lblBadgeText = new JLabel("🛡 AI Fraud Protection Active");
        lblBadgeText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadgeText.setForeground(COLOR_PURPLE_PRIMARY);
        badge.add(lblBadgeText);

        hero.add(badge, BorderLayout.EAST);
        return hero;
    }

    private JPanel createInteractiveFormCard() {
        JPanel card = DashboardFrame.createStyledCardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));

        JLabel lblSec1 = new JLabel("Transaction Details");
        lblSec1.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblSec1.setForeground(COLOR_TEXT_MAIN);
        card.add(lblSec1);
        card.add(Box.createVerticalStrut(14));

        // 1. Transaction Type Dropdown
        JLabel lblTypePrompt = new JLabel("Select Transaction Type");
        lblTypePrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTypePrompt.setForeground(COLOR_TEXT_MAIN);
        card.add(lblTypePrompt);
        card.add(Box.createVerticalStrut(6));

        TransactionTypeItem[] typeItems = {
                new TransactionTypeItem(TransactionType.PAYMENT, "Payment", "Merchant & Peer-to-Peer payment", "Max ₹100,000"),
                new TransactionTypeItem(TransactionType.TRANSFER, "Transfer", "Account-to-Account funds transfer", "Max ₹200,000"),
                new TransactionTypeItem(TransactionType.CASH_OUT, "Cash Out", "ATM & Cash withdrawal", "Max ₹50,000"),
                new TransactionTypeItem(TransactionType.DEBIT, "Debit", "Direct debit card payment", "Max ₹100,000"),
                new TransactionTypeItem(TransactionType.CASH_IN, "Cash In", "Cash deposit & balance top-up", "Max ₹50,000")
        };

        cmbType = new JComboBox<>(typeItems);
        cmbType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbType.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        cmbType.setPreferredSize(new Dimension(420, 40));
        cmbType.setBackground(COLOR_INPUT_BG);
        cmbType.setRenderer(new TransactionTypeCellRenderer());
        cmbType.addActionListener(e -> updateLimitNotice());
        card.add(cmbType);
        card.add(Box.createVerticalStrut(6));

        // Limit Notice Pill
        lblLimitNotice = new JLabel();
        lblLimitNotice.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLimitNotice.setForeground(COLOR_TEXT_SUBTLE);
        updateLimitNotice();
        card.add(lblLimitNotice);
        card.add(Box.createVerticalStrut(16));

        // 2. Amount Input Field
        JLabel lblAmountPrompt = new JLabel("Transaction Amount (₹ INR)");
        lblAmountPrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblAmountPrompt.setForeground(COLOR_TEXT_MAIN);
        card.add(lblAmountPrompt);
        card.add(Box.createVerticalStrut(6));

        JPanel amountInputContainer = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_INPUT_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(txtAmount.hasFocus() ? COLOR_PURPLE_PRIMARY : COLOR_BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(txtAmount.hasFocus() ? 1.6f : 1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        amountInputContainer.setOpaque(false);
        amountInputContainer.setMaximumSize(new Dimension(Short.MAX_VALUE, 46));
        amountInputContainer.setPreferredSize(new Dimension(420, 46));
        amountInputContainer.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        JLabel lblRupee = new JLabel("₹");
        lblRupee.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblRupee.setForeground(COLOR_PURPLE_PRIMARY);
        amountInputContainer.add(lblRupee, BorderLayout.WEST);

        txtAmount = new JTextField();
        txtAmount.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtAmount.setForeground(COLOR_TEXT_MAIN);
        txtAmount.setOpaque(false);
        txtAmount.setBorder(null);
        txtAmount.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                amountInputContainer.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                amountInputContainer.repaint();
            }
        });
        amountInputContainer.add(txtAmount, BorderLayout.CENTER);
        card.add(amountInputContainer);
        card.add(Box.createVerticalStrut(10));

        // Preset Amount Chips (₹500, ₹1,000, ₹5,000, ₹15,000, ₹50,000)
        JPanel presetsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        presetsRow.setOpaque(false);

        String[] presets = {"500", "1,000", "5,000", "15,000", "50,000"};
        presetChips = new JButton[presets.length];
        for (int i = 0; i < presets.length; i++) {
            String val = presets[i];
            JButton chip = createPresetChip("₹ " + val);
            chip.addActionListener(e -> {
                txtAmount.setText(val.replace(",", ""));
                txtAmount.requestFocusInWindow();
                lblValidationError.setText(" ");
            });
            presetChips[i] = chip;
            presetsRow.add(chip);
        }
        card.add(presetsRow);
        card.add(Box.createVerticalStrut(14));

        // Client-side Validation Error Label
        lblValidationError = new JLabel(" ");
        lblValidationError.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblValidationError.setForeground(COLOR_STATUS_BLOCKED);
        card.add(lblValidationError);
        card.add(Box.createVerticalStrut(8));

        // Submitting / Processing Notification Label
        lblProcessingNotice = new JLabel(" ");
        lblProcessingNotice.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblProcessingNotice.setForeground(COLOR_PURPLE_PRIMARY);
        card.add(lblProcessingNotice);
        card.add(Box.createVerticalStrut(10));

        // 3. Submit Action Button
        btnSubmit = DashboardFrame.createGradientPillButton("Authorize & Process Transaction →", 320, 44);
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSubmit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> executeTransactionSubmission());
        card.add(btnSubmit);

        return card;
    }

    private JButton createPresetChip(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(new Color(233, 213, 255));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(243, 232, 255));
                } else {
                    g2.setColor(new Color(248, 250, 252));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(COLOR_TEXT_MAIN);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(74, 28));
        return btn;
    }

    private JPanel createSecurityAndLimitsCard() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setPreferredSize(new Dimension(320, 0));

        // 1. Transaction Limits Reference Table
        JPanel limitsCard = DashboardFrame.createStyledCardPanel();
        limitsCard.setLayout(new BoxLayout(limitsCard, BoxLayout.Y_AXIS));
        limitsCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel lblLimitsTitle = new JLabel("Transaction Type Limits");
        lblLimitsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLimitsTitle.setForeground(COLOR_TEXT_MAIN);
        limitsCard.add(lblLimitsTitle);
        limitsCard.add(Box.createVerticalStrut(10));

        limitsCard.add(createLimitRow("Transfer", "₹ 200,000", "Account-to-Account"));
        limitsCard.add(Box.createVerticalStrut(6));
        limitsCard.add(createLimitRow("Payment", "₹ 100,000", "Peer / Merchant"));
        limitsCard.add(Box.createVerticalStrut(6));
        limitsCard.add(createLimitRow("Debit", "₹ 100,000", "Direct Debit"));
        limitsCard.add(Box.createVerticalStrut(6));
        limitsCard.add(createLimitRow("Cash Out", "₹ 50,000", "ATM Withdrawal"));
        limitsCard.add(Box.createVerticalStrut(6));
        limitsCard.add(createLimitRow("Cash In", "₹ 50,000", "Cash Deposit"));

        container.add(limitsCard);
        container.add(Box.createVerticalStrut(12));

        // 2. AI Security Assurance Card
        JPanel secCard = DashboardFrame.createStyledCardPanel();
        secCard.setLayout(new BoxLayout(secCard, BoxLayout.Y_AXIS));
        secCard.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel lblSecTitle = new JLabel("SafePay AI Fraud Inspection");
        lblSecTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSecTitle.setForeground(COLOR_PURPLE_PRIMARY);
        secCard.add(lblSecTitle);
        secCard.add(Box.createVerticalStrut(8));

        secCard.add(createBulletPoint("Weka Random Forest model risk classification"));
        secCard.add(Box.createVerticalStrut(4));
        secCard.add(createBulletPoint("Behavioral velocity & anomaly rule engine"));
        secCard.add(Box.createVerticalStrut(4));
        secCard.add(createBulletPoint("Real-time balance deduction on APPROVED status"));
        secCard.add(Box.createVerticalStrut(4));
        secCard.add(createBulletPoint("Immutable audit trail for all transaction events"));

        container.add(secCard);
        return container;
    }

    private JPanel createLimitRow(String type, String limit, String desc) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel lblT = new JLabel(type);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblT.setForeground(COLOR_TEXT_MAIN);
        left.add(lblT);

        JLabel lblD = new JLabel(desc);
        lblD.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblD.setForeground(COLOR_TEXT_SUBTLE);
        left.add(lblD);

        row.add(left, BorderLayout.WEST);

        JLabel lblL = new JLabel(limit);
        lblL.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblL.setForeground(COLOR_PURPLE_PRIMARY);
        row.add(lblL, BorderLayout.EAST);

        return row;
    }

    private JPanel createBulletPoint(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        JLabel dot = new JLabel("•");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dot.setForeground(COLOR_PURPLE_PRIMARY);
        row.add(dot);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COLOR_TEXT_SUBTLE);
        row.add(lbl);
        return row;
    }

    private void updateLimitNotice() {
        TransactionTypeItem item = (TransactionTypeItem) cmbType.getSelectedItem();
        if (item != null) {
            lblLimitNotice.setText("ℹ Maximum transaction limit for " + item.type.name() + " is " + item.limitText + " INR.");
        }
    }

    // =========================================================================
    // 2. TRANSACTION SUBMISSION & VALIDATION
    // =========================================================================

    public void executeTransactionSubmission() {
        if (isSubmitting) return;

        // Reset validation label
        lblValidationError.setText(" ");
        lblProcessingNotice.setText(" ");

        String rawAmount = txtAmount.getText().trim();
        if (rawAmount.isEmpty()) {
            lblValidationError.setText("Please enter a transaction amount.");
            txtAmount.requestFocusInWindow();
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount);
        } catch (NumberFormatException e) {
            lblValidationError.setText("Invalid amount format. Please enter a valid decimal number (e.g. 1500.00).");
            txtAmount.requestFocusInWindow();
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            lblValidationError.setText("Amount must be greater than zero.");
            txtAmount.requestFocusInWindow();
            return;
        }

        if (amount.scale() > 2) {
            lblValidationError.setText("Amount cannot have more than 2 decimal places.");
            txtAmount.requestFocusInWindow();
            return;
        }

        TransactionTypeItem selectedItem = (TransactionTypeItem) cmbType.getSelectedItem();
        if (selectedItem == null) {
            lblValidationError.setText("Please select a transaction type.");
            return;
        }

        // Set submittal state (prevents duplicate submission)
        setSubmittingState(true);
        lblProcessingNotice.setText("⏳ Evaluating transaction with SafePay AI Fraud Protection...");

        final TransactionRequest req = new TransactionRequest(amount, selectedItem.type.name());

        new SwingWorker<TransactionResponse, Void>() {
            @Override
            protected TransactionResponse doInBackground() throws Exception {
                return apiClient.createTransaction(req);
            }

            @Override
            protected void done() {
                try {
                    TransactionResponse response = get();
                    if (response != null) {
                        displayResultScreen(response);
                        if (onTransactionSuccess != null) {
                            onTransactionSuccess.run();
                        }
                    } else {
                        lblValidationError.setText("Received empty response from transaction service.");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String errorMsg = cause.getMessage() != null ? cause.getMessage() : "Transaction failed.";
                    lblValidationError.setText("Notice: " + errorMsg);
                } finally {
                    setSubmittingState(false);
                    lblProcessingNotice.setText(" ");
                }
            }
        }.execute();
    }

    private void setSubmittingState(boolean submitting) {
        this.isSubmitting = submitting;
        btnSubmit.setEnabled(!submitting);
        txtAmount.setEnabled(!submitting);
        cmbType.setEnabled(!submitting);
        for (JButton chip : presetChips) {
            chip.setEnabled(!submitting);
        }
    }

    // =========================================================================
    // 3. TRANSACTION RESULT SCREEN (RECEIPT & DECISION)
    // =========================================================================

    private JScrollPane createResultScreen() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(10, 18, 18, 18));

        // 1. Result Status Hero Banner
        resultBannerCard = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                Color c1 = COLOR_STATUS_APPROVED;
                Color c2 = new Color(21, 128, 61);

                String st = lblReceiptStatus != null ? lblReceiptStatus.getText() : "APPROVED";
                if ("BLOCKED".equalsIgnoreCase(st)) {
                    c1 = COLOR_STATUS_BLOCKED;
                    c2 = new Color(185, 28, 28);
                } else if ("DECLINED".equalsIgnoreCase(st)) {
                    c1 = COLOR_STATUS_DECLINED;
                    c2 = new Color(180, 83, 9);
                }

                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                // Watermark decorative vector circles
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(w - 120, -30, 160, 160);
                g2.fillOval(w - 70, 20, 100, 100);

                g2.dispose();
            }
        };
        resultBannerCard.setOpaque(false);
        resultBannerCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        resultBannerCard.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        JPanel bannerTextCluster = new JPanel();
        bannerTextCluster.setLayout(new BoxLayout(bannerTextCluster, BoxLayout.Y_AXIS));
        bannerTextCluster.setOpaque(false);

        lblResultTitle = new JLabel("Transaction Approved");
        lblResultTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblResultTitle.setForeground(Color.WHITE);
        bannerTextCluster.add(lblResultTitle);
        bannerTextCluster.add(Box.createVerticalStrut(4));

        lblResultSubtitle = new JLabel("Your payment has been authorized and settled.");
        lblResultSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblResultSubtitle.setForeground(new Color(241, 245, 249));
        bannerTextCluster.add(lblResultSubtitle);

        resultBannerCard.add(bannerTextCluster, BorderLayout.CENTER);
        content.add(resultBannerCard);
        content.add(Box.createVerticalStrut(14));

        // 2. Receipt Details Card
        JPanel receiptCard = DashboardFrame.createStyledCardPanel();
        receiptCard.setLayout(new BoxLayout(receiptCard, BoxLayout.Y_AXIS));
        receiptCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblReceiptHeader = new JLabel("Official Audit Receipt");
        lblReceiptHeader.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblReceiptHeader.setForeground(COLOR_TEXT_MAIN);
        receiptCard.add(lblReceiptHeader);
        receiptCard.add(Box.createVerticalStrut(14));

        lblReceiptId = new JLabel();
        lblReceiptAmount = new JLabel();
        lblReceiptType = new JLabel();
        lblReceiptStatus = new JLabel();
        lblReceiptTime = new JLabel();
        lblReceiptRisk = new JLabel();
        lblReceiptReason = new JLabel();
        lblReceiptBalance = new JLabel();

        receiptCard.add(createReceiptRow("Transaction ID", lblReceiptId));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Amount", lblReceiptAmount));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Type", lblReceiptType));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Final Status", lblReceiptStatus));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Timestamp", lblReceiptTime));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Risk Evaluation", lblReceiptRisk));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Detection Reason", lblReceiptReason));
        receiptCard.add(createDivider());
        receiptCard.add(createReceiptRow("Available Balance", lblReceiptBalance));

        content.add(receiptCard);
        content.add(Box.createVerticalStrut(16));

        // 3. Navigation & Reset Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        actionRow.setOpaque(false);

        JButton btnNewTx = DashboardFrame.createGradientPillButton("Initiate Another Transaction", 220, 42);
        btnNewTx.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnNewTx.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnNewTx.addActionListener(e -> resetFormAndShow());
        actionRow.add(btnNewTx);

        JButton btnBackDash = new JButton("Return to Dashboard") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(COLOR_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnBackDash.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBackDash.setForeground(COLOR_TEXT_MAIN);
        btnBackDash.setOpaque(false);
        btnBackDash.setContentAreaFilled(false);
        btnBackDash.setBorderPainted(false);
        btnBackDash.setFocusPainted(false);
        btnBackDash.setPreferredSize(new Dimension(180, 42));
        btnBackDash.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBackDash.addActionListener(e -> {
            if (onNavigateToDashboard != null) {
                onNavigateToDashboard.run();
            }
        });
        actionRow.add(btnBackDash);

        content.add(actionRow);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private JPanel createReceiptRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 26));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(COLOR_TEXT_SUBTLE);
        row.add(lbl, BorderLayout.WEST);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueLabel.setForeground(COLOR_TEXT_MAIN);
        row.add(valueLabel, BorderLayout.EAST);

        return row;
    }

    private JComponent createDivider() {
        JComponent comp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(COLOR_CARD_OUTLINE);
                g.drawLine(0, 3, getWidth(), 3);
            }
        };
        comp.setMaximumSize(new Dimension(Short.MAX_VALUE, 6));
        comp.setPreferredSize(new Dimension(0, 6));
        return comp;
    }

    public void displayResultScreen(TransactionResponse res) {
        String status = res.status() != null ? res.status().toUpperCase() : "UNKNOWN";

        if ("APPROVED".equals(status)) {
            lblResultTitle.setText("Transaction Approved & Settled");
            lblResultSubtitle.setText("Your payment was evaluated as legitimate and processed successfully.");
            lblReceiptStatus.setForeground(COLOR_STATUS_APPROVED);
        } else if ("BLOCKED".equals(status)) {
            lblResultTitle.setText("Transaction Blocked by SafePay AI");
            lblResultSubtitle.setText("High-risk or anomalous activity flagged by AI Fraud Protection.");
            lblReceiptStatus.setForeground(COLOR_STATUS_BLOCKED);
        } else {
            lblResultTitle.setText("Transaction Declined");
            lblResultSubtitle.setText("The request was declined based on business rules or account constraints.");
            lblReceiptStatus.setForeground(COLOR_STATUS_DECLINED);
        }

        lblReceiptId.setText(res.transactionId() != null ? "#" + res.transactionId() : "N/A");
        lblReceiptAmount.setText(res.amount() != null ? DashboardFrame.formatCurrency(res.amount()) + " INR" : "₹ 0.00");
        lblReceiptType.setText(res.transactionType() != null ? res.transactionType() : "PAYMENT");
        lblReceiptStatus.setText(status);
        lblReceiptTime.setText(res.transactionTime() != null ? res.transactionTime() : "Just now");

        String probStr = res.fraudProbability() != null
                ? String.format(Locale.ENGLISH, " (ML Risk: %.1f%%)", res.fraudProbability() * 100)
                : "";
        lblReceiptRisk.setText((res.riskAssessment() != null ? res.riskAssessment() : "LOW") + probStr);
        lblReceiptReason.setText(res.detectionReason() != null ? res.detectionReason() : "Normal / Legitimate");
        lblReceiptBalance.setText(res.newBalance() != null ? DashboardFrame.formatCurrency(res.newBalance()) + " INR" : "N/A");

        resultBannerCard.repaint();
        cardLayout.show(containerPanel, VIEW_RESULT);
    }

    public void resetFormAndShow() {
        txtAmount.setText("");
        lblValidationError.setText(" ");
        lblProcessingNotice.setText(" ");
        cardLayout.show(containerPanel, VIEW_FORM);
        txtAmount.requestFocusInWindow();
    }

    // Accessors for unit testing
    public JComboBox<TransactionTypeItem> getCmbType() {
        return cmbType;
    }

    public JTextField getTxtAmount() {
        return txtAmount;
    }

    public JLabel getLblValidationError() {
        return lblValidationError;
    }

    public JButton getBtnSubmit() {
        return btnSubmit;
    }

    public JLabel getLblResultTitle() {
        return lblResultTitle;
    }

    public JLabel getLblReceiptStatus() {
        return lblReceiptStatus;
    }

    public JLabel getLblReceiptAmount() {
        return lblReceiptAmount;
    }

    public JLabel getLblReceiptId() {
        return lblReceiptId;
    }

    public JLabel getLblReceiptRisk() {
        return lblReceiptRisk;
    }

    public JLabel getLblReceiptReason() {
        return lblReceiptReason;
    }

    public JLabel getLblReceiptBalance() {
        return lblReceiptBalance;
    }

    // =========================================================================
    // INNER CLASSES: TRANSACTION TYPE ITEM & RENDERER
    // =========================================================================

    public static class TransactionTypeItem {
        public final TransactionType type;
        public final String title;
        public final String description;
        public final String limitText;

        public TransactionTypeItem(TransactionType type, String title, String description, String limitText) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.limitText = limitText;
        }

        @Override
        public String toString() {
            return title + " (" + limitText + ")";
        }
    }

    private static class TransactionTypeCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TransactionTypeItem item) {
                label.setText(item.title + " — " + item.description + " [" + item.limitText + "]");
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            }
            return label;
        }
    }
}
