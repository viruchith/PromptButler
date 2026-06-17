package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.JsonPromptRepository;
import com.viruchith.PromptButler.core.storage.SafePathResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ImportExportService {

    private final JsonSchemaValidator validator;

    public ImportExportService(JsonSchemaValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public List<PromptTemplate> importFromFile(Path importFile) throws IOException {
        Path validated = SafePathResolver.validateImportSource(importFile);
        long size = Files.size(validated);
        if (size > JsonSchemaValidator.MAX_IMPORT_BYTES) {
            throw new IOException("Import file exceeds maximum allowed size (10MB)");
        }
        String json = new String(Files.readAllBytes(validated), java.nio.charset.StandardCharsets.UTF_8);
        return JsonPromptRepository.parseValidatedJsonString(json, validator);
    }

    public List<PromptTemplate> importFromStream(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        return JsonPromptRepository.parseValidatedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8), validator);
    }

    public void exportToFile(Path exportFile, List<PromptTemplate> templates) throws IOException {
        Objects.requireNonNull(exportFile, "exportFile");
        Objects.requireNonNull(templates, "templates");
        JsonPromptRepository repo = new JsonPromptRepository(exportFile, validator);
        repo.saveAll(templates);
    }
}
