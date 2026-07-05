package com.viruchith.PromptButler;

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
    public static void main(String[] args) {
        PromptButlerApp.main(args);
    }
}
