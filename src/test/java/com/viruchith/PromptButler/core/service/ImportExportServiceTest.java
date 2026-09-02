package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportExportServiceTest {

    private final JsonSchemaValidator validator = new JsonSchemaValidator();
    private final ImportExportService service = new ImportExportService(validator);

    @Test
    void importExportRoundTrip(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("in.json");
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[\"x\"]}]}";
        Files.write(src, json.getBytes());
        List<PromptTemplate> imported = service.importFromFile(src);
        assertEquals(1, imported.size());
        Path out = dir.resolve("out.json");
        service.exportToFile(out, imported);
        List<PromptTemplate> again = service.importFromFile(out);
        assertEquals(imported.get(0).getTitle(), again.get(0).getTitle());
    }

    @Test
    void importFromStream() throws Exception {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[]}]}";
        List<PromptTemplate> list = service.importFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, list.size());
    }

    @Test
    void importRejectsNonFile(@TempDir Path dir) {
        assertThrows(Exception.class, () -> service.importFromFile(dir));
    }

    @Test
    void remapImportedTemplatesAssignsNewIds() {
        List<PromptTemplate> imported = Arrays.asList(
                new PromptTemplate("old-1", "T1", "b1", java.util.Collections.emptyList()),
                new PromptTemplate("old-2", "T2", "b2", java.util.Collections.emptyList()));
        List<PromptTemplate> remapped = service.remapImportedTemplates(imported, new java.util.function.Supplier<String>() {
            private int i = 0;
            @Override
            public String get() {
                i++;
                return "new-" + i;
            }
        });
        assertEquals(2, remapped.size());
        assertEquals("new-1", remapped.get(0).getId());
        assertEquals("new-2", remapped.get(1).getId());
        assertTrue(remapped.stream().noneMatch(p -> p.getId().startsWith("old-")));
    }

    @Test
    void importExportRoundTripPreservesUnicode() throws Exception {
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"தமிழ் ✅\",\"body\":\"中文 العربية 👩🏾‍💻\",\"tags\":[\"हिन्दी\",\"עברית\"]}]}";
        List<PromptTemplate> imported = service.importFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals("தமிழ் ✅", imported.get(0).getTitle());
        assertEquals("中文 العربية 👩🏾‍💻", imported.get(0).getBody());
        assertEquals(Arrays.asList("हिन्दी", "עברית"), imported.get(0).getTags());
    }

    @Test
    void importSupportsRequestedLanguageSet() throws Exception {
        String multilingual = "English Français Deutsch Español Português Italiano Nederlands Polski Türkçe Tiếng Việt "
                + "Русский Українська Български Ελληνικά हिन्दी தமிழ் తెలుగు ಕನ್ನಡ മലയാളം मराठी বাংলা ગુજરાતી ਪੰਜਾਬੀ "
                + "العربية עברית فارسی اردو 简体中文 繁體中文 日本語 한국어 ไทย Bahasa Indonesia Bahasa Melayu Kiswahili";
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"" + multilingual + "\",\"body\":\"" + multilingual + "\",\"tags\":[\"العربية\",\"日本語\",\"Kiswahili\"]}]}";
        List<PromptTemplate> imported = service.importFromStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(multilingual, imported.get(0).getTitle());
        assertEquals(multilingual, imported.get(0).getBody());
        assertEquals(Arrays.asList("العربية", "日本語", "Kiswahili"), imported.get(0).getTags());
    }
}
