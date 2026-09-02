package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateCompilerTest {

    private final TemplateCompiler compiler = new TemplateCompiler();

    // -------------------------------------------------------------------------
    // Basic substitution (no quoting)
    // -------------------------------------------------------------------------

    @Test
    void substitutesInOrder() {
        String body = "Hello {{name}} ({{name}})";
        Map<String, String> m = new HashMap<String, String>();
        m.put("name", "Ada");
        assertEquals("Hello Ada (Ada)", compiler.compile(body, m));
    }

    @Test
    void missingKeyBecomesEmpty() {
        String body = "{{a}}-{{b}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("a", "1");
        assertEquals("1-", compiler.compile(body, m));
    }

    @Test
    void unquotedValuePassedThroughAsIs() {
        String body = "x={{p}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("p", "say \"hi\"\\");
        // no quoting → raw value inserted
        assertEquals("x=say \"hi\"\\", compiler.compile(body, m));
    }

    @Test
    void rejectsNullValues() {
        assertThrows(NullPointerException.class, () -> compiler.compile("{{a}}", null));
    }

    @Test
    void rejectsNullBody() {
        assertThrows(NullPointerException.class, () -> compiler.compile(null, new HashMap<>()));
    }

    // -------------------------------------------------------------------------
    // quoteEnclosedValue — unit tests for the static helper
    // -------------------------------------------------------------------------

    @Test
    void quoteEnclosedSimpleWord() {
        assertEquals("\"hello\"", TemplateCompiler.quoteEnclosedValue("hello"));
    }

    @Test
    void quoteEnclosedEmptyString() {
        // empty input → just two double-quote characters
        assertEquals("\"\"", TemplateCompiler.quoteEnclosedValue(""));
    }

    @Test
    void quoteEnclosedNull() {
        // null treated as empty
        assertEquals("\"\"", TemplateCompiler.quoteEnclosedValue(null));
    }

    @Test
    void quoteEnclosedValueWithSpaces() {
        assertEquals("\"hello world\"", TemplateCompiler.quoteEnclosedValue("hello world"));
    }

    @Test
    void quoteEnclosedValueWithDoubleQuote() {
        // embedded " → \"
        assertEquals("\"say \\\"hello\\\"\"", TemplateCompiler.quoteEnclosedValue("say \"hello\""));
    }

    @Test
    void quoteEnclosedValueWithSingleQuote() {
        // single quote needs no escaping
        assertEquals("\"it's ok\"", TemplateCompiler.quoteEnclosedValue("it's ok"));
    }

    @Test
    void quoteEnclosedValueWithBackslash() {
        // single backslash → escaped to \\
        assertEquals("\"path\\\\to\"", TemplateCompiler.quoteEnclosedValue("path\\to"));
    }

    @Test
    void quoteEnclosedValueWithTrailingBackslash() {
        // trailing backslash must also be doubled
        assertEquals("\"trailing\\\\\"", TemplateCompiler.quoteEnclosedValue("trailing\\"));
    }

    @Test
    void quoteEnclosedValueWithConsecutiveBackslashes() {
        // two backslashes → four backslashes in output
        assertEquals("\"a\\\\\\\\b\"", TemplateCompiler.quoteEnclosedValue("a\\\\b"));
    }

    @Test
    void quoteEnclosedValueWithBackslashBeforeDoubleQuote() {
        // \" in input → \\\", i.e. backslash doubled then quote escaped
        assertEquals("\"\\\\\\\"\"", TemplateCompiler.quoteEnclosedValue("\\\""));
    }

    @Test
    void quoteEnclosedValueWithLiteralNewline() {
        // newline passes through (not escaped) — caller controls whether multi-line values are used
        assertEquals("\"line1\nline2\"", TemplateCompiler.quoteEnclosedValue("line1\nline2"));
    }

    @Test
    void quoteEnclosedValueOnlyDoubleQuotes() {
        assertEquals("\"\\\"\\\"\"", TemplateCompiler.quoteEnclosedValue("\"\""));
    }

    @Test
    void quoteEnclosedValueOnlyBackslash() {
        assertEquals("\"\\\\\"", TemplateCompiler.quoteEnclosedValue("\\"));
    }

    // -------------------------------------------------------------------------
    // compile() with quoteValues = true
    // -------------------------------------------------------------------------

    @Test
    void quotedCompilationWrapsValue() {
        String body = "greet --name {{name}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("name", "John");
        assertEquals("greet --name \"John\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationWithDoubleQuoteInValue() {
        String body = "x={{p}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("p", "say \"hi\"\\");
        assertEquals("x=\"say \\\"hi\\\"\\\\\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationMissingKeyBecomesEmptyQuoted() {
        String body = "a={{x}};b={{y}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("x", "1");
        // {{y}} is missing → becomes ""
        assertEquals("a=\"1\";b=\"\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationMultipleVarsEachWrapped() {
        String body = "{{a}} {{b}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("a", "foo");
        m.put("b", "bar");
        assertEquals("\"foo\" \"bar\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationSameVarUsedTwice() {
        String body = "{{x}}-{{x}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("x", "z");
        assertEquals("\"z\"-\"z\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationValueWithOnlyBackslash() {
        String body = "cmd={{p}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("p", "\\");
        assertEquals("cmd=\"\\\\\"", compiler.compile(body, m, true));
    }

    @Test
    void quotedCompilationValueWithSpacesAndQuotes() {
        String body = "echo {{msg}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("msg", "hello \"world\"");
        assertEquals("echo \"hello \\\"world\\\"\"", compiler.compile(body, m, true));
    }

    @Test
    void compilesRequestedLanguagesWithoutLoss() {
        String body = "{{english}}\n{{french}}\n{{russian}}\n{{hindi}}\n{{tamil}}\n{{arabic}}\n{{farsi}}\n{{chinese_simplified}}\n{{chinese_traditional}}\n{{japanese}}\n{{korean}}\n{{thai}}\n{{swahili}}";
        Map<String, String> m = new HashMap<String, String>();
        m.put("english", "English");
        m.put("french", "Français");
        m.put("russian", "Русский");
        m.put("hindi", "हिन्दी");
        m.put("tamil", "தமிழ்");
        m.put("arabic", "العربية");
        m.put("farsi", "فارسی");
        m.put("chinese_simplified", "简体中文");
        m.put("chinese_traditional", "繁體中文");
        m.put("japanese", "日本語");
        m.put("korean", "한국어");
        m.put("thai", "ไทย");
        m.put("swahili", "Kiswahili");
        assertEquals("English\nFrançais\nРусский\nहिन्दी\nதமிழ்\nالعربية\nفارسی\n简体中文\n繁體中文\n日本語\n한국어\nไทย\nKiswahili", compiler.compile(body, m));
    }
}
