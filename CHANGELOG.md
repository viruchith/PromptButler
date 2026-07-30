# Changelog

All notable changes to Prompt Butler are documented in this file.

## 0.4.2-SNAPSHOT

### Fixed

- Startup now ignores obviously invalid persisted window bounds (non-finite/negative/extreme values) and automatically recenters the window, preventing unusable launches after monitor layout or DPI changes.
- Auto-hide now defers defocus handling until JavaFX focus settles, preventing owned app windows (for example the variable-entry window) from unintentionally triggering main-window minimize/hide behavior when switching focus.

### Tests

- Added `PromptButlerAppBoundsTest` coverage for persisted-window-bounds sanity checks, including invalid finite checks and extreme-value detection.

## 0.4.1-SNAPSHOT

### Fixed

- Variable values are now wrapped in double quotes by default (`quoteCompiledVariables` defaults to `true`). Previously they were inserted as raw text unless the setting was explicitly enabled in Settings.
- Existing `preferences.json` files with `quoteCompiledVariables: false` are now automatically migrated to `true` on first load (schema v2 migration). User opt-out via Settings is still respected in newly saved files.
- Window position is now clamped to the union of all connected screen visual bounds on startup. Moving the window fully off-screen no longer causes it to restore to an inaccessible position — at least 80 px of the window is always kept within visible area.
- `preferences.json` save no longer throws when window bounds have not been set yet (NaN values are skipped instead of passed to Gson).

### Tests

- Expanded `TemplateCompilerTest` with comprehensive escape-scenario coverage: empty/null values, single quotes, double quotes, backslash, trailing backslash, consecutive backslashes, backslash-before-quote, literal newlines, multiple variables, and missing-key cases.
- Added `PreferencesRepositoryTest` cases for schema version persistence, legacy file migration, explicit opt-out round-trip, and missing-file defaults.

## 0.4.0-SNAPSHOT

### Added

- Category support with category filter, category dropdown selection in New/Edit dialogs, and in-dialog **New Category** creation.
- Category deletion flow in Settings; deleting a category reassigns affected prompts to **General**.
- Prompt duplicate action and undo-delete action.
- Drag-and-drop JSON import.
- Selected-row export (falls back to full export when nothing is selected).
- Shortcut help dialog (**F1**).
- Markdown-style rendered preview in prompt details.
- Runtime file watching and reload for `prompts.json` and `preferences.json`.
- Window position/size persistence in preferences.
- Prompt metadata fields: `usageCount`, `lastUsedEpochMillis`, and bounded `revisions`.

### Changed

- Search now uses tiered ranking (prefix/contains/title+tags+body) with bounded fuzzy fallback and relevance cutoffs.
- Template compile substitution supports optional quote wrapping of substituted values via the `quoteCompiledVariables` preference (see 0.4.1 for default change).
- Preferences model expanded with `defaultCategory`, quote behavior, and persisted window bounds.
- Toolbar updated with Shortcuts, Settings, and Undo Delete actions.

### Reliability

- Atomic writes for `prompts.json` and `preferences.json`.
- Import ID remapping centralized in `ImportExportService`.

### Fixed

- Settings/dialog focus transitions in auto-hide tray mode no longer hide the main app unexpectedly.
- Main category dropdown selection now applies immediately and remains selected.
- Dark-mode icon glyph visibility improved (theme-driven white icon color).
- Initial window size now derives minimum dimensions from content, preventing compressed toolbar buttons.
- About dialog fallback version updated to the current app release line.

### Tests

- Expanded tests for PromptTemplate metadata/revisions/equality.
- Expanded tests for schema/repository handling of new fields.
- Expanded tests for search behavior and compile modes.
- Added/expanded MainViewModel tests for category filtering, usage tracking, and category deletion reassignment.

## 0.2.0-SNAPSHOT

- Initial public version: overlay window, fuzzy search, `{{variable}}` fill-in, import/export, global hotkey, tray integration.
