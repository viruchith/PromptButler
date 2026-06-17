package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainViewModelTest {

    @Test
    void addDeleteAndIdExists() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Collections.<PromptTemplate>emptyList());
        PromptTemplate a = new PromptTemplate("a", "A", "body", Collections.singletonList("t"));
        vm.addTemplate(a);
        assertTrue(vm.idExists("a"));
        vm.deleteTemplate(a);
        assertFalse(vm.idExists("a"));
        Mockito.verify(repo, Mockito.atLeastOnce()).saveAll(Mockito.anyList());
    }

    @Test
    void rejectsDuplicateId() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(new PromptTemplate("x", "X", "b", Collections.emptyList())));
        assertThrows(IllegalArgumentException.class, () ->
                vm.addTemplate(new PromptTemplate("x", "Y", "b", Collections.emptyList())));
    }

    @Test
    void replaceTemplateByIdUpdatesMasterAndPersists() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(new PromptTemplate("a", "Old", "old-body", Collections.singletonList("t1"))));
        PromptTemplate next = new PromptTemplate("a", "New", "new-body", Collections.singletonList("t2"));
        vm.replaceTemplateById("a", next);
        assertEquals("new-body", vm.getMasterTemplates().get(0).getBody());
        assertEquals("New", vm.getMasterTemplates().get(0).getTitle());
        Mockito.verify(repo, Mockito.atLeastOnce()).saveAll(Mockito.anyList());
    }

    @Test
    void replaceTemplateByIdRejectsMismatchedIds() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(new PromptTemplate("a", "A", "b", Collections.emptyList())));
        assertThrows(IllegalArgumentException.class, () ->
                vm.replaceTemplateById("a", new PromptTemplate("b", "B", "c", Collections.emptyList())));
    }

    @Test
    void allocateNewTemplateIdIsUniqueUuid() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Collections.emptyList());
        String id1 = vm.allocateNewTemplateId();
        String id2 = vm.allocateNewTemplateId();
        assertNotEquals(id1, id2);
        vm.addTemplate(new PromptTemplate(id1, "T", "b", Collections.emptyList()));
        assertTrue(vm.idExists(id1));
        String id3 = vm.allocateNewTemplateId();
        assertFalse(vm.idExists(id3));
    }
}
