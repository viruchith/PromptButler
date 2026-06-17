package com.viruchith.PromptButler.ui.clipboard;

import com.viruchith.PromptButler.core.clipboard.ClipboardPort;
import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Arrays;

public final class JavaFxClipboardAdapter implements ClipboardPort {

    private volatile char[] retained;

    @Override
    public void copyPlainText(String text) {
        final String payload = text == null ? "" : text;
        Runnable job = new Runnable() {
            @Override
            public void run() {
                ClipboardContent content = new ClipboardContent();
                content.putString(payload);
                Clipboard.getSystemClipboard().setContent(content);
            }
        };
        if (Platform.isFxApplicationThread()) {
            job.run();
        } else {
            Platform.runLater(job);
        }
        retained = payload.toCharArray();
    }

    @Override
    public void clearRetainedSensitiveData() {
        char[] s = retained;
        retained = null;
        if (s != null) {
            Arrays.fill(s, '\0');
        }
    }
}
