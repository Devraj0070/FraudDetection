package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import com.frauddetection.frauddetection.client.ApiClientException;
import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.AuthResponse;

/**
 * Premium Java Swing Desktop Authentication Window (Login & Registration) for SafePay.
 *
 * Implements the Unified SafePay Design Language:
 * - Canvas: Deep Obsidian (#080808) with ambient floating particles.
 * - Left Stage: BrandPane featuring Interactive 3D Titanium Banking Card with real-time
 *   mouse tilt physics, specular lighting reflection, and enterprise zero-fraud security guarantee.
 * - Right Stage: Obsidian Glassmorphic container (#161616, border #242424) with
 *   Electric Neon Lime (#D8FF00) primary action button, quick demo credential pills,
 *   dark textfields with focused lime glow, and eye password visibility toggles.
 * - Off-EDT background authentication via SwingWorker with instant visual feedback.
 */
public class AuthFrame extends BaseAppFrame {

    private static final String CARD_LOGIN = "CARD_LOGIN";
    private static final String CARD_REGISTER = "CARD_REGISTER";

    // Design Tokens (Matching Website)
    public static final Color COLOR_OBSIDIAN_BG = new Color(8, 8, 8);         // #080808
    public static final Color COLOR_SURFACE_DARK = new Color(17, 17, 17);      // #111111
    public static final Color COLOR_CARD_SURFACE = new Color(22, 22, 22);      // #161616
    public static final Color COLOR_CARD_BORDER = new Color(36, 36, 36);       // #242424
    public static final Color COLOR_LIME_PRIMARY = new Color(216, 255, 0);     // #D8FF00
    public static final Color COLOR_LIME_HOVER = new Color(194, 230, 0);       // #C2E600
    public static final Color COLOR_TEXT_WHITE = Color.WHITE;
    public static final Color COLOR_TEXT_MUTED = new Color(156, 163, 175);     // #9CA3AF

    // Backward compatibility aliases
    public static final Color COLOR_CANVAS_PINK = COLOR_OBSIDIAN_BG;
    public static final Color COLOR_CANVAS_PURPLE = COLOR_OBSIDIAN_BG;
    public static final Color COLOR_BLUE_PRIMARY = COLOR_LIME_PRIMARY;
    public static final Color COLOR_BLUE_HOVER = COLOR_LIME_HOVER;
    public static final Color COLOR_BG_PANEL = COLOR_CARD_SURFACE;
    public static final Color COLOR_TEXT_MAIN = COLOR_TEXT_WHITE;
    public static final Color COLOR_BORDER_INPUT = COLOR_CARD_BORDER;
    public static final Color COLOR_INPUT_BG = COLOR_SURFACE_DARK;

    public static final Color COLOR_ALERT_ERR_BG = new Color(40, 10, 15);
    public static final Color COLOR_ALERT_ERR_BORDER = new Color(239, 68, 68);
    public static final Color COLOR_ALERT_ERR_TEXT = new Color(254, 202, 202);
    public static final Color COLOR_ALERT_SUCC_BG = new Color(10, 35, 20);
    public static final Color COLOR_ALERT_SUCC_BORDER = new Color(16, 185, 129);
    public static final Color COLOR_ALERT_SUCC_TEXT = new Color(167, 243, 208);

    private final SafePayApiClient apiClient;
    private final Consumer<AuthResponse> onLoginSuccess;

    // Card Layout Container
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // 3D Card & Brand Pane
    private final BrandPane brandPane = new BrandPane();
    private final ParticleCanvas ambientCanvas = new ParticleCanvas();
    private Timer animationTimer;

    // Login Form Components
    private final JTextField txtLoginUsername = createStyledTextField("Username or Email");
    private final JPasswordField txtLoginPassword = createStyledPasswordField();
    private final JButton btnToggleLoginPass = createEyeToggleButton();
    private final JButton btnLoginSubmit = createPillActionButton("SIGN IN");
    private final JLabel lblLoginAlert = createAlertLabel();

    // Register Form Components
    private final JTextField txtRegUsername = createStyledTextField("Choose a username");
    private final JTextField txtRegEmail = createStyledTextField("your.email@example.com");
    private final JPasswordField txtRegPassword = createStyledPasswordField();
    private final JPasswordField txtRegConfirmPassword = createStyledPasswordField();
    private final JButton btnToggleRegPass = createEyeToggleButton();
    private final JButton btnToggleRegConfirmPass = createEyeToggleButton();
    private final JButton btnRegSubmit = createPillActionButton("CREATE ACCOUNT");
    private final JLabel lblRegAlert = createAlertLabel();

