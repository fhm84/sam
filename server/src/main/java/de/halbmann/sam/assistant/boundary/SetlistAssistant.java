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

    @SystemMessage(fromResource = "/prompts/assistant/setlist-assistant-system.txt")
    @UserMessage(fromResource = "/prompts/assistant/setlist-assistant-user.txt")
    Result<SetlistSuggestions> suggest(@V("goal") String goal, @V("existingItems") String existingItems);
}
