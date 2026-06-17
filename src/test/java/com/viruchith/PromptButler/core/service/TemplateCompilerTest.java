package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateCompilerTest {

    private final TemplateCompiler compiler = new TemplateCompiler();

    @Test
    void substitutesInOrder() {
        String body = "Hello {{name}} ({{name}})";
        java.util.HashMap<String, String> m = new java.util.HashMap<String, String>();
        m.put("name", "Ada");
        assertEquals("Hello Ada (Ada)", compiler.compile(body, m));
    }

    @Test
    void missingKeyBecomesEmpty() {
        String body = "{{a}}-{{b}}";
        java.util.HashMap<String, String> m = new java.util.HashMap<String, String>();
        m.put("a", "1");
        assertEquals("1-", compiler.compile(body, m));
    }

    @Test
    void rejectsNullCompilerArgs() {
        assertThrows(NullPointerException.class, () -> compiler.compile("{{a}}", null));
    }
}
