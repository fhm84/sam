package de.halbmann.sam.assistant.boundary;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * Plain (non-tool) AI service that drafts a short spoken introduction for a piece in a concert
 * programme, for the Dirigent/announcer to review and edit before use.
 */
@RegisterAiService
public interface ProgrammeTextDrafter {

    @SystemMessage(fromResource = "/prompts/assistant/programme-text-drafter-system.txt")
    @UserMessage(fromResource = "/prompts/assistant/programme-text-drafter-user.txt")
    Result<String> draft(@V("pieceMetadata") String pieceMetadata, @V("language") String language);
}
