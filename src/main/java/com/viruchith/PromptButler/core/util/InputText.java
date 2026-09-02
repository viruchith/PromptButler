package com.viruchith.PromptButler.core.util;

// SPDX-License-Identifier: GPL-3.0-only

import java.text.Normalizer;

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
        return normalizeUnicode(s).trim();
    }

    public static String normalizeUnicode(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }
}
