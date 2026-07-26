package com.viruchith.PromptButler.core.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplateTest {

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () ->
                new PromptTemplate(null, "t", "b", Collections.<String>emptyList()));
    }

    @Test
    void nullTagsBecomeEmpty() {
        PromptTemplate p = new PromptTemplate("i", "t", "b", null);
        assertEquals(0, p.getTags().size());
    }

    @Test
    void trimsIdTitleBodyAndTags() {
        PromptTemplate p = new PromptTemplate(
                "  id1  ",
                "  T  ",
                "  body  ",
                Arrays.asList("  a  ", "", null, "  b "));
        assertEquals("id1", p.getId());
        assertEquals("T", p.getTitle());
        assertEquals("body", p.getBody());
        assertEquals(Arrays.asList("a", "b"), p.getTags());
    }

    @Test
    void rejectsBlankIdAfterTrim() {
        assertThrows(IllegalArgumentException.class, () ->
                new PromptTemplate("   ", "t", "b", Collections.<String>emptyList()));
    }

    @Test
    void defaultFavoriteIsFalse() {
        PromptTemplate p = new PromptTemplate("id", "title", "body", Collections.emptyList());
        assertFalse(p.isFavorite());
    }

    @Test
    void favoriteConstructorSetsField() {
        PromptTemplate p = new PromptTemplate("id", "title", "body", Collections.emptyList(), true);
        assertTrue(p.isFavorite());
    }

    @Test
    void favoriteCanBeFalseExplicitly() {
        PromptTemplate p = new PromptTemplate("id", "title", "body", Collections.emptyList(), false);
        assertFalse(p.isFavorite());
    }

    @Test
    void defaultsCategoryAndUsageMetadata() {
        PromptTemplate p = new PromptTemplate("id", "title", "body", Collections.emptyList());
        assertEquals("General", p.getCategory());
        assertEquals(0L, p.getUsageCount());
        assertEquals(0L, p.getLastUsedEpochMillis());
        assertTrue(p.getRevisions().isEmpty());
    }

    @Test
    void equalsAndHashCodeMatchForEquivalentInstances() {
        PromptTemplate a = new PromptTemplate("id", "title", "body", Arrays.asList("x"), true, "Dev", 2L, 3L, Collections.emptyList());
        PromptTemplate b = new PromptTemplate("id", "title", "body", Arrays.asList("x"), true, "Dev", 2L, 3L, Collections.emptyList());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void editedBodyCapturesRevisionHistory() {
        PromptTemplate base = new PromptTemplate("id", "title", "body", Collections.emptyList());
        PromptTemplate edited = base.withEditedContent("title", "new body", Collections.emptyList(), "General");
        assertEquals(1, edited.getRevisions().size());
        assertEquals("body", edited.getRevisions().get(0).getBody());
    }

    @Test
    void duplicateResetsUsageAndHistory() {
        List<PromptTemplate.Revision> history = Collections.singletonList(new PromptTemplate.Revision("v1", 1L));
        PromptTemplate base = new PromptTemplate("id", "title", "body", Collections.emptyList(), true, "General", 8L, 12L, history);
        PromptTemplate duplicated = base.withDuplicatedId("id-2", "title copy");
        assertEquals("id-2", duplicated.getId());
        assertEquals("title copy", duplicated.getTitle());
        assertEquals(0L, duplicated.getUsageCount());
        assertEquals(0L, duplicated.getLastUsedEpochMillis());
        assertTrue(duplicated.getRevisions().isEmpty());
    }

    @Test
    void revisionEqualityAndHashCode() {
        PromptTemplate.Revision a = new PromptTemplate.Revision("body", 123L);
        PromptTemplate.Revision b = new PromptTemplate.Revision("body", 123L);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
