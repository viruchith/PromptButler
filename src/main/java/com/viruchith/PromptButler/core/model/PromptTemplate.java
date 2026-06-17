package com.viruchith.PromptButler.core.model;

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
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.body = Objects.requireNonNull(body, "body");
        if (tags == null) {
            this.tags = Collections.emptyList();
        } else {
            this.tags = Collections.unmodifiableList(new ArrayList<String>(tags));
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
