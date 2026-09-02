package com.viruchith.PromptButler.ui;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.clipboard.ClipboardPort;
import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.model.UserPreferences;
import com.viruchith.PromptButler.core.service.ImportExportService;
import com.viruchith.PromptButler.core.storage.StoragePaths;
import com.viruchith.PromptButler.core.util.InputText;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Callback;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Primary JavaFX UI for the overlay: search, template list, toolbar, and auxiliary {@link Stage}s
 * (read-only detail, modeless variable fill-in).
 * <p>
 * Responsibilities include:
 * </p>
 * <ul>
 *   <li><b>Title strip</b> — app icon, “Prompt Butler” label, full-width drag handle (no OS title bar under
 *       {@link javafx.stage.StageStyle#TRANSPARENT}), help ({@code ?}) opening an About window</li>
 *   <li><b>List</b> — fuzzy-filtered items from {@link MainViewModel#getFilteredList()}; single-click detail timer;
 *       double-click / Enter runs {@link #onTemplateChosen(com.viruchith.PromptButler.core.model.PromptTemplate)}</li>
 *   <li><b>Variables</b> — separate modeless window; copy paths use {@link #copyPlainTextThenMaybeHide(String, boolean)}
 *       so “Copy &amp; close” does not hide the main stage</li>
 *   <li><b>Clipboard</b> — {@link ClipboardPort}; {@link #hideOverlay()} clears retained adapter state</li>
 *   <li><b>Import/export / data folder</b> — file and directory choosers; restart notices for pointer changes</li>
 * </ul>
 */
public final class MainView extends VBox {

    /** Base light theme (dialogs do not inherit the main scene stylesheet). */
    private static final String LIGHT_STYLESHEET_URL = stylesheetUrl("/styles/overlay.css");
    private static final String DARK_STYLESHEET_URL = stylesheetUrl("/styles/overlay-dark.css");

    private final Stage stage;
    private final MainViewModel viewModel;
    private final ClipboardPort clipboard;
    private final ImportExportService importExportService;
    private final UserPreferences preferences;
    private final Consumer<UserPreferences> preferencesSaver;
    private final Consumer<Boolean> themeSwitcher;

    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<String>();
    private final ListView<PromptTemplate> listView = new ListView<PromptTemplate>();
    private final VBox listSection = new VBox(6);
    private final HBox toolbar = new HBox(8);
    private final Label statusLabel = new Label("");
    private final HBox titleBar = new HBox(8);

    private PauseTransition singleClickDetailTimer;
    private PromptTemplate pendingSingleClickTemplate;
    private Stage promptDetailStage;
    /** Modeless window for {{variable}} fill-in; user can keep using the main window. */
    private Stage variableParamsStage;
    private boolean categoryFilterUpdating;

    /** About / help window (single instance while open). */
    private Stage aboutStage;
    private PromptTemplate lastDeletedTemplate;
    private Button undoDeleteButton;

    private PromptTemplate variableTarget;
    private final List<TextArea> variableFields = new ArrayList<TextArea>();

    public MainView(
            Stage stage,
            MainViewModel viewModel,
            ClipboardPort clipboard,
            ImportExportService importExportService,
            UserPreferences preferences,
            Consumer<UserPreferences> preferencesSaver,
            Consumer<Boolean> themeSwitcher) {
        super(10);
        this.stage = Objects.requireNonNull(stage, "stage");
        this.viewModel = Objects.requireNonNull(viewModel, "viewModel");
        this.clipboard = Objects.requireNonNull(clipboard, "clipboard");
        this.importExportService = Objects.requireNonNull(importExportService, "importExportService");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.preferencesSaver = Objects.requireNonNull(preferencesSaver, "preferencesSaver");
        this.themeSwitcher = Objects.requireNonNull(themeSwitcher, "themeSwitcher");
        configureSearch();
        configureList();
        configureToolbar();
        Label appTitle = new Label("Prompt Butler");
        appTitle.getStyleClass().add("window-app-title");
        titleBar.getStyleClass().add("window-drag-region");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        ImageView titleIcon = createTitleBarIcon();
        Region titleDragSpacer = new Region();
        HBox.setHgrow(titleDragSpacer, Priority.ALWAYS);
        Button helpAbout = new Button();
        helpAbout.setGraphic(UiIcons.solid(FontAwesomeSolid.QUESTION_CIRCLE));
        helpAbout.getStyleClass().addAll("icon-toolbar-button", "title-bar-help-button", "title-bar-no-drag");
        helpAbout.setFocusTraversable(false);
        helpAbout.setCursor(Cursor.DEFAULT);
        helpAbout.setTooltip(new Tooltip("About Prompt Butler — license and links."));
        helpAbout.setOnAction(e -> {
            e.consume();
            showAboutWindow();
        });
        titleBar.getChildren().addAll(titleIcon, appTitle, titleDragSpacer, helpAbout);
        titleBar.setCursor(Cursor.MOVE);
        installUndecoratedStageDrag(titleBar, stage);
        statusLabel.getStyleClass().add("hint-label");
        statusLabel.getStyleClass().add("toast-label");
        statusLabel.setMinHeight(18);
        listSection.getChildren().addAll(new HBox(8, searchField, categoryFilter), listView, toolbar, statusLabel);
        getChildren().addAll(titleBar, listSection);
        VBox.setVgrow(listSection, Priority.ALWAYS);
        VBox.setVgrow(listView, Priority.ALWAYS);
        configureCategoryFilter();
        configureDragAndDropImport();
        showTemplateCount();
    }

    /* ----- Title bar: icon + undecorated window drag ----- */

    /**
     * Loads {@code /appicon.png} for the in-scene title strip (taskbar icon is set on the {@link Stage} in
     * {@link com.viruchith.PromptButler.PromptButlerApp}).
     */
    private static ImageView createTitleBarIcon() {
        ImageView iv = new ImageView();
        iv.getStyleClass().add("window-app-icon");
        iv.setFitWidth(22);
        iv.setFitHeight(22);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.setPickOnBounds(true);
        try (InputStream in = MainView.class.getResourceAsStream("/appicon.png")) {
            if (in != null) {
                iv.setImage(new Image(in, 22, 22, true, true));
            }
        } catch (Exception ignored) {
            // leave empty if resource missing or unreadable
        }
        return iv;
    }

    /**
     * {@link javafx.stage.StageStyle#TRANSPARENT} removes the native title bar; the user drags this
     * {@code dragHandle} node (the title {@link HBox}) to reposition the {@link Stage}.
     */
    private static void installUndecoratedStageDrag(Node dragHandle, Stage stage) {
        final double[] offset = new double[2];
        dragHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (isUnderNoDragTitleControl(e.getTarget())) {
                return;
            }
            if (e.getButton() == MouseButton.PRIMARY) {
                offset[0] = stage.getX() - e.getScreenX();
                offset[1] = stage.getY() - e.getScreenY();
            }
        });
        dragHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (isUnderNoDragTitleControl(e.getTarget())) {
                return;
            }
            if (e.isPrimaryButtonDown()) {
                stage.setX(e.getScreenX() + offset[0]);
                stage.setY(e.getScreenY() + offset[1]);
            }
        });
    }

    /**
     * Nodes marked {@code title-bar-no-drag} (and their descendants) must not move the undecorated stage.
     */
    private static boolean isUnderNoDragTitleControl(Object eventTarget) {
        if (!(eventTarget instanceof Node)) {
            return false;
        }
        for (Node n = (Node) eventTarget; n != null; n = n.getParent()) {
            if (n.getStyleClass().contains("title-bar-no-drag")) {
                return true;
            }
        }
        return false;
    }

    private static final String ABOUT_GITHUB_URL = "https://github.com/viruchith/PromptButler";

    private void showAboutWindow() {
        if (aboutStage != null && aboutStage.isShowing()) {
            aboutStage.toFront();
            return;
        }
        Stage w = new Stage();
        aboutStage = w;
        w.initOwner(stage);
        w.initModality(Modality.WINDOW_MODAL);
        w.setTitle("About Prompt Butler");

        Label heading = new Label("Prompt Butler");
        heading.getStyleClass().add("window-app-title");

        String implVer = MainView.class.getPackage() != null
                ? MainView.class.getPackage().getImplementationVersion()
                : null;
        String versionLine = (implVer == null || implVer.isEmpty()) ? "0.4.3" : implVer;
        Label version = new Label("Version " + versionLine);
        version.getStyleClass().add("preview-label");

        Label author = new Label("Author: Viruchith Ganesan");

        Hyperlink repo = new Hyperlink(ABOUT_GITHUB_URL);
        repo.setWrapText(true);
        repo.setOnAction(ev -> openExternalUri(ABOUT_GITHUB_URL));

        Label copyright = new Label("Copyright \u00a9 2026 Viruchith Ganesan");

        String licenseBody = "Licensed under the GNU General Public License v3.0 only (GPL-3.0-only). "
                + "You may redistribute and modify this program under those terms. "
                + "This is free software; there is NO WARRANTY, to the extent permitted by law. "
                + "See the LICENSE file in the repository for the full license text.";
        TextArea licenseArea = new TextArea(licenseBody);
        licenseArea.setEditable(false);
        licenseArea.setWrapText(true);
        licenseArea.setPrefRowCount(5);
        licenseArea.setMaxHeight(140);
        VBox.setVgrow(licenseArea, Priority.NEVER);

        Button closeB = new Button("Close");
        styleToolbarButton(closeB, FontAwesomeSolid.TIMES, "Close this window.");
        closeB.setOnAction(e -> w.close());

        VBox root = new VBox(10, heading, version, author, repo, copyright, licenseArea, closeB);
        root.getStyleClass().add("app-panel");
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 420, 420);
        copyApplicationStylesheetsTo(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                w.close();
                ev.consume();
            }
        });
        w.setScene(scene);
        w.setOnHidden(e -> aboutStage = null);
        w.show();
    }

    private static void openExternalUri(String uriString) {
        try {
            URI uri = URI.create(uriString);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            }
        } catch (Exception ignored) {
            // best-effort only
        }
    }

    /* ----- Search field: bidirectional bind to view model (trim applied when filtering) ----- */

    private void configureSearch() {
        searchField.setPromptText("Search prompts…");
        // Debounce: delay propagation to viewModel by 150ms after last keystroke
        PauseTransition searchDebounce = new PauseTransition(Duration.millis(150));
        searchDebounce.setOnFinished(ev -> viewModel.searchTextProperty().set(searchField.getText()));
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebounce.stop();
            searchDebounce.playFromStart();
        });
    }

    private void configureCategoryFilter() {
        categoryFilter.setPromptText("Category");
        refreshCategories();
        categoryFilter.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (categoryFilterUpdating) {
                return;
            }
            viewModel.categoryFilterProperty().set(newValue == null ? "All" : newValue);
        });
    }

    private void refreshCategories() {
        String selected = InputText.trimToEmpty(viewModel.categoryFilterProperty().get());
        if (selected.isEmpty()) {
            selected = "All";
        }
        categoryFilterUpdating = true;
        try {
            categoryFilter.getItems().setAll(viewModel.getKnownCategories());
            if (categoryFilter.getItems().contains(selected)) {
                categoryFilter.getSelectionModel().select(selected);
            } else {
                viewModel.categoryFilterProperty().set("All");
                categoryFilter.getSelectionModel().select("All");
            }
        } finally {
            categoryFilterUpdating = false;
        }
    }

    private void configureDragAndDropImport() {
        listSection.setOnDragOver(event -> {
            if (event.getGestureSource() != listSection
                    && event.getDragboard().hasFiles()
                    && event.getDragboard().getFiles().size() == 1
                    && event.getDragboard().getFiles().get(0).getName().toLowerCase().endsWith(".json")) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });
        listSection.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasFiles() && !event.getDragboard().getFiles().isEmpty()) {
                File file = event.getDragboard().getFiles().get(0);
                success = importFromPath(file.toPath());
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    /* ----- List: fuzzy-filtered items, row copy, single-/double-click semantics ----- */

    private void configureList() {
        listView.setItems(viewModel.getFilteredList());
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        singleClickDetailTimer = new PauseTransition(Duration.millis(320));
        singleClickDetailTimer.setOnFinished(ev -> {
            PromptTemplate t = pendingSingleClickTemplate;
            pendingSingleClickTemplate = null;
            if (t == null) {
                return;
            }
            if (listView.getSelectionModel().getSelectedItem() == t) {
                showPromptDetailWindow(t);
            }
        });
        listView.setCellFactory(new Callback<ListView<PromptTemplate>, ListCell<PromptTemplate>>() {
            @Override
            public ListCell<PromptTemplate> call(ListView<PromptTemplate> param) {
                return new ListCell<PromptTemplate>() {
                    private final Button rowCopy = new Button();
                    private final HBox row = new HBox(8);
                    private final Label titleLabel = new Label();

                    {
                        titleLabel.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(titleLabel, Priority.ALWAYS);
                        rowCopy.setGraphic(UiIcons.solid(FontAwesomeSolid.COPY));
                        rowCopy.getStyleClass().addAll("list-row-copy", "row-copy-action");
                        rowCopy.setFocusTraversable(false);
                        rowCopy.setTooltip(new Tooltip("Copy prompt body to clipboard."));
                        row.getChildren().addAll(titleLabel, rowCopy);
                    }

                    @Override
                    protected void updateItem(PromptTemplate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            rowCopy.setOnAction(null);
                        } else {
                            setText(null);
                            String prefix = item.isFavorite() ? "\u2605 " : "";
                            titleLabel.setText(prefix + item.getTitle()
                                    + " {" + item.getCategory() + "}"
                                    + PromptTextFormatter.formatTagsSuffixForCell(item.getTags()));
                            rowCopy.setOnAction(ev -> {
                                ev.consume();
                                copyTemplateBodyToClipboard(item, false);
                            });
                            setGraphic(row);
                        }
                    }
                };
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            viewModel.selectedTemplateProperty().set(n);
        });
        listView.addEventFilter(MouseEvent.MOUSE_CLICKED, this::handleListMouseClicked);
    }

    private void handleListMouseClicked(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (isEventFromRowCopyButton(e.getTarget())) {
            return;
        }
        PromptTemplate t = resolveTemplateFromClick(e);
        if (t == null) {
            return;
        }
        if (e.getClickCount() == 2) {
            singleClickDetailTimer.stop();
            pendingSingleClickTemplate = null;
            listView.getSelectionModel().select(t);
            Platform.runLater(() -> onTemplateChosen(t));
            e.consume();
            return;
        }
        if (e.getClickCount() == 1) {
            listView.getSelectionModel().select(t);
            pendingSingleClickTemplate = t;
            singleClickDetailTimer.stop();
            singleClickDetailTimer.playFromStart();
        }
    }

    private static boolean isEventFromRowCopyButton(Object target) {
        if (!(target instanceof Node)) {
            return false;
        }
        Node n = (Node) target;
        while (n != null) {
            if (n.getStyleClass().contains("row-copy-action")) {
                return true;
            }
            n = n.getParent();
        }
        return false;
    }

    /**
     * Resolves the row under the pointer; selection alone is unreliable on the 2nd click of a double-click.
     */
    private PromptTemplate resolveTemplateFromClick(MouseEvent e) {
        Node n = (Node) e.getTarget();
        while (n != null) {
            if (n instanceof ListCell) {
                Object item = ((ListCell<?>) n).getItem();
                if (item instanceof PromptTemplate) {
                    return (PromptTemplate) item;
                }
                return null;
            }
            n = n.getParent();
        }
        return listView.getSelectionModel().getSelectedItem();
    }

    private void configureToolbar() {
        Button newBtn = new Button("New");
        styleToolbarButton(newBtn, FontAwesomeSolid.PLUS, "Create a new prompt (id is assigned automatically).");
        newBtn.setOnAction(e -> showPromptEditorDialog(null));
        Button shortcutsBtn = new Button("Shortcuts");
        styleToolbarButton(shortcutsBtn, FontAwesomeSolid.KEYBOARD, "Show keyboard shortcuts.");
        shortcutsBtn.setOnAction(e -> showShortcutsDialog());
        Button importBtn = new Button("Import");
        styleToolbarButton(importBtn, FontAwesomeSolid.FILE_IMPORT, "Replace the library from a JSON file (ids are reassigned).");
        importBtn.setOnAction(e -> onImport());
        Button exportBtn = new Button("Export");
        styleToolbarButton(exportBtn, FontAwesomeSolid.FILE_EXPORT, "Save selected prompts (or all prompts) to JSON.");
        exportBtn.setOnAction(e -> onExport());
        Button settingsBtn = new Button("Settings");
        styleToolbarButton(settingsBtn, FontAwesomeSolid.COG, "Edit preferences including dark mode and compile behavior.");
        settingsBtn.setOnAction(e -> showSettingsDialog());
        Button dataBtn = new Button("Data Folder");
        styleToolbarButton(dataBtn, FontAwesomeSolid.FOLDER_OPEN, "Choose where prompts.json and preferences.json are stored (restart required after change).");
        dataBtn.setOnAction(e -> onDataFolderSettings());
        undoDeleteButton = new Button("Undo Delete");
        styleToolbarButton(undoDeleteButton, FontAwesomeSolid.UNDO, "Restore the most recently deleted prompt.");
        undoDeleteButton.setDisable(true);
        undoDeleteButton.setOnAction(e -> undoLastDelete());
        Button quit = new Button("Quit");
        styleToolbarButton(quit, FontAwesomeSolid.SIGN_OUT_ALT, "Exit the application.");
        quit.setOnAction(e -> System.exit(0));
        toolbar.getChildren().addAll(newBtn, shortcutsBtn, importBtn, exportBtn, settingsBtn, dataBtn, undoDeleteButton, quit);
    }

    private void onDataFolderSettings() {
        Dialog<ButtonType> dlg = new Dialog<ButtonType>();
        dlg.initOwner(stage);
        dlg.setTitle("Data storage");
        dlg.setHeaderText("Folder for prompts.json and preferences.json");
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Path current = StoragePaths.resolveDataDirectory();
        TextArea info = new TextArea(buildDataFolderHelpText(current));
        info.setEditable(false);
        info.setWrapText(true);
        info.setPrefRowCount(10);

        Button choose = new Button("Choose folder");
        styleToolbarButton(choose, FontAwesomeSolid.FOLDER_OPEN, "Pick a writable folder; a pointer is saved under your user home.");
        choose.setOnAction(ev -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select data folder");
            if (Files.isDirectory(current)) {
                dc.setInitialDirectory(current.toFile());
            }
            File dir = dc.showDialog(stage);
            if (dir == null) {
                return;
            }
            try {
                StoragePaths.persistCustomDataDirectory(dir.toPath());
                showRestartNotice("The new folder has been saved.");
                dlg.close();
            } catch (Exception ex) {
                showError("Could not save folder", messageOrClass(ex));
            }
        });

        Button useDefault = new Button("Use default");
        styleToolbarButton(useDefault, FontAwesomeSolid.HOME, "Remove the saved folder pointer (not env/property overrides).");
        useDefault.setOnAction(ev -> {
            try {
                StoragePaths.clearPersistedCustomDataDirectory();
                showRestartNotice("The app will use the default folder after restart (unless PROMPT_BUTLER_DIR or prompt.butler.dir is set).");
                dlg.close();
            } catch (IOException ex) {
                showError("Could not clear saved folder", messageOrClass(ex));
            }
        });

        VBox root = new VBox(10, info, new HBox(8, choose, useDefault));
        dlg.getDialogPane().setContent(root);
        attachLightDialogStyles(dlg.getDialogPane());
        Button closeBtn = (Button) dlg.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setGraphic(UiIcons.solid(FontAwesomeSolid.TIMES));
        }
        dlg.showAndWait();
    }

    private static String buildDataFolderHelpText(Path current) {
        StringBuilder sb = new StringBuilder();
        sb.append("Current data directory:\n").append(current.toAbsolutePath()).append("\n\n");
        sb.append("Saved pointer file (used when env/property overrides are not set):\n");
        sb.append(StoragePaths.getStoragePointerFile().toAbsolutePath()).append("\n\n");
        sb.append("Precedence: (1) PROMPT_BUTLER_DIR environment variable, ");
        sb.append("(2) Java system property prompt.butler.dir, ");
        sb.append("(3) the pointer file above, ");
        sb.append("(4) default ").append(StoragePaths.getBootstrapConfigRoot().toAbsolutePath()).append(".\n\n");
        sb.append("After changing the folder, restart Prompt Butler so it loads from the new location.");
        return sb.toString();
    }

    private void showRestartNotice(String detail) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(stage);
        attachLightDialogStyles(a.getDialogPane());
        a.setHeaderText("Restart required");
        a.setContentText(detail + "\n\nQuit and start the application again.");
        styleStandardAlertButtons(a.getDialogPane());
        a.showAndWait();
    }

    private void showShortcutsDialog() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(stage);
        attachLightDialogStyles(a.getDialogPane());
        a.setHeaderText("Keyboard shortcuts");
        a.setContentText(
                "Ctrl/Cmd+C (list focused): copy selected prompt body\n"
                        + "Enter: open selected prompt or variable form\n"
                        + "Ctrl/Cmd+Enter in variable form: next field / copy on last\n"
                        + "Up/Down: navigate list\n"
                        + "Escape: close variable window or hide overlay\n"
                        + "Global: Ctrl+Alt+P (Win/Linux) or Cmd+Alt+P (macOS)");
        styleStandardAlertButtons(a.getDialogPane());
        a.showAndWait();
    }

    private void showSettingsDialog() {
        stage.getProperties().put(AutoHideController.SUSPEND_AUTO_HIDE_KEY, Boolean.TRUE);
        try {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.initOwner(stage);
        dialog.setTitle("Settings");
        dialog.setHeaderText("Preferences");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        CheckBox darkMode = new CheckBox("Enable dark mode");
        darkMode.setSelected(preferences.isDarkMode());
        CheckBox quoteVars = new CheckBox("Wrap compiled {{variables}} in quotes");
        quoteVars.setSelected(preferences.isQuoteCompiledVariables());

        ComboBox<AutoHideMode> autoHideMode = new ComboBox<>();
        autoHideMode.getItems().setAll(AutoHideMode.values());
        autoHideMode.getSelectionModel().select(preferences.getAutoHideMode());

        TextField categoryField = new TextField(preferences.getDefaultCategory());
        categoryField.setPromptText("Default category");
        ComboBox<String> deleteCategoryDropdown = new ComboBox<String>();
        deleteCategoryDropdown.setMaxWidth(Double.MAX_VALUE);
        refreshDeleteCategoryDropdown(deleteCategoryDropdown);
        Button deleteCategoryButton = new Button("Delete Category");
        styleToolbarButton(deleteCategoryButton, FontAwesomeSolid.TRASH_ALT, "Delete selected category and move prompts to General.");
        deleteCategoryButton.setOnAction(e -> {
            String selectedCategory = deleteCategoryDropdown.getSelectionModel().getSelectedItem();
            if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
                showError("Validation", "Choose a category to delete.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(stage);
            attachLightDialogStyles(confirm.getDialogPane());
            styleStandardAlertButtons(confirm.getDialogPane());
            confirm.setHeaderText("Delete category \"" + selectedCategory + "\"?");
            confirm.setContentText("All prompts in this category will be reassigned to General.");
            Optional<ButtonType> result = confirm.showAndWait();
            if (!result.isPresent() || result.get() != ButtonType.OK) {
                return;
            }
            try {
                int affected = viewModel.deleteCategoryAndReassignToGeneral(selectedCategory);
                if (selectedCategory.equalsIgnoreCase(preferences.getDefaultCategory())) {
                    preferences.setDefaultCategory("General");
                    categoryField.setText("General");
                    preferencesSaver.accept(preferences);
                }
                refreshCategories();
                refreshDeleteCategoryDropdown(deleteCategoryDropdown);
                showTemplateCount();
                showToast("Deleted category \"" + selectedCategory + "\". Reassigned " + affected + " prompt(s) to General.", 3.5);
            } catch (Exception ex) {
                showError("Could not delete category", messageOrClass(ex));
            }
        });
        HBox deleteCategoryRow = new HBox(8, deleteCategoryDropdown, deleteCategoryButton);
        HBox.setHgrow(deleteCategoryDropdown, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(darkMode, 0, 0, 2, 1);
        grid.add(quoteVars, 0, 1, 2, 1);
        grid.add(new Label("Auto-hide mode"), 0, 2);
        grid.add(autoHideMode, 1, 2);
        grid.add(new Label("Default category"), 0, 3);
        grid.add(categoryField, 1, 3);
        grid.add(new Label("Delete category"), 0, 4);
        grid.add(deleteCategoryRow, 1, 4);
        dialog.getDialogPane().setContent(grid);
        attachLightDialogStyles(dialog.getDialogPane());

        Optional<ButtonType> result = dialog.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            return;
        }

        preferences.setDarkMode(darkMode.isSelected());
        preferences.setQuoteCompiledVariables(quoteVars.isSelected());
        preferences.setAutoHideMode(autoHideMode.getSelectionModel().getSelectedItem());
        preferences.setDefaultCategory(categoryField.getText());
        preferencesSaver.accept(preferences);
        themeSwitcher.accept(Boolean.valueOf(preferences.isDarkMode()));
        showToast("Settings updated.", 2.5);
        } finally {
            stage.getProperties().remove(AutoHideController.SUSPEND_AUTO_HIDE_KEY);
        }
    }

    private void undoLastDelete() {
        if (lastDeletedTemplate == null) {
            return;
        }
        try {
            viewModel.restoreDeletedTemplate(lastDeletedTemplate);
            listView.getSelectionModel().select(lastDeletedTemplate);
            showToast("Delete undone.", 2.5);
            lastDeletedTemplate = null;
            undoDeleteButton.setDisable(true);
            refreshCategories();
            showTemplateCount();
        } catch (Exception ex) {
            showError("Could not restore prompt", messageOrClass(ex));
        }
    }

    private static String messageOrClass(Throwable ex) {
        if (ex == null) {
            return "Unknown error";
        }
        String m = ex.getMessage();
        return m == null || m.isEmpty() ? ex.toString() : m;
    }

    private static void styleToolbarButton(Button b, FontAwesomeSolid icon, String tooltipText) {
        b.setGraphic(UiIcons.solid(icon));
        b.setTooltip(new Tooltip(tooltipText));
        b.getStyleClass().add("icon-toolbar-button");
    }

    /* ----- Global key filter on main scene (list navigation, Escape, template activation) ----- */

    public void attachGlobalKeys() {
        if (stage.getScene() == null) {
            return;
        }
        stage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
    }

    private void handleKey(KeyEvent event) {
        if (event.getCode() == KeyCode.F1) {
            showShortcutsDialog();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            if (isVariableParametersWindowOpen()) {
                closeVariableParametersWindow();
                focusSearch();
                event.consume();
                return;
            }
            hideOverlay();
            event.consume();
            return;
        }
        if (searchField.isFocused()) {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.ENTER) {
                return;
            }
        }
        if (event.getCode() == KeyCode.C && event.isShortcutDown()) {
            if (!searchField.isFocused() && listView.isFocused()) {
                PromptTemplate t = listView.getSelectionModel().getSelectedItem();
                if (t != null) {
                    copyTemplateBodyToClipboard(t, false);
                }
                event.consume();
                return;
            }
        }
        if (event.getCode() == KeyCode.DOWN) {
            listView.requestFocus();
            int i = listView.getSelectionModel().getSelectedIndex();
            if (i < listView.getItems().size() - 1) {
                listView.getSelectionModel().select(i + 1);
            } else if (i < 0 && !listView.getItems().isEmpty()) {
                listView.getSelectionModel().select(0);
            }
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            listView.requestFocus();
            int i = listView.getSelectionModel().getSelectedIndex();
            if (i > 0) {
                listView.getSelectionModel().select(i - 1);
            } else if (i < 0 && !listView.getItems().isEmpty()) {
                listView.getSelectionModel().select(0);
            }
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER) {
            PromptTemplate t = listView.getSelectionModel().getSelectedItem();
            if (t == null && !listView.getItems().isEmpty()) {
                listView.getSelectionModel().select(0);
                t = listView.getSelectionModel().getSelectedItem();
            }
            if (t != null) {
                final PromptTemplate chosen = t;
                Platform.runLater(() -> onTemplateChosen(chosen));
            }
            event.consume();
        }
    }

    public void focusSearch() {
        Platform.runLater(() -> {
            searchField.requestFocus();
            searchField.selectAll();
        });
    }

    public void reloadTemplatesFromDisk(List<PromptTemplate> templates) {
        viewModel.reloadFromDisk(templates);
        refreshCategories();
        showTemplateCount();
    }

    public void applyUpdatedPreferences(UserPreferences latestPreferences) {
        preferences.setAutoHideMode(latestPreferences.getAutoHideMode());
        preferences.setDefocusOpacity(latestPreferences.getDefocusOpacity());
        preferences.setDarkMode(latestPreferences.isDarkMode());
        preferences.setHotkeyKeyCode(latestPreferences.getHotkeyKeyCode());
        preferences.setHotkeyModifiers(latestPreferences.getHotkeyModifiers());
        preferences.setQuoteCompiledVariables(latestPreferences.isQuoteCompiledVariables());
        preferences.setDefaultCategory(latestPreferences.getDefaultCategory());
        preferences.setWindowX(latestPreferences.getWindowX());
        preferences.setWindowY(latestPreferences.getWindowY());
        preferences.setWindowWidth(latestPreferences.getWindowWidth());
        preferences.setWindowHeight(latestPreferences.getWindowHeight());
        themeSwitcher.accept(Boolean.valueOf(preferences.isDarkMode()));
    }

    /**
     * Invoked after double-click or Enter on a row: either copies raw body and hides overlay, or opens the
     * modeless variable window when the template body contains {@code {{placeholders}}}.
     */
    private void onTemplateChosen(PromptTemplate t) {
        List<String> vars = viewModel.variablesFor(t);
        if (vars.isEmpty()) {
            copyToClipboardAndHide(t.getBody());
            markUsedSafely(t);
            return;
        }
        openVariableParametersWindow(t, vars);
    }

    private boolean isVariableParametersWindowOpen() {
        return variableParamsStage != null && variableParamsStage.isShowing();
    }

    /** Builds the modeless variable form; main list stays usable. */
    private void openVariableParametersWindow(PromptTemplate t, List<String> vars) {
        closeVariableParametersWindow();
        variableTarget = t;
        variableFields.clear();

        Label header = new Label("Fill variables for: " + t.getTitle());
        header.setWrapText(true);
        VBox formRoot = new VBox(6, header);
        formRoot.getStyleClass().add("app-panel");
        formRoot.setPadding(new Insets(12));
        for (String v : vars) {
            Label l = new Label(v);
            TextArea ta = new TextArea();
            ta.setUserData(v);
            ta.setWrapText(true);
            ta.setPrefRowCount(3);
            ta.setPromptText("Value for {{" + v + "}}");
            ta.setTooltip(new Tooltip("Shortcut+Enter: next variable; on the last, same as Copy & close. (Ctrl on Windows/Linux, Cmd on macOS)"));
            variableFields.add(ta);
            formRoot.getChildren().add(new VBox(2, l, ta));
        }
        Button done = new Button("Copy & close");
        done.setGraphic(UiIcons.solid(FontAwesomeSolid.CHECK));
        done.setTooltip(new Tooltip("Fill variables, copy compiled text, and close this variables window (main library stays open)."));
        done.setDefaultButton(true);
        done.setOnAction(e -> commitVariables());
        Button copyKeep = new Button("Copy — keep open");
        copyKeep.setGraphic(UiIcons.solid(FontAwesomeSolid.COPY));
        copyKeep.setTooltip(new Tooltip("Copy compiled text and keep this window open."));
        copyKeep.setOnAction(e -> {
            String compiled = compileVariableFormPayload();
            if (compiled != null) {
                copyPlainTextThenMaybeHide(compiled, false);
                markUsedSafely(variableTarget);
            }
        });
        HBox actions = new HBox(8, copyKeep, done);
        formRoot.getChildren().add(actions);

        Stage w = new Stage();
        variableParamsStage = w;
        w.initOwner(stage);
        w.initModality(Modality.NONE);
        w.setTitle("Variables — " + t.getTitle());

        Scene scene = new Scene(formRoot, 440, Math.min(720, 140 + vars.size() * 100));
        copyApplicationStylesheetsTo(scene);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                closeVariableParametersWindow();
                focusSearch();
                ev.consume();
            }
        });
        w.setScene(scene);
        w.setOnHidden(e -> {
            variableParamsStage = null;
            variableTarget = null;
            variableFields.clear();
        });
        wireVariableEnter();
        w.show();
        Platform.runLater(() -> {
            if (!variableFields.isEmpty()) {
                variableFields.get(0).requestFocus();
            }
        });
    }

    private void wireVariableEnter() {
        for (int i = 0; i < variableFields.size(); i++) {
            final int idx = i;
            TextArea ta = variableFields.get(i);
            ta.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ENTER && ev.isShortcutDown()) {
                    ev.consume();
                    if (idx == variableFields.size() - 1) {
                        commitVariables();
                    } else {
                        variableFields.get(idx + 1).requestFocus();
                    }
                }
            });
        }
    }

    private void commitVariables() {
        if (variableTarget == null) {
            return;
        }
        String compiled = compileVariableFormPayload();
        if (compiled != null) {
            closeVariableParametersWindow();
            copyPlainTextThenMaybeHide(compiled, false);
            markUsedSafely(variableTarget);
        }
    }

    private String compileVariableFormPayload() {
        if (variableTarget == null) {
            return null;
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        for (TextArea ta : variableFields) {
            String key = (String) ta.getUserData();
            values.put(key, InputText.trimToEmpty(ta.getText()));
        }
        return viewModel.compile(variableTarget, values, preferences.isQuoteCompiledVariables());
    }

    private void closeVariableParametersWindow() {
        if (variableParamsStage != null) {
            Stage s = variableParamsStage;
            variableParamsStage = null;
            variableTarget = null;
            variableFields.clear();
            s.close();
        }
    }

    /* ----- Clipboard: delegate to ClipboardPort; optional stage hide (defers hide to avoid Glass issues) ----- */

    private void copyToClipboardAndHide(String text) {
        copyPlainTextThenMaybeHide(text, true, preferences.getAutoHideMode());
    }

    /**
     * Copies text to the clipboard, optionally hiding the overlay after a short delay so we are past
     * mouse/keyboard handling (avoids crashes when closing from double-click / Enter).
     */
    private void copyPlainTextThenMaybeHide(String text, boolean hideAfter) {
        copyPlainTextThenMaybeHide(text, hideAfter, AutoHideMode.HIDE);
    }

    private void copyPlainTextThenMaybeHide(String text, boolean hideAfter, AutoHideMode hideMode) {
        final String payload = text == null ? "" : text;
        if (!hideAfter) {
            clipboard.copyPlainText(payload);
            showCopiedStatus();
            return;
        }
        PauseTransition waitRelease = new PauseTransition(Duration.millis(90));
        waitRelease.setOnFinished(ev -> {
            clipboard.copyPlainText(payload);
            PauseTransition waitClipboard = new PauseTransition(Duration.millis(60));
            waitClipboard.setOnFinished(ev2 -> applyPostCopyHide(hideMode));
            waitClipboard.playFromStart();
        });
        waitRelease.playFromStart();
    }

    private void applyPostCopyHide(AutoHideMode hideMode) {
        AutoHideMode effectiveMode = hideMode == null ? AutoHideMode.HIDE : hideMode;
        closePromptDetailWindow();
        closeVariableParametersWindow();
        switch (effectiveMode) {
            case MINIMIZE:
                stage.setIconified(true);
                break;
            case OPACITY:
                stage.setOpacity(Math.max(0.01, preferences.getDefocusOpacity()));
                break;
            case TRAY:
            case HIDE:
            default:
                stage.hide();
                break;
        }
        clipboard.clearRetainedSensitiveData();
    }

    private void copyTemplateBodyToClipboard(PromptTemplate t, boolean hideAfter) {
        if (t == null) {
            return;
        }
        copyPlainTextThenMaybeHide(t.getBody(), hideAfter);
        markUsedSafely(t);
    }

    private void markUsedSafely(PromptTemplate template) {
        try {
            viewModel.markTemplateUsed(template);
            showTemplateCount();
        } catch (Exception ex) {
            showError("Could not update usage", messageOrClass(ex));
        }
    }

    private void showCopiedStatus() {
        showToast("Copied to clipboard.", 2.5);
    }

    private void showToast(String message, double seconds) {
        statusLabel.setText(message);
        statusLabel.setTextFill(Color.web("#047857"));
        PauseTransition clear = new PauseTransition(Duration.seconds(seconds));
        clear.setOnFinished(ev -> showTemplateCount());
        clear.playFromStart();
    }

    /** Displays the current template count in the status label. */
    public void showTemplateCount() {
        int count = viewModel.getTemplateCount();
        statusLabel.setText(count + (count == 1 ? " prompt" : " prompts"));
        statusLabel.setTextFill(Color.web("#047857"));
    }

    private void hideOverlay() {
        applyPostCopyHide(AutoHideMode.HIDE);
    }

    /* ----- Toolbar: JSON import/export ----- */

    private void onImport() {
        FileChooser ch = new FileChooser();
        ch.setTitle("Import prompts JSON");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = ch.showOpenDialog(stage);
        if (f == null) {
            return;
        }
        importFromPath(f.toPath());
    }

    private boolean importFromPath(Path path) {
        try {
            List<PromptTemplate> imported = importExportService.importFromFile(path);
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(stage);
            attachLightDialogStyles(confirm.getDialogPane());
            styleStandardAlertButtons(confirm.getDialogPane());
            confirm.setHeaderText("Replace all prompts?");
            confirm.setContentText("This will replace your current prompt library with " + imported.size() + " template(s).");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                List<PromptTemplate> remapped = importExportService.remapImportedTemplates(imported, viewModel::allocateNewTemplateId);
                viewModel.replaceAllTemplates(remapped);
                refreshCategories();
                showTemplateCount();
                showToast("Imported " + remapped.size() + " template(s).", 4);
                return true;
            }
        } catch (Exception ex) {
            showError("Import failed", ex.getMessage());
        }
        return false;
    }

    private void onExport() {
        FileChooser ch = new FileChooser();
        ch.setTitle("Export prompts JSON");
        ch.setInitialFileName("prompts-export.json");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = ch.showSaveDialog(stage);
        if (f == null) {
            return;
        }
        try {
            List<PromptTemplate> selected = new ArrayList<PromptTemplate>(listView.getSelectionModel().getSelectedItems());
            List<PromptTemplate> payload = selected.isEmpty() ? viewModel.getMasterTemplates() : selected;
            importExportService.exportToFile(f.toPath(), payload);
            showToast("Exported " + payload.size() + " template(s).", 3);
        } catch (Exception ex) {
            showError("Export failed", ex.getMessage());
        }
    }

    private void closePromptDetailWindow() {
        if (promptDetailStage != null) {
            Stage s = promptDetailStage;
            promptDetailStage = null;
            s.close();
        }
    }

    private void copyApplicationStylesheetsTo(Scene target) {
        Scene main = stage.getScene();
        if (main == null) {
            return;
        }
        for (String url : main.getStylesheets()) {
            if (!target.getStylesheets().contains(url)) {
                target.getStylesheets().add(url);
            }
        }
    }

    private void showPromptDetailWindow(PromptTemplate t) {
        closePromptDetailWindow();
        Stage detail = new Stage();
        promptDetailStage = detail;
        detail.initOwner(stage);
        detail.initModality(Modality.WINDOW_MODAL);
        detail.setTitle(t.getTitle());

        Label idCaption = new Label("Internal id (assigned by the app): " + t.getId());
        idCaption.getStyleClass().add("preview-label");
        idCaption.setWrapText(true);

        Label metadata = new Label(PromptTextFormatter.formatPromptMetadataSummary(t));
        metadata.getStyleClass().add("preview-label");
        metadata.setWrapText(true);

        TextArea content = buildReadOnlyPreviewArea(PromptTextFormatter.nullToEmpty(t.getBody()), "Prompt body");
        TextArea markdownPreview = buildReadOnlyPreviewArea(MarkdownPreviewRenderer.render(t.getBody()), "Markdown preview");
        SplitPane previewSplit = buildPromptPreviewSplit(content, markdownPreview);

        Button copyB = new Button("Copy");
        styleToolbarButton(copyB, FontAwesomeSolid.COPY, "Copy prompt body to clipboard.");
        copyB.setOnAction(e -> copyTemplateBodyToClipboard(t, false));

        Button favB = new Button(t.isFavorite() ? "Unfavorite" : "Favorite");
        styleToolbarButton(favB, t.isFavorite() ? FontAwesomeSolid.STAR : FontAwesomeSolid.STAR,
                t.isFavorite() ? "Remove from favorites." : "Mark as favorite (shown first in search).");
        favB.setOnAction(e -> {
            try {
                viewModel.toggleFavorite(t);
                closePromptDetailWindow();
            } catch (Exception ex) {
                showError("Could not update", messageOrClass(ex));
            }
        });

        Button editB = new Button("Edit");
        styleToolbarButton(editB, FontAwesomeSolid.PEN, "Edit title, body, and tags.");
        editB.setOnAction(e -> {
            closePromptDetailWindow();
            showPromptEditorDialog(t);
        });

        Button duplicateB = new Button("Duplicate");
        styleToolbarButton(duplicateB, FontAwesomeSolid.CLONE, "Create a copy of this template.");
        duplicateB.setOnAction(e -> {
            try {
                PromptTemplate copy = viewModel.duplicateTemplate(t);
                listView.getSelectionModel().select(copy);
                refreshCategories();
                closePromptDetailWindow();
                showToast("Template duplicated.", 2.5);
            } catch (Exception ex) {
                showError("Could not duplicate", messageOrClass(ex));
            }
        });

        Button delB = new Button("Delete");
        styleToolbarButton(delB, FontAwesomeSolid.TRASH_ALT, "Remove this prompt from the library.");
        delB.setOnAction(e -> {
            if (deletePromptWithConfirmation(t)) {
                closePromptDetailWindow();
            }
        });

        Button closeB = new Button("Close");
        styleToolbarButton(closeB, FontAwesomeSolid.TIMES, "Close this window.");
        closeB.setOnAction(e -> closePromptDetailWindow());

        HBox actions = new HBox(8, copyB, favB, editB, duplicateB, delB, closeB);
        VBox root = new VBox(10, idCaption, metadata, previewSplit, actions);
        root.getStyleClass().add("app-panel");
        root.setPadding(new Insets(12));

        Scene scene = new Scene(root, 660, 500);
        copyApplicationStylesheetsTo(scene);
        detail.setScene(scene);
        detail.setOnHidden(e -> promptDetailStage = null);
        detail.show();
    }

    private void showPromptEditorDialog(PromptTemplate existing) {
        final boolean isEdit = existing != null;
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.initOwner(stage);
        dialog.setTitle(isEdit ? "Edit prompt" : "New prompt");
        dialog.setHeaderText(isEdit
                ? "Update this template. The internal id does not change."
                : "Create a new template. An id will be assigned automatically. Use {{variable}} for placeholders.");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        TextField titleField = new TextField();
        TextArea bodyArea = new TextArea();
        bodyArea.setPromptText("Prompt body…");
        bodyArea.setPrefRowCount(8);
        bodyArea.setWrapText(true);
        TextArea bodyPreview = buildReadOnlyPreviewArea("", "Rendered markdown preview");
        bodyPreview.setPrefRowCount(8);
        TextField tagsField = new TextField();
        tagsField.setPromptText("comma, separated, tags");
        ComboBox<String> categoryDropdown = new ComboBox<String>();
        categoryDropdown.setPromptText("Category");
        categoryDropdown.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(categoryDropdown, Priority.ALWAYS);
        LinkedHashSet<String> knownCategories = new LinkedHashSet<String>();
        for (String category : viewModel.getKnownCategories()) {
            if (!"All".equalsIgnoreCase(category)) {
                knownCategories.add(category);
            }
        }
        String defaultCategory = InputText.trimToEmpty(preferences.getDefaultCategory());
        if (defaultCategory.isEmpty()) {
            defaultCategory = "General";
        }
        knownCategories.add(defaultCategory);
        categoryDropdown.getItems().setAll(knownCategories);
        categoryDropdown.getSelectionModel().select(defaultCategory);
        Button addCategoryButton = new Button("New Category");
        styleToolbarButton(addCategoryButton, FontAwesomeSolid.PLUS, "Create a new category and select it.");
        addCategoryButton.setOnAction(e -> {
            Optional<String> maybeCategory = showCreateCategoryDialog();
            if (!maybeCategory.isPresent()) {
                return;
            }
            String category = maybeCategory.get();
            if (!categoryDropdown.getItems().contains(category)) {
                categoryDropdown.getItems().add(category);
            }
            categoryDropdown.getSelectionModel().select(category);
        });
        HBox categoryRow = new HBox(8, categoryDropdown, addCategoryButton);
        if (isEdit) {
            titleField.setText(PromptTextFormatter.nullToEmpty(existing.getTitle()));
            bodyArea.setText(PromptTextFormatter.nullToEmpty(existing.getBody()));
            bodyPreview.setText(MarkdownPreviewRenderer.render(existing.getBody()));
            tagsField.setText(PromptTextFormatter.tagsCsvForEditor(existing.getTags()));
            String editCategory = InputText.trimToEmpty(existing.getCategory());
            if (editCategory.isEmpty()) {
                editCategory = defaultCategory;
            }
            if (!categoryDropdown.getItems().contains(editCategory)) {
                categoryDropdown.getItems().add(editCategory);
            }
            categoryDropdown.getSelectionModel().select(editCategory);
        }
        int r = 0;
        grid.add(new Label("Title"), 0, r);
        grid.add(titleField, 1, r++);
        grid.add(new Label("Body"), 0, r);
        grid.add(buildEditorBodyTabs(bodyArea, bodyPreview), 1, r++);
        grid.add(new Label("Tags"), 0, r);
        grid.add(tagsField, 1, r);
        r++;
        grid.add(new Label("Category"), 0, r);
        grid.add(categoryRow, 1, r);
        bodyArea.textProperty().addListener((obs, oldValue, newValue) ->
                bodyPreview.setText(MarkdownPreviewRenderer.render(newValue)));
        dialog.getDialogPane().setContent(grid);
        attachLightDialogStyles(dialog.getDialogPane());

        final String lockedId = isEdit ? existing.getId() : null;

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDefaultButton(true);
        okButton.setText("Save");
        okButton.setGraphic(UiIcons.solid(FontAwesomeSolid.SAVE));
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setGraphic(UiIcons.solid(FontAwesomeSolid.TIMES));
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String title = InputText.trimToEmpty(titleField.getText());
            if (title.isEmpty()) {
                showError("Validation", "Title is required.");
                evt.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            return;
        }
        String title = InputText.trimToEmpty(titleField.getText());
        String body = InputText.trimToEmpty(bodyArea.getText());
        try {
            PromptTemplate saved;
            String chosenCategory = InputText.trimToEmpty(categoryDropdown.getSelectionModel().getSelectedItem());
            if (chosenCategory.isEmpty()) {
                chosenCategory = defaultCategory;
            }
            if (isEdit) {
                saved = viewModel.editTemplate(existing, title, body, PromptTextFormatter.parseTags(tagsField.getText()), chosenCategory);
            } else {
                String newId = viewModel.allocateNewTemplateId();
                saved = new PromptTemplate(newId, title, body, PromptTextFormatter.parseTags(tagsField.getText()), false, chosenCategory, 0L, 0L, java.util.Collections.emptyList());
                viewModel.addTemplate(saved);
                viewModel.searchTextProperty().set("");
            }
            listView.getSelectionModel().select(saved);
            refreshCategories();
            showTemplateCount();
        } catch (Exception ex) {
            showError("Could not save", ex.getMessage());
        }
    }

    private Optional<String> showCreateCategoryDialog() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.initOwner(stage);
        dialog.setTitle("New category");
        dialog.setHeaderText("Add a category");
        dialog.setContentText("Category name");
        attachLightDialogStyles(dialog.getDialogPane());
        styleStandardAlertButtons(dialog.getDialogPane());
        Optional<String> raw = dialog.showAndWait();
        if (!raw.isPresent()) {
            return Optional.empty();
        }
        String category = InputText.trimToEmpty(raw.get());
        if (category.isEmpty()) {
            showError("Validation", "Category name is required.");
            return Optional.empty();
        }
        return Optional.of(category);
    }

    private static TextArea buildReadOnlyPreviewArea(String text, String promptText) {
        TextArea area = new TextArea(PromptTextFormatter.nullToEmpty(text));
        area.setEditable(false);
        area.setWrapText(true);
        area.setPromptText(promptText);
        area.getStyleClass().add("preview-text");
        VBox.setVgrow(area, Priority.ALWAYS);
        return area;
    }

    private static SplitPane buildPromptPreviewSplit(TextArea bodyArea, TextArea previewArea) {
        VBox bodyBox = new VBox(6, new Label("Prompt body"), bodyArea);
        VBox previewBox = new VBox(6, new Label("Rendered preview"), previewArea);
        VBox.setVgrow(bodyArea, Priority.ALWAYS);
        VBox.setVgrow(previewArea, Priority.ALWAYS);
        SplitPane split = new SplitPane(bodyBox, previewBox);
        split.setDividerPositions(0.52);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    private static TabPane buildEditorBodyTabs(TextArea bodyArea, TextArea previewArea) {
        Tab writeTab = new Tab("Write", bodyArea);
        writeTab.setClosable(false);
        Tab previewTab = new Tab("Preview", previewArea);
        previewTab.setClosable(false);
        TabPane tabs = new TabPane(writeTab, previewTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(280);
        return tabs;
    }

    private void refreshDeleteCategoryDropdown(ComboBox<String> dropdown) {
        ArrayList<String> categories = new ArrayList<String>();
        for (String category : viewModel.getKnownCategories()) {
            if (!"All".equalsIgnoreCase(category) && !"General".equalsIgnoreCase(category)) {
                categories.add(category);
            }
        }
        dropdown.getItems().setAll(categories);
        if (!categories.isEmpty()) {
            dropdown.getSelectionModel().select(0);
        } else {
            dropdown.getSelectionModel().clearSelection();
        }
    }

    private boolean deletePromptWithConfirmation(PromptTemplate t) {
        if (t == null) {
            return false;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(stage);
        attachLightDialogStyles(confirm.getDialogPane());
        styleStandardAlertButtons(confirm.getDialogPane());
        confirm.setHeaderText("Delete this prompt?");
        confirm.setContentText("\"" + t.getTitle() + "\" (" + t.getId() + ")");
        Optional<ButtonType> res = confirm.showAndWait();
        if (!res.isPresent() || res.get() != ButtonType.OK) {
            return false;
        }
        try {
            viewModel.deleteTemplate(t);
            listView.getSelectionModel().clearSelection();
            lastDeletedTemplate = t;
            undoDeleteButton.setDisable(false);
            refreshCategories();
            showToast("Deleted \"" + t.getTitle() + "\". Use Undo Delete to restore.", 4);
            return true;
        } catch (Exception ex) {
            showError("Could not delete", ex.getMessage());
            return false;
        }
    }

    private void styleStandardAlertButtons(DialogPane pane) {
        if (pane == null) {
            return;
        }
        Button ok = (Button) pane.lookupButton(ButtonType.OK);
        if (ok != null) {
            ok.setGraphic(UiIcons.solid(FontAwesomeSolid.CHECK));
        }
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancel != null) {
            cancel.setGraphic(UiIcons.solid(FontAwesomeSolid.TIMES));
        }
    }

    private void showError(String header, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(stage);
        attachLightDialogStyles(a.getDialogPane());
        a.setHeaderText(header);
        a.setContentText(message);
        styleStandardAlertButtons(a.getDialogPane());
        a.showAndWait();
    }

    private static String stylesheetUrl(String resource) {
        URL u = MainView.class.getResource(resource);
        return u == null ? null : u.toExternalForm();
    }

    private void attachLightDialogStyles(DialogPane pane) {
        String stylesheet = preferences.isDarkMode() ? DARK_STYLESHEET_URL : LIGHT_STYLESHEET_URL;
        if (pane == null || stylesheet == null) {
            return;
        }
        if (!pane.getStylesheets().contains(stylesheet)) {
            pane.getStylesheets().add(stylesheet);
        }
    }
}
