package com.frauddetection.frauddetection.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;

import com.frauddetection.frauddetection.dto.TransactionSummaryDto;

/**
 * Custom Java 2D vector chart rendering recent transaction activity with
 * a smooth spline trend curve, purple area gradient fill, glowing highlight markers,
 * and floating dark tooltip card matching the Finora specification (Reference A).
 */
public class TransactionAmountChart extends JComponent {

    private final List<TransactionSummaryDto> transactions = new ArrayList<>();

    // Status Colors
    public static final Color COLOR_APPROVED = new Color(22, 163, 74);   // #16A34A (Green)
    public static final Color COLOR_BLOCKED = new Color(220, 38, 38);    // #DC2626 (Red)
    public static final Color COLOR_DECLINED = new Color(217, 119, 6);   // #D97706 (Amber)
    public static final Color COLOR_PURPLE_PRIMARY = new Color(124, 58, 237); // #7C3AED (Finora Purple)
    public static final Color COLOR_PURPLE_LIGHT = new Color(196, 181, 253);   // #C4B5FD

    public static final Color COLOR_GRID_LINE = new Color(241, 245, 249);
    public static final Color COLOR_TEXT_LABEL = new Color(148, 163, 184);
    public static final Color COLOR_TOOLTIP_BG = new Color(24, 24, 27, 240); // Dark Slate

    public TransactionAmountChart() {
        setPreferredSize(new Dimension(540, 205));
        setMinimumSize(new Dimension(380, 160));
        setOpaque(false);
    }

