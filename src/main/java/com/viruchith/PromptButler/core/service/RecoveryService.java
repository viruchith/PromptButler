package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.logging.AppLogger;
import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.JsonPromptRepository;
import com.viruchith.PromptButler.core.repository.PromptRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Backs up corrupted store files and restores defaults from the classpath.
 */
public final class RecoveryService {

    private final JsonSchemaValidator validator;

    public RecoveryService(JsonSchemaValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator");
    }

    public List<PromptTemplate> loadWithRecovery(
            PromptRepository repository,
            Path promptsFile,
            InputStream defaultResourceStream) {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(promptsFile, "promptsFile");
        Objects.requireNonNull(defaultResourceStream, "defaultResourceStream");
        try {
            return repository.loadAll();
        } catch (Exception e) {
            AppLogger.get().error("prompts.json is missing or corrupt; recovering from defaults.", e);
            try {
                backupCorrupted(promptsFile);
                byte[] raw;
                try (InputStream in = defaultResourceStream) {
                    raw = readAll(in);
                }
                String json = new String(raw, StandardCharsets.UTF_8);
                List<PromptTemplate> defaults = JsonPromptRepository.parseValidatedJsonString(json, validator);
                repository.saveAll(defaults);
                return defaults;
            } catch (Exception fatal) {
                AppLogger.get().error("Recovery failed catastrophically.", fatal);
                throw new IllegalStateException("Unable to recover prompt store", fatal);
            }
        }
    }

    public void backupCorrupted(Path promptsFile) {
        try {
            if (Files.isRegularFile(promptsFile)) {
                Path bak = promptsFile.resolveSibling("prompts.json.bak");
                Files.copy(promptsFile, bak, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            AppLogger.get().warn("Could not write prompts.json.bak: " + e.getMessage());
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > JsonSchemaValidator.MAX_IMPORT_BYTES) {
                throw new IOException("Default resource too large");
            }
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
