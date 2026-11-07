package com.viruchith.PromptButler;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

// SPDX-License-Identifier: GPL-3.0-only
/*
 * Prompt Butler — fat-JAR entry point.
 * Copyright (C) 2026 Prompt Butler contributors.
 *
 * This class intentionally does NOT extend javafx.application.Application.
 * When the Main-Class manifest entry extends Application, JavaFX's launcher
 * checks that javafx.* modules are on --module-path and refuses to start if
 * they are on the classpath instead (fat-JAR scenario). Delegating through
 * this plain class bypasses that check while still launching the real app.
 */
public final class Launcher {

    private static final String[] LINUX_NATIVE_LIBS = {
            "libprism_common.so",
            "libglass.so",
            "libglassgtk3.so",
            "libprism_es2.so",
            "libprism_sw.so",
            "libdecora_sse.so",
            "libjavafx_font.so",
            "libjavafx_font_freetype.so",
            "libjavafx_font_pango.so",
            "libjavafx_iio.so"
    };

    public static void main(String[] args) {
        initLinuxNativesIfApplicable();
        PromptButlerApp.main(args);
    }

    /**
     * On Linux, OpenJFX native libraries cannot be combined into universal Mach-O binaries.
     * To support both x86_64 and AArch64 (ARM64) architectures in a single cross-platform fat JAR,
     * architecture-segregated native libraries are packaged under {@code /native/linux-<arch>/}.
     *
     * This method detects the host CPU architecture on Linux, extracts the corresponding
     * native libraries to a local cache directory, and prepends it to {@code java.library.path}.
     * It also pre-loads them into the JVM so that JavaFX initialization finds the correct binaries.
     */
    private static void initLinuxNativesIfApplicable() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            return;
        }

        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean isArm = arch.contains("aarch64") || arch.contains("arm");
        String archDir = isArm ? "linux-aarch64" : "linux-x86_64";
        String resourcePrefix = "/native/" + archDir + "/";

        try {
            String userHome = System.getProperty("user.home", ".");
            File cacheDir = new File(userHome, ".openjfx/native/" + archDir);
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                cacheDir = new File(System.getProperty("java.io.tmpdir", "."), ".openjfx_native_" + archDir);
                cacheDir.mkdirs();
            }

            for (String libName : LINUX_NATIVE_LIBS) {
                String resPath = resourcePrefix + libName;
                try (InputStream in = Launcher.class.getResourceAsStream(resPath)) {
                    if (in != null) {
                        File targetFile = new File(cacheDir, libName);
                        // Extract if missing or if file size differs
                        if (!targetFile.exists() || targetFile.length() == 0) {
                            Path tmpPath = new File(cacheDir, libName + ".tmp").toPath();
                            Files.copy(in, tmpPath, StandardCopyOption.REPLACE_EXISTING);
                            Files.move(tmpPath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            // Update java.library.path so NativeLibLoader's fallback scans this directory
            String existingPath = System.getProperty("java.library.path", "");
            String newPath = existingPath.isEmpty()
                    ? cacheDir.getAbsolutePath()
                    : cacheDir.getAbsolutePath() + File.pathSeparator + existingPath;
            System.setProperty("java.library.path", newPath);

            // Pre-load extracted libraries in dependency order into the JVM
            for (String libName : LINUX_NATIVE_LIBS) {
                File libFile = new File(cacheDir, libName);
                if (libFile.exists()) {
                    try {
                        System.load(libFile.getAbsolutePath());
                    } catch (Throwable ignored) {
                        // Some optional pipeline libraries (e.g. prism_es2 on software-only Linux)
                        // may fail to link without GPU drivers; JavaFX gracefully falls back to sw.
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[PromptButler] Warning: Unable to initialize Linux " + archDir + " native libraries: " + t.getMessage());
        }
    }
}
