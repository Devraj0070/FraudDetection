package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Custom Java 2D vector chart rendering recent transaction activity with
 * a smooth spline trend curve, Electric Neon Lime area gradient fill, glowing highlight markers,
 * and floating dark tooltip card matching the SafePay Obsidian design system.
 */
public class TransactionAmountChart extends JComponent {

    private final List<TransactionSummaryDto> transactions = new ArrayList<>();

    // Status & Accent Colors
    public static final Color COLOR_APPROVED = new Color(16, 185, 129);   // #10B981 (Green)
    public static final Color COLOR_BLOCKED = new Color(239, 68, 68);     // #EF4444 (Red)
    public static final Color COLOR_DECLINED = new Color(245, 158, 11);   // #F59E0B (Amber)
    public static final Color COLOR_PENDING = new Color(245, 158, 11);

    public static final Color COLOR_LIME_PRIMARY = new Color(216, 255, 0); // #D8FF00 Electric Lime
    public static final Color COLOR_LIME_DIM = new Color(216, 255, 0, 40);
    public static final Color COLOR_PURPLE_PRIMARY = COLOR_LIME_PRIMARY;   // Compatibility alias
    public static final Color COLOR_PURPLE_LIGHT = new Color(216, 255, 0, 120);

    public static final Color COLOR_GRID_LINE = new Color(36, 36, 36);     // #242424
    public static final Color COLOR_TEXT_LABEL = new Color(156, 163, 175); // #9CA3AF
    public static final Color COLOR_TOOLTIP_BG = new Color(17, 17, 17, 245); // #111111

    private static final Font FONT_AXIS = new Font("Segoe UI", Font.PLAIN, 10);
    private static final Font FONT_TOOLTIP_VAL = new Font("Consolas", Font.BOLD, 12);
    private static final Font FONT_TOOLTIP_LABEL = new Font("Segoe UI", Font.PLAIN, 10);
    private static final Font FONT_EMPTY = new Font("Segoe UI", Font.BOLD, 13);

    // Hover tooltip state
    private int hoveredPointIndex = -1;
    private final List<PointInfo> pointCoordinates = new ArrayList<>();

    // Draw-in animation progress (0.0 -> 1.0)
    private double animProgress = 1.0;
    private Timer animTimer;

    public TransactionAmountChart() {
        setPreferredSize(new Dimension(540, 205));
        setMinimumSize(new Dimension(380, 160));
        setOpaque(false);

        initHoverListener();
    }

