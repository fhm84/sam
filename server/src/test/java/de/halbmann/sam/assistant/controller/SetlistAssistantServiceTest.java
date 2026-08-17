package de.halbmann.sam.assistant.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.halbmann.sam.api.entity.assistant.SetlistSuggestions;
import de.halbmann.sam.api.entity.assistant.SuggestedSetlistItem;
import de.halbmann.sam.assistant.boundary.SetlistAssistant;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import dev.langchain4j.service.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the server-side grounding of the assistant's output: the "suggestions only reference
 * real archive sheets" guarantee must hold even when the LLM ignores its prompt, so anything the
 * candidate tool did not serve during the request is dropped.
 */
@ExtendWith(MockitoExtension.class)
class SetlistAssistantServiceTest {

    @Mock
    SetlistAssistant setlistAssistant;

    @Mock
    SheetCollectionRepository collectionRepository;

    @Spy
    SetlistAssistantContext context = new SetlistAssistantContext();

    @InjectMocks
    SetlistAssistantService service;

    final String collectionId = UUID.randomUUID().toString();
    final UUID ensembleId = UUID.randomUUID();

    @Test
    @SuppressWarnings("unchecked")
    void dropsSuggestionsTheToolNeverServed() {
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(new SheetCollectionEntity()));

        UUID servedId = UUID.randomUUID();
        UUID hallucinatedId = UUID.randomUUID();
        SetlistSuggestions suggestions = new SetlistSuggestions();
        suggestions.setItems(new ArrayList<>(List.of(item(servedId), item(hallucinatedId), item(null))));

        Result<SetlistSuggestions> result = mock(Result.class);
        when(result.content()).thenReturn(suggestions);
        when(setlistAssistant.suggest(anyString(), anyString())).thenAnswer(invocation -> {
            // the tool records what it actually served while the AI service runs
            context.recordServedSheetIds(List.of(servedId));
            return result;
        });

        SetlistAssistantService.SuggestionOutcome outcome = service.suggestItems(collectionId, ensembleId, "goal");

        assertEquals(
                List.of(servedId),
                outcome.suggestions().getItems().stream()
                        .map(SuggestedSetlistItem::getSheetId)
                        .toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsAllSuggestions_whenEveryIdWasServed() {
        when(collectionRepository.findByIdOptional(UUID.fromString(collectionId)))
                .thenReturn(Optional.of(new SheetCollectionEntity()));

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        SetlistSuggestions suggestions = new SetlistSuggestions();
        suggestions.setItems(new ArrayList<>(List.of(item(first), item(second))));

        Result<SetlistSuggestions> result = mock(Result.class);
        when(result.content()).thenReturn(suggestions);
        when(setlistAssistant.suggest(anyString(), anyString())).thenAnswer(invocation -> {
            context.recordServedSheetIds(List.of(first, second));
            return result;
        });

        SetlistAssistantService.SuggestionOutcome outcome = service.suggestItems(collectionId, ensembleId, "goal");

        assertEquals(2, outcome.suggestions().getItems().size());
    }

    private SuggestedSetlistItem item(UUID sheetId) {
        SuggestedSetlistItem item = new SuggestedSetlistItem();
        item.setSheetId(sheetId);
        item.setTitle("Piece " + sheetId);
        return item;
    }
}
