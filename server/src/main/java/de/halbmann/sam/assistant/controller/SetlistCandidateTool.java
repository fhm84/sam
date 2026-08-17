package de.halbmann.sam.assistant.controller;

import de.halbmann.sam.api.entity.ensembles.CoverageSnapshotSummary;
import de.halbmann.sam.api.entity.sheets.Genre;
import de.halbmann.sam.business.ensembles.controller.CoverageSnapshotService;
import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tool for the AI setlist assistant to search the real repertoire. Only sheets with {@code
 * COMPLETE} or {@code PLAYABLE} coverage for the request's ensemble are returned — the LLM never
 * sees or picks sheets the ensemble cannot actually perform, and it never chooses which ensemble
 * to search against (see {@link SetlistAssistantContext}). Every served sheet ID is recorded in
 * the request context so the final suggestions can be validated against what the tool actually
 * returned.
 */
@ApplicationScoped
public class SetlistCandidateTool {

    private static final int MAX_RESULTS = 20;

    @Inject
    SetlistAssistantContext context;

    @Inject
    SheetRepository sheetRepository;

    @Inject
    CoverageSnapshotService coverageSnapshotService;

    @Tool("""
            Search the archive's repertoire for pieces playable by the ensemble this setlist belongs
            to. All parameters are optional filters — pass an empty string to skip one. genre must
            match a known genre name (e.g. MARCH, OVERTURE, POLKA) or be left empty. maxDurationMinutes
            limits results to pieces no longer than that many minutes. tags is a comma-separated list;
            a sheet matches if it has ANY of the given tags. Returns up to 20 results ordered by title,
            each with id, title, composer, genre, difficulty, duration in minutes, and coverage status.
            Only COMPLETE or PLAYABLE sheets are returned — sheets the ensemble cannot actually perform
            (INCOMPLETE coverage) are excluded automatically, so every result here is a safe pick.
            """)
    public String searchRepertoire(String genre, String maxDurationMinutes, String tags) {
        UUID ensembleId = context.getEnsembleId();
        if (ensembleId == null) {
            return "No ensemble is configured for this setlist; cannot evaluate playability.";
        }

        Genre parsedGenre = parseGenre(genre);
        Duration maxDuration = parseMaxDuration(maxDurationMinutes);
        Set<String> tagFilter = parseTags(tags);

        var playable =
                sheetRepository.findAiAssistantCandidates(ensembleId, parsedGenre, maxDuration, tagFilter, MAX_RESULTS);
        if (playable.isEmpty()) {
            return "No playable sheets matched these filters.";
        }

        context.recordServedSheetIds(
                playable.stream().map(SheetMusicEntity::getId).toList());

        Map<UUID, CoverageSnapshotSummary> coverage = coverageSnapshotService.findSummaries(
                ensembleId, playable.stream().map(SheetMusicEntity::getId).toList());

        return playable.stream()
                .map(s -> formatCandidate(s, coverage.get(s.getId())))
                .collect(Collectors.joining("\n"));
    }

    private String formatCandidate(SheetMusicEntity s, CoverageSnapshotSummary coverage) {
        return String.format(
                "id=%s, title=%s, composer=%s, genre=%s, difficulty=%s, durationMinutes=%s, coverage=%s",
                s.getId(),
                s.getTitle(),
                s.getComposer() != null ? s.getComposer().getName() : "unknown",
                s.getGenre() != null ? s.getGenre() : "unknown",
                s.getDifficultyLevel() != null ? s.getDifficultyLevel() : "unknown",
                s.getDuration() != null ? s.getDuration().toMinutes() : "unknown",
                coverage != null ? coverage.getStatus() : "unknown");
    }

    private Genre parseGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            return null;
        }
        try {
            return Genre.valueOf(genre.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Duration parseMaxDuration(String maxDurationMinutes) {
        if (maxDurationMinutes == null || maxDurationMinutes.isBlank()) {
            return null;
        }
        try {
            return Duration.ofMinutes(Long.parseLong(maxDurationMinutes.trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Set<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Set.of();
        }
        // not Set.of(split(...)): the LLM may emit duplicates ("march,march"), which Set.of rejects
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toSet());
    }
}
