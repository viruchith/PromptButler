# Prompt Butler — Project State Checkpoint

> **Purpose:** Handoff document for a new chat session or a different AI agent. Last aligned with repository state as of checkpoint creation (Prompt Butler JavaFX overlay app).

---

## 1. Executive summary

**Prompt Butler** is a small **JavaFX** desktop overlay for **searching prompt templates**, filling **`{{variable}}`** placeholders via an **inline variable form** on the overlay, and **copying** compiled or raw text to the system clipboard. It uses **Gradle (Groovy)**, **JDK 17** toolchain, **OpenJFX 21**, minimal deps (**Gson**, **jNativeHook**), **JUnit 5** + **Mockito** for tests, and a **strict `core/` / `ui/` / `os/`** package split (`core` has **no** JavaFX imports).

Original product vision referenced **Java 8** + bundled FX; **Maven-hosted OpenJFX requires JDK 11+** bytecode, so the build intentionally uses **Java 17** + **OpenJFX 21** while keeping **core** code free of post-8 APIs where practical.

---

## 2. Repository layout (source of truth)

```
PromptButler/
├── build.gradle                 # Plugins, deps, profiles, JaCoCo, UFT env strip on :run
├── settings.gradle              # rootProject.name = 'prompt-butler'
├── gradle.properties
├── gradle/wrapper/              # Gradle 8.7 wrapper
├── README.md                    # User-facing run/config notes
├── PROJECT_STATE_CHECKPOINT.md  # This file
├── .gitignore
└── src/
    ├── main/java/com/viruchith/PromptButler/
    │   ├── PromptButlerApp.java       # JavaFX Application entry
    │   ├── core/                      # NO javafx.* imports
    │   │   ├── clipboard/ClipboardPort.java
    │   │   ├── logging/AppLogger.java
    │   │   ├── model/                 # PromptTemplate, BuildProfile, AutoHideMode, UserPreferences
    │   │   ├── repository/            # PromptRepository, JsonPromptRepository
    │   │   ├── service/               # FuzzySearch, VariableParser, TemplateCompiler, JSON validation, recovery, import/export, preferences
    │   │   └── storage/               # StoragePaths, SafePathResolver
    │   ├── ui/                        # JavaFX views + MVVM-ish MainViewModel
    │   │   ├── MainView.java          # Programmatic UI (no FXML)
    │   │   ├── MainViewModel.java
    │   │   ├── OverlayStageFactory.java
    │   │   ├── AutoHideController.java
    │   │   ├── TrayIntegration.java   # AWT SystemTray
    │   │   └── clipboard/JavaFxClipboardAdapter.java
    │   └── os/JNativeHookHotkeyService.java
    └── main/resources/
        ├── default-prompts.json
        ├── dev-prompts.json
        └── styles/overlay.css | overlay-dev.css
    └── test/java/…                  # JUnit 5 + Mockito; core-heavy + MainViewModelTest
```

---

## 3. Build, run, and profiles

| Command | Meaning |
|--------|---------|
| `./gradlew run` | Default **prod** profile (`-Penv` omitted → `prompt.butler.profile=prod`) |
| `./gradlew run -Penv=dev` | **Dev:** verbose `AppLogger`, red border CSS (`overlay-dev.css`), empty store seeds from `dev-prompts.json` |
| `./gradlew test` / `./gradlew check` | Tests + **JaCoCo** gate (**≥ 80% line coverage** on **`com.viruchith.PromptButler.core`** bundle only) |

**System property:** `prompt.butler.profile` is set on the `run` task from `-Penv=`.

**OpenJFX:** `org.openjfx.javafxplugin` **0.0.14**, modules `javafx.controls`, `javafx.graphics`, version **21.0.6**.

**Application JVM args** (`applicationDefaultJvmArgs`): `-Dfile.encoding=UTF-8`, `--add-exports` / `--add-opens` for `javafx.graphics/com.sun.glass.ui` (UFT / agent compatibility if hooks slip through).

