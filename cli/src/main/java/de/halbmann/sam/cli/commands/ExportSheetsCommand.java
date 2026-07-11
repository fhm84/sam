package de.halbmann.sam.cli.commands;

import de.halbmann.sam.api.boundary.SamResources;
import de.halbmann.sam.api.entity.sheets.SheetFilterRequest;
import de.halbmann.sam.api.entity.sheets.SheetMusic;
import de.halbmann.sam.api.entity.sheets.SheetMusicSearchResult;
import de.halbmann.sam.cli.util.FilenameUtils;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import picocli.CommandLine;

/**
 * Exports sheet music as plain {@code SheetMusic} JSON files, one per sheet — the same format
 * {@code cli import} reads back in. Metadata only: attachments/documents are not exported (see
 * migration/README.md for why).
 */
@Unremovable
@Singleton
@CommandLine.Command(
        name = "export",
        description = "Export music sheets to JSON file(s), one per sheet",
        mixinStandardHelpOptions = true)
public class ExportSheetsCommand implements Runnable {

    @Inject
    @RestClient
    SamResources client;

    @CommandLine.Parameters(description = "Directory to write exported sheet JSON files into.")
    Path outputDir;

    @CommandLine.Option(
            names = {"-q", "--query"},
            description = "Only export sheets matching this full-text query (same as `list`'s search).")
    String query;

    @Override
    public void run() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(
                    new CommandLine(this), "Could not create output directory: " + outputDir, e);
        }

        SheetFilterRequest request = new SheetFilterRequest();
        request.setSize(-1); // disable pagination, fetch everything in one call
        request.setQuery(query);

        var sheets = client.sheets().findSheets(request);
        if (sheets.getData().isEmpty()) {
            System.out.println("No music sheets found.");
            return;
        }

        Set<String> usedNames = new HashSet<>();
        int count = 0;
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            for (SheetMusicSearchResult sheet : sheets.getData()) {
                String filename = FilenameUtils.uniqueFilename(sheet.getTitle(), sheet.getId(), usedNames);
                writeSheet(sheet, filename, jsonb);
                System.out.println("  ✓ Exported: " + sheet.getTitle() + " -> " + filename);
                count++;
            }
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(new CommandLine(this), "Failed to export sheets", e);
        }

        System.out.println("Export completed: " + count + " sheet(s) written to " + outputDir);
    }

    private void writeSheet(final SheetMusicSearchResult sheet, final String filename, final Jsonb jsonb)
            throws IOException {
        try (OutputStream out = Files.newOutputStream(
                outputDir.resolve(filename),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            // serialize as the plain SheetMusic shape (drop search-only metrics/coverage fields)
            jsonb.toJson(sheet, SheetMusic.class, out);
        }
    }
}
