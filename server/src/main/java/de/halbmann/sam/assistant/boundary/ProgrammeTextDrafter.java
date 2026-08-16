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

    @SystemMessage("""
            You draft short spoken introductions that a conductor or announcer reads out loud
            between pieces at a concert. Keep it to 2-4 sentences, spoken/conversational tone, no
            markdown formatting. Base it only on the metadata given — do not invent facts about the
            piece or composer that weren't provided. Write the entire draft in the language given
            by its ISO 639-1 code (e.g. "en" = English, "de" = German) — never mix languages.
            """)
    @UserMessage("""
            Draft a short spoken introduction, in language "{{language}}", for the following piece:

            {{pieceMetadata}}
            """)
    Result<String> draft(@V("pieceMetadata") String pieceMetadata, @V("language") String language);
}
