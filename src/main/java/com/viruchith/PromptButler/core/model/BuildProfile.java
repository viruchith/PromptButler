package com.viruchith.PromptButler.core.model;

import java.util.Locale;

/**
 * Build/runtime profile from {@code -Penv=dev|prod} passed as {@code prompt.butler.profile}.
 */
public enum BuildProfile {
    DEV,
    PROD;

    public static BuildProfile current() {
        String raw = System.getProperty("prompt.butler.profile", "prod");
        if (raw == null) {
            return PROD;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("dev".equals(v)) {
            return DEV;
        }
        return PROD;
    }

    public boolean isDev() {
        return this == DEV;
    }
}
