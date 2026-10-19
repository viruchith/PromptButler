package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.PromptButlerApp;
import com.viruchith.PromptButler.core.clipboard.ClipboardPort;
import com.viruchith.PromptButler.core.model.BuildProfile;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.model.UserPreferences;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import com.viruchith.PromptButler.core.service.ImportExportService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WindowSizingTest {

    @BeforeAll
    public static void initJfx() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testApplyCenteredDefaultBoundsHandlesUnshownStage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                OverlayStageFactory.applyOverlayChrome(stage, BuildProfile.PROD);
                stage.setMinWidth(PromptButlerApp.MIN_WINDOW_WIDTH);
                stage.setMinHeight(PromptButlerApp.MIN_WINDOW_HEIGHT);

                StackPane root = new StackPane();
                Scene scene = new Scene(root, PromptButlerApp.DEFAULT_WINDOW_WIDTH, PromptButlerApp.DEFAULT_WINDOW_HEIGHT);
                stage.setScene(scene);

                // Before stage.show(), stage.getWidth() is Double.NaN.
                Assertions.assertTrue(Double.isNaN(stage.getWidth()));
                Assertions.assertTrue(Double.isNaN(stage.getHeight()));

                PromptButlerApp.applyCenteredDefaultBounds(stage);

                Assertions.assertEquals(PromptButlerApp.DEFAULT_WINDOW_WIDTH, stage.getWidth(), 0.001);
                Assertions.assertEquals(PromptButlerApp.DEFAULT_WINDOW_HEIGHT, stage.getHeight(), 0.001);
                Assertions.assertTrue(Double.isFinite(stage.getX()));
                Assertions.assertTrue(Double.isFinite(stage.getY()));
                Assertions.assertFalse(Double.isNaN(stage.getX()));
                Assertions.assertFalse(Double.isNaN(stage.getY()));
            } catch (Throwable t) {
                t.printStackTrace();
                Assertions.fail(t.getMessage());
            } finally {
                latch.countDown();
            }
        });
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testInflatedBugFootprintDetection() {
        Assertions.assertTrue(PromptButlerApp.isBugInflatedBounds(787.0, 582.0));
        Assertions.assertTrue(PromptButlerApp.isBugInflatedBounds(786.5, 582.4));
        Assertions.assertFalse(PromptButlerApp.isBugInflatedBounds(420.0, 520.0));
        Assertions.assertFalse(PromptButlerApp.isBugInflatedBounds(600.0, 700.0));
    }

    @Test
    public void testFixedSizingStabilityAcrossRestarts() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                PromptRepository repo = Mockito.mock(PromptRepository.class);
                java.util.List<PromptTemplate> templates = Collections.emptyList();
                ClipboardPort clipboard = Mockito.mock(ClipboardPort.class);
                ImportExportService ies = new ImportExportService(new com.viruchith.PromptButler.core.service.JsonSchemaValidator());
                UserPreferences prefs = new UserPreferences();

                // Run 1: First Launch (no window bounds persisted)
                Stage stage1 = new Stage();
                OverlayStageFactory.applyOverlayChrome(stage1, BuildProfile.PROD);
                stage1.setMinWidth(PromptButlerApp.MIN_WINDOW_WIDTH);
                stage1.setMinHeight(PromptButlerApp.MIN_WINDOW_HEIGHT);

                MainViewModel vm1 = new MainViewModel(repo, templates);
                MainView view1 = new MainView(stage1, vm1, clipboard, ies, prefs, p -> {}, b -> {});
                view1.getStyleClass().add("app-panel");

                StackPane shell1 = new StackPane();
                shell1.getStyleClass().add("root");
                StackPane.setAlignment(view1, Pos.TOP_LEFT);
                view1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                shell1.getChildren().add(view1);

                Scene scene1 = new Scene(shell1, PromptButlerApp.DEFAULT_WINDOW_WIDTH, PromptButlerApp.DEFAULT_WINDOW_HEIGHT);
                OverlayStageFactory.applySceneBackgroundTransparent(scene1);
                stage1.setScene(scene1);

                PromptButlerApp.applyCenteredDefaultBounds(stage1);
                stage1.show();

                Assertions.assertEquals(420.0, stage1.getWidth(), 1.0);
                Assertions.assertEquals(520.0, stage1.getHeight(), 1.0);

                // Save bounds
                prefs.setWindowX(stage1.getX());
                prefs.setWindowY(stage1.getY());
                prefs.setWindowWidth(stage1.getWidth());
                prefs.setWindowHeight(stage1.getHeight());
                stage1.hide();

                // Run 2: Second launch (with saved bounds)
                Stage stage2 = new Stage();
                OverlayStageFactory.applyOverlayChrome(stage2, BuildProfile.PROD);
                stage2.setMinWidth(PromptButlerApp.MIN_WINDOW_WIDTH);
                stage2.setMinHeight(PromptButlerApp.MIN_WINDOW_HEIGHT);

                MainViewModel vm2 = new MainViewModel(repo, templates);
                MainView view2 = new MainView(stage2, vm2, clipboard, ies, prefs, p -> {}, b -> {});
                view2.getStyleClass().add("app-panel");

                StackPane shell2 = new StackPane();
                shell2.getStyleClass().add("root");
                StackPane.setAlignment(view2, Pos.TOP_LEFT);
                view2.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                shell2.getChildren().add(view2);

                Scene scene2 = new Scene(shell2, PromptButlerApp.DEFAULT_WINDOW_WIDTH, PromptButlerApp.DEFAULT_WINDOW_HEIGHT);
                OverlayStageFactory.applySceneBackgroundTransparent(scene2);
                stage2.setScene(scene2);

                double persistedW = prefs.getWindowWidth();
                double persistedH = prefs.getWindowHeight();
                if (PromptButlerApp.isBugInflatedBounds(persistedW, persistedH)) {
                    PromptButlerApp.applyCenteredDefaultBounds(stage2);
                } else {
                    persistedW = Math.max(stage2.getMinWidth(), persistedW);
                    persistedH = Math.max(stage2.getMinHeight(), persistedH);
                    stage2.setWidth(persistedW);
                    stage2.setHeight(persistedH);
                }
                stage2.show();

                // Sizing must NOT grow
                Assertions.assertEquals(420.0, stage2.getWidth(), 1.0);
                Assertions.assertEquals(520.0, stage2.getHeight(), 1.0);
                stage2.hide();

                // Run 3: Simulate recovery from previously bug-inflated preferences (787x582)
                prefs.setWindowWidth(787.0);
                prefs.setWindowHeight(582.0);

                Stage stage3 = new Stage();
                OverlayStageFactory.applyOverlayChrome(stage3, BuildProfile.PROD);
                stage3.setMinWidth(PromptButlerApp.MIN_WINDOW_WIDTH);
                stage3.setMinHeight(PromptButlerApp.MIN_WINDOW_HEIGHT);

                MainViewModel vm3 = new MainViewModel(repo, templates);
                MainView view3 = new MainView(stage3, vm3, clipboard, ies, prefs, p -> {}, b -> {});
                view3.getStyleClass().add("app-panel");

                StackPane shell3 = new StackPane();
                shell3.getStyleClass().add("root");
                shell3.getChildren().add(view3);

                Scene scene3 = new Scene(shell3, PromptButlerApp.DEFAULT_WINDOW_WIDTH, PromptButlerApp.DEFAULT_WINDOW_HEIGHT);
                OverlayStageFactory.applySceneBackgroundTransparent(scene3);
                stage3.setScene(scene3);

                double persistedW3 = prefs.getWindowWidth();
                double persistedH3 = prefs.getWindowHeight();
                if (PromptButlerApp.isBugInflatedBounds(persistedW3, persistedH3)) {
                    PromptButlerApp.applyCenteredDefaultBounds(stage3);
                } else {
                    persistedW3 = Math.max(stage3.getMinWidth(), persistedW3);
                    persistedH3 = Math.max(stage3.getMinHeight(), persistedH3);
                    stage3.setWidth(persistedW3);
                    stage3.setHeight(persistedH3);
                }
                stage3.show();

                // Must reset to clean 420x520 default bounds
                Assertions.assertEquals(420.0, stage3.getWidth(), 1.0);
                Assertions.assertEquals(520.0, stage3.getHeight(), 1.0);
                stage3.hide();

            } catch (Throwable t) {
                t.printStackTrace();
                Assertions.fail(t.getMessage());
            } finally {
                latch.countDown();
            }
        });
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