**Micro Focus UFT / `JAVA_TOOL_OPTIONS`:** The **`run`** task **`doFirst`** copies `System.getenv()`, **removes** `JAVA_TOOL_OPTIONS` and `_JAVA_OPTIONS`, and **`setEnvironment`** on the child JVM so UFT’s Java agent does not load the app process. Override with **`-PkeepUftJvmHooks=true`** if needed. Gradle’s own JVM may still print “Picked up JAVA_TOOL_OPTIONS” — that is the **Gradle** process, not necessarily the app.

**Do not** set `jvmArgs '-D…'` alone on `:run` without merging — Gradle 8 can **drop** `applicationDefaultJvmArgs` if `jvmArgs` replaces the convention (current `build.gradle` avoids that).

---

## 4. Runtime behavior (product)

### Window / overlay

- **Undecorated**, **transparent** `StageStyle.TRANSPARENT`, **always on top** (`OverlayStageFactory`).
- **Scene fill** transparent so the **rounded light panel** (`MainView` + CSS) shows as a card over the desktop.
- **UI theme:** **Light only** — `overlay.css` / `overlay-dev.css` (solid light gray/white, dark text). **Dialogs** do not inherit the main scene stylesheet; **`MainView.attachLightDialogStyles(DialogPane)`** adds `/styles/overlay.css` to every `Alert` / `Dialog` pane.

### Auto-hide (`AutoHideController` + `UserPreferences`)

- Modes: `OPACITY`, `MINIMIZE`, `TRAY`, `HIDE` (`AutoHideMode`).
- Preferences loaded/saved via `PreferencesRepository` → `preferences.json` under data dir.

### Global hotkey (`JNativeHookHotkeyService`)

- **Windows/Linux:** Ctrl+Alt+P  
- **macOS:** Cmd+Alt+P  
- Toggles show/hide + focus; implemented with **jNativeHook** (`com.github.kwhat:jnativehook:2.2.2`).

### Data / storage

- **Default data dir (all platforms):** `${user.home}/PromptButler/` (same path shape on Windows, Linux, and macOS).
- **Saved custom folder:** Toolbar **Data** writes `${user.home}/PromptButler/storage.json` (`dataDirectory`) when the user picks another folder; **restart** to load from it. **Clear** removes that pointer (unless env/property overrides apply).
- **Override precedence:** `PROMPT_BUTLER_DIR` env → `prompt.butler.dir` system property → `storage.json` → default directory above.
- **Upgrading** from older builds that used `%APPDATA%\PromptButler` or `~/.config/prompt-butler/` does not migrate automatically; copy `prompts.json` / `preferences.json` or set an env/property override.
- **Files:** `prompts.json`, `preferences.json`; **`SafePathResolver`** for child filenames under canonical base; import validates size (**≤ 10MB**) and strict JSON shape (`JsonSchemaValidator`).
- **Corruption:** `RecoveryService` + backup `prompts.json.bak`; classpath **`default-prompts.json`** for recovery / first seed.

### Dynamic variable resolution (inline form generation)

Prompts stored in JSON are often **partly dynamic**: each template’s **`body`** string may embed placeholders such as `{{code_block}}`, `{{language}}`, or `{{context}}` (allowed names: §8).

- **Trigger:** Choosing a template that contains **at least one** `{{var}}` placeholder—via **double-click** or **Enter** on the list (same path as `onTemplateChosen` after `resolveTemplateFromClick` / `Platform.runLater`)—does **not** copy the raw body. **`MainViewModel.variablesFor`** delegates to **`VariableParser`** on the **live `body` text** from the selected `PromptTemplate` (values come from the persisted library / in-memory model—the placeholders are defined **in** the JSON `body`, not in a separate per-field schema).
- **UI (same card, same light theme):** **`MainView`** opens a **modeless `Stage`** (owner = main) titled *Variables — …* with header *Fill variables for: …*, one **TextField** per variable, and **Copy — keep open** / **Copy & close**. The **main overlay stays visible** so the user can keep searching the list.
- **Compilation:** **`MainViewModel.compile`** uses **`TemplateCompiler`** to substitute each placeholder with the text entered in the matching field (blank field → empty string).
- **Actions:** **Copy & close** (default button) compiles, copies via **`ClipboardPort`**, closes the variables window, and **does not** hide the main overlay. **Copy — keep open** compiles and copies without closing the variables window. **`wireVariableEnter`:** **Enter** focuses the **next** field; **Enter** on the **last** field runs the same path as **Copy & close**.
- **Escape:** With the variables window focused, **Escape** closes it (`closeVariableParametersWindow`) and focuses search; **Escape** on the main overlay follows existing hide behavior when the variables window is not open.
- **Outcome:** The user fills values in the variables window, confirms with **Enter** or a button, and the **fully populated prompt** lands on the **system clipboard** for paste into an IDE, chat client, or ticket.

