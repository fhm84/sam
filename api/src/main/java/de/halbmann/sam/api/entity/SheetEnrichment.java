package de.halbmann.sam.api.entity;

import java.util.Set;

/**
 * AI-generated enrichment suggestions for an existing sheet music entry.
 * Each field is null if no suggestion could be confidently determined.
 */
public record SheetEnrichment(
        Set<String> suggestedTags,
        Style suggestedStyle,
        Integer suggestedDifficultyLevel,
        String suggestedAdditionalNotes,
        Integer suggestedYearOfComposition) {}
