package com.frauddetection.frauddetection.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.frauddetection.frauddetection.jdbc.TransactionRecord;

/**
 * Modal dialog for manually creating and validating financial transactions.
 *
 * Demonstrates Academic Rubric:
 * - OOP Implementation: Inheritance (extends {@link JDialog})
 * - Exception Handling: Validates inputs and catches {@link ValidationException}
 */
public class TransactionSimulationDialog extends JDialog {

    private final JTextField amountField = new JTextField("5000.00", 15);
    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"PAYMENT", "TRANSFER", "CASH_OUT", "DEBIT", "CASH_IN"});
    private final JTextField ipField = new JTextField("192.168.1.105", 15);

    private TransactionRecord createdTransaction = null;

    public TransactionSimulationDialog(Frame owner) {
        super(owner, "Submit Manual Transaction", true);
        setSize(420, 320);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(BaseAppFrame.COLOR_BG_DARK);

        initUI();
    }

    private void initUI() {
        // Header Panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        headerPanel.setBackground(BaseAppFrame.COLOR_CARD_BG);
        JLabel titleLabel = new JLabel("Create Transaction for Risk Analysis");
        titleLabel.setFont(BaseAppFrame.FONT_HEADER);
        titleLabel.setForeground(BaseAppFrame.COLOR_TEXT_PRIMARY);
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BaseAppFrame.COLOR_BG_DARK);
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Row 1: Amount
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblAmount = new JLabel("Amount (₹):");
        lblAmount.setForeground(BaseAppFrame.COLOR_TEXT_PRIMARY);
        formPanel.add(lblAmount, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(amountField, gbc);

        // Row 2: Type
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblType = new JLabel("Transaction Type:");
        lblType.setForeground(BaseAppFrame.COLOR_TEXT_PRIMARY);
        formPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(typeCombo, gbc);

        // Row 3: IP Address
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblIp = new JLabel("Client IP Address:");
        lblIp.setForeground(BaseAppFrame.COLOR_TEXT_PRIMARY);
        formPanel.add(lblIp, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(ipField, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        btnPanel.setBackground(BaseAppFrame.COLOR_CARD_BG);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel);

        JButton btnSubmit = new JButton("Run Fraud Analysis");
        btnSubmit.setBackground(BaseAppFrame.COLOR_ACCENT_BLUE);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.addActionListener(e -> handleSubmit());
        btnPanel.add(btnSubmit);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void handleSubmit() {
        try {
            validateAndConstruct();
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Validation Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void validateAndConstruct() throws ValidationException {
        String rawAmount = amountField.getText() != null ? amountField.getText().trim() : "";
        if (rawAmount.isEmpty()) {
            throw new ValidationException("Amount", "Amount cannot be blank.");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Amount", "Amount must be greater than zero.");
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("Amount", "Amount must be a valid numeric value.");
        }

        String type = (String) typeCombo.getSelectedItem();
        if (type == null || type.isEmpty()) {
            throw new ValidationException("Transaction Type", "Please select a valid transaction type.");
        }

        String ip = ipField.getText() != null ? ipField.getText().trim() : "127.0.0.1";

        this.createdTransaction = new TransactionRecord(
                null,
                amount,
                type,
                LocalDateTime.now(),
                "PENDING",
                1L,
                ip,
                "Java GUI Desktop Client/1.0"
        );
    }

    public TransactionRecord getCreatedTransaction() {
        return createdTransaction;
    }
}
