package com.viruchith.PromptButler;

import com.viruchith.PromptButler.core.logging.AppLogger;
import com.viruchith.PromptButler.core.model.BuildProfile;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.JsonPromptRepository;
import com.viruchith.PromptButler.core.repository.PromptRepository;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public final class PromptButlerApp extends Application {

    private MainView mainView;
    private JNativeHookHotkeyService hotkeyService;
    private TrayIntegration trayIntegration;

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
                t.printStackTrace();
            }
            Platform.exit();
        }
    }

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
        stage.setMinWidth(320);
        stage.setMinHeight(360);
        stage.setResizable(true);
        mainView = new MainView(stage, viewModel, clipboard, importExportService);
        mainView.getStyleClass().add("app-panel");
        StackPane shell = new StackPane();
        shell.getStyleClass().add("root");
        StackPane.setAlignment(mainView, Pos.TOP_LEFT);
        mainView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        Region resizeGrip = createSouthEastResizeGrip(stage, 320, 360);
        shell.getChildren().addAll(mainView, resizeGrip);
        Scene scene = new Scene(shell, 420, 520);
        OverlayStageFactory.applySceneBackgroundTransparent(scene);
        java.net.URL css = getClass().getResource(profile.isDev() ? "/styles/overlay-dev.css" : "/styles/overlay.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setTitle("Prompt Butler");
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

        stage.show();
        mainView.focusSearch();
    }

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

    private static Region createSouthEastResizeGrip(Stage stage, double minWidth, double minHeight) {
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
            double nw = Math.max(minWidth, start[2] + e.getScreenX() - start[0]);
            double nh = Math.max(minHeight, start[3] + e.getScreenY() - start[1]);
            stage.setWidth(nw);
            stage.setHeight(nh);
        });
        return grip;
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
        if (hotkeyService != null) {
            hotkeyService.stop();
        }
        if (trayIntegration != null) {
            trayIntegration.remove();
        }
    }
}
