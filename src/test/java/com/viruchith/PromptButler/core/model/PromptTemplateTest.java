package com.viruchith.PromptButler.core.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
