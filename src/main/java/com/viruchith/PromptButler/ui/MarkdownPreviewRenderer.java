package com.viruchith.PromptButler.ui;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiShortcutType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

final class MarkdownPreviewRenderer {

    private static final Parser PARSER;
    private static final HtmlRenderer HTML_RENDERER;
    private static final Safelist SAFE_HTML = buildSafelist();
    private static final String LIGHT_THEME_CLASS = "pb-theme-light";
    private static final String DARK_THEME_CLASS = "pb-theme-dark";

    static {
        MutableDataSet options = new MutableDataSet();
        Collection<Extension> extensions = Arrays.<Extension>asList(
                TablesExtension.create(),
                TaskListExtension.create(),
                StrikethroughExtension.create(),
                FootnoteExtension.create(),
                DefinitionExtension.create(),
                AttributesExtension.create(),
                AbbreviationExtension.create(),
                AutolinkExtension.create(),
                EmojiExtension.create(),
                TocExtension.create(),
                TypographicExtension.create(),
                GitLabExtension.create()
        );
        options.set(Parser.EXTENSIONS, extensions);
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(EmojiExtension.USE_SHORTCUT_TYPE, EmojiShortcutType.GITHUB);
        options.set(TablesExtension.WITH_CAPTION, false);
        PARSER = Parser.builder(options).build();
        HTML_RENDERER = HtmlRenderer.builder(options).escapeHtml(false).build();
    }

    private MarkdownPreviewRenderer() {
    }

    static String render(String markdown) {
        return renderDocument(markdown, false);
    }

    static String renderDocument(String markdown, boolean darkMode) {
        String normalized = normalizeMarkdown(markdown);
        String htmlBody = normalized.isEmpty()
                ? "<p class=\"pb-empty\">Nothing to preview yet.</p>"
                : sanitizeBody(HTML_RENDERER.render(PARSER.parse(normalized)));
        htmlBody = replaceEmojiWithImages(htmlBody);
        return wrapHtml(htmlBody, darkMode, detectDirection(normalized));
    }

    private static String normalizeMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        return Normalizer.normalize(markdown, Normalizer.Form.NFC)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static String sanitizeBody(String rawHtml) {
        Document.OutputSettings settings = new Document.OutputSettings();
        settings.prettyPrint(false);
        return Jsoup.clean(rawHtml, "", SAFE_HTML, settings);
    }

