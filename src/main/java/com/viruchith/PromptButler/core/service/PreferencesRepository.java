package com.viruchith.PromptButler.core.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

public final class PreferencesRepository {

    /** Files written with schemaVersion >= CURRENT_SCHEMA_VERSION honour explicit field values.
     *  Older files (no schemaVersion) trigger migrations that reset fields whose defaults changed. */
    private static final int CURRENT_SCHEMA_VERSION = 2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public UserPreferences loadOrDefaults(Path file) {
        if (!Files.isRegularFile(file)) {
            return new UserPreferences();
        }
        try {
            long sz = Files.size(file);
            if (sz > JsonSchemaValidator.MAX_IMPORT_BYTES) {
                return new UserPreferences();
            }
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            PrefsDto dto = GSON.fromJson(json, PrefsDto.class);
            if (dto == null) {
                return new UserPreferences();
            }
            UserPreferences p = new UserPreferences();
            if (dto.autoHideMode != null) {
                p.setAutoHideMode(parseMode(dto.autoHideMode));
            }
            if (dto.defocusOpacity != null) {
                p.setDefocusOpacity(dto.defocusOpacity.doubleValue());
            }
            if (dto.darkMode != null) {
                p.setDarkMode(dto.darkMode.booleanValue());
            }
            if (dto.hotkeyKeyCode != null) {
                p.setHotkeyKeyCode(dto.hotkeyKeyCode.intValue());
            }
            if (dto.hotkeyModifiers != null) {
                p.setHotkeyModifiers(dto.hotkeyModifiers.intValue());
            }
            if (dto.quoteCompiledVariables != null) {
                p.setQuoteCompiledVariables(dto.quoteCompiledVariables.booleanValue());
            }
            // Migration: files written before schema v2 had quoteCompiledVariables defaulting to
            // false; reset to true (the new default) so existing installations get the correct
            // out-of-the-box behaviour.
            int schemaVersion = (dto.schemaVersion != null) ? dto.schemaVersion.intValue() : 0;
            if (schemaVersion < CURRENT_SCHEMA_VERSION) {
                p.setQuoteCompiledVariables(true);
            }
            if (dto.defaultCategory != null) {
                p.setDefaultCategory(dto.defaultCategory);
            }
            if (dto.windowX != null) {
                p.setWindowX(dto.windowX.doubleValue());
            }
            if (dto.windowY != null) {
                p.setWindowY(dto.windowY.doubleValue());
            }
            if (dto.windowWidth != null) {
                p.setWindowWidth(dto.windowWidth.doubleValue());
            }
            if (dto.windowHeight != null) {
                p.setWindowHeight(dto.windowHeight.doubleValue());
            }
            return p;
        } catch (Exception e) {
            return new UserPreferences();
        }
    }

    public void save(Path file, UserPreferences prefs) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(prefs, "prefs");
        PrefsDto dto = new PrefsDto();
        dto.schemaVersion = Integer.valueOf(CURRENT_SCHEMA_VERSION);
        dto.autoHideMode = prefs.getAutoHideMode().name();
        dto.defocusOpacity = Double.valueOf(prefs.getDefocusOpacity());
        dto.darkMode = Boolean.valueOf(prefs.isDarkMode());
        dto.hotkeyKeyCode = Integer.valueOf(prefs.getHotkeyKeyCode());
        dto.hotkeyModifiers = Integer.valueOf(prefs.getHotkeyModifiers());
        dto.quoteCompiledVariables = Boolean.valueOf(prefs.isQuoteCompiledVariables());
        dto.defaultCategory = prefs.getDefaultCategory();
        dto.windowX = prefs.hasWindowBounds() ? Double.valueOf(prefs.getWindowX()) : null;
        dto.windowY = prefs.hasWindowBounds() ? Double.valueOf(prefs.getWindowY()) : null;
        dto.windowWidth = prefs.hasWindowBounds() ? Double.valueOf(prefs.getWindowWidth()) : null;
        dto.windowHeight = prefs.hasWindowBounds() ? Double.valueOf(prefs.getWindowHeight()) : null;
        String json = GSON.toJson(dto);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.write(tmp, json.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static AutoHideMode parseMode(String raw) {
        try {
            return AutoHideMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return AutoHideMode.OPACITY;
        }
    }

    @SuppressWarnings("unused")
    private static final class PrefsDto {
        Integer schemaVersion;
        String autoHideMode;
        Double defocusOpacity;
        Boolean darkMode;
        Integer hotkeyKeyCode;
        Integer hotkeyModifiers;
        Boolean quoteCompiledVariables;
        String defaultCategory;
        Double windowX;
        Double windowY;
        Double windowWidth;
        Double windowHeight;
    }
}
