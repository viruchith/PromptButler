package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.util.InputText;

import java.util.ArrayList;
import java.util.List;

final class PromptTextFormatter {

    private PromptTextFormatter() {
    }

    static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    static String formatPromptDetailTextArea(PromptTemplate t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(nullToEmpty(t.getTitle())).append("\n");
        sb.append("Category: ").append(nullToEmpty(t.getCategory())).append("\n");
        sb.append("Tags: ").append(tagsCsvForEditor(t.getTags())).append("\n");
        sb.append("Usage count: ").append(t.getUsageCount()).append("\n");
        sb.append("Last used (epoch ms): ").append(t.getLastUsedEpochMillis()).append("\n\n");
        sb.append(nullToEmpty(t.getBody()));
        return sb.toString();
    }

    static String formatPromptMetadataSummary(PromptTemplate t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Category: ").append(nullToEmpty(t.getCategory()));
        sb.append("   Tags: ").append(tagsCsvForEditor(t.getTags()));
        sb.append("   Usage count: ").append(t.getUsageCount());
        sb.append("   Last used (epoch ms): ").append(t.getLastUsedEpochMillis());
        return sb.toString();
    }

    static String formatTagsSuffixForCell(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        ArrayList<String> safe = new ArrayList<String>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String x = tag.trim();
            if (!x.isEmpty()) {
                safe.add(x);
            }
        }
        if (safe.isEmpty()) {
            return "";
        }
        return "  [" + String.join(", ", safe) + "]";
    }

    static String tagsCsvForEditor(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        ArrayList<String> safe = new ArrayList<String>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String x = tag.trim();
            if (!x.isEmpty()) {
                safe.add(x);
            }
        }
        return String.join(", ", safe);
    }

    static List<String> parseTags(String raw) {
        ArrayList<String> out = new ArrayList<String>();
        String trimmed = InputText.trimToEmpty(raw);
        if (trimmed.isEmpty()) {
            return out;
        }
        for (String part : trimmed.split(",")) {
            String s = InputText.trimToEmpty(part);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}
