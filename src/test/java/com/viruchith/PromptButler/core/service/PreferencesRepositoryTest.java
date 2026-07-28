package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreferencesRepositoryTest {

    private final PreferencesRepository repo = new PreferencesRepository();

    @Test
    void loadSaveRoundTrip(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("preferences.json");
        UserPreferences p = new UserPreferences();
        p.setAutoHideMode(AutoHideMode.MINIMIZE);
        p.setDefocusOpacity(0.25);
        p.setDarkMode(true);
        p.setQuoteCompiledVariables(true);
        p.setDefaultCategory("Development");
        p.setWindowX(100);
        p.setWindowY(110);
        p.setWindowWidth(420);
        p.setWindowHeight(520);
        repo.save(f, p);
        UserPreferences loaded = repo.loadOrDefaults(f);
        assertEquals(AutoHideMode.MINIMIZE, loaded.getAutoHideMode());
        assertEquals(0.25, loaded.getDefocusOpacity(), 0.0001);
        assertTrue(loaded.isDarkMode());
        assertTrue(loaded.isQuoteCompiledVariables());
        assertEquals("Development", loaded.getDefaultCategory());
        assertTrue(loaded.hasWindowBounds());
    }

    @Test
    void savedFileContainsSchemaVersion(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("preferences.json");
        repo.save(f, new UserPreferences());
        String json = new String(Files.readAllBytes(f), StandardCharsets.UTF_8);
        assertTrue(json.contains("schemaVersion"), "saved file must include schemaVersion");
        assertTrue(json.contains(": 2") || json.contains(":2"), "schemaVersion must be 2");
    }

    /** Pre-v2 file with quoteCompiledVariables:false must be migrated to true. */
    @Test
    void migrationFromV1ResetsQuoteCompiledVariables(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("preferences.json");
        // Simulate an old preferences.json: no schemaVersion, quoteCompiledVariables explicitly false
        String oldJson = "{\"autoHideMode\":\"OPACITY\",\"defocusOpacity\":0.1,"
                + "\"darkMode\":false,\"hotkeyKeyCode\":-1,\"hotkeyModifiers\":-1,"
                + "\"quoteCompiledVariables\":false,\"defaultCategory\":\"General\"}";
        Files.write(f, oldJson.getBytes(StandardCharsets.UTF_8));

        UserPreferences loaded = repo.loadOrDefaults(f);
        assertTrue(loaded.isQuoteCompiledVariables(),
                "migration must reset quoteCompiledVariables to true for pre-v2 files");
    }

    /** V2 file with quoteCompiledVariables:false must respect the user's explicit choice. */
    @Test
    void v2FileRespectsFalseQuoteCompiledVariables(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("preferences.json");
        UserPreferences p = new UserPreferences();
        p.setQuoteCompiledVariables(false); // user explicitly opted out
        repo.save(f, p);                    // saved with schemaVersion=2

        UserPreferences loaded = repo.loadOrDefaults(f);
        assertFalse(loaded.isQuoteCompiledVariables(),
                "v2 file must preserve explicit user choice of false");
    }

    /** Missing file returns default preferences with quoteCompiledVariables=true. */
    @Test
    void missingFileReturnsDefaultsWithQuotingEnabled(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("nonexistent.json");
        UserPreferences defaults = repo.loadOrDefaults(f);
        assertTrue(defaults.isQuoteCompiledVariables());
    }
}
