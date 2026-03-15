package de.halbmann.sam.enrichment.entity;

import java.util.List;

/**
 * Internal AI result for sheet music enrichment.
 *
 * @param tags               Suggested free-form contextual tags (e.g., "christmas", "youth band").
 * @param style              Stylistic language (one of CLASSICAL, ROMANTIC, MODERN, CONTEMPORARY,
 *                           POP, ROCK, FUNK, SWING, LATIN, TRADITIONAL, FOLKLORISTIC, EXPERIMENTAL).
 * @param difficultyLevel    Overall difficulty (one of VERY_EASY, EASY, MEDIUM, ADVANCED, DIFFICULT,
 *                           VERY_DIFFICULT).
 * @param additionalNotes    Short descriptive notes about the piece.
 * @param yearOfComposition  Year the piece was composed.
 */
public record SheetEnrichmentResult(
        List<String> tags,
        String style,
        String difficultyLevel,
        String additionalNotes,
        Integer yearOfComposition) {}
