package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.util.InputText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts unique variable names from {@code {{name}}} placeholders in declaration order.
 */
public final class VariableParser {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_-]+)\\}\\}");

    public List<String> parseOrderedUniqueVariables(String templateBody) {
        String body = InputText.trimToEmpty(Objects.requireNonNull(templateBody, "templateBody"));
        if (body.isEmpty()) {
            return new ArrayList<String>();
        }
        Matcher m = PLACEHOLDER.matcher(body);
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        while (m.find()) {
            unique.add(m.group(1));
        }
        return new ArrayList<String>(unique);
    }

    public boolean containsVariables(String templateBody) {
        return !parseOrderedUniqueVariables(templateBody).isEmpty();
    }
}