    private static String replaceEmojiWithImages(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(html.length() + 32);
        for (int i = 0; i < html.length(); ) {
            int codePoint = html.codePointAt(i);
            if (isEmojiCodePoint(codePoint)) {
                out.append("<span class=\"pb-emoji\">")
                        .append(codePointToHtmlEntity(codePoint))
                        .append("</span>");
            } else {
                out.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return out.toString();
    }

    private static boolean isEmojiCodePoint(int codePoint) {
        return (codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || codePoint == 0xFE0F
                || codePoint == 0x200D;
    }

    private static String codePointToHtmlEntity(int codePoint) {
        return "&#x" + Integer.toHexString(codePoint).toUpperCase() + ";";
    }

    private static Safelist buildSafelist() {
        Safelist safe = Safelist.relaxed()
                .addTags("input", "section")
                .addAttributes(":all", "class", "lang", "dir", "data-language", "data-mermaid")
                .addAttributes("a", "target", "rel")
                .addAttributes("input", "type", "checked", "disabled")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data");
        safe.removeTags("script", "style");
        return safe;
    }

    private static String wrapHtml(String body, boolean darkMode, String direction) {
        String themeClass = darkMode ? DARK_THEME_CLASS : LIGHT_THEME_CLASS;
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\" dir=\"" + direction + "\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\" />\n"
                + "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src data: https: http:; style-src 'unsafe-inline'; script-src 'unsafe-inline'; font-src 'unsafe-inline' data:;\" />\n"
                + "<style>\n"
                + baseCss()
                + "\n</style>\n"
                + scriptBlock()
                + "</head>\n"
                + "<body class=\"" + themeClass + "\" dir=\"" + direction + "\">\n"
                + "<article class=\"markdown-body\">" + body + "</article>\n"
                + "</body>\n"
                + "</html>";
    }

    private static String detectDirection(String text) {
        if (text == null || text.isEmpty()) {
            return "auto";
        }
        for (int i = 0; i < text.length(); i++) {
            byte directionality = Character.getDirectionality(text.charAt(i));
            if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING
                    || directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE) {
                return "rtl";
            }
            if (Character.isLetterOrDigit(text.charAt(i))) {
                return "ltr";
            }
        }
        return "auto";
    }

    private static String scriptBlock() {
        return "<script>\n"
                + highlightJsSource()
                + "\n"
                + mermaidSource()
                + "\n"
                + "document.addEventListener('DOMContentLoaded', function () {\n"
                + "  if (window.hljs) {\n"
                + "    document.querySelectorAll('pre code').forEach(function (block) { window.hljs.highlightElement(block); });\n"
                + "  }\n"
                + "  if (window.mermaid) {\n"
                + "    window.mermaid.initialize({ startOnLoad: true, securityLevel: 'strict', theme: document.body.classList.contains('pb-theme-dark') ? 'dark' : 'default' });\n"
                + "    document.querySelectorAll('code.language-mermaid, pre code.language-mermaid').forEach(function (block) {\n"
                + "      var pre = block.closest('pre');\n"
                + "      var wrapper = document.createElement('div');\n"
                + "      wrapper.className = 'mermaid';\n"
                + "      wrapper.textContent = block.textContent;\n"
                + "      if (pre && pre.parentNode) { pre.parentNode.replaceChild(wrapper, pre); }\n"
                + "    });\n"
                + "    if (window.mermaid.run) { window.mermaid.run({ querySelector: '.mermaid' }); }\n"
                + "  }\n"
                + "});\n"
                + "</script>";
    }

    private static String baseCss() {
        return "body { margin: 0; padding: 12px; font-family: system-ui, -apple-system, 'Segoe UI', Roboto, 'Noto Sans', 'Nirmala UI', 'Yu Gothic UI', 'Malgun Gothic', 'Meiryo', 'Microsoft YaHei UI', sans-serif; line-height: 1.55; word-break: break-word; overflow-wrap: anywhere; text-rendering: optimizeLegibility; }\n"
                + ".markdown-body { max-width: 100%; }\n"
                + ".pb-theme-light { background: #ffffff; color: #111827; }\n"
                + ".pb-theme-dark { background: #313244; color: #cdd6f4; }\n"
                + ".emoji, .pb-emoji, img.emoji { font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', 'Noto Emoji', 'Segoe UI Symbol', emoji, sans-serif; }\n"
                + ".markdown-body, .markdown-body * { unicode-bidi: plaintext; }\n"
                + ".pb-empty { opacity: 0.75; font-style: italic; }\n"
                + "h1,h2,h3,h4,h5,h6 { margin: 0.8em 0 0.4em; line-height: 1.25; }\n"
                + "p, ul, ol, blockquote, table, pre { margin: 0.6em 0; }\n"
                + "blockquote { border-left: 4px solid rgba(125,125,125,0.35); padding-left: 12px; margin-left: 0; }\n"
                + "code, pre { font-family: 'Cascadia Code', 'Consolas', 'Courier New', monospace; font-variant-ligatures: none; }\n"
                + "pre { padding: 12px; border-radius: 8px; overflow-x: auto; background: rgba(15,23,42,0.08); }\n"
                + ".pb-theme-dark pre { background: rgba(15,23,42,0.55); }\n"
                + "table { border-collapse: collapse; width: 100%; }\n"
                + "th, td { border: 1px solid rgba(125,125,125,0.35); padding: 6px 8px; text-align: left; }\n"
                + "a { color: #2563eb; text-decoration: none; }\n"
                + ".pb-theme-dark a { color: #89b4fa; }\n"
                + "img { max-width: 100%; height: auto; }\n"
                + "img.emoji { width: 1.2em; height: 1.2em; vertical-align: -0.2em; }\n"
                + "input[type='checkbox'] { margin-right: 0.45em; }\n"
                + ".mermaid { overflow-x: auto; }\n";
    }

    private static String highlightJsSource() {
        return "window.hljs = window.hljs || { highlightElement: function () {} };";
    }

    private static String mermaidSource() {
        return "window.mermaid = window.mermaid || { initialize: function () {}, run: function () {} };";
    }
}
