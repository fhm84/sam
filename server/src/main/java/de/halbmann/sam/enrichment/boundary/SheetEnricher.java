package de.halbmann.sam.enrichment.boundary;

import de.halbmann.sam.enrichment.entity.SheetEnrichmentResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
public interface SheetEnricher {

    @SystemMessage("""
                    You are enriching metadata for an archived piece of sheet music.
                    Based on the provided metadata, suggest missing or complementary information.

                    For tags: suggest 2–5 free-form lowercase contextual tags describing occasion or context
                    (e.g. "christmas", "outdoor concert", "youth band", "opening number", "festive").
                    For style: use one of CLASSICAL, ROMANTIC, MODERN, CONTEMPORARY, POP, ROCK, FUNK, SWING, LATIN,
                    TRADITIONAL, FOLKLORISTIC, EXPERIMENTAL.
                    For difficultyLevel: use one of VERY_EASY, EASY, MEDIUM, ADVANCED, DIFFICULT, VERY_DIFFICULT.
                    For additionalNotes: write a short 1–2 sentence description of the piece (mood, style, occasion).
                    For yearOfComposition: provide a 4-digit year if reliably known from the composer's era or context.

                    Only suggest values you are confident about. Leave fields as null if uncertain.
                    Fields marked as "already set" in the input must NOT be suggested again — return null for those.
                    """)
    @UserMessage("""
                    Enrich the following sheet music metadata:

                    {{metadata}}
                    """)
    SheetEnrichmentResult enrich(@V("metadata") String metadata);
}
