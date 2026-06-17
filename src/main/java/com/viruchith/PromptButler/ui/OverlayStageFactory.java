package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.model.BuildProfile;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

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
