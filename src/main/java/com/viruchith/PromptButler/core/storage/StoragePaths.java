package com.viruchith.PromptButler.core.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.viruchith.PromptButler.core.logging.AppLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the application data directory (where {@code prompts.json} and {@code preferences.json} live).
 * <p>
 * Precedence (first wins):
 * </p>
 * <ol>
 *   <li>{@code PROMPT_BUTLER_DIR} environment variable</li>
 *   <li>System property {@code prompt.butler.dir}</li>
 *   <li>{@code ${user.home}/PromptButler/storage.json} pointer file} (field {@code dataDirectory})</li>
 *   <li>Default directory {@code ${user.home}/PromptButler}</li>
 * </ol>
 * The pointer file always lives under {@code ${user.home}/PromptButler/} so a custom data folder can live anywhere
 * while the app still finds the override on the next launch.
 */
public final class StoragePaths {

    private static final String DIR_NAME = "PromptButler";
    private static final String POINTER_FILE = "storage.json";

    private StoragePaths() {
    }

    /**
     * Directory that always contains {@link #getStoragePointerFile()} (may differ from the active data directory).
     */
    public static Path getBootstrapConfigRoot() {
        return Paths.get(System.getProperty("user.home"), DIR_NAME);
    }

    /**
     * JSON file storing an optional {@code dataDirectory} absolute path override.
     */
    public static Path getStoragePointerFile() {
        return getBootstrapConfigRoot().resolve(POINTER_FILE);
    }

    /**
     * Ensures the folder exists, is a directory, and is writable (probe file).
     */
    public static Path validateWritableDataDirectory(Path userSelected) throws IOException {
        Objects.requireNonNull(userSelected, "path");
        Path abs = userSelected.toAbsolutePath().normalize();
        Files.createDirectories(abs);
        if (!Files.isDirectory(abs)) {
            throw new IOException("Path is not a directory: " + abs);
        }
        Path probe = abs.resolve(".prompt-butler-write-test-" + Thread.currentThread().getId());
        try {
            Files.writeString(probe, "ok", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Folder is not writable: " + abs, e);
        } finally {
            try {
                Files.deleteIfExists(probe);
            } catch (IOException ignored) {
                AppLogger.get().warn("Could not delete probe file: " + probe);
            }
        }
        return abs;
    }

    /**
     * Writes {@link #getStoragePointerFile()} so the next JVM start uses {@code dataDirectory}.
     */
    public static void persistCustomDataDirectory(Path dataDirectory) throws IOException {
        Path validated = validateWritableDataDirectory(dataDirectory);
        Path bootstrap = getBootstrapConfigRoot();
        Files.createDirectories(bootstrap);
        Path pointer = getStoragePointerFile();
        StoragePointerDto dto = new StoragePointerDto();
        dto.dataDirectory = validated.toString();
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Files.writeString(pointer, gson.toJson(dto), StandardCharsets.UTF_8);
    }

    /**
     * Removes the pointer file so the default {@code ${user.home}/PromptButler} data directory is used
     * (unless env or system property overrides). Ignores missing file.
     */
    public static void clearPersistedCustomDataDirectory() throws IOException {
        Files.deleteIfExists(getStoragePointerFile());
    }

    public static Path resolveDataDirectory() {
        String override = firstNonEmpty(System.getenv("PROMPT_BUTLER_DIR"), System.getProperty("prompt.butler.dir"));
        if (override != null) {
            Path p = Paths.get(override).toAbsolutePath().normalize();
            try {
                if (!Files.exists(p)) {
                    Files.createDirectories(p);
                }
            } catch (IOException e) {
                AppLogger.get().error("Failed to create override data directory: " + p, e);
            }
            return p;
        }
        Optional<Path> fromFile = readPersistedDirectoryPointer();
        if (fromFile.isPresent()) {
            return fromFile.get();
        }
        Path base = Paths.get(System.getProperty("user.home"), DIR_NAME);
        try {
            if (!Files.exists(base)) {
                Files.createDirectories(base);
            }
        } catch (IOException e) {
            AppLogger.get().error("Failed to create data directory: " + base, e);
        }
        return base.normalize();
    }

    private static Optional<Path> readPersistedDirectoryPointer() {
        Path f = getStoragePointerFile();
        if (!Files.isRegularFile(f)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(f, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            StoragePointerDto dto = gson.fromJson(json, StoragePointerDto.class);
            if (dto == null || dto.dataDirectory == null || dto.dataDirectory.trim().isEmpty()) {
                return Optional.empty();
            }
            Path candidate = Paths.get(dto.dataDirectory.trim()).toAbsolutePath().normalize();
            try {
                return Optional.of(validateWritableDataDirectory(candidate));
            } catch (IOException e) {
                AppLogger.get().warn("Ignored storage pointer (not usable): " + candidate + " — " + e.getMessage());
                return Optional.empty();
            }
        } catch (Exception e) {
            AppLogger.get().warn("Could not read storage pointer file " + f + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    public static Path promptsFile(Path dataDir) {
        return Objects.requireNonNull(dataDir, "dataDir").resolve("prompts.json");
    }

    public static Path preferencesFile(Path dataDir) {
        return Objects.requireNonNull(dataDir, "dataDir").resolve("preferences.json");
    }

    private static final class StoragePointerDto {
        @SuppressWarnings("unused")
        String dataDirectory;
    }
}
