package com.viruchith.PromptButler;

// SPDX-License-Identifier: GPL-3.0-only
/*
 * Prompt Butler — JavaFX overlay for reusable prompts and clipboard workflows.
 * Copyright (C) 2026 Prompt Butler contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. See the LICENSE file.
 */

import com.viruchith.PromptButler.core.logging.AppLogger;
import com.viruchith.PromptButler.core.model.BuildProfile;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.JsonPromptRepository;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import com.viruchith.PromptButler.core.service.DataFileWatchService;
import com.viruchith.PromptButler.core.service.ImportExportService;
import com.viruchith.PromptButler.core.service.JsonSchemaValidator;
import com.viruchith.PromptButler.core.service.PreferencesRepository;
import com.viruchith.PromptButler.core.service.RecoveryService;
import com.viruchith.PromptButler.core.model.UserPreferences;
import com.viruchith.PromptButler.core.storage.SafePathResolver;
import com.viruchith.PromptButler.core.storage.StoragePaths;
import com.viruchith.PromptButler.os.JNativeHookHotkeyService;
import com.viruchith.PromptButler.ui.AutoHideController;
import com.viruchith.PromptButler.ui.MainView;
import com.viruchith.PromptButler.ui.MainViewModel;
import com.viruchith.PromptButler.ui.OverlayStageFactory;
import com.viruchith.PromptButler.ui.TrayIntegration;
import com.viruchith.PromptButler.ui.clipboard.JavaFxClipboardAdapter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * JavaFX {@link Application} entry point: resolves storage, loads or seeds prompts, builds the
 * transparent overlay {@link Stage}, wires global hotkey and optional tray, and registers auto-hide
 * behaviour from {@link com.viruchith.PromptButler.core.model.UserPreferences}.
 * <p>
 * Closing the window hides it ({@code setImplicitExit(false)}); exit is explicit via toolbar or tray.
 * </p>
 */
public final class PromptButlerApp extends Application {