    private void initHoverListener() {
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldIndex = hoveredPointIndex;
                hoveredPointIndex = -1;

                int mx = e.getX();
                int my = e.getY();

                for (int i = 0; i < pointCoordinates.size(); i++) {
                    PointInfo pt = pointCoordinates.get(i);
                    double dist = Math.hypot(mx - pt.x, my - pt.y);
                    if (dist <= 18.0) {
                        hoveredPointIndex = i;
                        break;
                    }
                }

                if (oldIndex != hoveredPointIndex) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredPointIndex != -1) {
                    hoveredPointIndex = -1;
                    repaint();
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public void setTransactions(List<TransactionSummaryDto> list) {
        this.transactions.clear();
        if (list != null) {
            List<TransactionSummaryDto> copy = new ArrayList<>(list);
            Collections.reverse(copy);
            this.transactions.addAll(copy);
        }

        if (isShowing()) {
            animProgress = 0.1;
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animTimer = new Timer(16, e -> {
                animProgress += 0.08;
                if (animProgress >= 1.0) {
                    animProgress = 1.0;
                    animTimer.stop();
                }
                repaint();
            });
            animTimer.start();
        } else {
            animProgress = 1.0;
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        // Dark card background
        g2.setColor(new Color(22, 22, 22));
        g2.fillRoundRect(0, 0, w, h, 14, 14);
        g2.setColor(new Color(36, 36, 36));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        if (transactions.isEmpty()) {
            paintEmptyState(g2, w, h);
            g2.dispose();
            return;
        }

        double maxVal = 1000.0;
        for (TransactionSummaryDto tx : transactions) {
            if (tx.amount() != null && tx.amount().doubleValue() > maxVal) {
                maxVal = tx.amount().doubleValue();
            }
        }
        maxVal = Math.ceil(maxVal * 1.25);

        int padLeft = 65;
        int padRight = 35;
        int padTop = 35;
        int padBottom = 38;

        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        // 1. Draw Subtle Dark Grid Lines & Y-Axis Labels
        drawGridAndAxes(g2, padLeft, padRight, padTop, padBottom, chartW, chartH, maxVal);

        // 2. Compute Data Points
        pointCoordinates.clear();
        int n = transactions.size();
        double stepX = n > 1 ? (double) chartW / (n - 1) : chartW / 2.0;

        for (int i = 0; i < n; i++) {
            TransactionSummaryDto tx = transactions.get(i);
            double val = tx.amount() != null ? tx.amount().doubleValue() : 0.0;
            double px = n > 1 ? padLeft + i * stepX : padLeft + chartW / 2.0;
            double normalizedY = (val / maxVal);
            // Apply draw-in animation
            double py = (padTop + chartH) - (normalizedY * chartH * animProgress);
            pointCoordinates.add(new PointInfo(px, py, tx));
        }

        // 3. Draw Spline Area Gradient
        drawSplineArea(g2, pointCoordinates, padTop + chartH);

        // 4. Draw Electric Lime Spline Stroke
        drawSplineLine(g2, pointCoordinates);

        // 5. Draw Glowing Data Point Markers
        drawPointMarkers(g2, pointCoordinates);

        // 6. Draw Hover Tooltip if active
        if (hoveredPointIndex >= 0 && hoveredPointIndex < pointCoordinates.size()) {
            drawTooltip(g2, pointCoordinates.get(hoveredPointIndex), w, h);
        }

        g2.dispose();
    }

    private void drawGridAndAxes(Graphics2D g2, int padLeft, int padRight, int padTop, int padBottom,
                                 int chartW, int chartH, double maxVal) {
        g2.setFont(FONT_AXIS);
        g2.setColor(COLOR_GRID_LINE);
        g2.setStroke(new BasicStroke(1.0f));

        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            int y = padTop + (int) ((double) i / gridLines * chartH);
            g2.setColor(COLOR_GRID_LINE);
            g2.drawLine(padLeft, y, padLeft + chartW, y);

            double labelVal = maxVal * (1.0 - (double) i / gridLines);
            g2.setColor(COLOR_TEXT_LABEL);
            String label = "₹ " + String.format("%,.0f", labelVal);
            g2.drawString(label, 8, y + 4);
        }
    }

    private void drawSplineArea(Graphics2D g2, List<PointInfo> pts, int baselineY) {
        if (pts.size() < 2) return;

        GeneralPath path = new GeneralPath();
        path.moveTo(pts.get(0).x, pts.get(0).y);

        for (int i = 0; i < pts.size() - 1; i++) {
            PointInfo p0 = pts.get(i);
            PointInfo p1 = pts.get(i + 1);
            double ctrlX1 = p0.x + (p1.x - p0.x) / 2.0;
            double ctrlY1 = p0.y;
            double ctrlX2 = p0.x + (p1.x - p0.x) / 2.0;
            double ctrlY2 = p1.y;
            path.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, p1.x, p1.y);
        }

        // Close path to baseline for area fill
        path.lineTo(pts.get(pts.size() - 1).x, baselineY);
        path.lineTo(pts.get(0).x, baselineY);
        path.closePath();

        GradientPaint areaGrad = new GradientPaint(
                0, (float) pts.get(0).y, new Color(216, 255, 0, 75),
                0, baselineY, new Color(216, 255, 0, 0)
        );
        g2.setPaint(areaGrad);
        g2.fill(path);
    }

