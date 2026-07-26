package com.viruchith.PromptButler.ui;

// SPDX-License-Identifier: GPL-3.0-only

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.PromptRepository;
import com.viruchith.PromptButler.core.util.InputText;
import com.viruchith.PromptButler.core.service.FuzzySearchService;
import com.viruchith.PromptButler.core.service.TemplateCompiler;
import com.viruchith.PromptButler.core.service.VariableParser;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * FX-friendly view model: owns the in-memory prompt library, drives fuzzy filtering, and bridges to
 * {@link com.viruchith.PromptButler.core.repository.PromptRepository} for persistence.
 * <p>
 * UI binds to {@link #searchTextProperty()} and {@link #getFilteredList()}; mutations call
 * {@link #refreshFilter()} after repository writes.
 * </p>
 */
public final class MainViewModel {
    private static final String GENERAL_CATEGORY = "General";

    private final PromptRepository repository;
    private final FuzzySearchService fuzzySearchService = new FuzzySearchService();
    private final VariableParser variableParser = new VariableParser();
    private final TemplateCompiler templateCompiler = new TemplateCompiler();

    private final ObservableList<PromptTemplate> masterList = FXCollections.observableArrayList();
    private final ObservableList<PromptTemplate> filteredList = FXCollections.observableArrayList();
    private final StringProperty searchText = new SimpleStringProperty("");
    private final StringProperty categoryFilter = new SimpleStringProperty("All");
    private final ObjectProperty<PromptTemplate> selectedTemplate = new SimpleObjectProperty<PromptTemplate>();

    /** O(1) lookup index for template IDs — kept in sync with masterList mutations. */
    private final Set<String> idIndex = new HashSet<>();

    public MainViewModel(PromptRepository repository, List<PromptTemplate> initial) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(initial, "initial");
        this.masterList.setAll(initial);
        rebuildIdIndex();
        this.searchText.addListener((obs, o, n) -> refreshFilter());
        this.categoryFilter.addListener((obs, o, n) -> refreshFilter());
        refreshFilter();
    }

    private void rebuildIdIndex() {
        idIndex.clear();
        for (PromptTemplate p : masterList) {
            idIndex.add(p.getId());
        }
    }

    public StringProperty searchTextProperty() {
        return searchText;
    }

    public ObservableList<PromptTemplate> filteredListProperty() {
        return filteredList;
    }

    public ObservableList<PromptTemplate> getFilteredList() {
        return filteredList;
    }

    public ObjectProperty<PromptTemplate> selectedTemplateProperty() {
        return selectedTemplate;
    }

    public VariableParser getVariableParser() {
        return variableParser;
    }

    public TemplateCompiler getTemplateCompiler() {
        return templateCompiler;
    }

    public void refreshFilter() {
        String q = InputText.trimToEmpty(searchText.get());
        String category = InputText.trimToEmpty(categoryFilter.get());
        ArrayList<PromptTemplate> base = new ArrayList<PromptTemplate>();
        if (category.isEmpty() || "All".equalsIgnoreCase(category)) {
            base.addAll(masterList);
        } else {
            for (PromptTemplate template : masterList) {
                if (category.equalsIgnoreCase(template.getCategory())) {
                    base.add(template);
                }
            }
        }
        List<PromptTemplate> ranked = fuzzySearchService.rank(q, base);
        filteredList.setAll(ranked);
    }

    public void replaceAllTemplates(List<PromptTemplate> next) throws IOException {
        masterList.setAll(next);
        rebuildIdIndex();
        repository.saveAll(new ArrayList<PromptTemplate>(masterList));
        refreshFilter();
    }

    public void persist() throws IOException {
        repository.saveAll(new ArrayList<PromptTemplate>(masterList));
    }

    public List<PromptTemplate> getMasterTemplates() {
        return new ArrayList<PromptTemplate>(masterList);
    }

    public List<String> variablesFor(PromptTemplate t) {
        return variableParser.parseOrderedUniqueVariables(t.getBody());
    }

    public String compile(PromptTemplate t, Map<String, String> values) {
        return templateCompiler.compile(t.getBody(), values, false);
    }

    public String compile(PromptTemplate t, Map<String, String> values, boolean quoteValues) {
        return templateCompiler.compile(t.getBody(), values, quoteValues);
    }

    public Map<String, String> emptyVariableMap(PromptTemplate t) {
        List<String> keys = variablesFor(t);
        HashMap<String, String> m = new HashMap<String, String>();
        for (String k : keys) {
            m.put(k, "");
        }
        return m;
    }

    public boolean idExists(String id) {
        String needle = InputText.trimToEmpty(id);
        if (needle.isEmpty()) {
            return false;
        }
        return idIndex.contains(needle);
    }

    /**
     * Adds a template and persists. {@code template.id} must be unique in the library.
     */
    public void addTemplate(PromptTemplate template) throws IOException {
        Objects.requireNonNull(template, "template");
        if (idExists(template.getId())) {
            throw new IllegalArgumentException("A prompt with this id already exists: " + template.getId());
        }
        masterList.add(template);
        idIndex.add(template.getId());
        persist();
        refreshFilter();
    }

    /**
     * Removes a template and persists.
     */
    public void deleteTemplate(PromptTemplate template) throws IOException {
        if (template == null) {
            return;
        }
        masterList.remove(template);
        idIndex.remove(template.getId());
        persist();
        refreshFilter();
    }

    /**
     * Replaces the template with the same {@code id} (title/body/tags may change). Id must match {@code updated.getId()}.
     */
    public void replaceTemplateById(String id, PromptTemplate updated) throws IOException {
        String idNorm = InputText.trimToEmpty(Objects.requireNonNull(id, "id"));
        Objects.requireNonNull(updated, "updated");
        if (!idNorm.equals(updated.getId())) {
            throw new IllegalArgumentException("Template id cannot change when editing");
        }
        for (int i = 0; i < masterList.size(); i++) {
            if (idNorm.equals(masterList.get(i).getId())) {
                masterList.set(i, updated);
                persist();
                refreshFilter();
                return;
            }
        }
        throw new IllegalArgumentException("No template with id: " + idNorm);
    }

    public void reloadFromDisk(List<PromptTemplate> templates) {
        masterList.setAll(templates);
        rebuildIdIndex();
        refreshFilter();
    }

    /** Returns the total number of templates in the library. */
    public int getTemplateCount() {
        return masterList.size();
    }

    /**
     * Allocates a new random UUID string that is not already used in the master list.
     */
    public String allocateNewTemplateId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (idExists(id));
        return id;
    }

    /**
     * Toggles the favorite status of the given template and persists.
     */
    public void toggleFavorite(PromptTemplate template) throws IOException {
        Objects.requireNonNull(template, "template");
        PromptTemplate toggled = template.withFavorite(!template.isFavorite());
        replaceTemplateById(template.getId(), toggled);
    }

    public PromptTemplate duplicateTemplate(PromptTemplate template) throws IOException {
        Objects.requireNonNull(template, "template");
        PromptTemplate duplicated = template.withDuplicatedId(
                allocateNewTemplateId(),
                template.getTitle() + " (copy)");
        addTemplate(duplicated);
        return duplicated;
    }

    public void markTemplateUsed(PromptTemplate template) throws IOException {
        if (template == null) {
            return;
        }
        PromptTemplate used = template.withUsageNow(System.currentTimeMillis());
        replaceTemplateById(template.getId(), used);
    }

    public PromptTemplate editTemplate(PromptTemplate existing, String title, String body, List<String> tags, String category) throws IOException {
        Objects.requireNonNull(existing, "existing");
        PromptTemplate updated = existing.withEditedContent(title, body, tags, category);
        replaceTemplateById(existing.getId(), updated);
        return updated;
    }

    public void restoreDeletedTemplate(PromptTemplate deleted) throws IOException {
        if (deleted == null) {
            return;
        }
        if (!idExists(deleted.getId())) {
            addTemplate(deleted);
        }
    }

    public List<String> getKnownCategories() {
        ArrayList<String> categories = new ArrayList<String>();
        categories.add("All");
        for (PromptTemplate template : masterList) {
            String category = template.getCategory();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    public int deleteCategoryAndReassignToGeneral(String categoryToDelete) throws IOException {
        String target = InputText.trimToEmpty(categoryToDelete);
        if (target.isEmpty() || "all".equalsIgnoreCase(target) || GENERAL_CATEGORY.equalsIgnoreCase(target)) {
            return 0;
        }
        int changed = 0;
        ArrayList<PromptTemplate> next = new ArrayList<PromptTemplate>(masterList.size());
        for (PromptTemplate template : masterList) {
            if (target.equalsIgnoreCase(template.getCategory())) {
                PromptTemplate reassigned = new PromptTemplate(
                        template.getId(),
                        template.getTitle(),
                        template.getBody(),
                        template.getTags(),
                        template.isFavorite(),
                        GENERAL_CATEGORY,
                        template.getUsageCount(),
                        template.getLastUsedEpochMillis(),
                        template.getRevisions());
                next.add(reassigned);
                changed++;
            } else {
                next.add(template);
            }
        }
        if (changed > 0) {
            masterList.setAll(next);
            persist();
            refreshFilter();
        }
        return changed;
    }

    public StringProperty categoryFilterProperty() {
        return categoryFilter;
    }
}
