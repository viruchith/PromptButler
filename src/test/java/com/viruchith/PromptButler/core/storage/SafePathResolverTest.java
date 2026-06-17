package com.viruchith.PromptButler.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafePathResolverTest {

    @Test
    void resolveChildRejectsTraversal(@TempDir Path dir) throws Exception {
        SafePathResolver r = new SafePathResolver(dir);
        assertThrows(SecurityException.class, () -> r.resolveChildFileName("../secret.txt"));
    }

    @Test
    void resolveChildRejectsSeparators(@TempDir Path dir) throws Exception {
        SafePathResolver r = new SafePathResolver(dir);
        assertThrows(SecurityException.class, () -> r.resolveChildFileName("a/b"));
    }

    @Test
    void validateImportRequiresRegularFile(@TempDir Path dir) throws Exception {
        Path p = dir.resolve("x.txt");
        Files.write(p, "hi".getBytes());
        Path ok = SafePathResolver.validateImportSource(p);
        assertTrue(Files.isRegularFile(ok));
    }
}
