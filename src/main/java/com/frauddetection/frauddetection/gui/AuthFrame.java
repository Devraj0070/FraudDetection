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
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
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
 * Premium Java Swing desktop authentication window (Login & Registration) for SafePay.
 *
 * Faithfully reproduces the visual specification of Reference B (auth-reference.png)
 * and incorporates decoupled vector animations:
 * - Ambient outer canvas: Soft coral-pink to vibrant purple gradient with botanical
 *   tropical leaves in the corners and soft drifting petals in the air.
 * - Centered floating white container (corner radius 26px) with multi-layer drop shadow.
 * - Left Illustration Panel (~48% width):
 *     - Top brand mark: solid dark circle + lowercase "safepay" typography.
 *     - Atmospheric landscape: pink textured sky with distant hills, midground
 *       city skyline with skyscrapers and classical spire tower, foreground dark
 *       indigo cliff with standing figure in blue dress, and lush bottom tropical foliage.
 *     - Floating payment-card illustration with embossed SafePay branding, contactless wave,
 *       and metallic gold EMV chip with circuitry grid.
 *     - Slowly floating 3D gold coins (₹ and S) with bevel rims and specular star glints.
 *     - Glowing emerald-and-blue security shield with crisp white checkmark and pulsing aura.
 *     - Animated atmospheric particles drifting smoothly, and breeze-animated foliage petals.
 * - Right Form Panel (~52% width):
 *     - Top navigation links: "Features", "Security", "Support", "Login" (active).
 *     - Bold "Login" / "Create Account" headline with signature thick royal blue underline pill.
 *     - Fully rounded pill input fields with soft border and eye password toggles.
 *     - Prominent royal blue pill action button with smooth hover and press feedback.
 *     - Clean links for password reset and account switching.
 * - Strictly native Java Swing and Java 2D vector graphics.
 * - Decoupled animation engine powered by javax.swing.Timer, stopping automatically on window disposal.
 */
public class AuthFrame extends BaseAppFrame {

    private static final String CARD_LOGIN = "CARD_LOGIN";
    private static final String CARD_REGISTER = "CARD_REGISTER";

