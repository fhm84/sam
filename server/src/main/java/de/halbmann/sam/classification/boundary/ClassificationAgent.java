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

    @SystemMessage("""
            You are a music archive assistant. Your task is to resolve entity references for a
            classified sheet music document. Use the provided tools to search for existing sheets,
            musicians (composers/arrangers), and instruments in the archive.

            Rules:
            - Use searchSheets to find an existing sheet. If a result has a clearly matching title,
              set sheetId to link to it; otherwise leave sheetId null (a new sheet will be created).
            - Use searchMusicians for composer and arranger names. If a result has score >= 0.4,
              set composerId / arrangerId; otherwise set composerName / arrangerName for creation.
            - Use searchInstruments for the instrument name. If a result has score >= 0.4,
              set instrumentId; otherwise set instrumentName for creation.
            - Only search when the corresponding field is present in the metadata.
            - Populate all other metadata fields (title, subtitle, publisher, genre,
              yearOfComposition, edition, iswc, partLabel, clef, notationType) directly from the
              provided input without searching.
            - Leave fields null when the information is not available.
            """)
    @UserMessage("""
            Resolve entity references for the following sheet music metadata:

            {{metadata}}

            Return a fully populated ClassificationApplyRequest.
            """)
    ClassificationApplyRequest resolve(@V("metadata") String metadata);
}
