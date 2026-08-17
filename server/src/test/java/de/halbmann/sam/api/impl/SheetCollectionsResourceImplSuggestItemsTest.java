package de.halbmann.sam.api.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.assistant.SetlistSuggestions;
import de.halbmann.sam.api.entity.assistant.SuggestSetlistItemsRequest;
import de.halbmann.sam.api.entity.collections.SheetCollection;
import de.halbmann.sam.api.entity.eventlog.EventType;
import de.halbmann.sam.assistant.controller.SetlistAssistantService;
import de.halbmann.sam.business.collections.controller.SheetCollectionService;
import de.halbmann.sam.business.eventlog.controller.EventLogService;
import de.halbmann.sam.core.exception.ValidationException;
import dev.langchain4j.model.output.TokenUsage;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the enforcement points in {@link SheetCollectionsResourceImpl#suggestItems}: role gating
 * lives in {@code @RolesAllowed} (no per-ensemble check — the allowed roles may access every
 * ensemble), an ensemble must be assigned, and event logging must survive providers that report
 * no token usage.
 */
@ExtendWith(MockitoExtension.class)
class SheetCollectionsResourceImplSuggestItemsTest {

    @Mock
    SheetCollectionService service;

    @Mock
    SetlistAssistantService setlistAssistantService;

    @Mock
    EventLogService eventLogService;

    @InjectMocks
    SheetCollectionsResourceImpl resource;

    final String collectionId = UUID.randomUUID().toString();
    final UUID ensembleId = UUID.randomUUID();

    @Test
    void noEnsembleOnCollection_throwsValidationException() {
        SheetCollection collection = new SheetCollection();
        collection.setEnsembleId(null);
        when(service.load(collectionId)).thenReturn(collection);

        assertThrows(ValidationException.class, () -> resource.suggestItems(collectionId, request("goal")));
        verifyNoInteractions(setlistAssistantService, eventLogService);
    }

    @Test
    void authorizedCaller_returnsSuggestionsAndLogsTokenUsage() {
        SheetCollection collection = new SheetCollection();
        collection.setEnsembleId(ensembleId);
        when(service.load(collectionId)).thenReturn(collection);

        SetlistSuggestions suggestions = new SetlistSuggestions();
        TokenUsage tokenUsage = new TokenUsage(100, 50);
        when(setlistAssistantService.suggestItems(collectionId, ensembleId, "goal"))
                .thenReturn(new SetlistAssistantService.SuggestionOutcome(suggestions, tokenUsage));

        SetlistSuggestions result = resource.suggestItems(collectionId, request("goal"));

        assertSame(suggestions, result);
        verify(eventLogService)
                .log(
                        eq(EventType.SETLIST_AI_SUGGESTION_GENERATED),
                        eq("collection"),
                        eq(UUID.fromString(collectionId)),
                        eq(Map.of("suggestionCount", 0, "inputTokens", 100, "outputTokens", 50)));
    }

    @Test
    void nullTokenCounts_areLoggedAsZero_insteadOfFailingTheRequest() {
        SheetCollection collection = new SheetCollection();
        collection.setEnsembleId(ensembleId);
        when(service.load(collectionId)).thenReturn(collection);

        SetlistSuggestions suggestions = new SetlistSuggestions();
        // providers may return a TokenUsage whose individual counts are null
        TokenUsage tokenUsage = new TokenUsage(null, null);
        when(setlistAssistantService.suggestItems(collectionId, ensembleId, "goal"))
                .thenReturn(new SetlistAssistantService.SuggestionOutcome(suggestions, tokenUsage));

        SetlistSuggestions result = resource.suggestItems(collectionId, request("goal"));

        assertSame(suggestions, result);
        verify(eventLogService)
                .log(
                        eq(EventType.SETLIST_AI_SUGGESTION_GENERATED),
                        eq("collection"),
                        eq(UUID.fromString(collectionId)),
                        eq(Map.of("suggestionCount", 0, "inputTokens", 0, "outputTokens", 0)));
    }

    private SuggestSetlistItemsRequest request(String goal) {
        SuggestSetlistItemsRequest request = new SuggestSetlistItemsRequest();
        request.setGoal(goal);
        return request;
    }
}
