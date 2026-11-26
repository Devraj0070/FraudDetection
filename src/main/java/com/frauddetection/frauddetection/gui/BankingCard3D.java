package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Premium 3D Interactive Animated Banking Card component built in pure Java 2D.
 *
 * Capabilities:
 * - 3D Euler Matrix Perspective Projection (Pitch, Yaw, Roll).
 * - Interactive mouse tracking: Card smoothly tilts toward the cursor with spring physics.
 * - Idle floating motion: Gentle sinusoidal vertical breathing and yaw oscillation.
 * - Dynamic Specular Lighting: Specular reflection sheen that sweeps across the card surface tracking tilt.
 * - Card elements: Deep Black Titanium body, Electric Neon Lime perimeter glow,
 *   metallic gold EMV chip with micro-circuit traces, contactless wave symbol,
 *   embossed cardholder name, masked card number, and SafePay brand insignia.
 * - Non-blocking 60 FPS animation loop powered by javax.swing.Timer.
 */
public class BankingCard3D extends JComponent {

    public static final Color COLOR_CARD_BODY_START = new Color(22, 22, 22);   // #161616
    public static final Color COLOR_CARD_BODY_END = new Color(10, 10, 10);     // #0A0A0A
    public static final Color COLOR_LIME_ACCENT = new Color(216, 255, 0);      // #D8FF00
    public static final Color COLOR_LIME_DIM = new Color(216, 255, 0, 70);
    public static final Color COLOR_GOLD_CHIP_1 = new Color(234, 179, 8);      // #EAB308
    public static final Color COLOR_GOLD_CHIP_2 = new Color(202, 138, 4);      // #CA8A04
    public static final Color COLOR_GOLD_BORDER = new Color(254, 240, 138);

