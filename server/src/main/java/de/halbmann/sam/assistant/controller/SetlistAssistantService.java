package de.halbmann.sam.assistant.controller;

import de.halbmann.sam.api.entity.assistant.SetlistSuggestions;
import de.halbmann.sam.assistant.boundary.SetlistAssistant;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the AI program builder. Authorization (whether the caller may access the target
 * ensemble) is the resource layer's responsibility, matching how {@code CurrentUserService} is
 * used elsewhere in the codebase — this service assumes {@code ensembleId} has already been
 * checked and trusts it, but never lets the LLM choose or override it (see {@link
 * SetlistAssistantContext}).
 */
@Slf4j
@ApplicationScoped
public class SetlistAssistantService {

    public record SuggestionOutcome(SetlistSuggestions suggestions, TokenUsage tokenUsage) {}

    @Inject
    SetlistAssistant setlistAssistant;

    @Inject
    SetlistAssistantContext context;

    @Inject
    SheetCollectionRepository collectionRepository;

    public SuggestionOutcome suggestItems(String collectionId, UUID ensembleId, String goal) {
        SheetCollectionEntity collection = collectionRepository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));

        context.setEnsembleId(ensembleId);

        String existingItems = describeExistingItems(collection);
        log.info("Requesting setlist suggestions for collection {} (ensemble {})", collectionId, ensembleId);

        Result<SetlistSuggestions> result;
        try {
            result = setlistAssistant.suggest(goal, existingItems);
        } catch (Exception e) {
            log.warn("Setlist assistant call failed for collection {}: {}", collectionId, e.getMessage());
            throw new RuntimeException("AI setlist suggestion failed: " + e.getMessage(), e);
        }

        return new SuggestionOutcome(dropUnservedSuggestions(result.content(), collectionId), result.tokenUsage());
    }

    /**
     * The "suggestions only ever reference real archive sheets" guarantee must not rest on prompt
     * text alone: keep only items whose sheet ID the candidate tool actually served during this
     * request, and drop anything else as a hallucination.
     */
    private SetlistSuggestions dropUnservedSuggestions(SetlistSuggestions suggestions, String collectionId) {
        var validated = suggestions.getItems().stream()
                .filter(item -> context.wasServed(item.getSheetId()))
                .toList();
        int dropped = suggestions.getItems().size() - validated.size();
        if (dropped > 0) {
            log.warn(
                    "Dropped {} suggestion(s) for collection {} referencing sheets the candidate tool never served",
                    dropped,
                    collectionId);
            suggestions.setItems(new ArrayList<>(validated));
        }
        return suggestions;
    }

    private String describeExistingItems(SheetCollectionEntity collection) {
        String lines = collection.getItems().stream()
                .filter(SheetCollectionItemEntity.class::isInstance)
                .map(SheetCollectionItemEntity.class::cast)
                .filter(item -> item.getSheet() != null)
                .map(item -> "- "
                        + item.getSheet().getTitle()
                        + (item.getSheet().getComposer() != null
                                ? " (" + item.getSheet().getComposer().getName() + ")"
                                : ""))
                .collect(Collectors.joining("\n"));
        return lines.isEmpty() ? "(none yet)" : lines;
    }
}
