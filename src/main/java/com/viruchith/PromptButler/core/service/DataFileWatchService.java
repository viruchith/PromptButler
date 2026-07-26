package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.logging.AppLogger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;

/**
 * Watches the app data directory and triggers callbacks when prompts or preferences files change.
 */
public final class DataFileWatchService {

    private final Path dataDirectory;
    private final String promptsFileName;
    private final String preferencesFileName;
    private final Runnable onPromptsChanged;
    private final Runnable onPreferencesChanged;

    private WatchService watchService;
    private Thread watcherThread;
    private volatile boolean running;

    public DataFileWatchService(
            Path dataDirectory,
            String promptsFileName,
            String preferencesFileName,
            Runnable onPromptsChanged,
            Runnable onPreferencesChanged) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.promptsFileName = Objects.requireNonNull(promptsFileName, "promptsFileName");
        this.preferencesFileName = Objects.requireNonNull(preferencesFileName, "preferencesFileName");
        this.onPromptsChanged = Objects.requireNonNull(onPromptsChanged, "onPromptsChanged");
        this.onPreferencesChanged = Objects.requireNonNull(onPreferencesChanged, "onPreferencesChanged");
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        watchService = FileSystems.getDefault().newWatchService();
        dataDirectory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        running = true;
        watcherThread = new Thread(this::runWatchLoop, "prompt-butler-data-watch");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    private void runWatchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    Object context = event.context();
                    if (!(context instanceof Path)) {
                        continue;
                    }
                    String changed = ((Path) context).getFileName().toString();
                    if (promptsFileName.equals(changed)) {
                        onPromptsChanged.run();
                    } else if (preferencesFileName.equals(changed)) {
                        onPreferencesChanged.run();
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                AppLogger.get().warn("Data watch loop error: " + e.getMessage());
            }
        }
    }

    public synchronized void stop() {
        running = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                AppLogger.get().warn("Could not close watch service: " + e.getMessage());
            }
        }
    }
}
