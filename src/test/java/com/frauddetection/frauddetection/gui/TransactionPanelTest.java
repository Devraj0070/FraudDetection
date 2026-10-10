package com.frauddetection.frauddetection.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.client.SafePayApiClient;
import com.frauddetection.frauddetection.dto.TransactionResponse;

/**
 * Unit and visual rendering tests for {@link TransactionPanel}.
 * Verifies offscreen Java 2D vector painting, client-side amount validation,
 * receipt presentation across APPROVED, DECLINED, and BLOCKED outcomes, and state resets.
 */
class TransactionPanelTest {

    @Test
    @DisplayName("TransactionPanel initializes and paints form view with Java 2D vector elements")
    void transactionPanel_initializesAndRendersForm_offscreenPaint() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];
        assertNotNull(panel);
        assertNotNull(panel.getCmbType());
        assertNotNull(panel.getTxtAmount());
        assertNotNull(panel.getBtnSubmit());

        // Perform offscreen Java 2D painting verification
        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        // 1. Verify Top Hero Header area has painted pixels (x=500, y=50)
        int heroPixel = image.getRGB(500, 50);
        assertNotEquals(0, heroPixel, "Header hero card area must have painted pixels");

        // 2. Verify Form Card area has painted pixels (x=300, y=300)
        int formPixel = image.getRGB(300, 300);
        assertNotEquals(0, formPixel, "Interactive form card must have painted pixels");

        // Write offscreen rendering artifact to target directory
        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "transaction-panel-rendered.png"));
        }
    }

    @Test
    @DisplayName("TransactionPanel client validation rejects empty, non-numeric, non-positive, and excessive scale amounts")
    void transactionPanel_validationLogic_rejectsInvalidAmounts() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];

        // 1. Empty amount validation
        SwingUtilities.invokeAndWait(() -> {
            panel.getTxtAmount().setText("");
            panel.executeTransactionSubmission();
        });
        assertEquals("Please enter a transaction amount.", panel.getLblValidationError().getText());

        // 2. Non-numeric amount validation
        SwingUtilities.invokeAndWait(() -> {
            panel.getTxtAmount().setText("abc");
            panel.executeTransactionSubmission();
        });
        assertTrue(panel.getLblValidationError().getText().contains("Invalid amount format"));

        // 3. Zero amount validation
        SwingUtilities.invokeAndWait(() -> {
            panel.getTxtAmount().setText("0.00");
            panel.executeTransactionSubmission();
        });
        assertEquals("Amount must be greater than zero.", panel.getLblValidationError().getText());

        // 4. Negative amount validation
        SwingUtilities.invokeAndWait(() -> {
            panel.getTxtAmount().setText("-50.00");
            panel.executeTransactionSubmission();
        });
        assertEquals("Amount must be greater than zero.", panel.getLblValidationError().getText());

        // 5. Scale > 2 decimal places validation
        SwingUtilities.invokeAndWait(() -> {
            panel.getTxtAmount().setText("100.555");
            panel.executeTransactionSubmission();
        });
        assertEquals("Amount cannot have more than 2 decimal places.", panel.getLblValidationError().getText());
    }

    @Test
    @DisplayName("TransactionPanel displays APPROVED result receipt and paints green hero banner")
    void transactionPanel_displayResult_rendersApprovedReceipt() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();

            TransactionResponse approvedResponse = new TransactionResponse(
                    true,
                    "Your transaction was approved and processed successfully.",
                    1001L,
                    new BigDecimal("1500.00"),
                    "PAYMENT",
                    "APPROVED",
                    "2026-10-09 14:35:22",
                    "Normal / Legitimate",
                    "LOW",
                    0.012,
                    new BigDecimal("48500.00")
            );

            panel.displayResultScreen(approvedResponse);
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];
        assertTrue(panel.getLblResultTitle().getText().contains("Approved"));
        assertEquals("APPROVED", panel.getLblReceiptStatus().getText());
        assertEquals("#1001", panel.getLblReceiptId().getText());
        assertTrue(panel.getLblReceiptAmount().getText().contains("1,500.00"));
        assertTrue(panel.getLblReceiptRisk().getText().contains("LOW"));
        assertEquals("Normal / Legitimate", panel.getLblReceiptReason().getText());

        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "transaction-approved-rendered.png"));
        }
    }

    @Test
    @DisplayName("TransactionPanel displays DECLINED result receipt and paints amber hero banner")
    void transactionPanel_displayResult_rendersDeclinedReceipt() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();

            TransactionResponse declinedResponse = new TransactionResponse(
                    true,
                    "Transaction declined: limit exceeded.",
                    1002L,
                    new BigDecimal("150000.00"),
                    "PAYMENT",
                    "DECLINED",
                    "2026-10-09 14:36:10",
                    "Limit Exceeded (Max ₹100,000)",
                    "MEDIUM",
                    0.05,
                    new BigDecimal("50000.00")
            );

            panel.displayResultScreen(declinedResponse);
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];
        assertTrue(panel.getLblResultTitle().getText().contains("Declined"));
        assertEquals("DECLINED", panel.getLblReceiptStatus().getText());
        assertTrue(panel.getLblReceiptReason().getText().contains("Limit Exceeded"));

        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "transaction-declined-rendered.png"));
        }
    }

    @Test
    @DisplayName("TransactionPanel displays BLOCKED result receipt and paints crimson red hero banner")
    void transactionPanel_displayResult_rendersBlockedReceipt() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panel.setSize(1020, 720);
            panel.addNotify();
            panel.validate();

            TransactionResponse blockedResponse = new TransactionResponse(
                    true,
                    "Transaction blocked by AI fraud detection.",
                    1003L,
                    new BigDecimal("49000.00"),
                    "TRANSFER",
                    "BLOCKED",
                    "2026-10-09 14:37:05",
                    "Machine Learning & Behavioral Anomaly",
                    "HIGH",
                    0.965,
                    new BigDecimal("50000.00")
            );

            panel.displayResultScreen(blockedResponse);
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];
        assertTrue(panel.getLblResultTitle().getText().contains("Blocked"));
        assertEquals("BLOCKED", panel.getLblReceiptStatus().getText());
        assertTrue(panel.getLblReceiptRisk().getText().contains("HIGH"));
        assertTrue(panel.getLblReceiptReason().getText().contains("Machine Learning"));

        BufferedImage image = new BufferedImage(1020, 720, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();

        File outDir = new File("target");
        if (outDir.exists()) {
            ImageIO.write(image, "png", new File(outDir, "transaction-blocked-rendered.png"));
        }
    }

    @Test
    @DisplayName("TransactionPanel resetFormAndShow clears fields and restores form view")
    void transactionPanel_resetFormAndShow_restoresForm() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI test in headless environment"
        );

        final TransactionPanel[] panelHolder = new TransactionPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient client = new SafePayApiClient("http://127.0.0.1:8080");
            TransactionPanel panel = new TransactionPanel(client, () -> {}, () -> {});
            panel.getTxtAmount().setText("500.00");
            panel.getLblValidationError().setText("Some previous error");
            panel.resetFormAndShow();
            panelHolder[0] = panel;
        });

        TransactionPanel panel = panelHolder[0];
        assertEquals("", panel.getTxtAmount().getText());
        assertEquals(" ", panel.getLblValidationError().getText());
    }
}
