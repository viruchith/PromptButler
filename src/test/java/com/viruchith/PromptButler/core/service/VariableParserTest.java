package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class VariableParserTest {

    private final VariableParser parser = new VariableParser();

    static Stream<Arguments> bodiesAndExpected() {
        return Stream.of(
                Arguments.of("Hello {{name}}", Arrays.asList("name")),
                Arguments.of("{{a}} and {{b}} and {{a}}", Arrays.asList("a", "b")),
                Arguments.of("no vars", Collections.emptyList()),
                Arguments.of("{{role}} {{language}}", Arrays.asList("role", "language")),
                Arguments.of("bad {not a {{ var }}", Collections.emptyList()),
                Arguments.of("{{x1_y-2}}", Arrays.asList("x1_y-2")),
                Arguments.of("  {{name}}  ", Arrays.asList("name"))
        );
    }

    @ParameterizedTest
    @MethodSource("bodiesAndExpected")
    void parseOrderedUnique(String body, List<String> expected) {
        assertEquals(expected, parser.parseOrderedUniqueVariables(body));
    }

    @Test
    void rejectsNullBody() {
        assertThrows(NullPointerException.class, () ->
                parser.parseOrderedUniqueVariables(null));
    }

    @Test
    void containsVariables() {
        assertTrue(parser.containsVariables("{{x}}"));
    }

    /* --- Edge case tests --- */

    @Test
    void emptyStringReturnsEmptyList() {
        assertEquals(Collections.emptyList(), parser.parseOrderedUniqueVariables(""));
    }

    @Test
    void whitespaceOnlyReturnsEmptyList() {
        assertEquals(Collections.emptyList(), parser.parseOrderedUniqueVariables("   \t\n  "));
    }

    @Test
    void nestedBracesAreNotMatched() {
        // {{{name}}} — the regex should match 'name' from the inner {{ }}
        List<String> vars = parser.parseOrderedUniqueVariables("{{{name}}}");
        assertEquals(Arrays.asList("name"), vars);
    }

    @Test
    void specialCharsOutsideBracesIgnored() {
        String body = "Hello! @#$%^&*() {{var1}} more stuff {{var2}}";
        assertEquals(Arrays.asList("var1", "var2"), parser.parseOrderedUniqueVariables(body));
    }

    @Test
    void invalidVariableNameNotMatched() {
        // Spaces, dots, special chars in variable name should not match
        assertEquals(Collections.emptyList(), parser.parseOrderedUniqueVariables("{{invalid name}}"));
        assertEquals(Collections.emptyList(), parser.parseOrderedUniqueVariables("{{invalid.name}}"));
        assertEquals(Collections.emptyList(), parser.parseOrderedUniqueVariables("{{}}"));
    }

    @Test
    void veryLongBodyWithManyVariables() {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            body.append("text {{var").append(i).append("}} ");
        }
        List<String> vars = parser.parseOrderedUniqueVariables(body.toString());
        assertEquals(100, vars.size());
        assertEquals("var0", vars.get(0));
        assertEquals("var99", vars.get(99));
    }

    @Test
    void unicodeTextAroundPlaceholders() {
        String body = "こんにちは {{name}} さん、{{greeting}} を送ります";
        List<String> vars = parser.parseOrderedUniqueVariables(body);
        assertEquals(Arrays.asList("name", "greeting"), vars);
    }

    @Test
    void adjacentPlaceholders() {
        String body = "{{a}}{{b}}{{c}}";
        assertEquals(Arrays.asList("a", "b", "c"), parser.parseOrderedUniqueVariables(body));
    }

    @Test
    void placeholderWithHyphenAndUnderscore() {
        String body = "{{my-var}} {{my_var}} {{myVar123}}";
        assertEquals(Arrays.asList("my-var", "my_var", "myVar123"),
                parser.parseOrderedUniqueVariables(body));
    }

    @Test
    void containsVariablesReturnsFalseForNoVars() {
        assertFalse(parser.containsVariables("no placeholders here"));
    }

    @Test
    void preservesDeclarationOrder() {
        String body = "{{z}} {{a}} {{m}} {{z}} {{b}}";
        assertEquals(Arrays.asList("z", "a", "m", "b"), parser.parseOrderedUniqueVariables(body));
    }
}
