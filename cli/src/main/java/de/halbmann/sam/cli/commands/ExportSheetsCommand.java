package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetMusicSearchResult;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports sheet music as plain {@code SheetMusic} JSON files, one per sheet — the same format
 * {@code import} reads back in. Metadata only: attachment files are not exported (see
 * migration/README.md for why).
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "export",
        description = "Export music sheets to JSON file(s), one per sheet",
        mixinStandardHelpOptions = true)
public class ExportSheetsCommand extends AbstractExportCommand<SheetMusicSearchResult> {

    @Inject
    @RestClient
    SamResources client;

    @CommandLine.Option(
            names = {"-q", "--query"},
            description = "Only export sheets matching this full-text query (same as `list`'s search).")
    String query;

    @Override
    protected List<SheetMusicSearchResult> fetchAll() {
        SheetFilterRequest request = new SheetFilterRequest();
        request.setSize(-1); // disable pagination, fetch everything in one call
        request.setQuery(query);
        return client.sheets().findSheets(request).getData();
    }

    @Override
    protected String filenameFor(final SheetMusicSearchResult sheet, final Set<String> usedNames) {
        return FilenameUtils.uniqueFilename(sheet.getTitle(), sheet.getId(), usedNames);
    }

    @Override
    protected String describe(final SheetMusicSearchResult sheet) {
        return sheet.getTitle();
    }

    @Override
    protected String noun() {
        return "sheet(s)";
    }

    @Override
    protected Class<?> serializeAs() {
        // serialize as the plain SheetMusic shape (drops search-only metrics/coverage fields)
        return SheetMusic.class;
    }
}
