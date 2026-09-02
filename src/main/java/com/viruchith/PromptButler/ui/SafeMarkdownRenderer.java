package com.viruchith.PromptButler.ui;

import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.definition.DefinitionExtension;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SafeMarkdownRenderer {

    private static final Parser PARSER;
    private static final HtmlRenderer HTML_RENDERER;
    private static final String LIGHT_THEME_CLASS = "pb-theme-light";
    private static final String DARK_THEME_CLASS = "pb-theme-dark";
    private static final int MAX_MARKDOWN_CHARS = 100_000;
    private static final int MAX_RENDERED_HTML_CHARS = 400_000;
    private static final int MAX_TABLE_CELLS = 2_000;
    private static final int MAX_MERMAID_BLOCK_CHARS = 12_000;
    private static final int MAX_CODE_BLOCK_CHARS = 40_000;
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern SCRIPT_BLOCK = Pattern.compile("(?is)<script\\b.*?>.*?</script>");
    private static final Pattern STYLE_BLOCK = Pattern.compile("(?is)<style\\b.*?>.*?</style>");
    private static final Pattern IFRAME_BLOCK = Pattern.compile("(?is)<iframe\\b.*?>.*?</iframe>");
    private static final Pattern SVG_BLOCK = Pattern.compile("(?is)<svg\\b.*?>.*?</svg>");
    private static final Pattern EVENT_HANDLER = Pattern.compile("(?i)\\son[a-z]+\\s*=\\s*(['\"]).*?\\1");
    private static final Pattern DANGEROUS_URI = Pattern.compile("(?i)(href|src)\\s*=\\s*(['\"]?)\\s*(javascript:|data:|file:|ftp:|jar:|vbscript:)");
    private static final Pattern ANCHOR_TAG = Pattern.compile("(?is)<a\\b([^>]*)>(.*?)</a>");
    private static final Pattern ATTR_DOUBLE_QUOTED = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern ATTR_SINGLE_QUOTED = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*'([^']*)'");
    private static final Pattern ATTR_UNQUOTED = Pattern.compile("([A-Za-z_:][-A-Za-z0-9_:.]*)\\s*=\\s*([^\\s\"'>/]+)");
    private static final Set<Character> DANGEROUS_BIDI = new HashSet<Character>(Arrays.asList(
            '\u202A', '\u202B', '\u202D', '\u202E', '\u2066', '\u2067', '\u2068', '\u2069'
    ));
    private static final Set<Character> INVISIBLE_FORMAT = new HashSet<Character>(Arrays.asList(
            '\u200B', '\u200C', '\u2060', '\uFEFF'
    ));
    private static final Set<String> ALLOWED_TAGS = new LinkedHashSet<String>(Arrays.asList(
            "a", "abbr", "article", "blockquote", "br", "code", "dd", "del", "details", "div",
            "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "input", "li", "ol",
            "p", "pre", "section", "strong", "summary", "sup", "sub", "table", "tbody", "td",
            "thead", "th", "tr", "ul"
    ));
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
                TocExtension.create(),
                TypographicExtension.create(),
                GitLabExtension.create()
        );
        options.set(Parser.EXTENSIONS, extensions);
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(TablesExtension.WITH_CAPTION, false);
        PARSER = Parser.builder(options).build();
        HTML_RENDERER = HtmlRenderer.builder(options).escapeHtml(true).build();
    }

    private SafeMarkdownRenderer() {
    }

    static String renderDocument(String markdown, boolean darkMode) {
        RenderResult rendered = render(markdown);
        return wrapHtml(rendered.bodyHtml, darkMode, rendered.direction);
    }

    static String renderBodyFragment(String markdown) {
        return render(markdown).bodyHtml;
    }

    private static RenderResult render(String markdown) {
        String normalized = normalizeMarkdown(markdown);
        normalized = normalized.replaceAll("\\{\\{[^}]*\\}\\}", "{}");
        String warnings = unicodeWarnings(normalized);
        if (normalized.length() > MAX_MARKDOWN_CHARS) {
            return new RenderResult(limitExceededMarkup("Prompt is too large to preview safely.") + warnings, detectDirection(normalized));
        }
        if (normalized.isEmpty()) {
            return new RenderResult("<p class=\"pb-empty\">Nothing to preview yet.</p>" + warnings, "auto");
        }
        String rawHtml = preserveEmojiEntities(HTML_RENDERER.render(PARSER.parse(normalized)));
        String finalHtml = hardenHtmlFragment(rawHtml, normalized);
        if (finalHtml.length() > MAX_RENDERED_HTML_CHARS) {
            finalHtml = limitExceededMarkup("Rendered preview exceeds safe size limits.");
        }
        return new RenderResult(warnings + finalHtml, detectDirection(normalized));
    }

    private static String normalizeMarkdown(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        return CONTROL_CHARS.matcher(Normalizer.normalize(markdown, Normalizer.Form.NFC)
                        .replace("\r\n", "\n")
                        .replace('\r', '\n'))
                .replaceAll("");
    }

    private static String hardenHtmlFragment(String rawHtml, String normalizedMarkdown) {
        String html = rawHtml;
        html = SCRIPT_BLOCK.matcher(html).replaceAll("");
        html = STYLE_BLOCK.matcher(html).replaceAll("");
        html = IFRAME_BLOCK.matcher(html).replaceAll("");
        html = SVG_BLOCK.matcher(html).replaceAll("");
        html = EVENT_HANDLER.matcher(html).replaceAll("");
        html = DANGEROUS_URI.matcher(html).replaceAll("$1=\"#blocked\"");
        html = hardenAnchors(html);
        html = html.replaceAll("(?i)<(h[1-6])\\s+id=\"[^\"]*\"(\\s*>)", "<$1$2");
        html = html.replaceAll("(?i)<img\\b[^>]*>", "");
        html = html.replaceAll("(?i)<input\\b(?![^>]*type\\s*=\\s*\"checkbox\")[^>]*>", "<span></span>");
        html = html.replaceAll("(?i)<input\\b([^>]*?)type\\s*=\\s*\"checkbox\"([^>]*)>", "<input type=\"checkbox\" disabled=\"disabled\"$1$2>");
        html = limitLargeCodeBlocks(html);
        html = limitLargeTables(html);
        html = upgradeMermaidBlocks(html, normalizedMarkdown);
        return html;
    }

    private static String hardenAnchors(String html) {
        Matcher matcher = ANCHOR_TAG.matcher(html);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String attributes = matcher.group(1);
            String body = matcher.group(2);
            String href = extractAttribute(attributes, "href");
            String replacement;
            if (isAllowedUriScheme(href, true)) {
                replacement = "<a href=\"" + escapeHtmlAttribute(href)
                        + "\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">"
                        + body + "</a>";
            } else {
                replacement = "<span class=\"pb-unsafe-link\">" + body + "</span>";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String extractAttribute(String attributes, String name) {
        String lowered = name.toLowerCase(Locale.ROOT);
        Matcher matcher = ATTR_DOUBLE_QUOTED.matcher(attributes);
        while (matcher.find()) {
            if (matcher.group(1).toLowerCase(Locale.ROOT).equals(lowered)) {
                return matcher.group(2);
            }
        }
        matcher = ATTR_SINGLE_QUOTED.matcher(attributes);
        while (matcher.find()) {
            if (matcher.group(1).toLowerCase(Locale.ROOT).equals(lowered)) {
                return matcher.group(2);
            }
        }
        matcher = ATTR_UNQUOTED.matcher(attributes);
        while (matcher.find()) {
            if (matcher.group(1).toLowerCase(Locale.ROOT).equals(lowered)) {
                return matcher.group(2);
            }
        }
        return null;
    }

    private static String limitLargeCodeBlocks(String html) {
        if (html.length() > MAX_RENDERED_HTML_CHARS) {
            return limitExceededMarkup("Rendered preview exceeds safe size limits.");
        }
        return html;
    }

    private static String limitLargeTables(String html) {
        int cellCount = countOccurrences(html, "<td") + countOccurrences(html, "<th");
        if (cellCount > MAX_TABLE_CELLS) {
            return html.replaceAll("(?is)<table\\b.*?</table>", limitExceededMarkup("Table omitted from preview because it is too large."));
        }
        return html;
    }

    private static String upgradeMermaidBlocks(String html, String normalizedMarkdown) {
        if (!normalizedMarkdown.contains("```mermaid")) {
            return html;
        }
        int start = normalizedMarkdown.indexOf("```mermaid");
        int bodyStart = normalizedMarkdown.indexOf('\n', start);
        int end = normalizedMarkdown.indexOf("\n```", bodyStart >= 0 ? bodyStart : start);
        if (bodyStart < 0 || end <= bodyStart) {
            return html;
        }
        String mermaidSource = normalizedMarkdown.substring(bodyStart + 1, end);
        String replacement = mermaidSource.length() > MAX_MERMAID_BLOCK_CHARS
                ? "<pre class=\"pb-mermaid-blocked\">Mermaid diagram omitted from preview because it exceeds safe limits.</pre>"
                : "<div class=\"mermaid\" data-processed=\"false\">" + escapeHtml(mermaidSource) + "</div>";
        return html.replaceFirst("(?is)<pre><code class=\"language-mermaid\"[^>]*>.*?</code></pre>", replacement);
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String preserveEmojiEntities(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(html.length() + 64);
        boolean insideTag = false;
        int i = 0;
        while (i < html.length()) {
            int codePoint = html.codePointAt(i);
            if (codePoint == '<') {
                insideTag = true;
                out.append('<');
                i += Character.charCount(codePoint);
            } else if (codePoint == '>') {
                insideTag = false;
                out.append('>');
                i += Character.charCount(codePoint);
            } else if (!insideTag && shouldPreserveAsEntity(codePoint)) {
                // Collect consecutive emoji/modifier code points
                StringBuilder emojiSeq = new StringBuilder();
                int startIdx = i;
                while (i < html.length()) {
                    int cp = html.codePointAt(i);
                    if (shouldPreserveAsEntity(cp)) {
                        emojiSeq.appendCodePoint(cp);
                        i += Character.charCount(cp);
                    } else {
                        break;
                    }
                }
                out.append("<span class=\"pb-emoji\">").append(emojiSeq).append("</span>");
            } else {
                out.appendCodePoint(codePoint);
                i += Character.charCount(codePoint);
            }
        }
        return out.toString();
    }

    private static boolean shouldPreserveAsEntity(int codePoint) {
        return (codePoint >= 0x1F1E6 && codePoint <= 0x1FAFF)
                || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                || (codePoint >= 0x2300 && codePoint <= 0x23FF)
                || codePoint == 0x200D
                || codePoint == 0xFE0F
                || codePoint == 0x2B50;
    }

    private static String unicodeWarnings(String text) {
        List<String> warnings = new ArrayList<String>();
        if (containsDangerousBidi(text)) {
            warnings.add("Bidirectional override characters were detected and previewed with caution.");
        }
        if (containsInvisibleFormat(text)) {
            warnings.add("Invisible Unicode formatting characters were detected.");
        }
        if (containsMixedScriptSuspicion(text)) {
            warnings.add("Mixed-script text was detected. Review for homoglyph spoofing.");
        }
        if (warnings.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String warning : warnings) {
            out.append("<div class=\"pb-security-warning\">").append(escapeHtml(warning)).append("</div>");
        }
        return out.toString();
    }

    private static boolean containsDangerousBidi(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (DANGEROUS_BIDI.contains(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsInvisibleFormat(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (INVISIBLE_FORMAT.contains(ch)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsMixedScriptSuspicion(String text) {
        boolean latin = false;
        boolean cyrillic = false;
        boolean greek = false;
        for (int i = 0; i < text.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(text.charAt(i));
            if (script == Character.UnicodeScript.LATIN) {
                latin = true;
            } else if (script == Character.UnicodeScript.CYRILLIC) {
                cyrillic = true;
            } else if (script == Character.UnicodeScript.GREEK) {
                greek = true;
            }
        }
        int count = (latin ? 1 : 0) + (cyrillic ? 1 : 0) + (greek ? 1 : 0);
        return count > 1;
    }

    private static String onlyCodeLanguageClass(String className) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        for (String part : className.split("\\s+")) {
            if (part.startsWith("language-")) {
                String normalized = part.replaceAll("[^a-zA-Z0-9_-]", "");
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        return "";
    }

    private static boolean isAllowedUriScheme(String uri, boolean allowMailto) {
        if (uri == null) {
            return false;
        }
        String normalized = uri.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("https://")
                || normalized.startsWith("http://")
                || (allowMailto && normalized.startsWith("mailto:"));
    }

    private static boolean containsEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if ((codePoint >= 0x1F300 && codePoint <= 0x1FAFF)
                    || (codePoint >= 0x2600 && codePoint <= 0x27BF)
                    || codePoint == 0xFE0F
                    || codePoint == 0x200D) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
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

    private static String limitExceededMarkup(String message) {
        return "<div class=\"pb-limit-warning\">" + escapeHtml(message) + "</div>";
    }

    private static String escapeHtmlAttribute(String value) {
        return escapeHtml(value).replace("'", "&#39;");
    }

    private static String wrapHtml(String body, boolean darkMode, String direction) {
        String themeClass = darkMode ? DARK_THEME_CLASS : LIGHT_THEME_CLASS;
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\" dir=\"" + direction + "\">\n"
                + "<head>\n"
                + "<meta charset=\"UTF-8\" />\n"
                + "<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; img-src 'none'; style-src 'unsafe-inline'; script-src 'unsafe-inline'; font-src data:; connect-src 'none'; frame-src 'none'; media-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none';\" />\n"
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

    private static String baseCss() {
        return "body { margin: 0; padding: 12px; font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', 'Segoe UI', 'Noto Sans', 'Nirmala UI', 'Yu Gothic UI', 'Malgun Gothic', 'Meiryo', 'Microsoft YaHei UI', system-ui, -apple-system, Roboto, sans-serif; line-height: 1.55; word-break: break-word; overflow-wrap: anywhere; text-rendering: optimizeLegibility; }\n"
                + ".markdown-body { max-width: 100%; }\n"
                + ".pb-theme-light { background: #ffffff; color: #111827; }\n"
                + ".pb-theme-dark { background: #313244; color: #cdd6f4; }\n"
                + ".pb-empty { opacity: 0.75; font-style: italic; }\n"
                + ".pb-limit-warning, .pb-image-blocked, .pb-mermaid-blocked, .pb-security-warning { padding: 10px 12px; border-radius: 8px; margin: 0 0 10px; background: rgba(234, 179, 8, 0.18); color: inherit; }\n"
                + ".pb-theme-dark .pb-limit-warning, .pb-theme-dark .pb-image-blocked, .pb-theme-dark .pb-mermaid-blocked, .pb-theme-dark .pb-security-warning { background: rgba(250, 204, 21, 0.16); }\n"
                + ".emoji, img.emoji, .markdown-body { font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', 'Segoe UI', 'Noto Sans', 'Nirmala UI', 'Yu Gothic UI', 'Malgun Gothic', 'Meiryo', 'Microsoft YaHei UI', system-ui, -apple-system, Roboto, sans-serif; }\n"
                + ".markdown-body, .markdown-body * { unicode-bidi: plaintext; }\n"
                + "h1,h2,h3,h4,h5,h6 { margin: 0.8em 0 0.4em; line-height: 1.25; }\n"
                + "p, ul, ol, blockquote, table, pre { margin: 0.6em 0; }\n"
                + "blockquote { border-left: 4px solid rgba(125,125,125,0.35); padding-left: 12px; margin-left: 0; }\n"
                + "code, pre { font-family: 'Cascadia Code', 'Consolas', 'Courier New', monospace; font-variant-ligatures: none; }\n"
                + "pre { padding: 12px; border-radius: 8px; overflow-x: auto; background: rgba(15,23,42,0.08); }\n"
                + ".pb-theme-dark pre { background: rgba(15,23,42,0.55); }\n"
                + "table { border-collapse: collapse; width: 100%; }\n"
                + "th, td { border: 1px solid rgba(125,125,125,0.35); padding: 6px 8px; text-align: left; }\n"
                + "a { color: #2563eb; text-decoration: none; pointer-events: none; cursor: default; }\n"
                + ".pb-theme-dark a { color: #89b4fa; }\n"
                + ".pb-unsafe-link { text-decoration: line-through; opacity: 0.75; }\n"
                + "input[type='checkbox'] { margin-right: 0.45em; }\n"
                + ".mermaid { overflow-x: auto; }\n"
                + resourceOrEmpty("/web/markdown/highlight-light.css")
                + "\n"
                + resourceOrEmpty("/web/markdown/highlight-dark.css");
    }

    private static String scriptBlock() {
        return "<script>\n"
                + resourceOrEmpty("/web/markdown/highlight.bundle.js")
                + "\n"
                + resourceOrEmpty("/web/markdown/mermaid.min.js")
                + "\n"
                + "document.addEventListener('DOMContentLoaded', function () {\n"
                + "  if (window.hljs) {\n"
                + "    document.querySelectorAll('pre code').forEach(function (block) { window.hljs.highlightElement(block); });\n"
                + "  }\n"
                + "  if (window.mermaid) {\n"
                + "    window.mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: document.body.classList.contains('pb-theme-dark') ? 'dark' : 'default' });\n"
                + "    if (window.mermaid.run) { window.mermaid.run({ querySelector: '.mermaid' }); }\n"
                + "  }\n"
                + "});\n"
                + "</script>";
    }

    private static String resourceOrEmpty(String resourcePath) {
        try (InputStream in = SafeMarkdownRenderer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return "";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static final class RenderResult {
        private final String bodyHtml;
        private final String direction;

        private RenderResult(String bodyHtml, String direction) {
            this.bodyHtml = bodyHtml;
            this.direction = direction;
        }
    }
}
