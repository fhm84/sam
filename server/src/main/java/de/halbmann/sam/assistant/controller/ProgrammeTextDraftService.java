package de.halbmann.sam.assistant.controller;

import de.halbmann.sam.assistant.boundary.ProgrammeTextDrafter;
import de.halbmann.sam.business.collections.boundary.SheetCollectionRepository;
import de.halbmann.sam.business.collections.entity.CollectionItemEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionEntity;
import de.halbmann.sam.business.collections.entity.SheetCollectionItemEntity;
import de.halbmann.sam.business.collections.entity.TextCollectionItemEntity;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import de.halbmann.sam.core.exception.EntityNotFoundException;
import de.halbmann.sam.core.exception.ValidationException;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates AI drafting of the spoken introduction text for a {@code TextCollectionItemEntity}
 * placed between pieces in a setlist, based on the neighboring piece's metadata.
 */
@Slf4j
@ApplicationScoped
public class ProgrammeTextDraftService {

    /** Mirrors the Angular app's supported UI locales (see {@code TranslationService}). */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("en", "de");

    private static final String DEFAULT_LANGUAGE = "en";

    public record DraftOutcome(String draftText, TokenUsage tokenUsage) {}

    @Inject
    ProgrammeTextDrafter drafter;

    @Inject
    SheetCollectionRepository collectionRepository;

    public DraftOutcome draft(String collectionId, String itemId, String language) {
        SheetCollectionEntity collection = collectionRepository
                .findByIdOptional(UUID.fromString(collectionId))
                .orElseThrow(() -> new EntityNotFoundException("SheetCollection", collectionId));

        List<CollectionItemEntity> items = collection.getItems();
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().toString().equals(itemId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new EntityNotFoundException("CollectionItem", itemId);
        }
        if (!(items.get(index) instanceof TextCollectionItemEntity)) {
            throw new ValidationException("Drafting is only supported for TEXT items");
        }

        SheetMusicEntity neighbor = findNeighborSheet(items, index);
        if (neighbor == null) {
            throw new ValidationException("No neighboring piece found to draft an introduction for");
        }

        String metadata = describeSheet(neighbor);
        String resolvedLanguage = normalizeLanguage(language);
        log.info(
                "Drafting programme text for collection {} item {} (neighbor sheet {}, language {})",
                collectionId,
                itemId,
                neighbor.getId(),
                resolvedLanguage);

        Result<String> result;
        try {
            result = drafter.draft(metadata, resolvedLanguage);
        } catch (Exception e) {
            log.warn("Programme text drafting failed for item {}: {}", itemId, e.getMessage());
            throw new RuntimeException("AI text drafting failed: " + e.getMessage(), e);
        }

        return new DraftOutcome(result.content(), result.tokenUsage());
    }

    /** Prefers the next piece in the running order (an intro precedes what it introduces). */
    private SheetMusicEntity findNeighborSheet(List<CollectionItemEntity> items, int index) {
        for (int i = index + 1; i < items.size(); i++) {
            if (items.get(i) instanceof SheetCollectionItemEntity sheetItem && sheetItem.getSheet() != null) {
                return sheetItem.getSheet();
            }
        }
        for (int i = index - 1; i >= 0; i--) {
            if (items.get(i) instanceof SheetCollectionItemEntity sheetItem && sheetItem.getSheet() != null) {
                return sheetItem.getSheet();
            }
        }
        return null;
    }

    /** Falls back to English for anything not in the supported set, rather than trusting client input verbatim. */
    private String normalizeLanguage(String language) {
        if (language == null) {
            return DEFAULT_LANGUAGE;
        }
        String lower = language.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_LANGUAGES.contains(lower) ? lower : DEFAULT_LANGUAGE;
    }

    private String describeSheet(SheetMusicEntity sheet) {
        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(sheet.getTitle()).append("\n");
        if (sheet.getComposer() != null) {
            sb.append("Composer: ").append(sheet.getComposer().getName()).append("\n");
        }
        if (sheet.getGenre() != null) {
            sb.append("Genre: ").append(sheet.getGenre()).append("\n");
        }
        if (sheet.getAdditionalNotes() != null && !sheet.getAdditionalNotes().isBlank()) {
            sb.append("Notes: ").append(sheet.getAdditionalNotes()).append("\n");
        }
        return sb.toString().trim();
    }
}
