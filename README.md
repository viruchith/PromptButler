# Prompt Butler

Lightweight JavaFX overlay for searching prompt templates, filling `{{variables}}`, and copying to the clipboard. Global hotkey: **Ctrl+Alt+P** (Windows/Linux) or **Cmd+Alt+P** (macOS).

For a **full handoff** (architecture, build quirks, UFT, UI behavior), see **[`PROJECT_STATE_CHECKPOINT.md`](PROJECT_STATE_CHECKPOINT.md)**.

## Requirements

- **JDK 17+** (toolchain) — Maven-hosted **OpenJFX** targets bytecode newer than Java 8; core code avoids Java 9+ APIs where practical.
- Gradle 8.7 (wrapper included).
- UI is **light theme only** (solid light gray/white surfaces, dark text) for readability over the transparent stage chrome.

## Run

```bash
./gradlew run                    # production-style profile (default)
./gradlew run -Penv=dev          # verbose logs + dev overlay border + seed from dev-prompts.json when store is empty
./gradlew run -PkeepUftJvmHooks=true   # rare: keep UFT JVM agents on the app process (not recommended for JavaFX)
```

The **`run`** task starts Prompt Butler in a child JVM **without** `JAVA_TOOL_OPTIONS` / `_JAVA_OPTIONS`, so Micro Focus UFT’s Java agent is not loaded and JavaFX stays stable. You may still see “Picked up JAVA_TOOL_OPTIONS” from the **Gradle** JVM; that line is harmless. Use `-PkeepUftJvmHooks=true` only if you intentionally need UFT on the application process.

## Configuration

| Mechanism | Purpose |
|-----------|---------|
| `PROMPT_BUTLER_DIR` or `-Dprompt.butler.dir=...` | Override data directory (highest precedence) |
| Toolbar **Data** | Choose JSON storage folder; writes `${user.home}/PromptButler/storage.json`; restart to apply |
| Default (all platforms) | `~/PromptButler/` (i.e. `${user.home}/PromptButler/`) |
| `preferences.json` | `autoHideMode`: `OPACITY`, `MINIMIZE`, `TRAY`, `HIDE`; `defocusOpacity` (0–1) |

## Build & tests

```bash
./gradlew test check             # unit tests + JaCoCo ≥ 80% line coverage on `com.viruchith.PromptButler.core`
./gradlew jacocoTestReport
```

## Notes

- **Micro Focus UFT / JAVA_TOOL_OPTIONS:** The `run` task strips `JAVA_TOOL_OPTIONS` and `_JAVA_OPTIONS` from the **application** process so UFT’s agent is not loaded. `applicationDefaultJvmArgs` still includes `--add-exports` / `--add-opens` for `javafx.graphics/com.sun.glass.ui` as a safety net if you launch the app another way while those variables are set.
- **jnativehook** may require elevated permissions on some systems if OS security blocks global listeners.
- **Library editing:** Each list row has a **Copy** icon. **Single-click** a prompt (not the row copy button) opens a **modal detail window** (read-only id, Copy / Edit / Delete / Close). **New** on the toolbar opens the editor (title/body/tags only; **new ids are random UUIDs**). **Import** replaces the library and **reassigns UUID ids** to every imported template. **Double-click** or **Enter** (list focused) still runs **choose template** (variable form or copy-and-hide). **Ctrl+C** with the list focused copies the selected body. Icons + tooltips are on toolbar, list, popup, variable actions, and common alert buttons.
- Quit from the **Quit** button or the tray menu (**TRAY** auto-hide). Closing the window hides it (`Platform.setImplicitExit(false)`).