    public void setTransactions(List<TransactionSummaryDto> list) {
        this.transactions.clear();
        if (list != null) {
            // Display transactions chronologically from left to right
            List<TransactionSummaryDto> copy = new ArrayList<>(list);
            Collections.reverse(copy);
            this.transactions.addAll(copy);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();

        if (transactions.isEmpty()) {
            paintEmptyState(g2, w, h);
            g2.dispose();
            return;
        }

        // Determine maximum value for Y-axis scaling
        double maxVal = 1000.0;
        for (TransactionSummaryDto tx : transactions) {
            if (tx.amount() != null && tx.amount().doubleValue() > maxVal) {
                maxVal = tx.amount().doubleValue();
            }
        }
        maxVal = Math.ceil(maxVal * 1.25);

        int padLeft = 65;
        int padRight = 35;
        int padTop = 45;
        int padBottom = 42;

        int chartW = w - padLeft - padRight;
        int chartH = h - padTop - padBottom;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        int groundY = padTop + chartH;

        // 1. Draw Horizontal Gridlines and Currency Labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= 3; i++) {
            double ratio = (double) i / 3.0;
            int gy = padTop + (int) ((1.0 - ratio) * chartH);
            double val = maxVal * ratio;

            g2.setColor(COLOR_GRID_LINE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(padLeft, gy, w - padRight, gy);

            g2.setColor(COLOR_TEXT_LABEL);
            String valLabel = "₹" + String.format("%,.0f", val);
            int strW = g2.getFontMetrics().stringWidth(valLabel);
            g2.drawString(valLabel, padLeft - strW - 8, gy + 4);
        }

        // 2. Compute Points on Chart
        int n = transactions.size();
        int[] xs = new int[n];
        int[] ys = new int[n];

        int highlightIdx = 0;
        double maxTxAmount = 0.0;

        for (int i = 0; i < n; i++) {
            TransactionSummaryDto tx = transactions.get(i);
            double val = tx.amount() != null ? tx.amount().doubleValue() : 0.0;
            if (val >= maxTxAmount) {
                maxTxAmount = val;
                highlightIdx = i;
            }

            if (n == 1) {
                xs[i] = padLeft + chartW / 2;
            } else {
                xs[i] = padLeft + (int) ((double) i / (n - 1) * chartW);
            }
            double ratio = Math.min(1.0, val / maxVal);
            ys[i] = groundY - (int) (ratio * chartH);
        }

        // 3. Build Smooth Spline Curve Path
        GeneralPath curvePath = new GeneralPath();
        curvePath.moveTo(xs[0], ys[0]);

        if (n == 1) {
            curvePath.lineTo(xs[0] + 40, ys[0]);
        } else {
            for (int i = 0; i < n - 1; i++) {
                int x0 = (i > 0) ? xs[i - 1] : xs[i];
                int y0 = (i > 0) ? ys[i - 1] : ys[i];
                int x1 = xs[i];
                int y1 = ys[i];
                int x2 = xs[i + 1];
                int y2 = ys[i + 1];
                int x3 = (i + 2 < n) ? xs[i + 2] : x2;
                int y3 = (i + 2 < n) ? ys[i + 2] : y2;

                // Catmull-Rom to Cubic Bézier conversion
                double cp1x = x1 + (x2 - x0) / 6.0;
                double cp1y = y1 + (y2 - y0) / 6.0;
                double cp2x = x2 - (x3 - x1) / 6.0;
                double cp2y = y2 - (y3 - y1) / 6.0;

                curvePath.curveTo(cp1x, cp1y, cp2x, cp2y, x2, y2);
            }
        }

        // 4. Fill Translucent Gradient Under Curve (Finora Purple Glow)
        GeneralPath areaPath = new GeneralPath(curvePath);
        if (n == 1) {
            areaPath.lineTo(xs[0] + 40, groundY);
            areaPath.lineTo(xs[0], groundY);
        } else {
            areaPath.lineTo(xs[n - 1], groundY);
            areaPath.lineTo(xs[0], groundY);
        }
        areaPath.closePath();

        GradientPaint fillGrad = new GradientPaint(
                0, padTop, new Color(124, 58, 237, 70),
                0, groundY, new Color(243, 232, 255, 5)
        );
        g2.setPaint(fillGrad);
        g2.fill(areaPath);

        // 5. Draw Smooth Spline Line
        g2.setColor(COLOR_PURPLE_PRIMARY);
        g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(curvePath);

        // 6. Draw Data Points along the Curve
        for (int i = 0; i < n; i++) {
            TransactionSummaryDto tx = transactions.get(i);
            Color dotColor = COLOR_PURPLE_PRIMARY;
            String st = tx.status() != null ? tx.status().toUpperCase() : "";
            if ("APPROVED".equals(st)) {
                dotColor = COLOR_APPROVED;
            } else if ("BLOCKED".equals(st) || "FRAUD".equals(st)) {
                dotColor = COLOR_BLOCKED;
            } else if ("DECLINED".equals(st)) {
                dotColor = COLOR_DECLINED;
            }

            if (i != highlightIdx) {
                // Subtle white ring with status center
                g2.setColor(Color.WHITE);
                g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);
                g2.setColor(dotColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(xs[i] - 5, ys[i] - 5, 10, 10);
                g2.fillOval(xs[i] - 2, ys[i] - 2, 4, 4);
            }

            // X-axis Date/Time Label
            if (tx.transactionTime() != null) {
                String dateLabel = tx.transactionTime().length() >= 16
                        ? tx.transactionTime().substring(11, 16)
                        : tx.transactionTime();
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(COLOR_TEXT_LABEL);
                int lw = g2.getFontMetrics().stringWidth(dateLabel);
                g2.drawString(dateLabel, xs[i] - lw / 2, groundY + 18);
            }
        }

        // 7. Draw Highlighted Data Point with Halo Ring and Floating Dark Tooltip Card (Reference A)
        if (n > 0) {
            int hx = xs[highlightIdx];
            int hy = ys[highlightIdx];
            TransactionSummaryDto hTx = transactions.get(highlightIdx);

            // Glowing Outer Halo Ring
            g2.setColor(new Color(124, 58, 237, 45));
            g2.fillOval(hx - 12, hy - 12, 24, 24);

            // Inner White Ring
            g2.setColor(Color.WHITE);
            g2.fillOval(hx - 6, hy - 6, 12, 12);

            // Solid Purple Center
            g2.setColor(COLOR_PURPLE_PRIMARY);
            g2.fillOval(hx - 4, hy - 4, 8, 8);

            // Floating Dark Tooltip Card above highlighted point
            paintTooltipCard(g2, hx, hy, hTx);
        }

        // 8. X-axis Range Labels (July 24 ... August 28 style)
        if (n > 1) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(100, 116, 139));
            String firstDate = formatDateLabel(transactions.get(0).transactionTime());
            String lastDate = formatDateLabel(transactions.get(n - 1).transactionTime());
            g2.drawString(firstDate, padLeft, groundY + 34);
            int lastW = g2.getFontMetrics().stringWidth(lastDate);
            g2.drawString(lastDate, w - padRight - lastW, groundY + 34);
        }

