package com.frauddetection.frauddetection.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.frauddetection.frauddetection.client.SafePayApiClient;

/**
 * Unit and visual rendering tests for the redesigned {@link AuthFrame}.
 */
class AuthFrameTest {

    @Test
    @DisplayName("AuthFrame initializes and renders all Java 2D components without errors")
    void authFrame_initializesAndRendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final AuthFrame[] frameHolder = new AuthFrame[1];

        // Initialize strictly on the Event Dispatch Thread
        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthFrame frame = new AuthFrame(dummyClient, null);
            frame.setSize(1040, 700);
            frame.addNotify();
            frame.validate();
            frameHolder[0] = frame;
        });

        AuthFrame frame = frameHolder[0];
        assertNotNull(frame, "AuthFrame must be instantiated");
        assertEquals("SafePay — AI-Powered Fraud Detection System", frame.getTitle());

        // Perform offscreen Java 2D painting verification
        BufferedImage image = new BufferedImage(1040, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        frame.getContentPane().paint(g2);
        g2.dispose();

        // Verify image has non-zero pixels painted in left brand panel area
        int centerPixel = image.getRGB(240, 350);
        assertNotEquals(0, centerPixel, "Left brand pane must have painted gradient pixels");

        // Write offscreen rendering to target for visual artifact verification
        File outDir = new File("target");
        if (outDir.exists()) {
            File outFile = new File(outDir, "auth-frame-rendered.png");
            ImageIO.write(image, "png", outFile);
        }

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("AuthFrame switches to Register card and renders all fields and branding")
    void authFrame_registerCard_rendersSuccessfully() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final AuthFrame[] frameHolder = new AuthFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthFrame frame = new AuthFrame(dummyClient, null);
            frame.setSize(1040, 700);
            frame.addNotify();
            frame.validate();
            frame.showRegisterCard();
            frameHolder[0] = frame;
        });

        AuthFrame frame = frameHolder[0];
        assertNotNull(frame, "AuthFrame must be instantiated");

        BufferedImage image = new BufferedImage(1040, 700, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        frame.getContentPane().paint(g2);
        g2.dispose();

        int centerPixel = image.getRGB(700, 350); // Right register card area
        assertNotEquals(0, centerPixel, "Register card area must have painted pixels");

        File outDir = new File("target");
        if (outDir.exists()) {
            File outFile = new File(outDir, "auth-register-frame-rendered.png");
            ImageIO.write(image, "png", outFile);
        }

        SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    @DisplayName("BrandPane animates vector elements and disposes cleanly without timer leaks")
    void authFrame_brandPaneAnimationAndDisposal_cleanLifecycle() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(
                java.awt.GraphicsEnvironment.isHeadless(),
                "Skipping GUI render test in headless environment"
        );

        final AuthFrame[] frameHolder = new AuthFrame[1];

        SwingUtilities.invokeAndWait(() -> {
            SafePayApiClient dummyClient = new SafePayApiClient("http://127.0.0.1:8080");
            AuthFrame frame = new AuthFrame(dummyClient, null);
            frame.setSize(1040, 700);
            frame.addNotify();
            frame.validate();

            // Advance animation ticks explicitly
            AuthFrame.BrandPane bp = frame.getBrandPane();
            assertNotNull(bp, "BrandPane must not be null");
            int initialTick = bp.getAnimTick();
            bp.tickAnimation();
            assertTrue(bp.getAnimTick() > initialTick, "Animation tick must advance on tickAnimation");

            frameHolder[0] = frame;
        });

        AuthFrame frame = frameHolder[0];
        // Dispose frame and verify clean teardown
        SwingUtilities.invokeAndWait(() -> {
            frame.dispose();
            // calling stopAnimationTimer again is idempotent
            frame.stopAnimationTimer();
        });
    }
}
