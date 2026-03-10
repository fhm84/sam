package de.halbmann.sam.classification.controller;

import de.halbmann.sam.api.entity.MusicianMatch;
import de.halbmann.sam.business.boundary.MusicianRepository;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class MusicianSearchTool {

    @Inject
    MusicianRepository musicianRepository;

    @Tool("""
            Search for existing musicians (composers or arrangers) in the archive by name.
            Returns candidates with similarity scores (0–1). Use score >= 0.4 as a confident match.
            If no good match, set the name field instead so a new musician will be created.
            """)
    public List<MusicianMatch> searchMusicians(String name) {
        return musicianRepository.findCandidates(name, 0.2, 5);
    }
}
