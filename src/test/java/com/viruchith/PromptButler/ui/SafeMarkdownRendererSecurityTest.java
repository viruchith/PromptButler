package com.viruchith.PromptButler.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeMarkdownRendererSecurityTest {

    @Test
    void stripsExecutableMarkupAndInlineHandlers() {
        String html = SafeMarkdownRenderer.renderBodyFragment(
                "<script>alert(1)</script><img src=x onerror=alert(1)><div onclick=alert(1)>safe</div>");
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<div onclick"));
        assertTrue(html.contains("safe"));
    }

    @Test
    void blocksJavascriptAndDataLinks() {
        String html = SafeMarkdownRenderer.renderBodyFragment(
                "[bad](javascript:alert(1)) [data](data:text/html;base64,WA==) [ok](https://example.com)");
        assertFalse(html.contains("javascript:"));
        assertFalse(html.contains("data:text/html"));
        assertTrue(html.contains("https://example.com"));
        assertTrue(html.contains("noopener noreferrer nofollow"));
    }

    @Test
    void disablesImagesEvenForSafeSchemes() {
        String html = SafeMarkdownRenderer.renderBodyFragment("![img](https://example.com/a.png)");
        assertFalse(html.contains("<img"));
        assertTrue(html.contains("<p></p>") || html.isEmpty());
    }

    @Test
    void keepsVariablePayloadSanitizedAfterMarkdownRender() {
        String html = SafeMarkdownRenderer.renderBodyFragment("Hello {{name}}\n\n<script>alert('x')</script>");
        assertTrue(html.contains("Hello"));
        assertTrue(html.contains("{}"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    void constrainsOversizedMermaidBlocks() {
        StringBuilder source = new StringBuilder("```mermaid\nflowchart LR\n");
        for (int i = 0; i < 13000; i++) {
            source.append("A").append(i).append(" --> B").append(i).append('\n');
        }
        source.append("```");
        String html = SafeMarkdownRenderer.renderBodyFragment(source.toString());
        assertTrue(html.contains("Prompt is too large to preview safely.")
                || html.contains("Mermaid diagram omitted from preview because it exceeds safe limits."));
    }

    @Test
    void constrainsOversizedMarkdownDocuments() {
        StringBuilder markdown = new StringBuilder();
        for (int i = 0; i < 101000; i++) {
            markdown.append('x');
        }
        String html = SafeMarkdownRenderer.renderBodyFragment(markdown.toString());
        assertTrue(html.contains("Prompt is too large to preview safely."));
    }

    @Test
    void warnsOnUnicodeSecurityMarkers() {
        String html = SafeMarkdownRenderer.renderBodyFragment("PayPal \u202Eabc\u200B Раураl");
        assertTrue(html.contains("Bidirectional override characters were detected"));
        assertTrue(html.contains("Invisible Unicode formatting characters were detected"));
        assertTrue(html.contains("Mixed-script text was detected"));
    }

    @Test
    void doesNotWarnForLegitimateEmojiJoiners() {
        String html = SafeMarkdownRenderer.renderBodyFragment("👨🏽‍💻 👩🏾‍💻 👨‍👩‍👧‍👦");
        assertFalse(html.contains("Invisible Unicode formatting characters were detected"));
    }
}
