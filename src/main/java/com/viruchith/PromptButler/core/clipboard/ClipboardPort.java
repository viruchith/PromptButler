package com.viruchith.PromptButler.core.clipboard;

/**
 * Clipboard abstraction for testability and post-copy buffer hygiene.
 */
public interface ClipboardPort {

    void copyPlainText(String text);

    /**
     * Best-effort wipe of any retained copy buffers in this adapter (not the OS clipboard).
     */
    void clearRetainedSensitiveData();
}
