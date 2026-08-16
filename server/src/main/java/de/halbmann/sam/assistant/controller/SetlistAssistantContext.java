package de.halbmann.sam.assistant.controller;

import jakarta.enterprise.context.RequestScoped;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries the ensemble that {@link SetlistCandidateTool} is allowed to evaluate coverage against
 * for the current request. Set once by {@link SetlistAssistantService} after resolving and
 * authorizing the target collection's ensemble — the AI service/LLM never supplies or chooses
 * this value itself, so there is no LLM-controlled argument to re-validate or that a prompt
 * injection could redirect to a different ensemble.
 */
@RequestScoped
@Getter
@Setter
public class SetlistAssistantContext {

    private UUID ensembleId;
}