    public AuthFrame(SafePayApiClient apiClient, Consumer<AuthResponse> onLoginSuccess) {
        super("SafePay — AI-Powered Fraud Detection System");
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.onLoginSuccess = onLoginSuccess;

        setSize(1100, 720);
        setMinimumSize(new Dimension(980, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
    }

    public AuthFrame() {
        this(new SafePayApiClient(), null);
    }

    public BrandPane getBrandPane() {
        return brandPane;
    }

    public void stopAnimationTimer() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        ambientCanvas.stop();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        ambientCanvas.setLayout(new GridBagLayout());

        // Main Centered Split Frame Container (Corner Radius 24px)
        JPanel mainContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Drop shadow
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRoundRect(6, 6, w - 12, h - 12, 24, 24);

                // Container body
                g2.setColor(COLOR_CARD_SURFACE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);

                // Perimeter Border
                g2.setColor(COLOR_CARD_BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 24, 24);

                g2.dispose();
            }
        };
        mainContainer.setPreferredSize(new Dimension(980, 580));
        mainContainer.setOpaque(false);

        // --- LEFT PANE: BrandPane with 3D Card ---
        brandPane.setPreferredSize(new Dimension(480, 580));
        mainContainer.add(brandPane, BorderLayout.WEST);

        // Divider
        JPanel divider = new JPanel();
        divider.setPreferredSize(new Dimension(1, 580));
        divider.setBackground(COLOR_CARD_BORDER);
        mainContainer.add(divider, BorderLayout.CENTER);

        // --- RIGHT PANE: Form Switcher (Login / Register) ---
        cardsPanel.setOpaque(false);
        cardsPanel.add(buildLoginCard(), CARD_LOGIN);
        cardsPanel.add(buildRegisterCard(), CARD_REGISTER);

        mainContainer.add(cardsPanel, BorderLayout.EAST);

