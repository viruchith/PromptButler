package com.viruchith.PromptButler.core.model;

import com.viruchith.PromptButler.core.util.InputText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable prompt template (id, title, body with {@code {{var}}} placeholders, tags).
 */
public final class PromptTemplate {

    private final String id;
    private final String title;
    private final String body;
    private final List<String> tags;

    public PromptTemplate(String id, String title, String body, List<String> tags) {
        this.id = InputText.trimToEmpty(Objects.requireNonNull(id, "id"));
        if (this.id.isEmpty()) {
            throw new IllegalArgumentException("id cannot be blank");
        }
        this.title = InputText.trimToEmpty(Objects.requireNonNull(title, "title"));
        this.body = InputText.trimToEmpty(Objects.requireNonNull(body, "body"));
        if (tags == null) {
            this.tags = Collections.emptyList();
        } else {
            ArrayList<String> normalized = new ArrayList<String>();
            for (String t : tags) {
                if (t == null) {
                    continue;
                }
                String x = InputText.trimToEmpty(t);
                if (!x.isEmpty()) {
                    normalized.add(x);
                }
            }
            this.tags = Collections.unmodifiableList(normalized);
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public List<String> getTags() {
        return tags;
    }
}
