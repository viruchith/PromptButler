package com.viruchith.PromptButler.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableParserTest {

    private final VariableParser parser = new VariableParser();

    static Stream<Arguments> bodiesAndExpected() {
        return Stream.of(
                Arguments.of("Hello {{name}}", Arrays.asList("name")),
                Arguments.of("{{a}} and {{b}} and {{a}}", Arrays.asList("a", "b")),
                Arguments.of("no vars", Collections.emptyList()),
                Arguments.of("{{role}} {{language}}", Arrays.asList("role", "language")),
                Arguments.of("bad {not a {{ var }}", Collections.emptyList()),
                Arguments.of("{{x1_y-2}}", Arrays.asList("x1_y-2"))
        );
    }

    @ParameterizedTest
    @MethodSource("bodiesAndExpected")
    void parseOrderedUnique(String body, List<String> expected) {
        assertEquals(expected, parser.parseOrderedUniqueVariables(body));
    }

    @Test
    void rejectsNullBody() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                parser.parseOrderedUniqueVariables(null));
    }

    @Test
    void containsVariables() {
        assertTrue(parser.containsVariables("{{x}}"));
    }
}
