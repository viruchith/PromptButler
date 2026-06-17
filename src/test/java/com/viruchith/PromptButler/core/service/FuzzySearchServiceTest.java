package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuzzySearchServiceTest {

    private final FuzzySearchService svc = new FuzzySearchService();

    private static PromptTemplate t(String title, String... tags) {
        return new PromptTemplate(title.toLowerCase().replace(' ', '-'), title, "body", Arrays.asList(tags));
    }

    @Test
    void emptyQuerySortsByTitle() {
        List<PromptTemplate> list = Arrays.asList(t("Zebra"), t("Alpha"));
        List<PromptTemplate> ranked = svc.rank("", list);
        assertEquals("Alpha", ranked.get(0).getTitle());
        assertEquals("Zebra", ranked.get(1).getTitle());
    }

    @Test
    void ranksByTagMisspelling() {
        PromptTemplate a = t("ZZZ", "typescript");
        PromptTemplate b = t("AAA", "java");
        List<PromptTemplate> ranked = svc.rank("typscript", Arrays.asList(a, b));
        assertEquals(a, ranked.get(0));
    }

    @ParameterizedTest
    @CsvSource({
            "x, y, 1",
            "kitten, sitting, 3",
            ", abc, 3",
            "abc, , 3"
    })
    void levenshteinExamples(String a, String b, int expected) {
        String sa = a == null ? "" : a;
        String sb = b == null ? "" : b;
        assertEquals(expected, svc.levenshtein(sa, sb));
    }

    @Test
    void nullQueryTreatedAsEmpty() {
        List<PromptTemplate> ranked = svc.rank(null, Arrays.asList(t("B"), t("A")));
        assertEquals("A", ranked.get(0).getTitle());
    }
}
