package com.viruchith.PromptButler.core.service;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitutes {@code {{name}}} placeholders using the provided map (missing keys become empty strings).
 */
public final class TemplateCompiler {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_-]+)\\}\\}");

    public String compile(String templateBody, Map<String, String> values) {
        Objects.requireNonNull(templateBody, "templateBody");
        Objects.requireNonNull(values, "values");
        Matcher m = PLACEHOLDER.matcher(templateBody);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String replacement = values.containsKey(key) ? nullToEmpty(values.get(key)) : "";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
