package de.halbmann.sam.classification.controller;

import de.halbmann.sam.api.entity.instruments.InstrumentMatch;
import de.halbmann.sam.business.instruments.boundary.InstrumentRepository;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class InstrumentSearchTool {

    @Inject
    InstrumentRepository instrumentRepository;

    @Tool("""
            Search for existing instruments in the archive by name. Returns candidates with similarity scores (0–1).
            Prefer candidates with score >= 0.4 as a match. If no good match exists, return null for instrumentId
            and set instrumentName so a new instrument will be created.
            """)
    public List<InstrumentMatch> searchInstruments(String name) {
        return instrumentRepository.findCandidates(name, 0.2, 5);
    }
}