### Clipboard

- **`ClipboardPort`** in `core`; **`JavaFxClipboardAdapter`** in `ui` (JavaFX `Clipboard`); **`clearRetainedSensitiveData`** wipes an internal `char[]` buffer (not the OS clipboard).

---

## 5. UI / UX (MainView) — current behavior

**Main window:** `PromptButlerApp` builds the scene root as a **`StackPane`**: inner **`MainView`** (`VBox` with style class **`app-panel`**) plus a small **south-east resize grip** (drag to resize). **`stage.setResizable(true)`** with min size **320×360**. **`StageStyle.TRANSPARENT`** removes the OS title bar; **`MainView`** implements **drag-to-move** on the top **Prompt Butler** strip (`installUndecoratedStageDrag`). Toolbar uses **Ikonli Font Awesome 5** icons + **tooltips**.

**Toolbar (order):** New, Import, Export, **Data**, Quit (no global Edit / Delete / Copy). **Data** opens storage settings (`storage.json` pointer, directory chooser, clear pointer).

**List**

- **Fuzzy search** bound to `MainViewModel` + `FuzzySearchService` (Levenshtein on title/tags; null tag entries skipped).
- **Custom cells:** title + null-safe tag suffix; **per-row Copy** icon (does not open the detail popup; consumes click separately).
- **Single primary-click** on a row (after a short delay so it is not a double-click): opens a **modal detail `Stage`** (`WINDOW_MODAL`, owner = main) with read-only internal id, body preview, **Copy / Edit / Delete / Close** (icons + tooltips). **Edit** closes the detail window and opens the same **save dialog** as **New**.
- **New / Edit dialog:** Title, body, tags only — **no id field**. New templates use **`MainViewModel.allocateNewTemplateId()`** (random UUID, unique in library). Edits use **`replaceTemplateById`** (id unchanged).
- **Double-click:** **`resolveTemplateFromClick`**, **`Platform.runLater`** → `onTemplateChosen` (raw copy + hide if no placeholders; otherwise variable mode).
- **Enter** (list focused, not search): same as double-click (deferred `onTemplateChosen`).

**Variable window (dynamic `{{variables}}`)**

- When the chosen template’s **`body`** contains placeholders, **`MainView`** opens the **variables `Stage`** (see §4). **Escape** closes that window.

**Copy semantics**

- **Row Copy icon** / **Ctrl+C** (list focused, search not focused): copies **raw prompt body**; **does not hide** overlay; **status label** shows “Copied to clipboard.” briefly.
- **Double-click / Enter** with **no** `{{var}}` placeholders: **copy body + hide** via **`copyPlainTextThenMaybeHide(..., true)`** with **`PauseTransition`** delays.
- **Double-click / Enter** with **one or more** `{{var}}` placeholders: **variables window** (see **§4**): **`TemplateCompiler`** output to clipboard; **Copy & close** closes the variables window and copies **without** hiding the main overlay; **Copy — keep open** copies only; **Enter** advances fields and commits on the last field as in §4.

**Import:** Confirm replace-all; imported templates are wrapped with **fresh UUID ids** before **`replaceAllTemplates`**.

**Dialogs / alerts:** Light stylesheet on `DialogPane`; standard **OK/Cancel** (and error **OK**) get icon graphics where applicable.

**`Platform.setImplicitExit(false)`** — closing the main window hides it; quit via toolbar or tray.

---

## 6. Architecture rules (for future changes)

