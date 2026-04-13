package de.halbmann.sam.classification.controller;

import de.halbmann.sam.business.sheets.boundary.SheetRepository;
import de.halbmann.sam.business.sheets.entity.SheetMusicEntity;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SheetSearchTool {

    @Inject
    SheetRepository sheetRepository;

    @Tool("""
            Search for existing sheet music in the archive by title. Returns up to 3 candidates ordered by similarity.
            Each result contains id, title and composer. Use score context to pick the best match.
            If a good match exists, set sheetId so the document attaches to it; otherwise leave sheetId null
            so a new sheet will be created.
            """)
    public String searchSheets(String title) {
        List<SheetMusicEntity> results = sheetRepository.findCandidatesByTitle(title, 0.3, 3);
        if (results.isEmpty()) {
            return "No matching sheets found.";
        }
        return results.stream()
                .map(s -> String.format(
                        "id=%s, title=%s, composer=%s",
                        s.getId(),
                        s.getTitle(),
                        s.getComposer() != null ? s.getComposer().getName() : "unknown"))
                .collect(Collectors.joining("\n"));
    }
}