    // Design Tokens matching Reference B
    public static final Color COLOR_CANVAS_PINK = new Color(253, 164, 175);     // Coral / Blush Pink
    public static final Color COLOR_CANVAS_PURPLE = new Color(124, 58, 237);    // Vibrant Purple
    public static final Color COLOR_BLUE_PRIMARY = new Color(29, 78, 216);      // #1D4ED8 (Royal Blue)
    public static final Color COLOR_BLUE_HOVER = new Color(30, 64, 175);        // #1E40AF
    public static final Color COLOR_PURPLE_PRIMARY = new Color(124, 58, 237);   // #7C3AED
    public static final Color COLOR_MAGENTA = new Color(192, 38, 211);          // #C026D3
    public static final Color COLOR_BG_PANEL = Color.WHITE;
    public static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);          // Deep Slate
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);      // Muted Slate
    public static final Color COLOR_BORDER_INPUT = new Color(226, 232, 240);    // Subtle Gray
    public static final Color COLOR_INPUT_BG = new Color(248, 250, 252);        // Soft Off-white
    public static final Color COLOR_LEAF_TEAL = new Color(13, 148, 136);        // #0D9488
    public static final Color COLOR_LEAF_TEAL_LIGHT = new Color(20, 184, 166);  // #14B8A6
    public static final Color COLOR_CLIFF_DARK = new Color(30, 27, 75);         // Deep Indigo

    public static final Color COLOR_ALERT_ERR_BG = new Color(254, 242, 242);
    public static final Color COLOR_ALERT_ERR_BORDER = new Color(252, 165, 165);
    public static final Color COLOR_ALERT_ERR_TEXT = new Color(153, 27, 27);
    public static final Color COLOR_ALERT_SUCC_BG = new Color(236, 253, 245);
    public static final Color COLOR_ALERT_SUCC_BORDER = new Color(110, 231, 183);
    public static final Color COLOR_ALERT_SUCC_TEXT = new Color(6, 95, 70);

    private final SafePayApiClient apiClient;
    private final Consumer<AuthResponse> onLoginSuccess;

    // Card Layout Container
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    // Left Animated Brand Illustration Pane
    private final BrandPane brandPane = new BrandPane();
    private Timer animationTimer;

    // Login Form Components
    private final JTextField txtLoginUsername = createStyledTextField("Username or Email");
    private final JPasswordField txtLoginPassword = createStyledPasswordField();
    private final JButton btnToggleLoginPass = createEyeToggleButton();
    private final JButton btnLoginSubmit = createPillActionButton("Login");
    private final JLabel lblLoginAlert = createAlertLabel();

    // Register Form Components
    private final JTextField txtRegUsername = createStyledTextField("Choose a username");
    private final JTextField txtRegEmail = createStyledTextField("your.email@example.com");
    private final JPasswordField txtRegPassword = createStyledPasswordField();
    private final JPasswordField txtRegConfirmPassword = createStyledPasswordField();
    private final JButton btnToggleRegPass = createEyeToggleButton();
    private final JButton btnToggleRegConfirmPass = createEyeToggleButton();
    private final JButton btnRegSubmit = createPillActionButton("Create Account");
    private final JLabel lblRegAlert = createAlertLabel();

    public AuthFrame(SafePayApiClient apiClient, Consumer<AuthResponse> onLoginSuccess) {
        super("SafePay — AI-Powered Fraud Detection System");
        this.apiClient = apiClient != null ? apiClient : new SafePayApiClient();
        this.onLoginSuccess = onLoginSuccess;

        setSize(1080, 720);
        setMinimumSize(new Dimension(980, 660));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
    }

    public AuthFrame() {
        this(new SafePayApiClient(), null);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Ambient outer canvas with gradient and botanical leaves (Reference B)
        JPanel outerCanvas = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 1. Diagonal Ambient Gradient (Peach/Blush Pink to Vibrant Purple)
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(254, 205, 211),
                        w, h, new Color(124, 58, 237)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                // 2. Botanical Foliage around Canvas Edges (Reference B)
                paintCanvasBotanicalLeaves(g2, w, h);

                // 3. Floating Soft Petals drifting in the air
                paintCanvasFloatingPetals(g2, w, h);

                g2.dispose();
            }
        };
        outerCanvas.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));

        // Floating Card Container (Reference B)
        JPanel floatingCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Multi-layer soft drop shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(0, 8, w, h - 8, 26, 26);
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fillRoundRect(2, 4, w - 4, h - 6, 26, 26);

                // White Card Body
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 26, 26);
                g2.dispose();
            }
        };
        floatingCard.setOpaque(false);

        // Left Branding Panel (~48% width)
        floatingCard.add(brandPane, BorderLayout.WEST);

        // Right Interactive Form Cards (~52% width)
        cardsPanel.setBackground(COLOR_BG_PANEL);
        cardsPanel.setOpaque(false);
        cardsPanel.add(createLoginCard(), CARD_LOGIN);
        cardsPanel.add(createRegisterCard(), CARD_REGISTER);
        floatingCard.add(cardsPanel, BorderLayout.CENTER);

        outerCanvas.add(floatingCard, BorderLayout.CENTER);
        add(outerCanvas, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        setStatusMessage("SafePay Security Client Ready • Connected to " + apiClient.getBaseUrl());
        setDatabaseStatus(true, "HTTP Session Active");

        // Window lifecycle management: stop animation timer when window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopAnimationTimer();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopAnimationTimer();
            }
        });

        // Start smooth 35 FPS vector animation timer if not in a headless test environment
        if (!GraphicsEnvironment.isHeadless()) {
            animationTimer = new Timer(28, e -> {
                if (brandPane != null) {
                    brandPane.tickAnimation();
                }
            });
            animationTimer.start();
        }
    }

    /**
     * Stops the Java 2D vector animation timer immediately to prevent resource leaks.
     */
    public void stopAnimationTimer() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    @Override
    public void dispose() {
        stopAnimationTimer();
        super.dispose();
    }

    public BrandPane getBrandPane() {
        return brandPane;
    }

    public void showRegisterCard() {
        cardLayout.show(cardsPanel, CARD_REGISTER);
    }

    public void showLoginCard() {
        cardLayout.show(cardsPanel, CARD_LOGIN);
    }

    /**
     * Paints the botanical leaves in the corners of the outer canvas matching Reference B.
     */
    private static void paintCanvasBotanicalLeaves(Graphics2D g2, int w, int h) {
        // Bottom-Left Corner: Large teal tropical leaves extending into the canvas
        paintBotanicalLeaf(g2, -20, h - 10, 80, h - 140, 48, COLOR_LEAF_TEAL, COLOR_LEAF_TEAL_LIGHT);
        paintBotanicalLeaf(g2, 10, h + 10, 140, h - 90, 52, new Color(15, 118, 110), COLOR_LEAF_TEAL);
        paintBotanicalLeaf(g2, -40, h - 90, 60, h - 220, 44, COLOR_LEAF_TEAL, new Color(45, 212, 191));
        paintBotanicalLeaf(g2, 50, h + 20, 180, h - 30, 40, new Color(13, 148, 136), COLOR_LEAF_TEAL_LIGHT);

        // Bottom-Right Corner: Stylized lavender and pink textured leaves
        paintBotanicalLeaf(g2, w + 30, h - 20, w - 80, h - 130, 46, new Color(192, 132, 252), new Color(244, 114, 182));
        paintBotanicalLeaf(g2, w - 10, h + 10, w - 130, h - 80, 50, new Color(168, 85, 247), new Color(236, 72, 153));
        paintBotanicalLeaf(g2, w + 40, h - 100, w - 50, h - 200, 42, new Color(216, 180, 254), new Color(249, 168, 212));
    }

    /**
     * Paints a single botanical leaf with stem and white herringbone veins.
     */
    private static void paintBotanicalLeaf(Graphics2D g2, int x0, int y0, int tipX, int tipY, int width, Color startCol, Color endCol) {
        int dx = tipX - x0;
        int dy = tipY - y0;
        double len = Math.hypot(dx, dy);
        if (len < 5) return;

        double nx = -dy / len;
        double ny = dx / len;

        int midX = (x0 + tipX) / 2;
        int midY = (y0 + tipY) / 2;

        int leftX = (int) (midX + nx * width);
        int leftY = (int) (midY + ny * width);
        int rightX = (int) (midX - nx * width);
        int rightY = (int) (midY - ny * width);

        // Leaf outline
        GeneralPath leaf = new GeneralPath();
        leaf.moveTo(x0, y0);
        leaf.quadTo(leftX, leftY, tipX, tipY);
        leaf.quadTo(rightX, rightY, x0, y0);
        leaf.closePath();

        GradientPaint lp = new GradientPaint(x0, y0, startCol, tipX, tipY, endCol);
        g2.setPaint(lp);
        g2.fill(leaf);

        // Central vein
        g2.setColor(new Color(255, 255, 255, 210));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x0, y0, tipX, tipY);

        // Side veins (herringbone pattern)
        g2.setStroke(new BasicStroke(1.0f));
        int numVeins = 6;
        for (int i = 1; i <= numVeins; i++) {
            double t = (double) i / (numVeins + 1);
            int vx = (int) (x0 + dx * t);
            int vy = (int) (y0 + dy * t);

            double span = (1.0 - Math.abs(t - 0.5) * 1.5) * width * 0.8;
            int vLeftX = (int) (vx + nx * span + dx * 0.15);
            int vLeftY = (int) (vy + ny * span + dy * 0.15);
            int vRightX = (int) (vx - nx * span + dx * 0.15);
            int vRightY = (int) (vy - ny * span + dy * 0.15);

            g2.drawLine(vx, vy, vLeftX, vLeftY);
            g2.drawLine(vx, vy, vRightX, vRightY);
        }
    }

    /**
     * Paints floating soft petals and specks drifting in the canvas air.
     */
    private static void paintCanvasFloatingPetals(Graphics2D g2, int w, int h) {
        int[][] petals = {
                {w / 6, 80, 16, 9, 35, 160},
                {w / 4, 140, 22, 11, -25, 200},
                {w / 3 + 40, 70, 14, 8, 45, 140},
                {w * 3 / 4, 90, 20, 10, -35, 180},
                {w * 5 / 6, 160, 24, 12, 50, 190},
                {w - 70, 240, 16, 8, -15, 150},
                {w - 110, 380, 18, 9, 30, 140},
                {w / 8, h - 260, 22, 10, -40, 180},
                {w / 5, h - 180, 15, 7, 20, 160},
                {w * 2 / 3, h - 140, 19, 9, -20, 150}
        };

        for (int[] p : petals) {
            int px = p[0];
            int py = p[1];
            int pw = p[2];
            int ph = p[3];
            int angle = p[4];
            int alpha = p[5];

            Graphics2D gPetal = (Graphics2D) g2.create();
            gPetal.translate(px, py);
            gPetal.rotate(Math.toRadians(angle));

            Color col = (px > w / 2) ? new Color(13, 148, 136, alpha) : new Color(20, 184, 166, alpha);
            gPetal.setColor(col);

            GeneralPath petalPath = new GeneralPath();
            petalPath.moveTo(-pw / 2, 0);
            petalPath.quadTo(0, -ph, pw / 2, 0);
            petalPath.quadTo(0, ph, -pw / 2, 0);
            petalPath.closePath();
            gPetal.fill(petalPath);

            // Leaf vein
            gPetal.setColor(new Color(255, 255, 255, alpha / 2));
            gPetal.setStroke(new BasicStroke(0.8f));
            gPetal.drawLine(-pw / 2, 0, pw / 2, 0);

            gPetal.dispose();
        }
    }

    /**
     * Left Branding Panel (~48% width):
     * - Clipped to the rounded corners of the floating card
     * - Top-left: Solid dark circle mark + "safepay" lowercase bold text
     * - Atmospheric landscape:
     *     - Pink textured sky with distant hills
     *     - Midground skyline with skyscrapers and classical spire tower
     *     - Foreground dark indigo cliff with standing figure in blue
     *     - Bottom lush tropical foliage and floating petals
     * - Rich fintech illustrations & animations:
     *     - Floating SafePay payment card with metallic EMV chip & contactless icon
     *     - Slowly floating gold coins (₹ and S) with specular star glints
     *     - Glowing emerald/blue security shield with white checkmark and pulsing aura
     *     - Atmospheric drifting particles and gently swaying foliage
     */
    public static class BrandPane extends JPanel {

        private int animTick = 0;

        // Particle model: [baseX, baseY, radius, speedY, phase, colorType]
        // colorType: 0 = gold, 1 = cyan, 2 = lavender, 3 = white
        private final double[][] particles = {
                {70, 270, 2.8, 0.45, 0.0, 1},
                {135, 175, 3.2, 0.35, 1.2, 0},
                {215, 235, 2.2, 0.50, 2.5, 3},
                {275, 115, 3.5, 0.40, 3.8, 1},
                {335, 195, 2.5, 0.30, 0.8, 2},
                {385, 145, 3.0, 0.42, 4.2, 0},
                {105, 95, 2.0, 0.38, 2.1, 3},
                {175, 305, 3.4, 0.48, 5.0, 2},
                {245, 65, 2.6, 0.32, 1.7, 0},
                {315, 265, 3.0, 0.44, 3.1, 1},
                {365, 75, 2.2, 0.36, 4.9, 3},
                {155, 215, 2.8, 0.40, 0.5, 0}
        };

        public BrandPane() {
            setPreferredSize(new Dimension(470, 0));
            setOpaque(false);
            setDoubleBuffered(true);
        }

        public void tickAnimation() {
            animTick++;
            repaint();
        }

        public int getAnimTick() {
            return animTick;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();

            // Clip to left rounded corners of the card
            g2.clip(new RoundRectangle2D.Double(0, 0, w + 26, h, 26, 26));

            // 1. Sky: Soft pastel pink textured gradient
            GradientPaint skyGrad = new GradientPaint(
                    0, 0, new Color(253, 242, 248),
                    w, h, new Color(244, 114, 182)
            );
            g2.setPaint(skyGrad);
            g2.fillRect(0, 0, w, h);

            // Subtle textured sky clouds / waves with gentle wave offset
            paintSkyWaves(g2, w, h);

            // 2. Distant Soft Mountains / Hills
            paintDistantHills(g2, w, h);

            // 3. Midground City Skyline in Lavender / Periwinkle-slate
            paintCitySkyline(g2, w, h);

            // 4. Foreground Dark Indigo Cliff on Left with Silhouette Figure
            paintCliffAndFigure(g2, w, h);

            // 5. Atmospheric Floating Particles
            paintAtmosphericParticles(g2, w, h);

            // 6. Glowing Security Shield & Checkmark Illustration
            paintGlowingSecurityShield(g2, w, h);

            // 7. Premium Floating SafePay Payment Card with Metallic EMV Chip
            paintFloatingPaymentCard(g2, w, h);

            // 8. Slowly Floating Gold Coins & Money-Themed Elements
            paintFloatingCoinsAndMoneyAccents(g2, w, h);

            // 9. Foreground Lush Foliage at the bottom
            paintForegroundFoliage(g2, w, h);

            // 10. Floating Leaves drifting across the scene with gentle flutter
            paintSceneFloatingLeaves(g2, w, h);

            // 11. Top-Left Brand Header (solid dark circle + "safepay" lowercase bold text)
            g2.setColor(new Color(24, 24, 27));
            g2.fillOval(36, 32, 16, 16);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            g2.setColor(new Color(24, 24, 27));
            g2.drawString("safepay", 58, 45);

            g2.dispose();
        }

        private void paintSkyWaves(Graphics2D g2, int w, int h) {
            double waveOffset = Math.sin(animTick * 0.025) * 4.0;
            g2.setColor(new Color(251, 207, 232, 140));
            GeneralPath cloud1 = new GeneralPath();
            cloud1.moveTo(0, h * 0.38 + waveOffset);
            cloud1.quadTo(w * 0.4, h * 0.32 - waveOffset, w, h * 0.42 + waveOffset);
            cloud1.lineTo(w, h * 0.52);
            cloud1.quadTo(w * 0.6, h * 0.45, 0, h * 0.48);
            cloud1.closePath();
            g2.fill(cloud1);
        }

        private void paintDistantHills(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(244, 114, 182, 160));
            GeneralPath hill1 = new GeneralPath();
            hill1.moveTo(0, h * 0.54);
            hill1.quadTo(w * 0.3, h * 0.48, w * 0.65, h * 0.56);
            hill1.quadTo(w * 0.85, h * 0.60, w, h * 0.52);
            hill1.lineTo(w, h);
            hill1.lineTo(0, h);
            hill1.closePath();
            g2.fill(hill1);
        }

        private void paintGlowingSecurityShield(Graphics2D g2, int w, int h) {
            int cx = (int) (w * 0.38);
            int cy = (int) (h * 0.16);
            double bob = Math.sin(animTick * 0.045 + 1.2) * 4.5;
            double pulse = 0.5 + 0.5 * Math.sin(animTick * 0.05);

            Graphics2D gs = (Graphics2D) g2.create();
            gs.translate(cx, cy + bob);

            int sw = 34;
            int sh = 40;

            // Multi-Layer Pulsing Glowing Aura
            int glowAlpha1 = (int) (38 + 25 * pulse);
            gs.setColor(new Color(16, 185, 129, glowAlpha1));
            gs.fillOval(-sw / 2 - 10, -sh / 2 - 10, sw + 20, sh + 20);

            int glowAlpha2 = (int) (22 + 18 * pulse);
            gs.setColor(new Color(59, 130, 246, glowAlpha2));
            gs.fillOval(-sw / 2 - 16, -sh / 2 - 16, sw + 32, sh + 32);

            // Shield Outline Path
            GeneralPath shield = new GeneralPath();
            shield.moveTo(0, -sh / 2);
            shield.quadTo(sw / 2, -sh / 2 - 2, sw / 2, -sh / 4);
            shield.quadTo(sw / 2, sh / 4, 0, sh / 2);
            shield.quadTo(-sw / 2, sh / 4, -sw / 2, -sh / 4);
            shield.quadTo(-sw / 2, -sh / 2 - 2, 0, -sh / 2);
            shield.closePath();

            // Shield Gradient Fill (Emerald Green to Royal Blue)
            GradientPaint sp = new GradientPaint(
                    -sw / 2, -sh / 2, new Color(16, 185, 129),
                    sw / 2, sh / 2, new Color(29, 78, 216)
            );
            gs.setPaint(sp);
            gs.fill(shield);

            // Inner Shield Bevel Stroke
            gs.setColor(new Color(255, 255, 255, 210));
            gs.setStroke(new BasicStroke(1.4f));
            gs.draw(shield);

            // Crisp White Checkmark
            GeneralPath check = new GeneralPath();
            check.moveTo(-sw * 0.22, -sh * 0.02);
            check.lineTo(-sw * 0.05, sh * 0.16);
            check.lineTo(sw * 0.24, -sh * 0.16);
            gs.setColor(Color.WHITE);
            gs.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gs.draw(check);

            gs.dispose();
        }

        private void paintFloatingPaymentCard(Graphics2D g2, int w, int h) {
            int cx = (int) (w * 0.65);
            int cy = (int) (h * 0.25);
            int cardW = 196;
            int cardH = 120;
            int arc = 14;

            double bob = Math.sin(animTick * 0.04) * 6.0;
            double tiltDeg = -6.5 + Math.sin(animTick * 0.03) * 1.5;

            Graphics2D gc = (Graphics2D) g2.create();
            gc.translate(cx, cy + bob);
            gc.rotate(Math.toRadians(tiltDeg));

            // 1. Multi-Layer Soft Drop Shadows
            gc.setColor(new Color(30, 27, 75, 38));
            gc.fillRoundRect(-cardW / 2 + 6, -cardH / 2 + 14, cardW - 4, cardH, arc, arc);
            gc.setColor(new Color(30, 27, 75, 24));
            gc.fillRoundRect(-cardW / 2 + 10, -cardH / 2 + 20, cardW - 8, cardH, arc, arc);

            // 2. Card Gradient Body (Royal Blue -> Vibrant Purple -> Deep Indigo)
            GradientPaint cardGrad = new GradientPaint(
                    -cardW / 2, -cardH / 2, new Color(29, 78, 216),
                    cardW / 2, cardH / 2, new Color(109, 40, 217)
            );
            gc.setPaint(cardGrad);
            gc.fillRoundRect(-cardW / 2, -cardH / 2, cardW, cardH, arc, arc);

            // 3. Subtle Gloss Diagonal Reflection
            GeneralPath gloss = new GeneralPath();
            gloss.moveTo(-cardW / 2, -cardH / 2);
            gloss.lineTo(-cardW / 2 + cardW * 0.65, -cardH / 2);
            gloss.lineTo(-cardW / 2 + cardW * 0.25, cardH / 2);
            gloss.lineTo(-cardW / 2, cardH / 2);
            gloss.closePath();
            gc.setPaint(new GradientPaint(
                    -cardW / 2, -cardH / 2, new Color(255, 255, 255, 55),
                    0, cardH / 2, new Color(255, 255, 255, 0)
            ));
            gc.fill(gloss);

            // 4. Glowing Card Border
            gc.setColor(new Color(255, 255, 255, 120));
            gc.setStroke(new BasicStroke(1.2f));
            gc.drawRoundRect(-cardW / 2, -cardH / 2, cardW, cardH, arc, arc);

            // 5. Embossed SafePay Brand Mark & Typography
            gc.setColor(Color.WHITE);
            gc.fillOval(-cardW / 2 + 14, -cardH / 2 + 15, 8, 8);
            gc.setFont(new Font("Segoe UI", Font.BOLD, 12));
            gc.drawString("SafePay", -cardW / 2 + 26, -cardH / 2 + 23);

            // Contactless Wave Symbol
            paintContactlessWaves(gc, cardW / 2 - 22, -cardH / 2 + 20);

            // 6. Metallic EMV Gold Chip
            paintEmvChip(gc, -cardW / 2 + 14, -cardH / 2 + 36, 26, 20);

            // 7. Embossed Masked Card Number
            gc.setFont(new Font("Consolas", Font.BOLD, 11));
            gc.setColor(new Color(241, 245, 249));
            gc.drawString("••••  ••••  ••••  8829", -cardW / 2 + 14, cardH / 2 - 25);

            // 8. Cardholder & Security labels
            gc.setFont(new Font("Segoe UI", Font.BOLD, 8));
            gc.setColor(new Color(203, 213, 225, 230));
            gc.drawString("SECURE SHIELD", -cardW / 2 + 14, cardH / 2 - 12);
            gc.drawString("VALID 10/29", cardW / 2 - 54, cardH / 2 - 12);

            gc.dispose();
        }

        private static void paintEmvChip(Graphics2D gc, int x, int y, int w, int h) {
            // Metallic Gold Base
            gc.setPaint(new GradientPaint(
                    x, y, new Color(254, 240, 138),
                    x + w, y + h, new Color(217, 119, 6)
            ));
            gc.fillRoundRect(x, y, w, h, 4, 4);

            // Outer gold border
            gc.setColor(new Color(180, 83, 9));
            gc.setStroke(new BasicStroke(0.8f));
            gc.drawRoundRect(x, y, w, h, 4, 4);

            // Microcircuit Contact Pad Etchings
            gc.setColor(new Color(146, 64, 14, 190));
            gc.drawLine(x + w / 3, y + 2, x + w / 3, y + h - 2);
            gc.drawLine(x + 2 * w / 3, y + 2, x + 2 * w / 3, y + h - 2);
            gc.drawLine(x + 2, y + h / 2, x + w - 2, y + h / 2);
            gc.drawRoundRect(x + w / 4, y + h / 4, w / 2, h / 2, 2, 2);
        }

        private static void paintContactlessWaves(Graphics2D gc, int cx, int cy) {
            gc.setColor(new Color(255, 255, 255, 200));
            gc.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int r = 4; r <= 12; r += 4) {
                gc.drawArc(cx - r, cy - r, r * 2, r * 2, -45, 90);
            }
        }

        private void paintFloatingCoinsAndMoneyAccents(Graphics2D g2, int w, int h) {
            // Coin 1: Top-Right of Card (₹ Gold Coin)
            int c1x = (int) (w * 0.84);
            int c1y = (int) (h * 0.17);
            double b1 = Math.sin(animTick * 0.048 + 2.0) * 5.5;
            paintGoldCoin(g2, c1x, c1y, 30, b1, "₹");
            double starPulse1 = 0.5 + 0.5 * Math.sin(animTick * 0.08);
            paintStarGlint(g2, c1x + 10, (int) (c1y + b1 - 10), starPulse1);

            // Coin 2: Lower-Left of Card (₹ Gold Coin)
            int c2x = (int) (w * 0.35);
            int c2y = (int) (h * 0.35);
            double b2 = Math.sin(animTick * 0.042 + 3.5) * 4.5;
            paintGoldCoin(g2, c2x, c2y, 24, b2, "₹");
            double starPulse2 = 0.5 + 0.5 * Math.sin(animTick * 0.07 + 1.5);
            paintStarGlint(g2, c2x + 8, (int) (c2y + b2 - 8), starPulse2);

            // Coin 3: Mid-Right of Card (S Gold Coin)
            int c3x = (int) (w * 0.88);
            int c3y = (int) (h * 0.33);
            double b3 = Math.sin(animTick * 0.052 + 0.8) * 3.5;
            paintGoldCoin(g2, c3x, c3y, 18, b3, "S");

            // Floating Geometric Sparkles / Currency Diamonds
            paintMoneySparkleDiamond(g2, (int) (w * 0.50), (int) (h * 0.12), animTick * 0.06);
            paintMoneySparkleDiamond(g2, (int) (w * 0.80), (int) (h * 0.28), animTick * 0.05 + 2.0);
        }

        private static void paintGoldCoin(Graphics2D g2, int cx, int cy, int size, double bob, String symbol) {
            Graphics2D gc = (Graphics2D) g2.create();
            gc.translate(cx, cy + bob);

            int r = size / 2;
            // Soft Drop Shadow
            gc.setColor(new Color(30, 27, 75, 45));
            gc.fillOval(-r + 2, -r + 6, size, size);

            // Outer Gold Rim
            gc.setPaint(new GradientPaint(-r, -r, new Color(251, 191, 36), r, r, new Color(180, 83, 9)));
            gc.fillOval(-r, -r, size, size);

            // Inner Coin Face
            int innerR = r - 2;
            gc.setPaint(new GradientPaint(-innerR, -innerR, new Color(254, 240, 138), innerR, innerR, new Color(245, 158, 11)));
            gc.fillOval(-innerR, -innerR, innerR * 2, innerR * 2);

            // Bevel Ring
            gc.setColor(new Color(217, 119, 6));
            gc.setStroke(new BasicStroke(0.8f));
            gc.drawOval(-innerR + 1, -innerR + 1, (innerR - 1) * 2, (innerR - 1) * 2);

            // Currency Symbol
            gc.setColor(new Color(146, 64, 14));
            gc.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.54)));
            int sw = gc.getFontMetrics().stringWidth(symbol);
            int sh = gc.getFontMetrics().getAscent();
            gc.drawString(symbol, -sw / 2, sh / 2 - 1);

            gc.dispose();
        }

        private static void paintStarGlint(Graphics2D g2, int x, int y, double pulse) {
            if (pulse <= 0.15) return;
            Graphics2D gs = (Graphics2D) g2.create();
            gs.translate(x, y);
            int arm = (int) (6 * pulse);
            int alpha = Math.min(255, (int) (220 * pulse));
            gs.setColor(new Color(255, 255, 255, alpha));
            Polygon star = new Polygon();
            star.addPoint(0, -arm);
            star.addPoint(1, -1);
            star.addPoint(arm, 0);
            star.addPoint(1, 1);
            star.addPoint(0, arm);
            star.addPoint(-1, 1);
            star.addPoint(-arm, 0);
            star.addPoint(-1, -1);
            gs.fill(star);
            gs.dispose();
        }

        private static void paintMoneySparkleDiamond(Graphics2D g2, int x, int y, double phase) {
            double scale = 0.6 + 0.4 * Math.sin(phase);
            int d = (int) (7 * scale);
            if (d < 2) return;
            int alpha = (int) (90 + 90 * scale);
            Graphics2D gd = (Graphics2D) g2.create();
            gd.translate(x, y);
            gd.setColor(new Color(254, 240, 138, alpha));
            Polygon poly = new Polygon();
            poly.addPoint(0, -d);
            poly.addPoint(d, 0);
            poly.addPoint(0, d);
            poly.addPoint(-d, 0);
            gd.fill(poly);
            gd.dispose();
        }

        private void paintAtmosphericParticles(Graphics2D g2, int w, int h) {
            for (double[] p : particles) {
                double bx = p[0];
                double by = p[1];
                double radius = p[2];
                double speedY = p[3];
                double phase = p[4];
                int colorType = (int) p[5];

                // Oscillate x and drift y smoothly
                double px = bx + Math.sin(animTick * 0.03 + phase) * 8.0;
                double currentY = (by - animTick * speedY) % (h * 0.75);
                if (currentY < 40) currentY += (h * 0.65);

                int alpha = (int) (60 + 50 * Math.sin(animTick * 0.04 + phase));
                Color col;
                switch (colorType) {
                    case 0: col = new Color(251, 191, 36, alpha); break;  // Gold
                    case 1: col = new Color(45, 212, 191, alpha); break;  // Cyan
                    case 2: col = new Color(192, 132, 252, alpha); break; // Lavender
                    default: col = new Color(255, 255, 255, alpha); break; // White
                }

                // Soft glowing particle halo
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), alpha / 3));
                int r2 = (int) (radius * 2.2);
                g2.fillOval((int) (px - r2), (int) (currentY - r2), r2 * 2, r2 * 2);

                g2.setColor(col);
                int r1 = (int) radius;
                g2.fillOval((int) (px - r1), (int) (currentY - r1), r1 * 2, r1 * 2);
            }
        }

        private void paintCitySkyline(Graphics2D g2, int w, int h) {
            int baseY = (int) (h * 0.88);

            // Distant building silhouettes (Layer 1)
            g2.setColor(new Color(199, 210, 254, 180));
            int[][] layer1Bldgs = {
                    {w - 240, baseY - 160, 42, 160},
                    {w - 190, baseY - 200, 36, 200},
                    {w - 150, baseY - 240, 50, 240},
                    {w - 95, baseY - 180, 48, 180},
                    {w - 42, baseY - 220, 44, 220}
            };
            for (int[] b : layer1Bldgs) {
                g2.fillRect(b[0], b[1], b[2], b[3]);
            }

            // Closer building silhouettes (Layer 2)
            g2.setColor(new Color(165, 180, 252));
            int[][] layer2Bldgs = {
                    {w - 270, baseY - 140, 38, 140},
                    {w - 226, baseY - 175, 44, 175},
                    {w - 178, baseY - 210, 52, 210},
                    {w - 120, baseY - 250, 56, 250},
                    {w - 60, baseY - 190, 50, 190}
            };
            for (int[] b : layer2Bldgs) {
                g2.fillRect(b[0], b[1], b[2], b[3]);
            }

            // Classical Tower with Spire (prominent landmark in Reference B)
            int towerX = w - 215;
            int towerW = 34;
            int towerTopY = baseY - 260;

            // Tower Base
            g2.setColor(new Color(147, 157, 235));
            g2.fillRect(towerX, towerTopY + 50, towerW, 210);

            // Neoclassical Tier
            g2.fillRect(towerX + 4, towerTopY + 28, towerW - 8, 24);

            // Dome cap
            g2.fillArc(towerX + 6, towerTopY + 14, towerW - 12, 18, 0, 180);

            // Slender Spire needle
            Polygon spire = new Polygon();
            spire.addPoint(towerX + towerW / 2, towerTopY - 30);
            spire.addPoint(towerX + towerW / 2 - 3, towerTopY + 14);
            spire.addPoint(towerX + towerW / 2 + 3, towerTopY + 14);
            g2.fill(spire);

            // Spire finial tip
            g2.fillOval(towerX + towerW / 2 - 2, towerTopY - 34, 4, 4);

            // Forefront skyscrapers (Layer 3 - darker slate blue)
            g2.setColor(new Color(129, 140, 248));
            int[][] layer3Bldgs = {
                    {w - 290, baseY - 110, 45, 110},
                    {w - 240, baseY - 150, 50, 150},
                    {w - 185, baseY - 190, 58, 190},
                    {w - 122, baseY - 225, 62, 225},
                    {w - 55, baseY - 165, 58, 165}
            };
            for (int[] b : layer3Bldgs) {
                g2.fillRect(b[0], b[1], b[2], b[3]);

                // Subtle illuminated window slits
                g2.setColor(new Color(238, 242, 255, 90));
                for (int wy = b[1] + 12; wy < baseY - 10; wy += 14) {
                    for (int wx = b[0] + 6; wx < b[0] + b[2] - 8; wx += 10) {
                        g2.fillRect(wx, wy, 4, 6);
                    }
                }
                g2.setColor(new Color(129, 140, 248));
            }
        }

        private void paintCliffAndFigure(Graphics2D g2, int w, int h) {
            // Dark Indigo Cliff Path
            GeneralPath cliff = new GeneralPath();
            cliff.moveTo(0, h * 0.60);
            cliff.lineTo(w * 0.24, h * 0.66);
            cliff.lineTo(w * 0.28, h * 0.70);
            cliff.lineTo(w * 0.32, h * 0.73);
            cliff.lineTo(w * 0.27, h * 0.76);
            cliff.lineTo(w * 0.34, h * 0.82);
            cliff.lineTo(w * 0.22, h * 0.86);
            cliff.lineTo(w * 0.30, h);
            cliff.lineTo(0, h);
            cliff.closePath();

            GradientPaint cliffGrad = new GradientPaint(
                    0, (float) (h * 0.60), COLOR_CLIFF_DARK,
                    w * 0.3f, h, new Color(49, 46, 129)
            );
            g2.setPaint(cliffGrad);
            g2.fill(cliff);

            // Shading & texture on cliff crags
            g2.setColor(new Color(67, 56, 202));
            Polygon crag = new Polygon();
            crag.addPoint((int) (w * 0.16), (int) (h * 0.74));
            crag.addPoint((int) (w * 0.28), (int) (h * 0.70));
            crag.addPoint((int) (w * 0.22), (int) (h * 0.86));
            g2.fill(crag);

            // Stylized Figure standing on cliff edge (Reference B)
            int figX = (int) (w * 0.15);
            int figY = (int) (h * 0.63);

            // Head & hair
            g2.setColor(new Color(30, 27, 75));
            g2.fillOval(figX - 3, figY - 32, 7, 7);

            // Slender body / Royal Blue Dress
            g2.setColor(COLOR_BLUE_PRIMARY);
            Polygon dress = new Polygon();
            dress.addPoint(figX, figY - 25);
            dress.addPoint(figX - 6, figY - 8);
            dress.addPoint(figX + 6, figY - 8);
            g2.fill(dress);

            // Legs
            g2.setColor(new Color(30, 27, 75));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(figX - 2, figY - 8, figX - 2, figY);
            g2.drawLine(figX + 2, figY - 8, figX + 2, figY);
        }

        private void paintForegroundFoliage(Graphics2D g2, int w, int h) {
            // Tropical leaves along bottom edge
            paintBotanicalLeaf(g2, 0, h + 10, (int) (w * 0.18), h - 70, 36, COLOR_LEAF_TEAL, COLOR_LEAF_TEAL_LIGHT);
            paintBotanicalLeaf(g2, (int) (w * 0.12), h + 20, (int) (w * 0.28), h - 50, 38, new Color(15, 118, 110), COLOR_LEAF_TEAL);
            paintBotanicalLeaf(g2, (int) (w * 0.22), h + 20, (int) (w * 0.38), h - 65, 34, new Color(109, 40, 217), new Color(168, 85, 247));
            paintBotanicalLeaf(g2, (int) (w * 0.32), h + 15, (int) (w * 0.44), h - 45, 32, COLOR_LEAF_TEAL, COLOR_LEAF_TEAL_LIGHT);
            paintBotanicalLeaf(g2, (int) (w * 0.40), h + 10, (int) (w * 0.52), h - 55, 30, new Color(13, 148, 136), COLOR_LEAF_TEAL_LIGHT);
        }

        private void paintSceneFloatingLeaves(Graphics2D g2, int w, int h) {
            int[][] scenePetals = {
                    {(int) (w * 0.18), (int) (h * 0.42), 16, 8, -30, 210},
                    {(int) (w * 0.28), (int) (h * 0.35), 18, 9, 40, 190},
                    {(int) (w * 0.45), (int) (h * 0.28), 14, 7, -20, 200},
                    {(int) (w * 0.38), (int) (h * 0.52), 15, 8, 35, 180},
                    {(int) (w * 0.55), (int) (h * 0.45), 20, 10, -45, 170},
                    {(int) (w * 0.62), (int) (h * 0.32), 16, 8, 25, 200}
            };

            for (int i = 0; i < scenePetals.length; i++) {
                int[] p = scenePetals[i];
                int px = p[0];
                int py = p[1] + (int) (Math.sin(animTick * 0.035 + i * 1.5) * 4);
                int pw = p[2];
                int ph = p[3];
                int angle = p[4] + (int) (Math.sin(animTick * 0.04 + i) * 6);
                int alpha = p[5];

                Graphics2D gp = (Graphics2D) g2.create();
                gp.translate(px, py);
                gp.rotate(Math.toRadians(angle));
                gp.setColor(new Color(13, 148, 136, alpha));

                GeneralPath path = new GeneralPath();
                path.moveTo(-pw / 2, 0);
                path.quadTo(0, -ph, pw / 2, 0);
                path.quadTo(0, ph, -pw / 2, 0);
                path.closePath();
                gp.fill(path);

                gp.setColor(new Color(255, 255, 255, alpha / 2));
                gp.setStroke(new BasicStroke(0.8f));
                gp.drawLine(-pw / 2, 0, pw / 2, 0);

                gp.dispose();
            }
        }
    }

    /**
     * Card 1: Login Form matching Reference B:
     * - Top navigation links: "Features", "Security", "Support", "Login" (active)
     * - Title: "Login" with signature royal blue underline pill
     * - Full pill input fields (height 44px, radius 22px)
     * - Royal blue pill button ("Login") with smooth hover and click feedback
     * - "Forgot your password?" link
     */
    private JPanel createLoginCard() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG_PANEL);

        // Top Navigation Bar (Reference B)
        wrapper.add(createTopNavPanel(true), BorderLayout.NORTH);

        // Centered Form
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setOpaque(false);
        formContainer.setBorder(BorderFactory.createEmptyBorder(20, 70, 40, 70));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 1. Title: "Login"
        JLabel lblTitle = new JLabel("Login", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        formContainer.add(lblTitle, gbc);

        // 2. Signature Royal Blue Accent Underline Pill (Reference B)
        JPanel underlinePill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int barW = 48;
                int barH = 4;
                int x = (w - barW) / 2;
                g2.setColor(COLOR_BLUE_PRIMARY);
                g2.fillRoundRect(x, 0, barW, barH, 4, 4);
                g2.dispose();
            }
        };
        underlinePill.setPreferredSize(new Dimension(100, 10));
        underlinePill.setOpaque(false);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 24, 0);
        formContainer.add(underlinePill, gbc);

        // 3. Alert Banner
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        formContainer.add(lblLoginAlert, gbc);

        // 4. Username / Mobile Field (Pill shape)
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 16, 0);
        formContainer.add(txtLoginUsername, gbc);

        // 5. Password Field with Toggle (Pill shape)
        JPanel passPanel = createPasswordContainer(txtLoginPassword, btnToggleLoginPass);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 22, 0);
        formContainer.add(passPanel, gbc);

        // 6. Action Button: Royal Blue Pill
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 18, 0);
        formContainer.add(btnLoginSubmit, gbc);

        // 7. Forgot Password Link
        JLabel lnkForgot = new JLabel("Forgot your password?", SwingConstants.CENTER);
        lnkForgot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lnkForgot.setForeground(COLOR_TEXT_MUTED);
        lnkForgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnkForgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showAlert(lblLoginAlert, "Contact SafePay security administration to reset credentials.", false);
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                lnkForgot.setForeground(COLOR_BLUE_PRIMARY);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                lnkForgot.setForeground(COLOR_TEXT_MUTED);
            }
        });
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 16, 0);
        formContainer.add(lnkForgot, gbc);

        // 8. Don't have an account? Create account
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        switchPanel.setOpaque(false);
        JLabel lblPrompt = new JLabel("Don't have an account?");
        lblPrompt.setForeground(COLOR_TEXT_MUTED);
        lblPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lnkSwitch = new JLabel("Create an account");
        lnkSwitch.setForeground(COLOR_BLUE_PRIMARY);
        lnkSwitch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lnkSwitch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnkSwitch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearAlerts();
                cardLayout.show(cardsPanel, CARD_REGISTER);
            }
        });

        switchPanel.add(lblPrompt);
        switchPanel.add(lnkSwitch);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        formContainer.add(switchPanel, gbc);

        // Wire Events
        btnLoginSubmit.addActionListener(e -> executeLogin());
        txtLoginPassword.addActionListener(e -> executeLogin());
        txtLoginUsername.addActionListener(e -> executeLogin());
        bindPasswordToggle(txtLoginPassword, btnToggleLoginPass);

        wrapper.add(formContainer, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Card 2: Create Account Form matching Reference B layout:
     * - Top navigation links: "Features", "Security", "Support", "Create Account" (active)
     * - Title: "Create Account" with signature royal blue underline pill
     * - Pill inputs for Username, Email, Password, Confirm Password
     * - Royal blue pill button ("Create Account") with smooth hover and click feedback
     * - "Already have an account? Log in"
     */
    private JPanel createRegisterCard() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG_PANEL);

        // Top Navigation Bar
        wrapper.add(createTopNavPanel(false), BorderLayout.NORTH);

        // Centered Form
        JPanel formContainer = new JPanel(new GridBagLayout());
        formContainer.setOpaque(false);
        formContainer.setBorder(BorderFactory.createEmptyBorder(10, 70, 24, 70));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // 1. Title: "Create Account"
        JLabel lblTitle = new JLabel("Create Account", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(COLOR_TEXT_MAIN);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        formContainer.add(lblTitle, gbc);

        // 2. Signature Royal Blue Accent Underline Pill
        JPanel underlinePill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int barW = 48;
                int barH = 4;
                int x = (w - barW) / 2;
                g2.setColor(COLOR_BLUE_PRIMARY);
                g2.fillRoundRect(x, 0, barW, barH, 4, 4);
                g2.dispose();
            }
        };
        underlinePill.setPreferredSize(new Dimension(100, 10));
        underlinePill.setOpaque(false);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 16, 0);
        formContainer.add(underlinePill, gbc);

        // 3. Alert Banner
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 10, 0);
        formContainer.add(lblRegAlert, gbc);

        // 4. Username Field (Pill)
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 12, 0);
        formContainer.add(txtRegUsername, gbc);

        // 5. Email Field (Pill)
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 12, 0);
        formContainer.add(txtRegEmail, gbc);

        // 6. Password Field (Pill)
        JPanel regPassPanel = createPasswordContainer(txtRegPassword, btnToggleRegPass);
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 12, 0);
        formContainer.add(regPassPanel, gbc);

        // 7. Confirm Password Field (Pill)
        JPanel regConfirmPanel = createPasswordContainer(txtRegConfirmPassword, btnToggleRegConfirmPass);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 18, 0);
        formContainer.add(regConfirmPanel, gbc);

        // 8. Submit Button: Royal Blue Pill
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 14, 0);
        formContainer.add(btnRegSubmit, gbc);

        // 9. Switch to Sign In Link
        JPanel switchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        switchPanel.setOpaque(false);
        JLabel lblPrompt = new JLabel("Already have an account?");
        lblPrompt.setForeground(COLOR_TEXT_MUTED);
        lblPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JLabel lnkSwitch = new JLabel("Log in here");
        lnkSwitch.setForeground(COLOR_BLUE_PRIMARY);
        lnkSwitch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lnkSwitch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lnkSwitch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clearAlerts();
                cardLayout.show(cardsPanel, CARD_LOGIN);
            }
        });

        switchPanel.add(lblPrompt);
        switchPanel.add(lnkSwitch);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        formContainer.add(switchPanel, gbc);

        // Wire Event Listeners
        btnRegSubmit.addActionListener(e -> executeRegister());
        txtRegUsername.addActionListener(e -> executeRegister());
        txtRegEmail.addActionListener(e -> executeRegister());
        txtRegPassword.addActionListener(e -> executeRegister());
        txtRegConfirmPassword.addActionListener(e -> executeRegister());
        bindPasswordToggle(txtRegPassword, btnToggleRegPass);
        bindPasswordToggle(txtRegConfirmPassword, btnToggleRegConfirmPass);

        wrapper.add(formContainer, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * Top navigation links matching Reference B:
     * "Features", "Security", "Support", "Login" (active)
     */
    private JPanel createTopNavPanel(boolean isLoginActive) {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        nav.setOpaque(false);
        nav.setBorder(BorderFactory.createEmptyBorder(16, 12, 8, 24));

        String[] links = {"Features", "Security", "Support"};
        for (String l : links) {
            JLabel lbl = new JLabel(l);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(COLOR_TEXT_MUTED);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 8));
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    lbl.setForeground(COLOR_TEXT_MAIN);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    lbl.setForeground(COLOR_TEXT_MUTED);
                }
            });
            nav.add(lbl);
        }

        // Active State Link
        JLabel lblActive = new JLabel(isLoginActive ? "Login" : "Sign Up");
        lblActive.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblActive.setForeground(COLOR_TEXT_MAIN);
        lblActive.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        lblActive.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblActive.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isLoginActive) {
                    showRegisterCard();
                } else {
                    showLoginCard();
                }
            }
        });
        nav.add(lblActive);

        return nav;
    }

    /**
     * Executes non-blocking asynchronous login on a background thread via SwingWorker.
     */
    private void executeLogin() {
        String username = txtLoginUsername.getText().trim();
        String password = new String(txtLoginPassword.getPassword());

        if (username.isEmpty() || password.isEmpty() || "Username or Email".equals(username)) {
            showAlert(lblLoginAlert, "Please enter both username and password.", false);
            return;
        }

        setFormBusy(true);
        setStatusMessage("Authenticating with SafePay security server...");

        new SwingWorker<AuthResponse, Void>() {
            @Override
            protected AuthResponse doInBackground() throws Exception {
                return apiClient.login(username, password);
            }

            @Override
            protected void done() {
                setFormBusy(false);
                try {
                    AuthResponse auth = get();
                    if (auth != null && auth.success()) {
                        setStatusMessage("Authenticated as " + auth.username() + " (" + auth.role() + ")");
                        showAlert(lblLoginAlert, "Login successful! Welcome " + auth.username(), true);

                        if (onLoginSuccess != null) {
                            onLoginSuccess.accept(auth);
                        } else {
                            DashboardFrame df = new DashboardFrame(apiClient, auth);
                            df.setVisible(true);
                        }
                        dispose();
                    } else {
                        String msg = auth != null ? auth.message() : "Login failed.";
                        showAlert(lblLoginAlert, msg, false);
                        setStatusMessage("Login failed: " + msg);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String errorMsg = cause.getMessage();
                    if (cause instanceof ApiClientException apiEx) {
                        errorMsg = apiEx.getMessage();
                    }
                    showAlert(lblLoginAlert, errorMsg, false);
                    setStatusMessage("Authentication error: " + errorMsg);
                }
            }
        }.execute();
    }

    /**
     * Executes non-blocking asynchronous registration with local confirmation check.
     */
    private void executeRegister() {
        String username = txtRegUsername.getText().trim();
        String email = txtRegEmail.getText().trim();
        String password = new String(txtRegPassword.getPassword());
        String confirmPass = new String(txtRegConfirmPassword.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(lblRegAlert, "Please complete all registration fields.", false);
            return;
        }

        if (username.length() < 3) {
            showAlert(lblRegAlert, "Username must be at least 3 characters.", false);
            return;
        }

        if (!password.equals(confirmPass)) {
            showAlert(lblRegAlert, "Passwords do not match. Please verify.", false);
            return;
        }

        setFormBusy(true);
        setStatusMessage("Creating your SafePay account & ledger...");

        new SwingWorker<AuthResponse, Void>() {
            @Override
            protected AuthResponse doInBackground() throws Exception {
                return apiClient.register(username, email, password);
            }

            @Override
            protected void done() {
                setFormBusy(false);
                try {
                    AuthResponse auth = get();
                    if (auth != null && auth.success()) {
                        setStatusMessage("Account created successfully for " + auth.username());
                        // Clear register form
                        txtRegUsername.setText("");
                        txtRegEmail.setText("");
                        txtRegPassword.setText("");
                        txtRegConfirmPassword.setText("");

                        // Switch to login card and display success banner
                        cardLayout.show(cardsPanel, CARD_LOGIN);
                        txtLoginUsername.setText(auth.username());
                        txtLoginPassword.setText("");
                        showAlert(lblLoginAlert, "Registration successful! Initial balance of ₹50,000 credited. Please sign in.", true);
                    } else {
                        String msg = auth != null ? auth.message() : "Registration failed.";
                        showAlert(lblRegAlert, msg, false);
                        setStatusMessage("Registration failed: " + msg);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String errorMsg = cause.getMessage();
                    if (cause instanceof ApiClientException apiEx) {
                        errorMsg = apiEx.getMessage();
                    }
                    showAlert(lblRegAlert, errorMsg, false);
                    setStatusMessage("Registration error: " + errorMsg);
                }
            }
        }.execute();
    }

    private void setFormBusy(boolean busy) {
        btnLoginSubmit.setEnabled(!busy);
        btnRegSubmit.setEnabled(!busy);
        txtLoginUsername.setEnabled(!busy);
        txtLoginPassword.setEnabled(!busy);
        txtRegUsername.setEnabled(!busy);
        txtRegEmail.setEnabled(!busy);
        txtRegPassword.setEnabled(!busy);
        txtRegConfirmPassword.setEnabled(!busy);

        btnLoginSubmit.setText(busy ? "Signing In..." : "Login");
        btnRegSubmit.setText(busy ? "Creating Account..." : "Create Account");
    }

    private void showAlert(JLabel alertLabel, String message, boolean success) {
        alertLabel.setText(message);
        alertLabel.setBackground(success ? COLOR_ALERT_SUCC_BG : COLOR_ALERT_ERR_BG);
        alertLabel.setForeground(success ? COLOR_ALERT_SUCC_TEXT : COLOR_ALERT_ERR_TEXT);
        alertLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(success ? COLOR_ALERT_SUCC_BORDER : COLOR_ALERT_ERR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        alertLabel.setVisible(true);
    }

    private void clearAlerts() {
        lblLoginAlert.setVisible(false);
        lblRegAlert.setVisible(false);
    }

    private JLabel createAlertLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setVisible(false);
        return label;
    }

    /**
     * Creates a pill-shaped input text field (radius 22px, height 44px).
     */
    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField(15) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Soft background
                g2.setColor(COLOR_INPUT_BG);
                g2.fillRoundRect(0, 0, w, h, 22, 22);

                // Subtle border
                g2.setColor(hasFocus() ? COLOR_BLUE_PRIMARY : COLOR_BORDER_INPUT);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setForeground(COLOR_TEXT_MAIN);
        field.setCaretColor(COLOR_TEXT_MAIN);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setPreferredSize(new Dimension(280, 44));
        field.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Placeholder support
        field.setText(placeholder);
        field.setForeground(COLOR_TEXT_MUTED);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (placeholder.equals(field.getText())) {
                    field.setText("");
                    field.setForeground(COLOR_TEXT_MAIN);
                }
                field.repaint();
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().trim().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(COLOR_TEXT_MUTED);
                }
                field.repaint();
            }
        });

        return field;
    }

    /**
     * Creates a pill-shaped password input field.
     */
    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField(15) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setBackground(new Color(0, 0, 0, 0));
        field.setForeground(COLOR_TEXT_MAIN);
        field.setCaretColor(COLOR_TEXT_MAIN);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 6));
        return field;
    }

    private JPanel createPasswordContainer(JPasswordField passField, JButton eyeBtn) {
        JPanel container = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Soft background
                g2.setColor(COLOR_INPUT_BG);
                g2.fillRoundRect(0, 0, w, h, 22, 22);

                // Border
                boolean focused = passField.hasFocus();
                g2.setColor(focused ? COLOR_BLUE_PRIMARY : COLOR_BORDER_INPUT);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        container.setOpaque(false);
        container.setPreferredSize(new Dimension(280, 44));
        container.add(passField, BorderLayout.CENTER);
        container.add(eyeBtn, BorderLayout.EAST);

        passField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                container.repaint();
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                container.repaint();
            }
        });

        return container;
    }

    private JButton createEyeToggleButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int ew = 16;
                int eh = 10;
                int ex = (w - ew) / 2;
                int ey = (h - eh) / 2;
                g2.setColor(getForeground());
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawOval(ex, ey, ew, eh);
                g2.fillOval(ex + (ew - 6) / 2, ey + (eh - 6) / 2, 6, 6);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(36, 44));
        btn.setForeground(COLOR_TEXT_MUTED);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Toggle password visibility");
        return btn;
    }

    private void bindPasswordToggle(JPasswordField field, JButton eyeBtn) {
        char defaultEcho = field.getEchoChar();
        eyeBtn.addActionListener(e -> {
            if (field.getEchoChar() != (char) 0) {
                field.setEchoChar((char) 0);
                eyeBtn.setForeground(COLOR_BLUE_PRIMARY);
            } else {
                field.setEchoChar(defaultEcho);
                eyeBtn.setForeground(COLOR_TEXT_MUTED);
            }
        });
    }

    /**
     * Creates a prominent Royal Blue Pill Action Button (Reference B) with smooth hover and click feedback.
     */
    private JButton createPillActionButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (isEnabled()) {
                    boolean pressed = getModel().isPressed();
                    boolean hover = getModel().isRollover();

                    if (pressed) {
                        g2.setColor(new Color(30, 58, 138)); // Deep Navy #1E3A8A
                    } else if (hover) {
                        g2.setColor(COLOR_BLUE_HOVER);       // Rich Royal Blue #1E40AF
                    } else {
                        g2.setColor(COLOR_BLUE_PRIMARY);     // #1D4ED8
                    }
                    g2.fillRoundRect(0, 0, w, h, 22, 22);

                    // Top subtle gloss highlight when hovered
                    if (hover) {
                        g2.setPaint(new GradientPaint(
                                0, 0, new Color(255, 255, 255, 45),
                                0, h / 2, new Color(255, 255, 255, 0)
                        ));
                        g2.fillRoundRect(2, 2, w - 4, h / 2, 18, 18);
                    }
                } else {
                    g2.setColor(new Color(148, 163, 184));
                    g2.fillRoundRect(0, 0, w, h, 22, 22);
                }

                // Text
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                int strW = g2.getFontMetrics().stringWidth(getText());
                int strH = g2.getFontMetrics().getAscent();
                g2.drawString(getText(), (w - strW) / 2, (h + strH) / 2 - 2);

                g2.dispose();
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(280, 44));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
