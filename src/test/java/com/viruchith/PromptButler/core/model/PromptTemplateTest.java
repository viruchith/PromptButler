package com.viruchith.PromptButler.core.model;

import org.junit.jupiter.api.Test;

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
}
