package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.CliErrorReporter;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * Shared skeleton for all export commands: fetches all records, writes one pretty-printed JSON
 * file per record into the target directory, and prints a summary. File formats match what the
 * corresponding import command reads back in.
 *
 * @param <T> the exported record type
 */
public abstract class AbstractExportCommand<T> implements Callable<Integer> {

    @CommandLine.Parameters(description = "Directory to write exported JSON files into.")
    Path outputDir;

    @Inject
    CliErrorReporter errorReporter;

    /** Fetches every record to export. */
    protected abstract List<T> fetchAll();

    /** Filename for the record, unique within {@code usedNames}. */
    protected abstract String filenameFor(T item, Set<String> usedNames);

    /** Human-readable identification for progress output. */
    protected abstract String describe(T item);

    /** Plural noun for summary messages, e.g. "sheets". */
    protected abstract String noun();

    /** Hook to strip or reshape fields before serialization; defaults to the record itself. */
    protected Object toExport(final T item) {
        return item;
    }

    /**
     * Runtime type to serialize as, or {@code null} for the object's own class. Overriding with a
     * supertype drops subclass-only fields (e.g. search metrics) from the output.
     */
    protected Class<?> serializeAs() {
        return null;
    }

    @Override
    public Integer call() {
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            System.err.println("Could not create output directory " + outputDir + ": " + e.getMessage());
            return 1;
        }

        Set<String> usedNames = new HashSet<>();
        int count = 0;
        try (Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            List<T> items = fetchAll();
            if (items.isEmpty()) {
                System.out.println("No " + noun() + " found.");
                return 0;
            }

            for (T item : items) {
                String filename = filenameFor(item, usedNames);
                try (OutputStream out = Files.newOutputStream(
                        outputDir.resolve(filename),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    Object export = toExport(item);
                    if (serializeAs() != null) {
                        jsonb.toJson(export, serializeAs(), out);
                    } else {
                        jsonb.toJson(export, out);
                    }
                }
                System.out.println("  ✓ Exported: " + describe(item) + " -> " + filename);
                count++;
            }
        } catch (Exception e) {
            errorReporter.printError("Error exporting " + noun(), e);
            return 1;
        }

        System.out.println("Export completed: " + count + " " + noun() + " written to " + outputDir);
        return 0;
    }
}
