package com.viruchith.PromptButler.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePathsFileNamesTest {

    @Test
    void promptAndPreferencesPaths(@TempDir Path dir) {
        assertEquals(dir.resolve("prompts.json"), StoragePaths.promptsFile(dir));
        assertEquals(dir.resolve("preferences.json"), StoragePaths.preferencesFile(dir));
    }
}
