package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.api.entity.classification.ClassificationApplyRequest;
import de.halbmann.sam.classification.controller.InstrumentSearchTool;
import de.halbmann.sam.classification.controller.MusicianSearchTool;
import de.halbmann.sam.classification.controller.SheetSearchTool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

/**
 * Agentic AI service that autonomously resolves entity references for a classified document.
 *
 * <p>Uses tool calls to search for existing sheets, musicians, and instruments in the archive
 * before deciding whether to link to an existing entity or create a new one.
 */
@RegisterAiService(tools = {InstrumentSearchTool.class, MusicianSearchTool.class, SheetSearchTool.class})
public interface ClassificationAgent {

    @SystemMessage(fromResource = "/prompts/classification/classification-agent-system.txt")
    @UserMessage(fromResource = "/prompts/classification/classification-agent-user.txt")
    ClassificationApplyRequest resolve(@V("metadata") String metadata);
}
