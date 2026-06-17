package com.viruchith.PromptButler.core.repository;

import com.viruchith.PromptButler.core.model.PromptTemplate;

import java.io.IOException;
import java.util.List;

public interface PromptRepository {

    List<PromptTemplate> loadAll() throws IOException;

    void saveAll(List<PromptTemplate> templates) throws IOException;
}
