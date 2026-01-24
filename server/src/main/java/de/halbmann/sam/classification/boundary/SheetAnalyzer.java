package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(
    chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
public interface SheetAnalyzer {

  @SystemMessage(
      """
            The given document is a music sheet. You are used for analyzing music sheet information to archive these documents.
            Please keep the fields you cannot find in the music sheet as null. Rely on given enum values.
            If you don't find a match, leave the related field as null or try to match to 'unknown'.
            """)
  @UserMessage("Analyze the given document.")
  SheetAnalyzerResult analyze(Image image);
}
