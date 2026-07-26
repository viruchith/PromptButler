package com.viruchith.PromptButler.ui;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.logging.AppLogger;
import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Listens to {@link Stage#focusedProperty()}; when the stage loses focus, applies {@link AutoHideMode}
 * from {@link UserPreferences} (reduce opacity, minimize, hide to tray, or hide completely).
 */
public final class AutoHideController {
    public static final String SUSPEND_AUTO_HIDE_KEY = "promptButler.suspendAutoHide";

    private final Stage stage;
    private final UserPreferences preferences;
    private final Consumer<Boolean> trayVisibleSetter;

    public AutoHideController(Stage stage, UserPreferences preferences, Consumer<Boolean> trayVisibleSetter) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.trayVisibleSetter = trayVisibleSetter == null ? new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) {
            }
        } : trayVisibleSetter;
    }

    public void attach() {
        ChangeListener<Boolean> listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> obs, Boolean was, Boolean isNow) {
                if (Boolean.TRUE.equals(isNow)) {
                    stage.setOpacity(1.0);
                    if (stage.isIconified()) {
                        stage.setIconified(false);
                    }
                    return;
                }
                applyDefocus();
            }
        };
        stage.focusedProperty().addListener(listener);
    }

    private void applyDefocus() {
        if (Boolean.TRUE.equals(stage.getProperties().get(SUSPEND_AUTO_HIDE_KEY))) {
            return;
        }
        // Opening app-owned dialogs/windows (Settings, confirmations, variable form) shifts focus away
        // from the main stage; do not auto-hide in that case.
        if (isOwnedChildWindowFocused()) {
            return;
        }
        AutoHideMode mode = preferences.getAutoHideMode();
        switch (mode) {
            case OPACITY:
                stage.setOpacity(Math.max(0.01, preferences.getDefocusOpacity()));
                break;
            case MINIMIZE:
                stage.setIconified(true);
                break;
            case TRAY:
                if (SystemTray.isSupported()) {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            stage.hide();
                            trayVisibleSetter.accept(Boolean.TRUE);
                        }
                    });
                } else {
                    AppLogger.get().warn("System tray not supported; falling back to opacity auto-hide.");
                    stage.setOpacity(Math.max(0.01, preferences.getDefocusOpacity()));
                }
                break;
            case HIDE:
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        stage.hide();
                    }
                });
                break;
            default:
                break;
        }
    }

    private boolean isOwnedChildWindowFocused() {
        for (Window window : Window.getWindows()) {
            if (!(window instanceof Stage)) {
                continue;
            }
            Stage candidate = (Stage) window;
            if (candidate == stage || !candidate.isShowing() || !candidate.isFocused()) {
                continue;
            }
            if (isOwnedByMainStage(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOwnedByMainStage(Stage candidate) {
        Window owner = candidate.getOwner();
        while (owner != null) {
            if (owner == stage) {
                return true;
            }
            if (!(owner instanceof Stage)) {
                return false;
            }
            owner = ((Stage) owner).getOwner();
        }
        return false;
    }
}
