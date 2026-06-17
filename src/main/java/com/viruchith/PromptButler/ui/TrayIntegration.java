package com.viruchith.PromptButler.ui;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.logging.AppLogger;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;

/**
 * Minimal AWT {@link SystemTray} integration: context menu to show the JavaFX {@link Stage} or exit.
 * Tray image is derived from {@code /appicon.png} when available.
 */
public final class TrayIntegration {

    private final Stage stage;
    private TrayIcon trayIcon;

    public TrayIntegration(Stage stage) {
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    public void install() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();
            if (trayIcon != null) {
                tray.remove(trayIcon);
            }
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem("Open Prompt Butler");
            open.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            stage.show();
                            stage.toFront();
                            stage.requestFocus();
                        }
                    });
                }
            });
            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Platform.exit();
                }
            });
            menu.add(open);
            menu.add(exit);
            Image image = loadTrayImageOrBlank();
            trayIcon = new TrayIcon(image, "Prompt Butler", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            stage.show();
                            stage.toFront();
                            stage.requestFocus();
                        }
                    });
                }
            });
            tray.add(trayIcon);
        } catch (Exception e) {
            AppLogger.get().error("Could not install system tray icon.", e);
        }
    }

    public void remove() {
        if (trayIcon == null) {
            return;
        }
        try {
            SystemTray.getSystemTray().remove(trayIcon);
        } catch (Exception ignored) {
        }
        trayIcon = null;
    }

    private static Image loadTrayImageOrBlank() {
        try (InputStream in = TrayIntegration.class.getResourceAsStream("/appicon.png")) {
            if (in == null) {
                return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            }
            int size = 22;
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 0, 0, size, size, null);
            } finally {
                g.dispose();
            }
            return out;
        } catch (Exception e) {
            AppLogger.get().warn("Could not load /appicon.png for tray; using blank icon.", e);
            return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        }
    }
}
