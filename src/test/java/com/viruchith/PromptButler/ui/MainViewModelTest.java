package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void getTemplateCountReflectsAddAndDelete() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Collections.emptyList());
        assertEquals(0, vm.getTemplateCount());
        PromptTemplate a = new PromptTemplate("a", "A", "b", Collections.emptyList());
        vm.addTemplate(a);
        assertEquals(1, vm.getTemplateCount());
        vm.deleteTemplate(a);
        assertEquals(0, vm.getTemplateCount());
    }

    @Test
    void toggleFavoriteFlipsFavoriteAndPersists() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        PromptTemplate orig = new PromptTemplate("a", "A", "b", Collections.emptyList(), false);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(orig));
        assertFalse(vm.getMasterTemplates().get(0).isFavorite());
        vm.toggleFavorite(orig);
        assertTrue(vm.getMasterTemplates().get(0).isFavorite());
        // Toggle back
        vm.toggleFavorite(vm.getMasterTemplates().get(0));
        assertFalse(vm.getMasterTemplates().get(0).isFavorite());
        Mockito.verify(repo, Mockito.atLeast(2)).saveAll(Mockito.anyList());
    }

    @Test
    void idIndexRebuiltOnReplaceAll() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(
                new PromptTemplate("a", "A", "b", Collections.emptyList())));
        assertTrue(vm.idExists("a"));
        vm.replaceAllTemplates(Arrays.asList(
                new PromptTemplate("b", "B", "b", Collections.emptyList())));
        assertFalse(vm.idExists("a"));
        assertTrue(vm.idExists("b"));
    }

    @Test
    void categoryFilterLimitsResults() {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(
                new PromptTemplate("a", "A", "b", Collections.emptyList(), false, "Development", 0L, 0L, Collections.emptyList()),
                new PromptTemplate("b", "B", "b", Collections.emptyList(), false, "Writing", 0L, 0L, Collections.emptyList())));
        vm.categoryFilterProperty().set("Development");
        assertEquals(1, vm.getFilteredList().size());
        assertEquals("a", vm.getFilteredList().get(0).getId());
    }

    @Test
    void markTemplateUsedIncrementsUsage() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        PromptTemplate p = new PromptTemplate("a", "A", "b", Collections.emptyList());
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(p));
        vm.markTemplateUsed(p);
        PromptTemplate loaded = vm.getMasterTemplates().get(0);
        assertEquals(1L, loaded.getUsageCount());
        assertTrue(loaded.getLastUsedEpochMillis() > 0L);
    }

    @Test
    void compileSupportsQuotedAndUnquotedValues() {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        PromptTemplate p = new PromptTemplate("a", "A", "Hello {{name}}", Collections.emptyList());
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(p));
        HashMap<String, String> values = new HashMap<>();
        values.put("name", "Ada");
        assertEquals("Hello Ada", vm.compile(p, values));
        assertEquals("Hello \"Ada\"", vm.compile(p, values, true));
    }

    @Test
    void deleteCategoryReassignsPromptsToGeneral() throws Exception {
        PromptRepository repo = Mockito.mock(PromptRepository.class);
        PromptTemplate a = new PromptTemplate("a", "A", "b", Collections.emptyList(), false, "Dev", 0L, 0L, Collections.emptyList());
        PromptTemplate b = new PromptTemplate("b", "B", "b", Collections.emptyList(), false, "Writing", 0L, 0L, Collections.emptyList());
        MainViewModel vm = new MainViewModel(repo, Arrays.asList(a, b));
        int affected = vm.deleteCategoryAndReassignToGeneral("Dev");
        assertEquals(1, affected);
        PromptTemplate reassigned = vm.getMasterTemplates().stream().filter(t -> "a".equals(t.getId())).findFirst().orElseThrow();
        assertEquals("General", reassigned.getCategory());
    }
}
