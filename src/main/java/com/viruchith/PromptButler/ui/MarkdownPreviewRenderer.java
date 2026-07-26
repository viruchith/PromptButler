package com.viruchith.PromptButler.ui;

final class MarkdownPreviewRenderer {

    private MarkdownPreviewRenderer() {
    }

    /**
     * Lightweight markdown renderer for prompt previews without adding extra runtime dependencies.
     */
    static String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        String text = markdown;
        text = text.replaceAll("(?m)^#{1,6}\\s*", "");
        text = text.replace("**", "");
        text = text.replace("__", "");
        text = text.replace("`", "");
        text = text.replaceAll("(?m)^\\s*[-*]\\s+", "\u2022 ");
        text = text.replaceAll("(?m)^\\s*\\d+\\.\\s+", "\u2022 ");
        return text;
    }
}
