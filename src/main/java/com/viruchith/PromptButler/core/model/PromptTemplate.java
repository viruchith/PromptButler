package com.viruchith.PromptButler.core.model;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.util.InputText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable prompt template (id, title, body with {@code {{var}}} placeholders, tags).
 * <p>
 * Constructor normalizes all text fields via {@link com.viruchith.PromptButler.core.util.InputText}
 * and drops blank tags so persisted and UI-derived data stay consistent.
 * </p>
 */
public final class PromptTemplate {

    public static final int MAX_HISTORY_ENTRIES = 10;

    private final String id;
    private final String title;
    private final String body;
    private final List<String> tags;
    private final boolean favorite;
    private final String category;
    private final long usageCount;
    private final long lastUsedEpochMillis;
    private final List<Revision> revisions;

    public PromptTemplate(String id, String title, String body, List<String> tags) {
        this(id, title, body, tags, false, "General", 0L, 0L, Collections.<Revision>emptyList());
    }

    public PromptTemplate(String id, String title, String body, List<String> tags, boolean favorite) {
        this(id, title, body, tags, favorite, "General", 0L, 0L, Collections.<Revision>emptyList());
    }

    public PromptTemplate(
            String id,
            String title,
            String body,
            List<String> tags,
            boolean favorite,
            String category,
            long usageCount,
            long lastUsedEpochMillis,
            List<Revision> revisions) {
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
        this.favorite = favorite;
        String normalizedCategory = InputText.trimToEmpty(category);
        this.category = normalizedCategory.isEmpty() ? "General" : normalizedCategory;
        if (usageCount < 0L) {
            throw new IllegalArgumentException("usageCount cannot be negative");
        }
        this.usageCount = usageCount;
        this.lastUsedEpochMillis = Math.max(0L, lastUsedEpochMillis);
        this.revisions = normalizeRevisions(revisions);
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

    public boolean isFavorite() {
        return favorite;
    }

    public String getCategory() {
        return category;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public long getLastUsedEpochMillis() {
        return lastUsedEpochMillis;
    }

    public List<Revision> getRevisions() {
        return revisions;
    }

    public PromptTemplate withFavorite(boolean nextFavorite) {
        return new PromptTemplate(id, title, body, tags, nextFavorite, category, usageCount, lastUsedEpochMillis, revisions);
    }

    public PromptTemplate withUsageNow(long nowEpochMillis) {
        long now = Math.max(0L, nowEpochMillis);
        return new PromptTemplate(id, title, body, tags, favorite, category, usageCount + 1L, now, revisions);
    }

    public PromptTemplate withEditedContent(String nextTitle, String nextBody, List<String> nextTags, String nextCategory) {
        String normalizedNextBody = InputText.trimToEmpty(nextBody);
        List<Revision> nextRevisions = revisions;
        if (!body.equals(normalizedNextBody)) {
            ArrayList<Revision> all = new ArrayList<Revision>();
            all.add(new Revision(body, System.currentTimeMillis()));
            all.addAll(revisions);
            if (all.size() > MAX_HISTORY_ENTRIES) {
                all = new ArrayList<Revision>(all.subList(0, MAX_HISTORY_ENTRIES));
            }
            nextRevisions = Collections.unmodifiableList(all);
        }
        return new PromptTemplate(
                id,
                nextTitle,
                normalizedNextBody,
                nextTags,
                favorite,
                nextCategory,
                usageCount,
                lastUsedEpochMillis,
                nextRevisions);
    }

    public PromptTemplate withDuplicatedId(String nextId, String nextTitle) {
        return new PromptTemplate(
                nextId,
                nextTitle,
                body,
                tags,
                favorite,
                category,
                0L,
                0L,
                Collections.<Revision>emptyList());
    }

    private static List<Revision> normalizeRevisions(List<Revision> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Revision> out = new ArrayList<Revision>();
        for (Revision revision : raw) {
            if (revision == null) {
                continue;
            }
            out.add(revision);
            if (out.size() == MAX_HISTORY_ENTRIES) {
                break;
            }
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PromptTemplate)) {
            return false;
        }
        PromptTemplate other = (PromptTemplate) obj;
        return favorite == other.favorite
                && usageCount == other.usageCount
                && lastUsedEpochMillis == other.lastUsedEpochMillis
                && Objects.equals(id, other.id)
                && Objects.equals(title, other.title)
                && Objects.equals(body, other.body)
                && Objects.equals(tags, other.tags)
                && Objects.equals(category, other.category)
                && Objects.equals(revisions, other.revisions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, body, tags, favorite, category, usageCount, lastUsedEpochMillis, revisions);
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "PromptTemplate{id='%s', title='%s', category='%s', favorite=%s, usageCount=%d}",
                id, title, category, favorite, usageCount);
    }

    public static final class Revision {
        private final String body;
        private final long updatedAtEpochMillis;

        public Revision(String body, long updatedAtEpochMillis) {
            this.body = InputText.trimToEmpty(Objects.requireNonNull(body, "body"));
            this.updatedAtEpochMillis = Math.max(0L, updatedAtEpochMillis);
        }

        public String getBody() {
            return body;
        }

        public long getUpdatedAtEpochMillis() {
            return updatedAtEpochMillis;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Revision)) {
                return false;
            }
            Revision other = (Revision) obj;
            return updatedAtEpochMillis == other.updatedAtEpochMillis
                    && Objects.equals(body, other.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(body, updatedAtEpochMillis);
        }
    }
}
