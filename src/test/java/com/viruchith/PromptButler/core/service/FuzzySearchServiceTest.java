package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FuzzySearchServiceTest {

    private final FuzzySearchService svc = new FuzzySearchService();

    private static PromptTemplate t(String title, String... tags) {
        return new PromptTemplate(title.toLowerCase().replace(' ', '-'), title, "body", Arrays.asList(tags));
    }

    private static PromptTemplate tFav(String title, String... tags) {
        return new PromptTemplate(title.toLowerCase().replace(' ', '-'), title, "body", Arrays.asList(tags), true);
    }

    @Test
    void emptyQuerySortsByTitle() {
        List<PromptTemplate> list = Arrays.asList(t("Zebra"), t("Alpha"));
        List<PromptTemplate> ranked = svc.rank("", list);
        assertEquals("Alpha", ranked.get(0).getTitle());
        assertEquals("Zebra", ranked.get(1).getTitle());
    }

    @Test
    void emptyQuerySortsFavoritesFirst() {
        List<PromptTemplate> list = Arrays.asList(t("Alpha"), tFav("Zebra"));
        List<PromptTemplate> ranked = svc.rank("", list);
        assertEquals("Zebra", ranked.get(0).getTitle()); // favorite first
        assertEquals("Alpha", ranked.get(1).getTitle());
    }

    @Test
    void searchSortsFavoritesFirstAtSameScore() {
        PromptTemplate favA = tFav("Hello world", "greeting");
        PromptTemplate regA = t("Hello earth", "greeting");
        List<PromptTemplate> ranked = svc.rank("Hello", Arrays.asList(regA, favA));
        // Both have similar scores; favorite should come first
        assertTrue(ranked.indexOf(favA) < ranked.indexOf(regA));
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

    /* --- Edge case tests --- */

    @Test
    void emptyTemplateListReturnsEmptyResults() {
        List<PromptTemplate> ranked = svc.rank("anything", Collections.emptyList());
        assertTrue(ranked.isEmpty());
    }

    @Test
    void unicodeCharactersInQuery() {
        PromptTemplate t = t("日本語テスト", "unicode");
        List<PromptTemplate> ranked = svc.rank("日本語", Arrays.asList(t, t("English")));
        assertEquals("日本語テスト", ranked.get(0).getTitle());
    }

    @Test
    void unicodeNormalizationMatchesEquivalentForms() {
        PromptTemplate t = t("Café", "accent");
        List<PromptTemplate> ranked = svc.rank("Cafe\u0301", Arrays.asList(t, t("Other")));
        assertEquals("Café", ranked.get(0).getTitle());
    }

    @Test
    void matchesAcrossRequestedLanguages() {
        List<PromptTemplate> templates = Arrays.asList(
                t("English prompt"),
                t("Français modèle"),
                t("Deutsch vorlage"),
                t("Español plantilla"),
                t("Português prompt"),
                t("Italiano modello"),
                t("Nederlands sjabloon"),
                t("Polski szablon"),
                t("Türkçe istem"),
                t("Tiếng Việt mẫu"),
                t("Русский шаблон"),
                t("Українська підказка"),
                t("Български шаблон"),
                t("Ελληνικά πρότυπο"),
                t("हिन्दी प्रॉम्प्ट"),
                t("தமிழ் வார்ப்புரு"),
                t("తెలుగు ప్రాంప్ట్"),
                t("ಕನ್ನಡ ಟೆಂಪ್ಲೇಟ್"),
                t("മലയാളം മാതൃക"),
                t("मराठी साचा"),
                t("বাংলা টেমপ্লেট"),
                t("ગુજરાતી નમૂનો"),
                t("ਪੰਜਾਬੀ ਟੈਂਪਲੇਟ"),
                t("العربية قالب"),
                t("עברית תבנית"),
                t("فارسی الگو"),
                t("اردو سانچہ"),
                t("简体中文 模板"),
                t("繁體中文 範本"),
                t("日本語 テンプレート"),
                t("한국어 템플릿"),
                t("ไทย เทมเพลต"),
                t("Bahasa Indonesia templat"),
                t("Bahasa Melayu templat"),
                t("Kiswahili kiolezo"));
        assertEquals("Français modèle", svc.rank("Français", templates).get(0).getTitle());
        assertEquals("Русский шаблон", svc.rank("Русский", templates).get(0).getTitle());
        assertEquals("தமிழ் வார்ப்புரு", svc.rank("தமிழ்", templates).get(0).getTitle());
        assertEquals("العربية قالب", svc.rank("العربية", templates).get(0).getTitle());
        assertEquals("日本語 テンプレート", svc.rank("日本語", templates).get(0).getTitle());
        assertEquals("ไทย เทมเพลต", svc.rank("ไทย", templates).get(0).getTitle());
        assertEquals("Kiswahili kiolezo", svc.rank("Kiswahili", templates).get(0).getTitle());
    }

    @Test
    void veryLongQueryDoesNotThrow() {
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longQuery.append("ab");
        }
        List<PromptTemplate> templates = Arrays.asList(t("Short"));
        assertDoesNotThrow(() -> svc.rank(longQuery.toString(), templates));
    }

    @Test
    void templateWithEmptyTagsList() {
        PromptTemplate t = new PromptTemplate("id-1", "Title", "body", Collections.emptyList());
        List<PromptTemplate> ranked = svc.rank("Title", Arrays.asList(t));
        assertEquals(1, ranked.size());
    }

    @Test
    void nullInTagsListIsSkipped() {
        List<String> tagsWithNull = new ArrayList<>();
        tagsWithNull.add("valid");
        tagsWithNull.add(null);
        // PromptTemplate normalizes nulls out, so just test the service doesn't fail
        PromptTemplate t = new PromptTemplate("id-1", "Title", "body", tagsWithNull);
        assertDoesNotThrow(() -> svc.rank("valid", Arrays.asList(t)));
    }

    @Test
    void levenshteinNullArgThrows() {
        assertThrows(IllegalArgumentException.class, () -> svc.levenshtein(null, "b"));
        assertThrows(IllegalArgumentException.class, () -> svc.levenshtein("a", null));
    }

    @Test
    void levenshteinIdenticalStringsReturnZero() {
        assertEquals(0, svc.levenshtein("hello", "hello"));
        assertEquals(0, svc.levenshtein("", ""));
    }

    @Test
    void querySingleCharacter() {
        List<PromptTemplate> templates = Arrays.asList(t("Apple"), t("Banana"));
        List<PromptTemplate> ranked = svc.rank("A", templates);
        // "A" is closer to "Apple" (Levenshtein on lowercased)
        assertFalse(ranked.isEmpty());
    }

    @Test
    void largeTemplateList() {
        List<PromptTemplate> templates = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            templates.add(t("Template " + i, "tag" + i));
        }
        List<PromptTemplate> ranked = svc.rank("Template 500", templates);
        assertFalse(ranked.isEmpty());
        assertEquals("Template 500", ranked.get(0).getTitle());
    }

    @Test
    void bodyContainsMatchIsConsidered() {
        PromptTemplate inBody = new PromptTemplate("id1", "Alpha", "mentions production incident", Collections.emptyList());
        PromptTemplate noMatch = new PromptTemplate("id2", "Bravo", "plain text", Collections.emptyList());
        List<PromptTemplate> ranked = svc.rank("incident", Arrays.asList(noMatch, inBody));
        assertEquals("Alpha", ranked.get(0).getTitle());
    }

    @Test
    void irrelevantTemplatesAreFilteredByCutoff() {
        PromptTemplate a = new PromptTemplate("a", "Completely unrelated", "none", Collections.emptyList());
        List<PromptTemplate> ranked = svc.rank("zzzzzzzz", Arrays.asList(a));
        assertTrue(ranked.isEmpty());
    }
}
