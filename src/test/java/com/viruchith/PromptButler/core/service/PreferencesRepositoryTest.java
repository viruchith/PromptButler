package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.AutoHideMode;
import com.viruchith.PromptButler.core.model.UserPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferencesRepositoryTest {

    private final PreferencesRepository repo = new PreferencesRepository();

    @Test
    void loadSaveRoundTrip(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("preferences.json");
        UserPreferences p = new UserPreferences();
        p.setAutoHideMode(AutoHideMode.MINIMIZE);
        p.setDefocusOpacity(0.25);
        repo.save(f, p);
        UserPreferences loaded = repo.loadOrDefaults(f);
        assertEquals(AutoHideMode.MINIMIZE, loaded.getAutoHideMode());
        assertEquals(0.25, loaded.getDefocusOpacity(), 0.0001);
    }
}
