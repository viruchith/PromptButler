package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.logging.AppLogger;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Minimal AWT system tray integration for {@link com.viruchith.PromptButler.core.model.AutoHideMode#TRAY}.
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
            Image image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
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
}
