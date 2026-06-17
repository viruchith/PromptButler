package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.logging.AppLogger;
import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;

import java.awt.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Applies defocus auto-hide behavior based on {@link UserPreferences}.
 */
public final class AutoHideController {

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
}
