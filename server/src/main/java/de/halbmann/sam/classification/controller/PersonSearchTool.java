package de.halbmann.sam.classification.controller;

import de.halbmann.sam.api.entity.persons.PersonMatch;
import de.halbmann.sam.business.persons.boundary.PersonRepository;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class PersonSearchTool {

    @Inject
    PersonRepository personRepository;

    @Tool("""
            Search for existing persons (composers or arrangers) in the archive by name.
            Returns candidates with similarity scores (0–1). Use score >= 0.4 as a confident match.
            If no good match, set the name field instead so a new person will be created.
            """)
    public List<PersonMatch> searchPersons(String name) {
        return personRepository.findCandidates(name, 0.2, 5);
    }
}
