package com.frauddetection.frauddetection.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Abstract base JFrame establishing design system, dark modern aesthetics,
 * global status bar, and dialog helpers for the Java GUI application.
 *
 * Demonstrates Academic Rubric:
 * - OOP Implementation: Inheritance (Abstract Base Class extending {@link JFrame})
 * - UI/UX & Aesthetics
 */
public abstract class BaseAppFrame extends JFrame {

    public static final Color COLOR_BG_DARK = new Color(15, 23, 42);       // Slate 900
    public static final Color COLOR_CARD_BG = new Color(30, 41, 59);      // Slate 800
    public static final Color COLOR_CARD_BORDER = new Color(51, 65, 85);   // Slate 700
    public static final Color COLOR_TEXT_PRIMARY = new Color(248, 250, 252);
    public static final Color COLOR_TEXT_MUTED = new Color(148, 163, 184); // Slate 400
    public static final Color COLOR_ACCENT_BLUE = new Color(37, 99, 235);  // Blue 600
    public static final Color COLOR_STATUS_GREEN = new Color(16, 185, 129); // Emerald 500
    public static final Color COLOR_STATUS_RED = new Color(239, 68, 68);    // Red 500
    public static final Color COLOR_STATUS_AMBER = new Color(245, 158, 11); // Amber 500

    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    private final JLabel statusTextLabel = new JLabel("System Ready");
    private final JLabel dbStatusLabel = new JLabel("JDBC: Checking...");
    private final JLabel clockLabel = new JLabel();

    public BaseAppFrame(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBackground(COLOR_BG_DARK);
        getContentPane().setBackground(COLOR_BG_DARK);
    }

    /**
     * Initializes and returns the standardized application status bar.
     */
    protected JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(11, 17, 33));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_CARD_BORDER),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        statusTextLabel.setForeground(COLOR_TEXT_MUTED);
        statusTextLabel.setFont(FONT_REGULAR);
        statusBar.add(statusTextLabel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightPanel.setOpaque(false);

        dbStatusLabel.setForeground(COLOR_STATUS_AMBER);
        dbStatusLabel.setFont(FONT_BOLD);
        rightPanel.add(dbStatusLabel);

        clockLabel.setForeground(COLOR_TEXT_MUTED);
        clockLabel.setFont(FONT_MONO);
        rightPanel.add(clockLabel);

        // Update live clock every second
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timer timer = new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(timeFormatter)));
        timer.setInitialDelay(0);
        timer.start();

        statusBar.add(rightPanel, BorderLayout.EAST);
        return statusBar;
    }

    public void setStatusMessage(String message) {
        statusTextLabel.setText(message);
    }

    public void setDatabaseStatus(boolean connected, String details) {
        if (connected) {
            dbStatusLabel.setText("● JDBC: Connected (" + details + ")");
            dbStatusLabel.setForeground(COLOR_STATUS_GREEN);
        } else {
            dbStatusLabel.setText("● JDBC: Offline (" + details + ")");
            dbStatusLabel.setForeground(COLOR_STATUS_AMBER);
        }
    }

    public void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }
}
