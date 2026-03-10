package de.halbmann.sam.api.entity;

import java.util.List;
import java.util.UUID;

/**
 * Result of AI-based classification of an uploaded document.
 * Contains detected sheet music metadata, pre-matched existing entity references,
 * and a pre-filled {@link #suggested} apply request ready for user review.
 */
public record SheetClassification(
        UUID documentId,
        ClassificationStatus status,
        // AI-detected sheet metadata
        String title,
        String subtitle,
        String publisher,
        String composer,
        String arranger,
        String genre,
        Integer yearOfComposition,
        String edition,
        String iswc,
        // AI-detected instrumentation
        String instrumentName,
        String partLabel,
        String transposition,
        Clef clef,
        NotationType notationType,
        // Pre-matched existing entities (null if no match found)
        UUID matchedSheetId,
        String matchedSheetTitle,
        UUID matchedComposerId,
        UUID matchedArrangerId,
        // Instrument candidates ordered by trigram similarity (best match first)
        List<InstrumentMatch> instrumentCandidates,
        // Pre-filled apply request for the user to review and confirm
        ClassificationApplyRequest suggested) {}
