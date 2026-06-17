package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.util.InputText;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {{name}}} placeholders using the provided map (missing keys become empty strings).
 * Each substituted value is wrapped in ASCII double quotes for the compiled result, with {@code \}
 * and {@code "} escaped inside the value so the output stays unambiguous.
 */
public final class TemplateCompiler {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_-]+)\\}\\}");

    public String compile(String templateBody, Map<String, String> values) {
        String body = InputText.trimToEmpty(Objects.requireNonNull(templateBody, "templateBody"));
        Objects.requireNonNull(values, "values");
        Matcher m = PLACEHOLDER.matcher(body);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String raw = values.containsKey(key) ? values.get(key) : "";
            String replacement = quoteEnclosedValue(InputText.trimToEmpty(raw));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Wraps {@code raw} in a pair of {@code "} characters and escapes {@code \} and {@code "} inside {@code raw}.
     */
    static String quoteEnclosedValue(String raw) {
        String s = raw == null ? "" : raw;
        StringBuilder out = new StringBuilder(s.length() + 2);
        out.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                out.append("\\\\");
            } else if (c == '"') {
                out.append("\\\"");
            } else {
                out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }
}
