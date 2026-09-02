package com.viruchith.PromptButler.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownPreviewRendererTest {

    @Test
    void rendersGithubFlavoredMarkdownFeatures() {
        String markdown = "# Title\n\n- [x] Done\n- [ ] Todo\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\n```java\nSystem.out.println(\"hi\");\n```";
        String html = MarkdownPreviewRenderer.renderDocument(markdown, false);
        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("type=\"checkbox\""));
        assertTrue(html.contains("language-java"));
    }

    @Test
    void preservesUnicodeAndEmojiContent() {
        String markdown = "தமிழ் हिन्दी 中文 日本語 한국어 Русский العربية עברית ไทย Tiếng Việt ✅ 👨🏽‍💻";
        String html = MarkdownPreviewRenderer.renderDocument(markdown, false);
        assertTrue(html.contains("தமிழ்"));
        assertTrue(html.contains("العربية"));
        assertTrue(html.contains("pb-emoji"));
        assertTrue(html.contains("Segoe UI Emoji"));
    }

    @Test
    void stripsExecutableScripts() {
        String html = MarkdownPreviewRenderer.renderDocument("<script>alert('xss')</script><div>safe</div>", false);
        assertFalse(html.contains("<script>alert('xss')</script>"));
        assertTrue(html.contains("<div>safe</div>"));
    }

    @Test
    void detectsRtlDocumentDirection() {
        String html = MarkdownPreviewRenderer.renderDocument("العربية עברית اردو", false);
        assertTrue(html.contains("dir=\"rtl\""));
    }

    @Test
    void supportsRequestedLanguageCoverageInPreview() {
        String markdown = "English\nFrançais\nDeutsch\nEspañol\nPortuguês\nItaliano\nNederlands\nPolski\nTürkçe\nTiếng Việt\n"
                + "Русский\nУкраїнська\nБългарски\nΕλληνικά\n"
                + "हिन्दी\nதமிழ்\nతెలుగు\nಕನ್ನಡ\nമലയാളം\nमराठी\nবাংলা\nગુજરાતી\nਪੰਜਾਬੀ\n"
                + "العربية\nעברית\nفارسی\nاردو\n"
                + "简体中文\n繁體中文\n日本語\n한국어\nไทย\n"
                + "Bahasa Indonesia\nBahasa Melayu\nKiswahili";
        String html = MarkdownPreviewRenderer.renderDocument(markdown, false);
        assertTrue(html.contains("English"));
        assertTrue(html.contains("Français"));
        assertTrue(html.contains("Türkçe"));
        assertTrue(html.contains("Русский"));
        assertTrue(html.contains("Ελληνικά"));
        assertTrue(html.contains("தமிழ்"));
        assertTrue(html.contains("ગુજરાતી"));
        assertTrue(html.contains("فارسی"));
        assertTrue(html.contains("简体中文"));
        assertTrue(html.contains("繁體中文"));
        assertTrue(html.contains("日本語"));
        assertTrue(html.contains("한국어"));
        assertTrue(html.contains("ไทย"));
        assertTrue(html.contains("Kiswahili"));
    }
}
