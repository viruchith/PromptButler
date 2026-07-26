# Changelog

All notable changes to Prompt Butler are documented in this file.

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
- Template compile substitution defaults to unquoted values; optional quoted substitution via preference (`quoteCompiledVariables`).
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
