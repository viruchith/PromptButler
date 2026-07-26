package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