    private MainView mainView;
    private JNativeHookHotkeyService hotkeyService;
    private TrayIntegration trayIntegration;
    private DataFileWatchService dataFileWatchService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        BuildProfile profile = BuildProfile.current();
        AppLogger.get().setVerbose(profile.isDev());
        try {
            startApplication(stage, profile);
        } catch (Throwable t) {
            AppLogger.get().error("Startup failed", t);
            try {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Prompt Butler");
                alert.setHeaderText("Could not start the application");
                String m = t.getMessage();
                alert.setContentText(m == null || m.isEmpty() ? t.toString() : m);
                alert.showAndWait();
            } catch (Throwable ignored) {
                AppLogger.get().error("Fatal startup error (alert failed)", t);
            }
            Platform.exit();
        }
    }

    /**
     * Wires persistence, UI, system integration (tray, hotkey, auto-hide), then shows the stage.
     * Heavy lifting is delegated to {@link MainView} / {@link MainViewModel}; this method only composes services.
     */
    private void startApplication(Stage stage, BuildProfile profile) throws Exception {
        Path dataDir = StoragePaths.resolveDataDirectory();
        SafePathResolver resolver;
        try {
            resolver = new SafePathResolver(dataDir);
        } catch (Exception e) {
            throw new IOException("Cannot use data directory: " + dataDir.toAbsolutePath(), e);
        }
        Path promptsFile;
        Path prefsFile;
        try {
            promptsFile = resolver.resolveChildFileName("prompts.json");
            prefsFile = resolver.resolveChildFileName("preferences.json");
        } catch (Exception e) {
            throw new IOException("Cannot resolve prompts or preferences path under " + dataDir.toAbsolutePath(), e);
        }

        JsonSchemaValidator validator = new JsonSchemaValidator();
        PromptRepository repository = new JsonPromptRepository(promptsFile, validator);
        RecoveryService recovery = new RecoveryService(validator);

        List<PromptTemplate> initial;
        try {
            initial = repository.loadAll();
            if (initial.isEmpty()) {
                initial = loadSeedTemplates(profile, validator);
                try {
                    repository.saveAll(initial);
                } catch (Exception saveEx) {
                    AppLogger.get().error("Could not save initial seed templates.", saveEx);
                    throw new IOException("Failed to write seed library to " + promptsFile.toAbsolutePath(), saveEx);
                }
            }
        } catch (Exception e) {
            AppLogger.get().warn("Could not load prompts store; attempting recovery.");
            try (InputStream in = PromptButlerApp.class.getResourceAsStream("/default-prompts.json")) {
                if (in == null) {
                    throw new IllegalStateException("Missing /default-prompts.json on classpath", e);
                }
                initial = recovery.loadWithRecovery(repository, promptsFile, in);
            } catch (Exception recoveryEx) {
                AppLogger.get().error("Recovery failed.", recoveryEx);
                throw new IOException("Could not load or recover prompts.json", recoveryEx);
            }
        }

        PreferencesRepository preferencesRepository = new PreferencesRepository();
        UserPreferences preferences = preferencesRepository.loadOrDefaults(prefsFile);

        ImportExportService importExportService = new ImportExportService(validator);
        MainViewModel viewModel = new MainViewModel(repository, initial);
        JavaFxClipboardAdapter clipboard = new JavaFxClipboardAdapter();

        OverlayStageFactory.applyOverlayChrome(stage, profile);
        // TRANSPARENT style has no native title bar; taskbar / OS may still use this string.
        stage.setTitle("Prompt Butler");
        loadApplicationIcon(stage);
        stage.setMinWidth(320);
        stage.setMinHeight(360);
        stage.setResizable(true);
        Consumer<UserPreferences> preferencesSaver = updated -> persistPreferencesQuietly(preferencesRepository, prefsFile, updated);
        Consumer<Boolean> themeSwitcher = dark -> {
            Scene existing = stage.getScene();
            if (existing != null) {
                applyTheme(existing, profile, dark.booleanValue());
            }
        };
        mainView = new MainView(stage, viewModel, clipboard, importExportService, preferences, preferencesSaver, themeSwitcher);
        mainView.getStyleClass().add("app-panel");
        StackPane shell = new StackPane();
        shell.getStyleClass().add("root");
        StackPane.setAlignment(mainView, Pos.TOP_LEFT);
        mainView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Region resizeGrip = createSouthEastResizeGrip(stage);
        shell.getChildren().addAll(mainView, resizeGrip);
        Scene scene = new Scene(shell, 420, 520);
        OverlayStageFactory.applySceneBackgroundTransparent(scene);
        applyTheme(scene, profile, preferences.isDarkMode());
        stage.setScene(scene);
        applyContentBasedMinimumSize(stage, mainView, shell);
        stage.setOnCloseRequest(e -> {
            e.consume();
            stage.hide();
        });

        mainView.attachGlobalKeys();

        try {
            trayIntegration = new TrayIntegration(stage);
            trayIntegration.install();
        } catch (Exception e) {
            AppLogger.get().warn("System tray could not be installed; use the window and hotkey only.", e);
            trayIntegration = null;
        }

        AutoHideController autoHide = new AutoHideController(stage, preferences, v -> {
        });
        try {
            autoHide.attach();
        } catch (Exception e) {
            AppLogger.get().warn("Auto-hide controller could not attach.", e);
        }

        stage.showingProperty().addListener((obs, was, showing) -> {
            if (Boolean.TRUE.equals(showing) && mainView != null) {
                mainView.focusSearch();
            }
        });

        hotkeyService = new JNativeHookHotkeyService(() -> toggleVisibility(stage));
        try {
            hotkeyService.start();
        } catch (Exception e) {
            AppLogger.get().error("Global hotkey registration failed; use window controls only.", e);
        }

        if (preferences.hasWindowBounds()) {
            double persistedW = Math.max(stage.getMinWidth(), preferences.getWindowWidth());
            double persistedH = Math.max(stage.getMinHeight(), preferences.getWindowHeight());
            if (isObviouslyBadPersistedBounds(preferences.getWindowX(), preferences.getWindowY(), persistedW, persistedH)) {
                AppLogger.get().warn("Ignoring invalid persisted window bounds; recentering window.");
                applyCenteredDefaultBounds(stage);
            } else {
                double[] xy = clampBoundsToScreens(preferences.getWindowX(), preferences.getWindowY(), persistedW, persistedH);
                stage.setX(xy[0]);
                stage.setY(xy[1]);
                stage.setWidth(persistedW);
                stage.setHeight(persistedH);
            }
        } else {
            applyCenteredDefaultBounds(stage);
        }
        stage.xProperty().addListener((obs, oldValue, newValue) -> {
            preferences.setWindowX(newValue.doubleValue());
            persistPreferencesQuietly(preferencesRepository, prefsFile, preferences);
        });
        stage.yProperty().addListener((obs, oldValue, newValue) -> {
            preferences.setWindowY(newValue.doubleValue());
            persistPreferencesQuietly(preferencesRepository, prefsFile, preferences);
        });
        stage.widthProperty().addListener((obs, oldValue, newValue) -> {
            preferences.setWindowWidth(newValue.doubleValue());
            persistPreferencesQuietly(preferencesRepository, prefsFile, preferences);
        });
        stage.heightProperty().addListener((obs, oldValue, newValue) -> {
            preferences.setWindowHeight(newValue.doubleValue());
            persistPreferencesQuietly(preferencesRepository, prefsFile, preferences);
        });

        dataFileWatchService = new DataFileWatchService(
                dataDir,
                promptsFile.getFileName().toString(),
                prefsFile.getFileName().toString(),
                () -> Platform.runLater(() -> {
                    try {
                        List<PromptTemplate> latest = repository.loadAll();
                        if (mainView != null) {
                            mainView.reloadTemplatesFromDisk(latest);
                        }
                    } catch (Exception e) {
                        AppLogger.get().warn("Could not reload prompts after filesystem change.", e);
                    }
                }),
                () -> Platform.runLater(() -> {
                    try {
                        UserPreferences latest = preferencesRepository.loadOrDefaults(prefsFile);
                        if (mainView != null) {
                            mainView.applyUpdatedPreferences(latest);
                        }
                    } catch (Exception e) {
                        AppLogger.get().warn("Could not reload preferences after filesystem change.", e);
                    }
                }));
        try {
            dataFileWatchService.start();
        } catch (Exception e) {
            AppLogger.get().warn("Data file watcher could not start.", e);
        }

        stage.show();
        mainView.focusSearch();
    }

    /** Hotkey callback (native thread): must hop to FX thread before touching {@link Stage}. */
    private void toggleVisibility(Stage stage) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (stage.isShowing() && stage.isFocused()) {
                    stage.hide();
                    return;
                }
                stage.show();
                stage.setOpacity(1.0);
                stage.toFront();
                stage.requestFocus();
                if (mainView != null) {
                    mainView.focusSearch();
                }
            }
        });
    }

    /** Undecorated stages need an explicit resize affordance; this region sits above the SE corner of the shell. */
    private static Region createSouthEastResizeGrip(Stage stage) {
        Region grip = new Region();
        grip.setPickOnBounds(true);
        grip.setPrefSize(14, 14);
        grip.setMinSize(14, 14);
        grip.setMaxSize(14, 14);
        StackPane.setAlignment(grip, Pos.BOTTOM_RIGHT);
        grip.setCursor(Cursor.SE_RESIZE);
        grip.getStyleClass().add("resize-grip");
        final double[] start = new double[4];
        grip.setOnMousePressed(e -> {
            start[0] = e.getScreenX();
            start[1] = e.getScreenY();
            start[2] = stage.getWidth();
            start[3] = stage.getHeight();
        });
        grip.setOnMouseDragged(e -> {
            double nw = Math.max(stage.getMinWidth(), start[2] + e.getScreenX() - start[0]);
            double nh = Math.max(stage.getMinHeight(), start[3] + e.getScreenY() - start[1]);
            stage.setWidth(nw);
            stage.setHeight(nh);
        });
        return grip;
    }

    private static void applyContentBasedMinimumSize(Stage stage, MainView view, StackPane shell) {
        shell.applyCss();
        shell.layout();
        double minWidth = Math.max(stage.getMinWidth(), Math.ceil(view.prefWidth(-1) + 24.0));
        double minHeight = Math.max(stage.getMinHeight(), Math.ceil(view.prefHeight(minWidth) + 24.0));
        stage.setMinWidth(minWidth);
        stage.setMinHeight(minHeight);
    }

    /** Taskbar / alt-tab icon; separate from in-scene title bar {@link com.viruchith.PromptButler.ui.MainView} icon. */
    private static void loadApplicationIcon(Stage stage) {
        try (InputStream in = PromptButlerApp.class.getResourceAsStream("/appicon.png")) {
            if (in == null) {
                AppLogger.get().warn("Missing classpath resource /appicon.png; window uses default icon.");
                return;
            }
            stage.getIcons().add(new Image(in));
        } catch (Exception e) {
            AppLogger.get().warn("Could not load application icon from /appicon.png.", e);
        }
    }

    private static List<PromptTemplate> loadSeedTemplates(BuildProfile profile, JsonSchemaValidator validator)
            throws IOException {
        String resource = profile.isDev() ? "/dev-prompts.json" : "/default-prompts.json";
        InputStream in = PromptButlerApp.class.getResourceAsStream(resource);
        if (in == null) {
            in = PromptButlerApp.class.getResourceAsStream("/default-prompts.json");
        }
        if (in == null) {
            throw new IOException("Missing default prompt templates on classpath");
        }
        try (InputStream input = in) {
            return JsonPromptRepository.parseValidatedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8), validator);
        }
    }

    @Override
    public void stop() {
        if (dataFileWatchService != null) {
            dataFileWatchService.stop();
        }
        if (hotkeyService != null) {
            hotkeyService.stop();
        }
        if (trayIntegration != null) {
            trayIntegration.remove();
        }
    }

    private void applyTheme(Scene scene, BuildProfile profile, boolean darkMode) {
        scene.getStylesheets().clear();
        String stylesheet = darkMode ? "/styles/overlay-dark.css" : (profile.isDev() ? "/styles/overlay-dev.css" : "/styles/overlay.css");
        java.net.URL css = getClass().getResource(stylesheet);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
    }

    private static void persistPreferencesQuietly(PreferencesRepository repository, Path prefsFile, UserPreferences preferences) {
        try {
            repository.save(prefsFile, preferences);
        } catch (Exception e) {
            AppLogger.get().warn("Could not persist preferences: " + e.getMessage());
        }
    }

    /**
     * Clamps {@code x, y} so that at least {@code MIN_VISIBLE_PX} pixels of the window
     * remain visible within the union of all connected screens.
     * Returns {@code [clampedX, clampedY]}.
     */
    private static final double MIN_VISIBLE_PX = 80.0;
    private static final double EXTREME_BOUNDS_FACTOR = 10.0;

    private static void applyCenteredDefaultBounds(Stage stage) {
        double w = Math.max(stage.getWidth(), stage.getMinWidth());
        double h = Math.max(stage.getHeight(), stage.getMinHeight());
        stage.setWidth(w);
        stage.setHeight(h);

        // First launch/fallback: position explicitly instead of relying on platform defaults.
        javafx.geometry.Rectangle2D primaryBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        double centeredX = primaryBounds.getMinX() + Math.max(0.0, (primaryBounds.getWidth() - w) / 2.0);
        double centeredY = primaryBounds.getMinY() + Math.max(0.0, (primaryBounds.getHeight() - h) / 2.0);
        double[] xy = clampBoundsToScreens(centeredX, centeredY, w, h);
        stage.setX(xy[0]);
        stage.setY(xy[1]);
    }

    private static boolean isObviouslyBadPersistedBounds(double x, double y, double w, double h) {
        if (!areFiniteAndPositiveBounds(x, y, w, h)) {
            return true;
        }

        double unionMinX = Double.MAX_VALUE;
        double unionMinY = Double.MAX_VALUE;
        double unionMaxX = -Double.MAX_VALUE;
        double unionMaxY = -Double.MAX_VALUE;
        for (javafx.stage.Screen screen : javafx.stage.Screen.getScreens()) {
            javafx.geometry.Rectangle2D vb = screen.getVisualBounds();
            unionMinX = Math.min(unionMinX, vb.getMinX());
            unionMinY = Math.min(unionMinY, vb.getMinY());
            unionMaxX = Math.max(unionMaxX, vb.getMaxX());
            unionMaxY = Math.max(unionMaxY, vb.getMaxY());
        }
        if (unionMinX >= unionMaxX || unionMinY >= unionMaxY) {
            return false;
        }

        return areBoundsExtremeForUnion(x, y, w, h, unionMinX, unionMinY, unionMaxX, unionMaxY);
    }

    private static boolean areFiniteAndPositiveBounds(double x, double y, double w, double h) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(w) || !Double.isFinite(h)) {
            return false;
        }
        return w > 0.0 && h > 0.0;
    }

    private static boolean areBoundsExtremeForUnion(
            double x,
            double y,
            double w,
            double h,
            double unionMinX,
            double unionMinY,
            double unionMaxX,
            double unionMaxY) {
        double unionWidth = unionMaxX - unionMinX;
        double unionHeight = unionMaxY - unionMinY;
        if (w > unionWidth * EXTREME_BOUNDS_FACTOR || h > unionHeight * EXTREME_BOUNDS_FACTOR) {
            return true;
        }

        double minAllowedX = unionMinX - (unionWidth * EXTREME_BOUNDS_FACTOR);
        double maxAllowedX = unionMaxX + (unionWidth * EXTREME_BOUNDS_FACTOR);
        double minAllowedY = unionMinY - (unionHeight * EXTREME_BOUNDS_FACTOR);
        double maxAllowedY = unionMaxY + (unionHeight * EXTREME_BOUNDS_FACTOR);
        return x < minAllowedX || x > maxAllowedX || y < minAllowedY || y > maxAllowedY;
    }

    private static double[] clampBoundsToScreens(double x, double y, double w, double h) {
        double unionMinX = Double.MAX_VALUE;
        double unionMinY = Double.MAX_VALUE;
        double unionMaxX = -Double.MAX_VALUE;
        double unionMaxY = -Double.MAX_VALUE;
        for (javafx.stage.Screen screen : javafx.stage.Screen.getScreens()) {
            javafx.geometry.Rectangle2D vb = screen.getVisualBounds();
            unionMinX = Math.min(unionMinX, vb.getMinX());
            unionMinY = Math.min(unionMinY, vb.getMinY());
            unionMaxX = Math.max(unionMaxX, vb.getMaxX());
            unionMaxY = Math.max(unionMaxY, vb.getMaxY());
        }
        if (unionMinX >= unionMaxX || unionMinY >= unionMaxY) {
            // No screens detected — return unchanged.
            return new double[]{x, y};
        }
        // Ensure at least MIN_VISIBLE_PX of the window is within screen bounds on each axis.
        double clampedX = Math.min(Math.max(x, unionMinX - w + MIN_VISIBLE_PX), unionMaxX - MIN_VISIBLE_PX);
        double clampedY = Math.min(Math.max(y, unionMinY - h + MIN_VISIBLE_PX), unionMaxY - MIN_VISIBLE_PX);
        return new double[]{clampedX, clampedY};
    }
}
