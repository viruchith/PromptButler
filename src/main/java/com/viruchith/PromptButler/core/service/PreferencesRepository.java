package com.viruchith.PromptButler.core.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class PreferencesRepository {

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
            return p;
        } catch (Exception e) {
            return new UserPreferences();
        }
    }

    public void save(Path file, UserPreferences prefs) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(prefs, "prefs");
        PrefsDto dto = new PrefsDto();
        dto.autoHideMode = prefs.getAutoHideMode().name();
        dto.defocusOpacity = Double.valueOf(prefs.getDefocusOpacity());
        String json = GSON.toJson(dto);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
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
        String autoHideMode;
        Double defocusOpacity;
    }
}
