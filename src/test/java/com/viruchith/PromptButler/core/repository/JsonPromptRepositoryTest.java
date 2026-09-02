package com.viruchith.PromptButler.core.repository;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.service.JsonSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPromptRepositoryTest {

    private final JsonSchemaValidator validator = new JsonSchemaValidator();

    @Test
    void roundTrip(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("prompts.json");
        JsonPromptRepository repo = new JsonPromptRepository(f, validator);
        List<PromptTemplate> data = Arrays.asList(
                new PromptTemplate("a", "Title A", "body {{x}}", Arrays.asList("t1"), true, "Development", 3L, 10L,
                        Arrays.asList(new PromptTemplate.Revision("old body", 5L))),
                new PromptTemplate("b", "Title B", "plain", Arrays.asList())
        );
        repo.saveAll(data);
        List<PromptTemplate> loaded = repo.loadAll();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(p -> "Title A".equals(p.getTitle())));
        PromptTemplate a = loaded.stream().filter(p -> "a".equals(p.getId())).findFirst().orElseThrow();
        assertEquals("Development", a.getCategory());
        assertEquals(3L, a.getUsageCount());
        assertEquals(1, a.getRevisions().size());
    }

    @Test
    void loadInvalidJson(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("prompts.json");
        Files.write(f, "{".getBytes(StandardCharsets.UTF_8));
        JsonPromptRepository repo = new JsonPromptRepository(f, validator);
        assertThrows(Exception.class, repo::loadAll);
    }

    @Test
    void parseValidatedReader() throws Exception {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[],\"category\":\"General\",\"usageCount\":2,\"lastUsedEpochMillis\":11}]}";
        List<PromptTemplate> list = JsonPromptRepository.parseValidatedReader(
                new InputStreamReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8),
                validator);
        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getUsageCount());
    }

    @Test
    void roundTripPreservesUnicodeAndEmoji(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("prompts.json");
        JsonPromptRepository repo = new JsonPromptRepository(f, validator);
        PromptTemplate template = new PromptTemplate(
                "unicode",
                "தமிழ் 😀",
                "हिन्दी 中文 日本語 한국어 Русский العربية עברית ไทย Tiếng Việt 👨🏽‍💻",
                Arrays.asList("emoji ✅", "עברית"));
        repo.saveAll(Arrays.asList(template));
        List<PromptTemplate> loaded = repo.loadAll();
        assertEquals(1, loaded.size());
        assertEquals(template.getTitle(), loaded.get(0).getTitle());
        assertEquals(template.getBody(), loaded.get(0).getBody());
        assertEquals(template.getTags(), loaded.get(0).getTags());
    }

    @Test
    void roundTripPreservesRequestedLanguageSet(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("prompts.json");
        JsonPromptRepository repo = new JsonPromptRepository(f, validator);
        String multilingual = "English | Français | Deutsch | Español | Português | Italiano | Nederlands | Polski | Türkçe | Tiếng Việt | "
                + "Русский | Українська | Български | Ελληνικά | हिन्दी | தமிழ் | తెలుగు | ಕನ್ನಡ | മലയാളം | मराठी | বাংলা | ગુજરાતી | ਪੰਜਾਬੀ | "
                + "العربية | עברית | فارسی | اردو | 简体中文 | 繁體中文 | 日本語 | 한국어 | ไทย | Bahasa Indonesia | Bahasa Melayu | Kiswahili";
        PromptTemplate template = new PromptTemplate("langs", multilingual, multilingual + " ✅ 👨🏽‍💻", Arrays.asList("日本語", "العربية", "தமிழ்"));
        repo.saveAll(Arrays.asList(template));
        PromptTemplate loaded = repo.loadAll().get(0);
        assertEquals(multilingual, loaded.getTitle());
        assertEquals(multilingual + " ✅ 👨🏽‍💻", loaded.getBody());
        assertEquals(Arrays.asList("日本語", "العربية", "தமிழ்"), loaded.getTags());
    }
}
