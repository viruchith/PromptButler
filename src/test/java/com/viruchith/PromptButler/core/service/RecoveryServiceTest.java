package com.viruchith.PromptButler.core.service;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    @Mock
    private PromptRepository repository;

    @Test
    void backupCorruptedNoFile(@TempDir Path dir) {
        RecoveryService r = new RecoveryService(new JsonSchemaValidator());
        r.backupCorrupted(dir.resolve("missing.json"));
    }

    @Test
    void returnsDataWhenLoadSucceeds(@TempDir Path dir) throws Exception {
        Path prompts = dir.resolve("prompts.json");
        List<PromptTemplate> expected = Collections.singletonList(
                new PromptTemplate("1", "t", "b", Collections.singletonList("x")));
        when(repository.loadAll()).thenReturn(expected);
        RecoveryService recovery = new RecoveryService(new JsonSchemaValidator());
        String json = "{\"version\":1,\"templates\":[{\"id\":\"z\",\"title\":\"z\",\"body\":\"z\",\"tags\":[]}]}";
        List<PromptTemplate> result = recovery.loadWithRecovery(repository, prompts,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, result);
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void recoversOnFailure(@TempDir Path dir) throws Exception {
        Path prompts = dir.resolve("prompts.json");
        Files.write(prompts, "{".getBytes(StandardCharsets.UTF_8));
        when(repository.loadAll()).thenThrow(new IOException("bad"));
        RecoveryService recovery = new RecoveryService(new JsonSchemaValidator());
        String json = "{\"version\":1,\"templates\":[{\"id\":\"z\",\"title\":\"z\",\"body\":\"z\",\"tags\":[]}]}";
        List<PromptTemplate> result = recovery.loadWithRecovery(repository, prompts,
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals(1, result.size());
        verify(repository).saveAll(anyList());
    }

    @Test
    void recoveryPropagatesWhenDefaultsInvalid() throws Exception {
        Path prompts = Files.createTempFile("pb", ".json");
        prompts.toFile().deleteOnExit();
        when(repository.loadAll()).thenThrow(new IOException("bad"));
        RecoveryService recovery = new RecoveryService(new JsonSchemaValidator());
        assertThrows(IllegalStateException.class, () ->
                recovery.loadWithRecovery(repository, prompts, new ByteArrayInputStream("{".getBytes(StandardCharsets.UTF_8))));
    }
}