1. **`com.viruchith.PromptButler.core`** — business logic, models, JSON, paths, clipboard **interface**; **must not** import `javafx.*`.
2. **`com.viruchith.PromptButler.ui`** — JavaFX, `MainView` / `MainViewModel`, stage chrome, tray, clipboard impl.
3. **`com.viruchith.PromptButler.os`** — native hook wrapper; callbacks should **`Platform.runLater`** for UI.
4. **MVVM-ish:** `MainViewModel` holds `ObservableList` + `StringProperty` search; **`PromptRepository`** abstracts persistence.
5. **Tests:** JUnit 5 + Mockito; **JaCoCo** verifies **core** line ratio ≥ **0.80** on filtered class dirs (`afterEvaluate` `fileTree` `**/com/viruchith/PromptButler/core/**`).

---

## 7. Dependencies (production)

| Artifact | Role |
|----------|------|
| `com.google.code.gson:gson:2.10.1` | JSON |
| `com.github.kwhat:jnativehook:2.2.2` | Global hotkey |
| `org.kordamp.ikonli:ikonli-javafx:12.3.1` + `ikonli-fontawesome5-pack:12.3.1` | Toolbar / list / dialog icons |
| OpenJFX 21 (plugin) | UI |

---

## 8. JSON schema (prompts store)

- Root object: **`version`** (optional number), **`templates`** (required array).
- Each template: **`id`**, **`title`**, **`body`** (strings), **`tags`** (array of strings). **Strict:** unknown fields rejected (`JsonSchemaValidator`).
- Placeholders: **`{{varName}}`** with `varName` matching **`[a-zA-Z0-9_-]+`** (`VariableParser` / `TemplateCompiler`).

---

## 9. Known constraints & pitfalls

1. **JDK / FX:** Not a Java-8-on-classpath project; **JDK 17 + OpenJFX 21** is intentional.
2. **UFT / agents:** Stripping env on `run` + `add-opens` for Glass; document for anyone with Micro Focus UFT global JVM hooks.
3. **Headless CI:** Tests use `-Djava.awt.headless=true`; tray/hotkey not covered by integration tests in-repo.
4. **Transparent stage:** Hiding the stage during raw input events caused crashes; mitigations include **`resolveTemplateFromClick`**, **`Platform.runLater`**, and **`PauseTransition`** for copy+hide. **List cells** must not call **`String.join`** on raw tag lists (null elements → NPE when selecting a row). **Resizing:** scene root is a **`StackPane`** with a corner **resize grip**; undecorated/transparent stages may still rely on the grip for resize on some platforms.
5. **Git:** User rules: **do not commit** unless explicitly asked.

---

## 10. Suggested next steps (optional backlog)

- Persist **toolbar / UX** preferences beyond `preferences.json` if needed.
- **Installer** / jlink / jpackage (not present).
- **Deeper keyboard** focus model (e.g. explicit focus policy between search vs list).
- **Internationalization** (all strings English today).
- **E2E** or UI tests (not present; only unit tests).

---

## 11. Quick verification after checkout

```bash
./gradlew test check
./gradlew run
# or
./gradlew run -Penv=dev
```

Expect: tests green, JaCoCo verification passes, app window opens, hotkey registers (OS permitting), list / detail / import flows work, and **double-click or Enter on a template with `{{variables}}` opens the inline variable form** and copies compiled text as in §4.

---

## 12. File index (non-exhaustive but navigable)

| Area | Key files |
|------|-----------|
| Entry | `PromptButlerApp.java` |
| Core models | `core/model/*.java` |
| Persistence | `JsonPromptRepository.java`, `PromptRepository.java` |
| Search / vars | `FuzzySearchService.java`, `VariableParser.java`, `TemplateCompiler.java` |
| Safety / IO | `SafePathResolver.java`, `StoragePaths.java`, `JsonSchemaValidator.java`, `ImportExportService.java`, `RecoveryService.java` |
| UI shell | `MainView.java`, `MainViewModel.java`, `UiIcons.java`, `OverlayStageFactory.java`, `AutoHideController.java` |
| Tray | `TrayIntegration.java` (AWT) |
| Hotkey | `os/JNativeHookHotkeyService.java` |
| Theme | `src/main/resources/styles/overlay.css`, `overlay-dev.css` |
| Seeds | `default-prompts.json`, `dev-prompts.json` |

---

*End of checkpoint. Update this file when major architecture, build, or UX contracts change.*
