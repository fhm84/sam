package de.halbmann.sam.assistant.boundary;

import de.halbmann.sam.api.entity.assistant.SetlistSuggestions;
import de.halbmann.sam.assistant.controller.SetlistCandidateTool;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * Tool-grounded AI service that suggests real repertoire pieces for a setlist based on a
 * free-text goal. Returns {@link Result} (rather than the plain DTO) so the caller can read
 * {@link Result#tokenUsage()} for per-call cost attribution.
 */
@RegisterAiService(tools = SetlistCandidateTool.class)
public interface SetlistAssistant {

    @SystemMessage("""
            You are a music archive assistant helping a Dirigent (conductor) build a concert
            setlist. Use the searchRepertoire tool to find real candidate pieces — never invent a
            piece, composer, or sheet ID that a tool call did not return.

            Rules:
            - Call searchRepertoire one or more times with different filters to explore the
              repertoire relevant to the goal (e.g. by genre, duration, or tags).
            - Only ever suggest sheets that a tool call actually returned. Copy their id exactly
              as sheetId.
            - Do not suggest a sheet that is already in "Existing setlist items" below.
            - Prefer variety: avoid suggesting many pieces of the same genre or by the same
              composer back to back unless the goal specifically asks for that.
            - For each suggestion, write a short one-sentence rationale explaining why it fits the
              goal and the existing program.
            - Return at most 8 suggestions, best fit first.
            """)
    @UserMessage("""
            Goal / constraints from the Dirigent:
            {{goal}}

            Existing setlist items (avoid duplicating these):
            {{existingItems}}
            """)
    Result<SetlistSuggestions> suggest(@V("goal") String goal, @V("existingItems") String existingItems);
}