        ambientCanvas.add(mainContainer);
        add(ambientCanvas, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAnimationTimer();
            }
        });

        // 30 FPS animation timer for BrandPane ticks
        animationTimer = new Timer(33, e -> brandPane.tickAnimation());
        animationTimer.start();
    }

    private JPanel buildLoginCard() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(36, 40, 36, 40));
        form.setPreferredSize(new Dimension(480, 580));

        // Segmented Switcher (LOGIN | REGISTER)
        form.add(buildSegmentedSwitcher(true));
        form.add(Box.createVerticalStrut(18));

        // Quick Demo Account Testing Pills
        form.add(buildDemoPills());
        form.add(Box.createVerticalStrut(18));

        // Alert Banner
        form.add(lblLoginAlert);
        form.add(Box.createVerticalStrut(10));

        // Username Field
        JLabel lblUser = new JLabel("ACCOUNT IDENTIFIER");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblUser.setForeground(COLOR_TEXT_MUTED);
        form.add(lblUser);
        form.add(Box.createVerticalStrut(6));
        form.add(txtLoginUsername);
        form.add(Box.createVerticalStrut(14));

        // Password Field
        JLabel lblPass = new JLabel("PASSWORD");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblPass.setForeground(COLOR_TEXT_MUTED);
        form.add(lblPass);
        form.add(Box.createVerticalStrut(6));

        JPanel passRow = new JPanel(new BorderLayout(8, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(400, 42));
        passRow.add(txtLoginPassword, BorderLayout.CENTER);
        passRow.add(btnToggleLoginPass, BorderLayout.EAST);
        form.add(passRow);
        form.add(Box.createVerticalStrut(24));

        // Submit Action Button
        btnLoginSubmit.addActionListener(e -> executeLogin());
        txtLoginPassword.addActionListener(e -> executeLogin());
        form.add(btnLoginSubmit);

        form.add(Box.createVerticalStrut(16));

        // Switch to Register Link
        JLabel lblSwitch = new JLabel("Don't have an account? Open SafePay Account");
        lblSwitch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSwitch.setForeground(COLOR_LIME_PRIMARY);
        lblSwitch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblSwitch.setAlignmentX(0.0f);
        lblSwitch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showRegisterCard();
            }
        });
        form.add(lblSwitch);

        return form;
    }

    private JPanel buildRegisterCard() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));
        form.setPreferredSize(new Dimension(480, 580));

        // Segmented Switcher (LOGIN | REGISTER)
        form.add(buildSegmentedSwitcher(false));
        form.add(Box.createVerticalStrut(14));

        // Alert Banner
        form.add(lblRegAlert);
        form.add(Box.createVerticalStrut(8));

        // Username
        JLabel lblUser = new JLabel("USERNAME");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblUser.setForeground(COLOR_TEXT_MUTED);
        form.add(lblUser);
        form.add(Box.createVerticalStrut(4));
        form.add(txtRegUsername);
        form.add(Box.createVerticalStrut(10));

        // Email
        JLabel lblEmail = new JLabel("EMAIL ADDRESS");
        lblEmail.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblEmail.setForeground(COLOR_TEXT_MUTED);
        form.add(lblEmail);
        form.add(Box.createVerticalStrut(4));
        form.add(txtRegEmail);
        form.add(Box.createVerticalStrut(10));

        // Password
        JLabel lblPass = new JLabel("PASSWORD");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblPass.setForeground(COLOR_TEXT_MUTED);
        form.add(lblPass);
        form.add(Box.createVerticalStrut(4));

        JPanel passRow = new JPanel(new BorderLayout(8, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(400, 38));
        passRow.add(txtRegPassword, BorderLayout.CENTER);
        passRow.add(btnToggleRegPass, BorderLayout.EAST);
        form.add(passRow);
        form.add(Box.createVerticalStrut(10));

        // Confirm Password
        JLabel lblConfirm = new JLabel("CONFIRM PASSWORD");
        lblConfirm.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblConfirm.setForeground(COLOR_TEXT_MUTED);
        form.add(lblConfirm);
        form.add(Box.createVerticalStrut(4));

        JPanel confirmRow = new JPanel(new BorderLayout(8, 0));
        confirmRow.setOpaque(false);
        confirmRow.setMaximumSize(new Dimension(400, 38));
        confirmRow.add(txtRegConfirmPassword, BorderLayout.CENTER);
        confirmRow.add(btnToggleRegConfirmPass, BorderLayout.EAST);
        form.add(confirmRow);
        form.add(Box.createVerticalStrut(20));

        // Submit Action Button
        btnRegSubmit.addActionListener(e -> executeRegister());
        form.add(btnRegSubmit);
        form.add(Box.createVerticalStrut(12));

        // Back to Login Link
        JLabel lblBack = new JLabel("Already registered? Sign In to SafePay");
        lblBack.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBack.setForeground(COLOR_LIME_PRIMARY);
        lblBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblBack.setAlignmentX(0.0f);
        lblBack.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLoginCard();
            }
        });
        form.add(lblBack);

        return form;
    }

    private JPanel buildSegmentedSwitcher(boolean isLoginActive) {
        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        switcher.setOpaque(false);
        switcher.setAlignmentX(0.0f);

        JButton btnLoginTab = createTabButton("SIGN IN", isLoginActive);
        btnLoginTab.addActionListener(e -> showLoginCard());

        JButton btnRegTab = createTabButton("REGISTER", !isLoginActive);
        btnRegTab.addActionListener(e -> showRegisterCard());

        switcher.add(btnLoginTab);
        switcher.add(btnRegTab);

        return switcher;
    }

    private JPanel buildDemoPills() {
        JPanel pillBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pillBox.setOpaque(false);
        pillBox.setAlignmentX(0.0f);

        JLabel lblQuick = new JLabel("DEMO ACCESS:");
        lblQuick.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblQuick.setForeground(COLOR_TEXT_MUTED);
        pillBox.add(lblQuick);

        JButton btnUserPill = createPillSmall("Demo Customer");
        btnUserPill.addActionListener(e -> {
            txtLoginUsername.setText("user");
            txtLoginPassword.setText("User@123");
            txtLoginUsername.setForeground(Color.WHITE);
            txtLoginPassword.setForeground(Color.WHITE);
            lblLoginAlert.setVisible(false);
        });
        pillBox.add(btnUserPill);

        JButton btnAdminPill = createPillSmall("Administrator");
        btnAdminPill.addActionListener(e -> {
            txtLoginUsername.setText("admin");
            txtLoginPassword.setText("Admin@123");
            txtLoginUsername.setForeground(Color.WHITE);
            txtLoginPassword.setForeground(Color.WHITE);
            lblLoginAlert.setVisible(false);
        });
        pillBox.add(btnAdminPill);

        return pillBox;
    }

    public void showLoginCard() {
        cardLayout.show(cardsPanel, CARD_LOGIN);
        lblLoginAlert.setVisible(false);
        txtLoginUsername.requestFocusInWindow();
    }

    public void showRegisterCard() {
        cardLayout.show(cardsPanel, CARD_REGISTER);
        lblRegAlert.setVisible(false);
        txtRegUsername.requestFocusInWindow();
    }

    private void executeLogin() {
        String username = txtLoginUsername.getText().trim();
        String password = new String(txtLoginPassword.getPassword()).trim();

        if (username.isEmpty() || username.equals("Username or Email")) {
            showErrorBanner(lblLoginAlert, "Please enter your username or account email.");
            txtLoginUsername.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            showErrorBanner(lblLoginAlert, "Please enter your account password.");
            txtLoginPassword.requestFocusInWindow();
            return;
        }

        performLogin(username, password);
    }

    public void performLogin(String username, String password) {
        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("AUTHENTICATING...");
        lblLoginAlert.setVisible(false);

        SwingWorker<AuthResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthResponse doInBackground() throws Exception {
                return apiClient.login(username, password);
            }

            @Override
            protected void done() {
                btnLoginSubmit.setEnabled(true);
                btnLoginSubmit.setText("SIGN IN");
                try {
                    AuthResponse response = get();
                    if (response != null && response.success()) {
                        showSuccessBanner(lblLoginAlert, "Authentication successful. Entering portal...");
                        Timer t = new Timer(350, evt -> {
                            dispose();
                            if (onLoginSuccess != null) {
                                onLoginSuccess.accept(response);
                            }
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        String msg = (response != null && response.message() != null)
                                ? response.message()
                                : "Invalid credentials. Please verify your account details.";
                        showErrorBanner(lblLoginAlert, msg);
                    }
                } catch (Exception ex) {
                    String msg = ex.getCause() instanceof ApiClientException ace
                            ? ace.getMessage()
                            : "Connection failure: Unable to reach SafePay auth service.";
                    showErrorBanner(lblLoginAlert, msg);
                }
            }
        };
        worker.execute();
    }

    private void executeRegister() {
        String username = txtRegUsername.getText().trim();
        String email = txtRegEmail.getText().trim();
        String password = new String(txtRegPassword.getPassword()).trim();
        String confirm = new String(txtRegConfirmPassword.getPassword()).trim();

        if (username.isEmpty() || username.equals("Choose a username")) {
            showErrorBanner(lblRegAlert, "Please enter a valid username.");
            return;
        }
        if (email.isEmpty() || !email.contains("@")) {
            showErrorBanner(lblRegAlert, "Please enter a valid email address.");
            return;
        }
        if (password.length() < 6) {
            showErrorBanner(lblRegAlert, "Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showErrorBanner(lblRegAlert, "Passwords do not match.");
            return;
        }

        btnRegSubmit.setEnabled(false);
        btnRegSubmit.setText("CREATING ACCOUNT...");
        lblRegAlert.setVisible(false);

        SwingWorker<AuthResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected AuthResponse doInBackground() throws Exception {
                return apiClient.register(username, email, password);
            }

            @Override
            protected void done() {
                btnRegSubmit.setEnabled(true);
                btnRegSubmit.setText("CREATE ACCOUNT");
                try {
                    AuthResponse resp = get();
                    if (resp != null && resp.success()) {
                        showSuccessBanner(lblRegAlert, "Account created! Redirecting to login...");
                        Timer t = new Timer(600, evt -> {
                            txtLoginUsername.setText(username);
                            txtLoginPassword.setText(password);
                            showLoginCard();
                        });
                        t.setRepeats(false);
                        t.start();
                    } else {
                        String msg = resp != null ? resp.message() : "Registration failed.";
                        showErrorBanner(lblRegAlert, msg);
                    }
                } catch (Exception ex) {
                    String msg = ex.getCause() instanceof ApiClientException ace
                            ? ace.getMessage()
                            : "Registration failed: Could not reach backend server.";
                    showErrorBanner(lblRegAlert, msg);
                }
            }
        };
        worker.execute();
    }

    private void showErrorBanner(JLabel label, String message) {
        label.setText("⚠ " + message);
        label.setBackground(COLOR_ALERT_ERR_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ALERT_ERR_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        label.setForeground(COLOR_ALERT_ERR_TEXT);
        label.setVisible(true);
    }

    private void showSuccessBanner(JLabel label, String message) {
        label.setText("✔ " + message);
        label.setBackground(COLOR_ALERT_SUCC_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ALERT_SUCC_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        label.setForeground(COLOR_ALERT_SUCC_TEXT);
        label.setVisible(true);
    }

    // Component Factory Helpers
    private JTextField createStyledTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SURFACE_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(hasFocus() ? COLOR_LIME_PRIMARY : COLOR_CARD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setOpaque(false);
        tf.setForeground(COLOR_TEXT_MUTED);
        tf.setCaretColor(COLOR_LIME_PRIMARY);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        tf.setMaximumSize(new Dimension(400, 42));
        tf.setAlignmentX(0.0f);

        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (tf.getText().equals(placeholder)) {
                    tf.setText("");
                    tf.setForeground(COLOR_TEXT_WHITE);
                }
                tf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (tf.getText().isBlank()) {
                    tf.setText(placeholder);
                    tf.setForeground(COLOR_TEXT_MUTED);
                }
                tf.repaint();
            }
        });
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SURFACE_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(hasFocus() ? COLOR_LIME_PRIMARY : COLOR_CARD_BORDER);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pf.setOpaque(false);
        pf.setForeground(COLOR_TEXT_WHITE);
        pf.setCaretColor(COLOR_LIME_PRIMARY);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pf.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return pf;
    }

    private JButton createEyeToggleButton() {
        JButton btn = new JButton("👁") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_SURFACE_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(COLOR_CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setPreferredSize(new Dimension(44, 42));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(COLOR_TEXT_MUTED);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            char echoChar = txtLoginPassword.getEchoChar();
            if (echoChar == 0) {
                txtLoginPassword.setEchoChar('•');
                txtRegPassword.setEchoChar('•');
                txtRegConfirmPassword.setEchoChar('•');
                btn.setForeground(COLOR_TEXT_MUTED);
            } else {
                txtLoginPassword.setEchoChar((char) 0);
                txtRegPassword.setEchoChar((char) 0);
                txtRegConfirmPassword.setEchoChar((char) 0);
                btn.setForeground(COLOR_LIME_PRIMARY);
            }
        });
        return btn;
    }

    private JButton createPillActionButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                g2.setColor(getModel().isRollover() ? COLOR_LIME_HOVER : COLOR_LIME_PRIMARY);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);

                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.drawRoundRect(1, 1, w - 3, h - 3, h - 2, h - 2);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setMaximumSize(new Dimension(400, 44));
        btn.setPreferredSize(new Dimension(400, 44));
        btn.setAlignmentX(0.0f);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(Color.BLACK);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createPillSmall(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(36, 36, 36) : COLOR_SURFACE_DARK);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(getModel().isRollover() ? COLOR_LIME_PRIMARY : COLOR_CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(COLOR_TEXT_WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(active ? COLOR_LIME_PRIMARY : COLOR_TEXT_MUTED);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createAlertLabel() {
        JLabel lbl = new JLabel();
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setOpaque(true);
        lbl.setMaximumSize(new Dimension(400, 34));
        lbl.setAlignmentX(0.0f);
        lbl.setVisible(false);
        return lbl;
    }

    /**
     * BrandPane: Hosts the 3D Titanium Banking Card stage with real-time tilt and animations.
     */
    public static class BrandPane extends JPanel {
        private int animTick = 0;
        private final BankingCard3D card3D = new BankingCard3D(300, 180);

        public BrandPane() {
            setLayout(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

            // Top Brand Header
            JPanel topBox = new JPanel();
            topBox.setLayout(new BoxLayout(topBox, BoxLayout.Y_AXIS));
            topBox.setOpaque(false);

            JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            logoRow.setOpaque(false);

            JLabel lblBadge = new JLabel(" SAFEPAY ");
            lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblBadge.setOpaque(true);
            lblBadge.setBackground(COLOR_LIME_PRIMARY);
            lblBadge.setForeground(Color.BLACK);
            lblBadge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            logoRow.add(lblBadge);

            JLabel lblSub = new JLabel("DIGITAL BANKING");
            lblSub.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblSub.setForeground(COLOR_TEXT_MUTED);
            logoRow.add(lblSub);
            topBox.add(logoRow);

            topBox.add(Box.createVerticalStrut(14));
            JLabel lblHeading = new JLabel("Next-Gen Financial Defense");
            lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 22));
            lblHeading.setForeground(COLOR_TEXT_WHITE);
            topBox.add(lblHeading);

            JLabel lblDesc = new JLabel("Autonomous pre-authorization screening & zero-liability escrow holds.");
            lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblDesc.setForeground(COLOR_TEXT_MUTED);
            topBox.add(lblDesc);

            add(topBox, BorderLayout.NORTH);

            // Center 3D Interactive Card
            JPanel cardHolder = new JPanel(new GridBagLayout());
            cardHolder.setOpaque(false);
            cardHolder.add(card3D);
            add(cardHolder, BorderLayout.CENTER);

            // Bottom Trust Badges
            JPanel bottomBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
            bottomBox.setOpaque(false);

            JLabel lblShield = new JLabel("● 256-Bit TLS 1.3");
            lblShield.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblShield.setForeground(new Color(16, 185, 129));
            bottomBox.add(lblShield);

            JLabel lblMl = new JLabel("● Pre-Auth Interception");
            lblMl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblMl.setForeground(COLOR_LIME_PRIMARY);
            bottomBox.add(lblMl);

            JLabel lblPci = new JLabel("● Zero-Liability Guarantee");
            lblPci.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblPci.setForeground(COLOR_TEXT_MUTED);
            bottomBox.add(lblPci);

            add(bottomBox, BorderLayout.SOUTH);
        }

        public int getAnimTick() {
            return animTick;
        }

        public void tickAnimation() {
            animTick++;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Dark Obsidian gradient background
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(14, 14, 14),
                    w, h, new Color(8, 8, 8)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, 20, 20);

            // Ambient subtle radial lime glow behind 3D card
            g2.setColor(new Color(216, 255, 0, 12));
            g2.fillOval(w / 2 - 160, h / 2 - 100, 320, 200);

            g2.dispose();
        }
    }

    /**
     * Ambient particle canvas: Drifts subtle neon lime and white stars across dark obsidian space.
     */
    private static class ParticleCanvas extends JPanel {
        private final List<Star> stars = new ArrayList<>();
        private Timer timer;

        ParticleCanvas() {
            setOpaque(true);
            setBackground(COLOR_OBSIDIAN_BG);

            Random r = new Random();
            for (int i = 0; i < 45; i++) {
                stars.add(new Star(
                        r.nextDouble() * 1200,
                        r.nextDouble() * 800,
                        r.nextDouble() * 1.5 + 0.8,
                        r.nextDouble() * 0.4 + 0.1,
                        r.nextBoolean()
                ));
            }

            timer = new Timer(33, e -> {
                for (Star s : stars) {
                    s.y -= s.speed;
                    if (s.y < 0) {
                        s.y = getHeight() > 0 ? getHeight() : 800;
                        s.x = Math.random() * (getWidth() > 0 ? getWidth() : 1200);
                    }
                }
                repaint();
            });
            timer.start();
        }

        void stop() {
            if (timer != null && timer.isRunning()) {
                timer.stop();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(COLOR_OBSIDIAN_BG);
            g2.fillRect(0, 0, w, h);

            GradientPaint glow = new GradientPaint(
                    w / 2.0f, h / 2.0f, new Color(216, 255, 0, 15),
                    w / 2.0f, 0, new Color(8, 8, 8, 0)
            );
            g2.setPaint(glow);
            g2.fillRect(0, 0, w, h);

            for (Star s : stars) {
                g2.setColor(s.isLime ? new Color(216, 255, 0, 90) : new Color(255, 255, 255, 60));
                int sz = (int) s.size;
                g2.fillOval((int) s.x, (int) s.y, sz, sz);
            }

            g2.dispose();
        }

        private static class Star {
            double x, y, size, speed;
            boolean isLime;
            Star(double x, double y, double size, double speed, boolean isLime) {
                this.x = x;
                this.y = y;
                this.size = size;
                this.speed = speed;
                this.isLime = isLime;
            }
        }
    }
}
