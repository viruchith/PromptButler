package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataFileWatchServiceTest {

    @Test
    void triggersCallbacksForPromptsAndPreferencesChanges(@TempDir Path dir) throws Exception {
        AtomicInteger promptsEvents = new AtomicInteger(0);
        AtomicInteger prefsEvents = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        DataFileWatchService watcher = new DataFileWatchService(
                dir,
                "prompts.json",
                "preferences.json",
                () -> {
                    promptsEvents.incrementAndGet();
                    latch.countDown();
                },
                () -> {
                    prefsEvents.incrementAndGet();
                    latch.countDown();
                });

        watcher.start();
        try {
            Files.write(dir.resolve("prompts.json"), "{}".getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("preferences.json"), "{}".getBytes(StandardCharsets.UTF_8));
            assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected file watch callbacks were not triggered");
            assertTrue(promptsEvents.get() > 0, "Expected prompts callback to fire");
            assertTrue(prefsEvents.get() > 0, "Expected preferences callback to fire");
        } finally {
            watcher.stop();
        }
    }

    @Test
    void startAndStopAreSafe(@TempDir Path dir) throws Exception {
        DataFileWatchService watcher = new DataFileWatchService(
                dir,
                "prompts.json",
                "preferences.json",
                () -> {
                },
                () -> {
                });

        watcher.start();
        try {
            watcher.start();
        } finally {
            watcher.stop();
            watcher.stop();
        }
    }
}
