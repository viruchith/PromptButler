package com.viruchith.PromptButler.core.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves paths strictly under a canonical base directory to mitigate path traversal.
 */
public final class SafePathResolver {

    private final Path canonicalBase;

    public SafePathResolver(Path base) throws IOException {
        Objects.requireNonNull(base, "base");
        File dir = base.toFile();
        if (!dir.exists()) {
            Files.createDirectories(base);
        }
        this.canonicalBase = dir.getCanonicalFile().toPath();
    }

    public Path getCanonicalBase() {
        return canonicalBase;
    }

    /**
     * Resolve a single file name (no separators) under the base directory.
     */
    public Path resolveChildFileName(String fileName) throws IOException {
        if (fileName == null || fileName.isEmpty()) {
            throw new SecurityException("fileName must be non-empty");
        }
        if (fileName.contains(File.separator) || fileName.contains("/") || fileName.contains("\\")) {
            throw new SecurityException("fileName must not contain path separators");
        }
        if (".".equals(fileName) || "..".equals(fileName)) {
            throw new SecurityException("Invalid fileName");
        }
        Path candidate = canonicalBase.resolve(fileName).normalize();
        File c = candidate.toFile();
        String basePath = canonicalBase.toFile().getCanonicalPath();
        String candPath = c.getCanonicalPath();
        if (!candPath.startsWith(basePath + File.separator) && !candPath.equals(basePath)) {
            throw new SecurityException("Resolved path escapes base directory");
        }
        return candidate;
    }

    /**
     * Validate an absolute user-selected path for import (regular file only).
     */
    public static Path validateImportSource(Path userSelected) throws IOException {
        Objects.requireNonNull(userSelected, "userSelected");
        Path abs = userSelected.toAbsolutePath().normalize();
        if (!Files.isRegularFile(abs)) {
            throw new IOException("Import path is not a regular file: " + abs);
        }
        return abs.toRealPath();
    }
}
