package de.halbmann.sam.assistant.controller;

import jakarta.enterprise.context.RequestScoped;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries the ensemble that {@link SetlistCandidateTool} is allowed to evaluate coverage against
 * for the current request. Set once by {@link SetlistAssistantService} after resolving and
 * authorizing the target collection's ensemble — the AI service/LLM never supplies or chooses
 * this value itself, so there is no LLM-controlled argument to re-validate or that a prompt
 * injection could redirect to a different ensemble.
 *
 * <p>Also collects every sheet ID the tool served during this request, so the final LLM output
 * can be validated against what the archive actually returned — a suggestion referencing any
 * other ID is a hallucination and gets dropped.
 */
@RequestScoped
public class SetlistAssistantContext {

    @Getter
    @Setter
    private UUID ensembleId;

    private final Set<UUID> servedSheetIds = new HashSet<>();

    public void recordServedSheetIds(Collection<UUID> sheetIds) {
        servedSheetIds.addAll(sheetIds);
    }

    public boolean wasServed(UUID sheetId) {
        return sheetId != null && servedSheetIds.contains(sheetId);
    }
}