    private void drawSplineLine(Graphics2D g2, List<PointInfo> pts) {
        if (pts.size() < 2) return;

        GeneralPath path = new GeneralPath();
        path.moveTo(pts.get(0).x, pts.get(0).y);

        for (int i = 0; i < pts.size() - 1; i++) {
            PointInfo p0 = pts.get(i);
            PointInfo p1 = pts.get(i + 1);
            double ctrlX1 = p0.x + (p1.x - p0.x) / 2.0;
            double ctrlY1 = p0.y;
            double ctrlX2 = p0.x + (p1.x - p0.x) / 2.0;
            double ctrlY2 = p1.y;
            path.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, p1.x, p1.y);
        }

        // Glowing outer halo
        g2.setColor(new Color(216, 255, 0, 60));
        g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);

        // Core crisp lime line
        g2.setColor(COLOR_LIME_PRIMARY);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(path);
    }

    private void drawPointMarkers(Graphics2D g2, List<PointInfo> pts) {
        for (int i = 0; i < pts.size(); i++) {
            PointInfo pt = pts.get(i);
            boolean isHover = (i == hoveredPointIndex);

            int outerR = isHover ? 14 : 8;
            int innerR = isHover ? 6 : 4;

            // Ambient glow
            g2.setColor(new Color(216, 255, 0, isHover ? 160 : 70));
            g2.fillOval((int) (pt.x - outerR / 2.0), (int) (pt.y - outerR / 2.0), outerR, outerR);

            // Core dot
            Color dotColor = getStatusColor(pt.dto.status());
            g2.setColor(dotColor);
            g2.fillOval((int) (pt.x - innerR / 2.0), (int) (pt.y - innerR / 2.0), innerR, innerR);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawOval((int) (pt.x - innerR / 2.0), (int) (pt.y - innerR / 2.0), innerR, innerR);
        }
    }

    private void drawTooltip(Graphics2D g2, PointInfo pt, int w, int h) {
        String amtStr = pt.dto.amount() != null
                ? "₹ " + String.format("%,.2f", pt.dto.amount().doubleValue())
                : "₹ 0.00";
        String typeStatus = pt.dto.transactionType() + " • " + pt.dto.status();

        int boxW = 150;
        int boxH = 50;
        int boxX = (int) pt.x - boxW / 2;
        int boxY = (int) pt.y - boxH - 12;

        if (boxX < 10) boxX = 10;
        if (boxX + boxW > w - 10) boxX = w - boxW - 10;
        if (boxY < 5) boxY = (int) pt.y + 14;

        // Tooltip box
        g2.setColor(COLOR_TOOLTIP_BG);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
        g2.setColor(new Color(36, 36, 36));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);

        // Tooltip text
        g2.setFont(FONT_TOOLTIP_VAL);
        g2.setColor(COLOR_LIME_PRIMARY);
        g2.drawString(amtStr, boxX + 12, boxY + 22);

        g2.setFont(FONT_TOOLTIP_LABEL);
        g2.setColor(getStatusColor(pt.dto.status()));
        g2.drawString(typeStatus, boxX + 12, boxY + 38);
    }

    private Color getStatusColor(String status) {
        if (status == null) return COLOR_APPROVED;
        return switch (status.toUpperCase()) {
            case "APPROVED" -> COLOR_APPROVED;
            case "BLOCKED" -> COLOR_BLOCKED;
            case "DECLINED" -> COLOR_DECLINED;
            default -> COLOR_PENDING;
        };
    }

    private void paintEmptyState(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(22, 22, 22));
        g2.fillRoundRect(10, 10, w - 20, h - 20, 16, 16);
        g2.setColor(new Color(36, 36, 36));
        g2.drawRoundRect(10, 10, w - 20, h - 20, 16, 16);

        g2.setFont(FONT_EMPTY);
        g2.setColor(new Color(156, 163, 175));
        String msg = "No transaction velocity recorded yet";
        int strW = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (w - strW) / 2, h / 2 + 5);
    }

    private static class PointInfo {
        final double x, y;
        final TransactionSummaryDto dto;
        PointInfo(double x, double y, TransactionSummaryDto dto) {
            this.x = x;
            this.y = y;
            this.dto = dto;
        }
    }
}
