package com.viruchith.PromptButler.core.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoragePathsTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty("prompt.butler.dir");
    }

    private static void assumeNoEnvOverride() {
        Assumptions.assumeTrue(
                System.getenv("PROMPT_BUTLER_DIR") == null || System.getenv("PROMPT_BUTLER_DIR").isBlank());
    }

    private static void withFakeUserHome(Path fakeHome, RunnableWithIo action) throws Exception {
        String savedHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", fakeHome.toString());
            action.run();
        } finally {
            if (savedHome != null) {
                System.setProperty("user.home", savedHome);
            } else {
                System.clearProperty("user.home");
            }
        }
    }

    @FunctionalInterface
    private interface RunnableWithIo {
        void run() throws IOException;
    }

    @Test
    void defaultDataDirectoryIsUserHomePromptButler(@TempDir Path fakeHome) throws Exception {
        assumeNoEnvOverride();
        System.clearProperty("prompt.butler.dir");
        withFakeUserHome(fakeHome, () -> {
            Path resolved = StoragePaths.resolveDataDirectory();
            Path expected = fakeHome.resolve("PromptButler").toAbsolutePath().normalize();
            assertEquals(expected, resolved.toAbsolutePath().normalize());
        });
    }

    @Test
    void persistedPointerSelectsCustomDirectory(@TempDir Path fakeHome) throws Exception {
        assumeNoEnvOverride();
        System.clearProperty("prompt.butler.dir");
        withFakeUserHome(fakeHome, () -> {
            Path custom = fakeHome.resolve("external-store");
            Files.createDirectories(custom);
            StoragePaths.persistCustomDataDirectory(custom);
            assertEquals(custom.toAbsolutePath().normalize(), StoragePaths.resolveDataDirectory().toAbsolutePath().normalize());
        });
    }

    @Test
    void honorsPromptButlerDirProperty(@TempDir Path dir, @TempDir Path fakeHome) throws Exception {
        withFakeUserHome(fakeHome, () -> {
            System.setProperty("prompt.butler.dir", dir.toString());
            Path resolved = StoragePaths.resolveDataDirectory();
            assertEquals(dir.toAbsolutePath().normalize(), resolved.toAbsolutePath().normalize());
        });
    }

    @Test
    void honorsPromptButlerDirOverPersistedPointer(@TempDir Path fakeHome) throws Exception {
        assumeNoEnvOverride();
        withFakeUserHome(fakeHome, () -> {
            Path custom = fakeHome.resolve("ignored-when-prop-set");
            Files.createDirectories(custom);
            StoragePaths.persistCustomDataDirectory(custom);
            Path overrideDir = fakeHome.resolve("prop-override");
            Files.createDirectories(overrideDir);
            System.setProperty("prompt.butler.dir", overrideDir.toString());
            assertEquals(overrideDir.toAbsolutePath().normalize(), StoragePaths.resolveDataDirectory().toAbsolutePath().normalize());
        });
    }
}
