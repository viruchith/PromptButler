package com.viruchith.PromptButler.core.util;

/**
 * Normalizes raw UI / file input before business logic runs.
 */
public final class InputText {

    private InputText() {
    }

    /** Null-safe trim; never returns null. */
    public static String trimToEmpty(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
