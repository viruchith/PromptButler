package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