        g2.dispose();
    }

    private void paintTooltipCard(Graphics2D g2, int hx, int hy, TransactionSummaryDto tx) {
        String line1 = (tx.transactionTime() != null && tx.transactionTime().length() >= 19)
                ? tx.transactionTime().substring(5, 19).replace("-", "/")
                : "Recent Transaction";
        String line2 = "₹" + String.format("%,.2f", tx.amount() != null ? tx.amount().doubleValue() : 0.0)
                + " " + (tx.status() != null ? tx.status() : "");

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int w1 = g2.getFontMetrics().stringWidth(line1);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        int w2 = g2.getFontMetrics().stringWidth(line2);

        int tipW = Math.max(w1, w2) + 20;
        int tipH = 40;
        int tipX = Math.max(10, Math.min(getWidth() - tipW - 10, hx - tipW / 2));
        int tipY = Math.max(6, hy - tipH - 12);

        // Tooltip Background Pill
        g2.setColor(COLOR_TOOLTIP_BG);
        g2.fillRoundRect(tipX, tipY, tipW, tipH, 8, 8);

        // Tooltip down arrow pointer
        Polygon arrow = new Polygon();
        arrow.addPoint(hx - 5, tipY + tipH);
        arrow.addPoint(hx + 5, tipY + tipH);
        arrow.addPoint(hx, tipY + tipH + 5);
        g2.fill(arrow);

        // Text lines
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        g2.drawString(line1, tipX + 10, tipY + 15);

        Color stColor = Color.WHITE;
        if ("APPROVED".equalsIgnoreCase(tx.status())) {
            stColor = new Color(74, 222, 128);
        } else if ("BLOCKED".equalsIgnoreCase(tx.status()) || "FRAUD".equalsIgnoreCase(tx.status())) {
            stColor = new Color(248, 113, 113);
        }
        g2.setColor(stColor);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString(line2, tipX + 10, tipY + 31);
    }

    private String formatDateLabel(String rawDate) {
        if (rawDate == null || rawDate.length() < 10) return "Recent";
        try {
            java.time.LocalDate ld = java.time.LocalDate.parse(rawDate.substring(0, 10));
            return ld.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d", java.util.Locale.ENGLISH));
        } catch (Exception e) {
            return rawDate.substring(0, 10);
        }
    }

    private void paintEmptyState(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(248, 245, 252));
        g2.fillRoundRect(20, 20, w - 40, h - 40, 14, 14);

        g2.setColor(new Color(230, 220, 240));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{4, 4}, 0));
        g2.drawRoundRect(20, 20, w - 40, h - 40, 14, 14);

        g2.setColor(COLOR_TEXT_LABEL);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String msg1 = "No Transaction Activity Recorded";
        int m1w = g2.getFontMetrics().stringWidth(msg1);
        g2.drawString(msg1, (w - m1w) / 2, h / 2 - 4);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        String msg2 = "Submit your first transaction to view real-time activity charts";
        int m2w = g2.getFontMetrics().stringWidth(msg2);
        g2.drawString(msg2, (w - m2w) / 2, h / 2 + 16);
    }
}
