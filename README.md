# Prompt Butler

[![Java](https://img.shields.io/badge/Java-17%2B-437291?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-5382a1)](https://openjfx.io/)
[![Gradle](https://img.shields.io/badge/Gradle-8.7-02303a?logo=gradle&logoColor=white)](https://gradle.org/)
[![JUnit 5](https://img.shields.io/badge/tests-JUnit%205-25A162?logo=junit5&logoColor=white)](https://junit.org/junit5/)
![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey)

**Prompt Butler** is a small **JavaFX** desktop utility for **developers, writers, and power users** who reuse text snippets and **LLM / chat prompts**. It stays on top of your screen as a **lightweight overlay**: **search** fuzzy-matched **prompt templates**, fill **`{{placeholders}}`**, and **copy** the result to the system clipboard—without hunting through files or browser tabs. A **global hotkey** (Ctrl+Alt+P on Windows/Linux, Cmd+Alt+P on macOS) shows or hides the window quickly.

Templates are stored as **JSON** on disk (with optional **import/export**), UUID-based ids, and a **configurable data folder** so you can keep prompts next to projects or in a shared drive.

For maintainers and deep technical context (architecture, UFT/Java agents, UI flows), see **[`PROJECT_STATE_CHECKPOINT.md`](PROJECT_STATE_CHECKPOINT.md)**.

---

## Table of contents

- [What you can do with Prompt Butler](#what-you-can-do-with-prompt-butler)
- [Quick start (run from source)](#quick-start-run-from-source)
- [Build from source](#build-from-source)
- [Tests and coverage](#tests-and-coverage)
- [Publishing and distribution](#publishing-and-distribution)
- [Configuration and data files](#configuration-and-data-files)
- [Troubleshooting](#troubleshooting)

---

## What you can do with Prompt Butler

| Capability | Description |
|------------|-------------|
| **Fuzzy search** | Find prompts by title and tags. |
| **Variables** | Templates support `{{name}}` placeholders; fill a small form, then copy compiled text. |
| **Clipboard** | One-click or keyboard copy of prompt body or compiled output. |
| **Import / export** | JSON library for backup, sharing, or migration (import reassigns UUID ids). |
| **Tray & auto-hide** | Optional system tray and defocus/minimize behaviors (see `preferences.json`). |
| **Data folder** | Toolbar **Data** sets where `prompts.json` / `preferences.json` live (pointer under `~/PromptButler/`; restart to apply). |

---

## Quick start (run from source)

**Requirements:** **JDK 17+** and network access for the first Gradle sync (OpenJFX and other dependencies resolve from Maven Central).

Clone the repository, then from the project root:

```bash
# Windows (PowerShell or cmd)
.\gradlew.bat run

# macOS / Linux
./gradlew run
```

**Profiles**

| Command | When to use |
|---------|-------------|
| `./gradlew run` | Default **prod**-style run (`prompt.butler.profile=prod`). |
| `./gradlew run -Penv=dev` | Verbose logging, dev overlay border, seeds from `dev-prompts.json` when the store is empty. |
| `./gradlew run -PkeepUftJvmHooks=true` | Rare: keep Micro Focus **UFT** JVM hooks on the **application** process (often breaks JavaFX; default is to strip `JAVA_TOOL_OPTIONS` / `_JAVA_OPTIONS` for the child JVM only). |

You may still see “Picked up JAVA_TOOL_OPTIONS” from the **Gradle** JVM; that is usually harmless. The app process is forked **without** those variables unless you pass `-PkeepUftJvmHooks=true`.

---

## Build from source

The repo includes the **Gradle Wrapper** (8.7); you do not need a global Gradle install.

```bash
./gradlew clean classes          # compile main + test classes
./gradlew jar                    # build library JAR (not a runnable fat JAR)
./gradlew installDist            # application distribution (recommended for local installs)
```

After **`installDist`**, scripts and runtime layout are under:

- **`build/install/prompt-butler/`** (name comes from `rootProject.name` in `settings.gradle`)
- **Windows:** `build\install\prompt-butler\bin\prompt-butler.bat`
- **Unix / macOS:** `build/install/prompt-butler/bin/prompt-butler`

Copy that folder to another machine with the **same OS family** and a **JRE 17+** on `PATH`, or ship it inside an installer you create (see [Publishing](#publishing-and-distribution)).

---

## Tests and coverage

```bash
./gradlew test                   # unit tests
./gradlew check                  # tests + JaCoCo coverage gate on com.viruchith.PromptButler.core (≥ 80% lines)
./gradlew jacocoTestReport       # HTML + XML reports under build/reports/jacoco
```

---

## Publishing and distribution

There is **no** `jpackage` or `shadowJar` task in this repository by default. Practical options:

### 1. Install layout (`installDist`) — simplest

1. Run `./gradlew installDist` on the target **OS** (JavaFX native bits are platform-specific).
2. Zip **`build/install/prompt-butler/`** and attach it to a release, or copy the directory to users.
3. Ensure **JDK or JRE 17+** is installed and **`java`** is on `PATH` when using the generated scripts.

### 2. Fat JAR (optional)

To ship a single JAR you would add a plugin such as **Shadow** and merge service files; you must still document **JavaFX module path** or use a **custom runtime image** (`jlink`). For most teams, **`installDist`** or **`jpackage`** (below) is easier than a raw fat JAR.

### 3. Native installers (`jpackage`) — recommended for “real” releases

Oracle / OpenJDK **`jpackage`** can build `.msi`, `.dmg`, `.deb`, etc., from the `installDist` output or from modules. Typical steps (high level):

1. Produce a runtime with **`jlink`** that includes the `javafx.*` modules you use, **or** rely on a JDK that bundles JavaFX (rare on modern JDKs).
2. Run **`jpackage`** with `--input build/install/prompt-butler`, `--main-jar` / `--main-class`, and platform-specific options.
3. Sign binaries as required by your OS / app store pipeline.

Exact `jpackage` flags depend on your JDK vendor and CI image; add a `docs/packaging.md` or Gradle convention when you standardize this.

### 4. Maven Central / internal Artifactory

The Gradle **`maven-publish`** plugin is **not** configured here. To publish the library coordinates (`com.viruchith.promptbutler`), add `maven-publish`, signing, and your repository URL in `build.gradle`.

---

## Configuration and data files

| Mechanism | Purpose |
|-----------|---------|
| `PROMPT_BUTLER_DIR` or `-Dprompt.butler.dir=...` | Override data directory (highest precedence). |
| Toolbar **Data** | Choose JSON storage folder; writes `${user.home}/PromptButler/storage.json`; **restart** to apply. |
| Default (all platforms) | `${user.home}/PromptButler/` for `prompts.json` and `preferences.json`. |
| `preferences.json` | `autoHideMode`: `OPACITY`, `MINIMIZE`, `TRAY`, `HIDE`; `defocusOpacity` (0–1). |

**UI summary:** Row **Copy**; single-click opens a **detail** window (copy / edit / delete). **New** creates prompts (UUID ids). **Import** replaces the library. **Double-click** or **Enter** on the list runs the “choose template” flow (variables or copy-and-hide). **Ctrl+C** copies the selected body when the list is focused.

---

## Troubleshooting

- **Micro Focus UFT / `JAVA_TOOL_OPTIONS`:** The `run` task strips `JAVA_TOOL_OPTIONS` and `_JAVA_OPTIONS` from the **application** process. `applicationDefaultJvmArgs` includes `--add-exports` / `--add-opens` for Glass as a safety net if you launch without Gradle.
- **Global hotkey (jnativehook):** Some corporate machines block low-level hooks; use the window and toolbar if registration fails (errors are logged).
- **JavaFX / transparent window issues:** See **`PROJECT_STATE_CHECKPOINT.md`** for mitigations (deferred clipboard, click resolution, etc.).

---

## Repository layout (short)

- **`src/main/java/com/viruchith/PromptButler/`** — `PromptButlerApp`, `core/` (no JavaFX), `ui/`, `os/`
- **`src/main/resources/`** — default prompts, CSS, seeds
- **`build.gradle`** — Java 17 toolchain, OpenJFX 21, JaCoCo, `installDist` / `run`

---

*Prompt Butler — JavaFX prompt library, clipboard workflow, and global hotkey for faster writing and coding.*
