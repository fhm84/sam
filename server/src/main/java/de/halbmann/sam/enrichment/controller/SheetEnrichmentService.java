package de.halbmann.sam.enrichment.controller;

import de.halbmann.sam.api.entity.*;
import de.halbmann.sam.business.controller.SheetService;
import de.halbmann.sam.enrichment.boundary.SheetEnricher;
import de.halbmann.sam.enrichment.entity.SheetEnrichmentResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SheetEnrichmentService {

    @Inject
    SheetEnricher enricher;

    @Inject
    SheetService sheetService;

    public SheetEnrichment enrich(String sheetId) {
        SheetMusic sheet = sheetService.getSheet(sheetId);
        String metadata = buildMetadataText(sheet);
        SheetEnrichmentResult result = enricher.enrich(metadata);
        return toEnrichment(result, sheet);
    }

    private String buildMetadataText(SheetMusic sheet) {
        var sb = new StringBuilder();
        sb.append("Title: ").append(sheet.getTitle()).append("\n");
        if (sheet.getSubtitle() != null) {
            sb.append("Subtitle: ").append(sheet.getSubtitle()).append("\n");
        }
        if (sheet.getComposer() != null && sheet.getComposer().getName() != null) {
            sb.append("Composer: ").append(sheet.getComposer().getName()).append("\n");
        }
        if (sheet.getArranger() != null && sheet.getArranger().getName() != null) {
            sb.append("Arranger: ").append(sheet.getArranger().getName()).append("\n");
        }
        if (sheet.getGenre() != null) {
            sb.append("Genre: ").append(sheet.getGenre()).append("\n");
        }
        if (sheet.getPublisher() != null) {
            sb.append("Publisher: ").append(sheet.getPublisher()).append("\n");
        }
        if (sheet.getStyle() != null) {
            sb.append("Style (already set): ").append(sheet.getStyle()).append("\n");
        }
        if (sheet.getYearOfComposition() != null) {
            sb.append("Year of composition (already set): ").append(sheet.getYearOfComposition()).append("\n");
        }
        if (sheet.getDifficultyLevel() != null) {
            sb.append("Difficulty (already set): ").append(sheet.getDifficultyLevel()).append("\n");
        }
        if (sheet.getTags() != null && !sheet.getTags().isEmpty()) {
            sb.append("Tags (already set): ").append(String.join(", ", sheet.getTags())).append("\n");
        }
        if (sheet.getAdditionalNotes() != null && !sheet.getAdditionalNotes().isBlank()) {
            sb.append("Notes (already set): ").append(sheet.getAdditionalNotes()).append("\n");
        }
        return sb.toString();
    }

    private SheetEnrichment toEnrichment(SheetEnrichmentResult result, SheetMusic sheet) {
        Style style = parseStyle(result.style(), sheet.getStyle());
        Integer difficultyGrade = parseDifficulty(result.difficultyLevel(), sheet.getDifficultyLevel());
        Integer year = parseYear(result.yearOfComposition(), sheet.getYearOfComposition());
        String notes = parseNotes(result.additionalNotes(), sheet.getAdditionalNotes());
        Set<String> newTags = filterTags(result.tags(), sheet.getTags());

        return new SheetEnrichment(newTags, style, difficultyGrade, notes, year);
    }

    private Style parseStyle(String suggested, Style existing) {
        if (suggested == null || existing != null) return null;
        try {
            return Style.valueOf(suggested.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer parseDifficulty(String suggested, DifficultyLevel existing) {
        if (suggested == null || existing != null) return null;
        try {
            var level = DifficultyLevel.valueOf(
                    suggested.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
            return level.getGrade();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Integer parseYear(Integer suggested, Integer existing) {
        return (suggested != null && existing == null) ? suggested : null;
    }

    private String parseNotes(String suggested, String existing) {
        if (suggested == null || suggested.isBlank()) return null;
        if (existing != null && !existing.isBlank()) return null;
        return suggested.trim();
    }

    private Set<String> filterTags(java.util.List<String> suggested, Set<String> existing) {
        Set<String> existingLower = existing != null
                ? existing.stream().map(String::toLowerCase).collect(Collectors.toSet())
                : Set.of();
        if (suggested == null) return new LinkedHashSet<>();
        return suggested.stream()
                .map(t -> t.trim().toLowerCase())
                .filter(t -> !t.isBlank() && !existingLower.contains(t))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