    private static final Font FONT_BRAND = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_NUMBER = new Font("Consolas", Font.BOLD, 16);
    private static final Font FONT_HOLDER = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 8);

    // Dimensions of the unprojected card
    private final double cardWidth;
    private final double cardHeight;

    // 3D Rotation State
    private double currentPitch = 0.0; // Rotation around X-axis (tilt up/down)
    private double currentYaw = 0.0;   // Rotation around Y-axis (tilt left/right)
    private double targetPitch = 0.0;
    private double targetYaw = 0.0;

    // Animation / Floating State
    private double animTime = 0.0;
    private boolean isHovered = false;
    private Timer animationTimer;

    // Card Details
    private String cardHolder = "ALEX MORGAN";
    private String cardNumber = "••••  ••••  ••••  8829";
    private String expiryDate = "12/29";
    private String balanceDisplay = "₹ 49,850.00";

    public BankingCard3D(int width, int height) {
        this.cardWidth = width > 0 ? width : 360;
        this.cardHeight = height > 0 ? height : 220;

        setPreferredSize(new Dimension((int) (cardWidth + 80), (int) (cardHeight + 80)));
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        initMouseListeners();
        initAnimationTimer();
    }

    public BankingCard3D() {
        this(360, 220);
    }

    public void setCardDetails(String cardHolder, String cardNumber, String expiryDate, String balanceDisplay) {
        if (cardHolder != null && !cardHolder.isBlank()) {
            this.cardHolder = cardHolder.toUpperCase().trim();
        }
        if (cardNumber != null && !cardNumber.isBlank()) {
            this.cardNumber = cardNumber;
        }
        if (expiryDate != null && !expiryDate.isBlank()) {
            this.expiryDate = expiryDate;
        }
        if (balanceDisplay != null && !balanceDisplay.isBlank()) {
            this.balanceDisplay = balanceDisplay;
        }
        repaint();
    }

    private void initMouseListeners() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                isHovered = true;
                double centerX = getWidth() / 2.0;
                double centerY = getHeight() / 2.0;

                // Max interactive tilt +/- 18 degrees
                double dx = (e.getX() - centerX) / centerX;
                double dy = (e.getY() - centerY) / centerY;

                targetYaw = dx * Math.toRadians(20.0);
                targetPitch = -dy * Math.toRadians(18.0);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                targetYaw = 0.0;
                targetPitch = 0.0;
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void initAnimationTimer() {
        // 60 FPS (~16ms) loop
        animationTimer = new Timer(16, e -> {
            animTime += 0.025;

            // Idle floating animation
            double idleFloatY = Math.sin(animTime * 1.5) * 6.0;
            double idleYaw = isHovered ? 0.0 : Math.cos(animTime * 1.1) * Math.toRadians(3.5);
            double idlePitch = isHovered ? 0.0 : Math.sin(animTime * 1.3) * Math.toRadians(2.5);

            // Damped spring physics toward target
            double lerpFactor = isHovered ? 0.12 : 0.06;
            currentPitch += (targetPitch + idlePitch - currentPitch) * lerpFactor;
            currentYaw += (targetYaw + idleYaw - currentYaw) * lerpFactor;

            repaint();
        });
        animationTimer.start();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Camera distance for perspective projection
        double cameraD = 650.0;

        // 4 Corners of unprojected card in 3D local coordinates
        double halfW = cardWidth / 2.0;
        double halfH = cardHeight / 2.0;

        Point3D[] corners = new Point3D[] {
                new Point3D(-halfW, -halfH, 0),
                new Point3D(halfW, -halfH, 0),
                new Point3D(halfW, halfH, 0),
                new Point3D(-halfW, halfH, 0)
        };

        // Project 4 corners
        Point2D.Double[] projCorners = new Point2D.Double[4];
        for (int i = 0; i < 4; i++) {
            projCorners[i] = projectPoint(corners[i], currentPitch, currentYaw, centerX, centerY, cameraD);
        }

        // 1. Draw 3D Ambient Drop Shadow under the card
        drawCardDropShadow(g2, projCorners, centerX, centerY);

        // 2. Draw 3D Card Body (Titanium Obsidian polygon)
        GeneralPath cardShape = new GeneralPath();
        cardShape.moveTo(projCorners[0].x, projCorners[0].y);
        cardShape.lineTo(projCorners[1].x, projCorners[1].y);
        cardShape.lineTo(projCorners[2].x, projCorners[2].y);
        cardShape.lineTo(projCorners[3].x, projCorners[3].y);
        cardShape.closePath();

        // Card base gradient
        GradientPaint bodyPaint = new GradientPaint(
                (float) projCorners[0].x, (float) projCorners[0].y, COLOR_CARD_BODY_START,
                (float) projCorners[2].x, (float) projCorners[2].y, COLOR_CARD_BODY_END
        );
        g2.setPaint(bodyPaint);
        g2.fill(cardShape);

        // 3. Draw Dynamic Specular Sheen (Reflective Light Ray tracking Yaw/Pitch)
        drawSpecularHighlight(g2, projCorners, cardShape);

        // 4. Perimeter Neon Electric Lime Border
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(216, 255, 0, 180));
        g2.draw(cardShape);

        // Inner subtle rim
        g2.setStroke(new BasicStroke(0.8f));
        g2.setColor(new Color(255, 255, 255, 30));
        g2.draw(cardShape);

        // 5. Draw Card Content Details (Logo, Chip, Number, Holder)
        drawCardFeatures(g2, currentPitch, currentYaw, centerX, centerY, cameraD);

        g2.dispose();
    }

    private void drawCardDropShadow(Graphics2D g2, Point2D.Double[] corners, int cx, int cy) {
        GeneralPath shadowShape = new GeneralPath();
        double shadowOffsetY = 18.0;
        shadowShape.moveTo(corners[0].x, corners[0].y + shadowOffsetY);
        shadowShape.lineTo(corners[1].x, corners[1].y + shadowOffsetY);
        shadowShape.lineTo(corners[2].x, corners[2].y + shadowOffsetY);
        shadowShape.lineTo(corners[3].x, corners[3].y + shadowOffsetY);
        shadowShape.closePath();

        // Multi-layer soft shadow
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fill(shadowShape);

        // Neon lime ambient glow below the card
        g2.setColor(new Color(216, 255, 0, 18));
        g2.fillOval(cx - (int) (cardWidth * 0.4), cy + (int) (cardHeight * 0.42), (int) (cardWidth * 0.8), 24);
    }

    private void drawSpecularHighlight(Graphics2D g2, Point2D.Double[] corners, GeneralPath cardShape) {
        Graphics2D gSpec = (Graphics2D) g2.create();
        gSpec.setClip(cardShape);

        // Specular highlight line sweeps across with yaw
        double sheenOffset = currentYaw * 300.0;
        float startX = (float) (corners[0].x + sheenOffset);
        float endX = (float) (corners[1].x + sheenOffset);

        float gradStart = Math.min(startX, endX);
        float gradEnd = Math.max(startX, endX) + 120.0f;

        if (gradEnd > gradStart) {
            LinearGradientPaint sheenPaint = new LinearGradientPaint(
                    gradStart, (float) corners[0].y,
                    gradEnd, (float) corners[2].y,
                    new float[] {0.0f, 0.45f, 0.50f, 0.55f, 1.0f},
                    new Color[] {
                            new Color(255, 255, 255, 0),
                            new Color(255, 255, 255, 10),
                            new Color(216, 255, 0, 50),
                            new Color(255, 255, 255, 25),
                            new Color(255, 255, 255, 0)
                    }
            );
            gSpec.setPaint(sheenPaint);
            gSpec.fill(cardShape);
        }
        gSpec.dispose();
    }

    private void drawCardFeatures(Graphics2D g2, double pitch, double yaw, int cx, int cy, double cameraD) {
        double halfW = cardWidth / 2.0;
        double halfH = cardHeight / 2.0;

        // A. SafePay Brand Badge (Top-Right)
        Point2D.Double logoPos = projectPoint(
                new Point3D(halfW - 80, -halfH + 34, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(COLOR_LIME_ACCENT);
        g2.setFont(FONT_BRAND);
        g2.drawString("SafePay", (float) logoPos.x, (float) logoPos.y);

        // Subtext "PLATINUM"
        Point2D.Double tagPos = projectPoint(
                new Point3D(halfW - 80, -halfH + 46, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(new Color(255, 255, 255, 140));
        g2.setFont(FONT_LABEL);
        g2.drawString("TITANIUM SHIELD", (float) tagPos.x, (float) tagPos.y);

        // B. Metallic Gold EMV Chip (Top-Left)
        drawEmvChip(g2, -halfW + 36, -halfH + 36, pitch, yaw, cx, cy, cameraD);

        // C. Contactless Payment Wave (Next to Chip)
        drawContactlessWave(g2, -halfW + 90, -halfH + 52, pitch, yaw, cx, cy, cameraD);

        // D. Card Number (Masked)
        Point2D.Double numPos = projectPoint(
                new Point3D(-halfW + 36, 12, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(Color.WHITE);
        g2.setFont(FONT_NUMBER);
        g2.drawString(cardNumber, (float) numPos.x, (float) numPos.y);

        // E. Cardholder Name & Expiry (Bottom)
        Point2D.Double holderLabelPos = projectPoint(
                new Point3D(-halfW + 36, halfH - 42, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(new Color(156, 163, 175));
        g2.setFont(FONT_LABEL);
        g2.drawString("CARDHOLDER", (float) holderLabelPos.x, (float) holderLabelPos.y);

        Point2D.Double holderValPos = projectPoint(
                new Point3D(-halfW + 36, halfH - 26, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(Color.WHITE);
        g2.setFont(FONT_HOLDER);
        g2.drawString(cardHolder, (float) holderValPos.x, (float) holderValPos.y);

        // Expiry Date (Bottom Right)
        Point2D.Double expLabelPos = projectPoint(
                new Point3D(halfW - 75, halfH - 42, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(new Color(156, 163, 175));
        g2.setFont(FONT_LABEL);
        g2.drawString("EXPIRES", (float) expLabelPos.x, (float) expLabelPos.y);

        Point2D.Double expValPos = projectPoint(
                new Point3D(halfW - 75, halfH - 26, 0), pitch, yaw, cx, cy, cameraD
        );
        g2.setColor(Color.WHITE);
        g2.setFont(FONT_HOLDER);
        g2.drawString(expiryDate, (float) expValPos.x, (float) expValPos.y);
    }

    private void drawEmvChip(Graphics2D g2, double localX, double localY,
                             double pitch, double yaw, int cx, int cy, double cameraD) {
        double chipW = 42.0;
        double chipH = 32.0;

        Point3D[] chipCorners = new Point3D[] {
                new Point3D(localX, localY, 0),
                new Point3D(localX + chipW, localY, 0),
                new Point3D(localX + chipW, localY + chipH, 0),
                new Point3D(localX, localY + chipH, 0)
        };

        GeneralPath chipPath = new GeneralPath();
        Point2D.Double first = projectPoint(chipCorners[0], pitch, yaw, cx, cy, cameraD);
        chipPath.moveTo(first.x, first.y);
        for (int i = 1; i < 4; i++) {
            Point2D.Double pt = projectPoint(chipCorners[i], pitch, yaw, cx, cy, cameraD);
            chipPath.lineTo(pt.x, pt.y);
        }
        chipPath.closePath();

        // Gold chip gradient
        Point2D.Double last = projectPoint(chipCorners[2], pitch, yaw, cx, cy, cameraD);
        GradientPaint goldPaint = new GradientPaint(
                (float) first.x, (float) first.y, COLOR_GOLD_CHIP_1,
                (float) last.x, (float) last.y, COLOR_GOLD_CHIP_2
        );
        g2.setPaint(goldPaint);
        g2.fill(chipPath);

        // Chip border
        g2.setColor(COLOR_GOLD_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(chipPath);

        // Chip circuitry traces (horizontal & vertical contact dividers)
        Point2D.Double midL = projectPoint(new Point3D(localX, localY + chipH / 2.0, 0), pitch, yaw, cx, cy, cameraD);
        Point2D.Double midR = projectPoint(new Point3D(localX + chipW, localY + chipH / 2.0, 0), pitch, yaw, cx, cy, cameraD);
        g2.setColor(new Color(133, 77, 14, 180));
        g2.drawLine((int) midL.x, (int) midL.y, (int) midR.x, (int) midR.y);

        Point2D.Double midT = projectPoint(new Point3D(localX + chipW * 0.45, localY, 0), pitch, yaw, cx, cy, cameraD);
        Point2D.Double midB = projectPoint(new Point3D(localX + chipW * 0.45, localY + chipH, 0), pitch, yaw, cx, cy, cameraD);
        g2.drawLine((int) midT.x, (int) midT.y, (int) midB.x, (int) midB.y);
    }

    private void drawContactlessWave(Graphics2D g2, double localX, double localY,
                                     double pitch, double yaw, int cx, int cy, double cameraD) {
        Point2D.Double basePos = projectPoint(new Point3D(localX, localY, 0), pitch, yaw, cx, cy, cameraD);
        g2.setColor(new Color(255, 255, 255, 160));
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // 3 concentric wave arcs
        int bx = (int) basePos.x;
        int by = (int) basePos.y;
        g2.drawArc(bx - 3, by - 6, 12, 12, -45, 90);
        g2.drawArc(bx + 1, by - 10, 20, 20, -45, 90);
        g2.drawArc(bx + 5, by - 14, 28, 28, -45, 90);
    }

    /**
     * 3D Euler matrix rotation and perspective projection calculation.
     */
    private Point2D.Double projectPoint(Point3D p, double pitch, double yaw, int cx, int cy, double cameraD) {
        // 1. Yaw rotation (around Y axis)
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        double x1 = p.x * cosY + p.z * sinY;
        double y1 = p.y;
        double z1 = -p.x * sinY + p.z * cosY;

        // 2. Pitch rotation (around X axis)
        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        double x2 = x1;
        double y2 = y1 * cosP - z1 * sinP;
        double z2 = y1 * sinP + z1 * cosP;

        // 3. Perspective division
        double distance = cameraD + z2;
        if (distance <= 10.0) distance = 10.0;
        double scale = cameraD / distance;

        double screenX = cx + (x2 * scale);
        double screenY = cy + (y2 * scale);

        return new Point2D.Double(screenX, screenY);
    }

    private static class Point3D {
        final double x, y, z;
        Point3D(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
