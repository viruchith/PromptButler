package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSchemaValidatorTest {

    private final JsonSchemaValidator validator = new JsonSchemaValidator();

    @Test
    void acceptsValidDocument() {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[]}]}";
        validator.validatePromptStoreJson(json);
    }

    @Test
    void rejectsMissingTemplates() {
        String json = "{\"version\":1}";
        assertThrows(IllegalArgumentException.class, () -> validator.validatePromptStoreJson(json));
    }

    @Test
    void rejectsUnknownRootField() {
        String json = "{\"version\":1,\"templates\":[],\"extra\":1}";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validatePromptStoreJson(json));
        assertTrue(ex.getMessage().contains("Unknown root"));
    }

    @Test
    void rejectsUnknownTemplateField() {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[],\"x\":1}]}";
        assertThrows(IllegalArgumentException.class, () -> validator.validatePromptStoreJson(json));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(Exception.class, () -> validator.validatePromptStoreJson("{"));
    }

    @Test
    void rejectsWrongTagType() {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[1]}]}";
        assertThrows(IllegalArgumentException.class, () -> validator.validatePromptStoreJson(json));
    }
}
