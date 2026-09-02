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
        String normalized = markdown.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder out = new StringBuilder(normalized.length() + 32);
        boolean inCodeBlock = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    inCodeBlock = false;
                    appendParagraphBreak(out);
                } else {
                    inCodeBlock = true;
                    String label = trimmed.length() > 3 ? trimmed.substring(3).trim() : "";
                    if (!label.isEmpty()) {
                        out.append("Code block (").append(label).append(")").append('\n');
                    } else {
                        out.append("Code block").append('\n');
                    }
                }
                continue;
            }
            if (inCodeBlock) {
                out.append("    ").append(line).append('\n');
                continue;
            }
            if (trimmed.isEmpty()) {
                appendParagraphBreak(out);
                continue;
            }
            String renderedLine = renderInline(trimmed);
            if (trimmed.startsWith(">")) {
                out.append("│ ").append(renderInline(trimmed.replaceFirst("^>\\s?", ""))).append('\n');
                continue;
            }
            if (trimmed.matches("^#{1,6}\\s+.*$")) {
                out.append(renderedLine.replaceFirst("^#{1,6}\\s+", "").toUpperCase()).append('\n');
                continue;
            }
            if (trimmed.matches("^\\s*[-*+]\\s+.*$")) {
                out.append("• ").append(renderInline(trimmed.replaceFirst("^[-*+]\\s+", ""))).append('\n');
                continue;
            }
            if (trimmed.matches("^\\s*\\d+\\.\\s+.*$")) {
                out.append("• ").append(renderInline(trimmed.replaceFirst("^\\d+\\.\\s+", ""))).append('\n');
                continue;
            }
            out.append(renderedLine).append('\n');
        }
        return out.toString().trim();
    }

    private static String renderInline(String text) {
        String rendered = text;
        rendered = rendered.replaceAll("!\\[[^\\]]*]\\(([^)]+)\\)", "[image: $1]");
        rendered = rendered.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1 <$2>");
        rendered = rendered.replaceAll("(?<!\\*)\\*\\*([^*]+)\\*\\*(?!\\*)", "$1");
        rendered = rendered.replaceAll("(?<!_)__([^_]+)__(?!_)", "$1");
        rendered = rendered.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "$1");
        rendered = rendered.replaceAll("(?<!_)_([^_]+)_(?!_)", "$1");
        rendered = rendered.replace("`", "");
        return rendered;
    }

    private static void appendParagraphBreak(StringBuilder out) {
        int len = out.length();
        if (len == 0) {
            return;
        }
        if (len >= 2 && out.charAt(len - 1) == '\n' && out.charAt(len - 2) == '\n') {
            return;
        }
        if (out.charAt(len - 1) != '\n') {
            out.append('\n');
        }
        out.append('\n');
    }
}
