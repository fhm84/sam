package de.halbmann.sam.classification.boundary;

import de.halbmann.sam.classification.entity.SheetAnalyzerResult;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService(chatMemoryProviderSupplier = RegisterAiService.NoChatMemoryProviderSupplier.class)
public interface SheetAnalyzer {

    @SystemMessage(fromResource = "/prompts/classification/sheet-analyzer-analyze-system.txt")
    @UserMessage(fromResource = "/prompts/classification/sheet-analyzer-analyze-user.txt")
    SheetAnalyzerResult analyze(Image image);

    @SystemMessage(fromResource = "/prompts/classification/sheet-analyzer-analyze-text-system.txt")
    @UserMessage(fromResource = "/prompts/classification/sheet-analyzer-analyze-text-user.txt")
    SheetAnalyzerResult analyzeText(@V("text") String text);
}
