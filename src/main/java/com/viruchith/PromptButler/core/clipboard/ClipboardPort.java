package com.viruchith.PromptButler.core.clipboard;

// SPDX-License-Identifier: GPL-3.0-only

/**
 * Clipboard abstraction for testability and post-copy buffer hygiene.
 * <p>
 * Implementations may retain character arrays for wiping via {@link #clearRetainedSensitiveData()};
 * that call does <strong>not</strong> clear the OS clipboard.
 * </p>
 */
public interface ClipboardPort {

    void copyPlainText(String text);

    /**
     * Best-effort wipe of any retained copy buffers in this adapter (not the OS clipboard).
     */
    void clearRetainedSensitiveData();
}
