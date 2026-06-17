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
                new PromptTemplate("a", "Title A", "body {{x}}", Arrays.asList("t1")),
                new PromptTemplate("b", "Title B", "plain", Arrays.asList())
        );
        repo.saveAll(data);
        List<PromptTemplate> loaded = repo.loadAll();
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(p -> "Title A".equals(p.getTitle())));
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
        String json = "{\"version\":1,\"templates\":[{\"id\":\"1\",\"title\":\"t\",\"body\":\"b\",\"tags\":[]}]}";
        List<PromptTemplate> list = JsonPromptRepository.parseValidatedReader(
                new InputStreamReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8),
                validator);
        assertEquals(1, list.size());
    }
}
