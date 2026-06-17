package com.viruchith.PromptButler.core.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.service.JsonSchemaValidator;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class JsonPromptRepository implements PromptRepository {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path promptsFile;
    private final JsonSchemaValidator validator;

    public JsonPromptRepository(Path promptsFile, JsonSchemaValidator validator) {
        this.promptsFile = Objects.requireNonNull(promptsFile, "promptsFile");
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    @Override
    public List<PromptTemplate> loadAll() throws IOException {
        if (!Files.isRegularFile(promptsFile)) {
            return Collections.emptyList();
        }
        long size = Files.size(promptsFile);
        if (size == 0) {
            return Collections.emptyList();
        }
        if (size > JsonSchemaValidator.MAX_IMPORT_BYTES) {
            throw new IOException("prompts.json exceeds maximum allowed size");
        }
        byte[] raw = Files.readAllBytes(promptsFile);
        String json = new String(raw, StandardCharsets.UTF_8);
        validator.validatePromptStoreJson(json);
        StoreDto dto = GSON.fromJson(json, StoreDto.class);
        if (dto == null || dto.templates == null) {
            return Collections.emptyList();
        }
        ArrayList<PromptTemplate> out = new ArrayList<PromptTemplate>();
        for (TemplateDto t : dto.templates) {
            if (t == null) {
                continue;
            }
            List<String> tags = t.tags == null ? Collections.<String>emptyList() : t.tags;
            out.add(new PromptTemplate(t.id, t.title, t.body, tags));
        }
        return out;
    }

    @Override
    public void saveAll(List<PromptTemplate> templates) throws IOException {
        Objects.requireNonNull(templates, "templates");
        StoreDto dto = new StoreDto();
        dto.version = 1;
        dto.templates = new ArrayList<TemplateDto>();
        for (PromptTemplate p : templates) {
            TemplateDto td = new TemplateDto();
            td.id = p.getId();
            td.title = p.getTitle();
            td.body = p.getBody();
            td.tags = new ArrayList<String>(p.getTags());
            dto.templates.add(td);
        }
        String json = GSON.toJson(dto);
        validator.validatePromptStoreJson(json);
        Path parent = promptsFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(promptsFile, json.getBytes(StandardCharsets.UTF_8));
    }

    public static List<PromptTemplate> parseValidatedJsonString(String json, JsonSchemaValidator validator) {
        Objects.requireNonNull(validator, "validator").validatePromptStoreJson(json);
        StoreDto dto = GSON.fromJson(json, StoreDto.class);
        if (dto == null || dto.templates == null) {
            return Collections.emptyList();
        }
        ArrayList<PromptTemplate> out = new ArrayList<PromptTemplate>();
        for (TemplateDto t : dto.templates) {
            if (t == null) {
                continue;
            }
            List<String> tags = t.tags == null ? Collections.<String>emptyList() : t.tags;
            out.add(new PromptTemplate(t.id, t.title, t.body, tags));
        }
        return out;
    }

    public static List<PromptTemplate> parseValidatedReader(Reader reader, JsonSchemaValidator validator) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        long total = 0;
        while ((n = reader.read(buf)) != -1) {
            total += n;
            if (total > JsonSchemaValidator.MAX_IMPORT_BYTES) {
                throw new IOException("JSON exceeds maximum allowed size");
            }
            sb.append(buf, 0, n);
        }
        String json = sb.toString();
        return parseValidatedJsonString(json, validator);
    }

    @SuppressWarnings("unused")
    private static final class StoreDto {
        int version;
        List<TemplateDto> templates;
    }

    @SuppressWarnings("unused")
    private static final class TemplateDto {
        String id;
        String title;
        String body;
        List<String> tags;
    }
}
