package com.viruchith.PromptButler.ui;

import com.viruchith.PromptButler.core.model.PromptTemplate;
import com.viruchith.PromptButler.core.repository.PromptRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MainViewModel {

    private final PromptRepository repository;
    private final FuzzySearchService fuzzySearchService = new FuzzySearchService();
    private final VariableParser variableParser = new VariableParser();
    private final TemplateCompiler templateCompiler = new TemplateCompiler();

    private final ObservableList<PromptTemplate> masterList = FXCollections.observableArrayList();
    private final ObservableList<PromptTemplate> filteredList = FXCollections.observableArrayList();
    private final StringProperty searchText = new SimpleStringProperty("");
    private final ObjectProperty<PromptTemplate> selectedTemplate = new SimpleObjectProperty<PromptTemplate>();

    public MainViewModel(PromptRepository repository, List<PromptTemplate> initial) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(initial, "initial");
        this.masterList.setAll(initial);
        this.searchText.addListener((obs, o, n) -> refreshFilter());
        refreshFilter();
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
        String q = searchText.get();
        List<PromptTemplate> ranked = fuzzySearchService.rank(q, new ArrayList<PromptTemplate>(masterList));
        filteredList.setAll(ranked);
    }

    public void replaceAllTemplates(List<PromptTemplate> next) throws IOException {
        masterList.setAll(next);
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
        return templateCompiler.compile(t.getBody(), values);
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
        if (id == null) {
            return false;
        }
        for (PromptTemplate p : masterList) {
            if (id.equals(p.getId())) {
                return true;
            }
        }
        return false;
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
        persist();
        refreshFilter();
    }

    /**
     * Replaces the template with the same {@code id} (title/body/tags may change). Id must match {@code updated.getId()}.
     */
    public void replaceTemplateById(String id, PromptTemplate updated) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(updated, "updated");
        if (!id.equals(updated.getId())) {
            throw new IllegalArgumentException("Template id cannot change when editing");
        }
        for (int i = 0; i < masterList.size(); i++) {
            if (id.equals(masterList.get(i).getId())) {
                masterList.set(i, updated);
                persist();
                refreshFilter();
                return;
            }
        }
        throw new IllegalArgumentException("No template with id: " + id);
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
}
