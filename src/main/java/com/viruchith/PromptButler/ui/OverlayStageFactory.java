package com.viruchith.PromptButler.ui;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.model.BuildProfile;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

/**
 * Shared JavaFX {@link Stage} and {@link Scene} settings for the floating overlay: transparent style
 * (no native decorations), always-on-top, and transparent scene fill so rounded CSS panels show correctly.
 */
public final class OverlayStageFactory {

    private OverlayStageFactory() {
    }

    public static void applyOverlayChrome(Stage stage, BuildProfile profile) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(profile, "profile");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        if (profile.isDev()) {
            stage.setOpacity(1.0);
        }
    }

    public static void applySceneBackgroundTransparent(Scene scene) {
        scene.setFill(Color.TRANSPARENT);
    }
}
