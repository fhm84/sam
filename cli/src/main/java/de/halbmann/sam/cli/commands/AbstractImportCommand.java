package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.AbstractImporter;
import de.halbmann.sam.cli.entity.ImportResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import picocli.CommandLine;

/**
 * Shared skeleton for all import commands: walks the given files/directories (one level deep),
 * delegates each file to the importer, and prints a created/updated/unchanged/failed summary
 * (ADR-0019). Exit code is 1 as soon as any file failed.
 */
public abstract class AbstractImportCommand implements Callable<Integer> {

    @CommandLine.Parameters(description = "JSON file(s) or directory to import. Single record per file.")
    File[] files;

    @CommandLine.Option(
            names = {"-d", "--dry-run"},
            description = "Validate the files and compare them with the target (read-only): print what would be"
                    + " created, added or updated, write nothing")
    boolean dryRun;

    @CommandLine.Option(
            names = {"-u", "--update"},
            description = "Also write fields of existing records that differ from the file (default: report them)."
                    + " Fields the file leaves empty are kept")
    boolean update;

    /** The importer handling a single file. */
    protected abstract AbstractImporter<?, ?> importer();

    /** Plural noun for summary messages, e.g. "sheets". */
    protected abstract String noun();

    @Override
    public Integer call() {
        final List<ImportResult> results = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                try (Stream<Path> fileStream = Files.walk(file.toPath(), 1)) {
                    fileStream
                            .map(Path::toFile)
                            .filter(File::isFile)
                            .forEach(f -> results.add(importer().importFile(f, dryRun, update)));
                } catch (IOException e) {
                    System.err.println("Error reading directory " + file.getPath() + ": " + e.getMessage());
                    return 1;
                }
            } else {
                results.add(importer().importFile(file, dryRun, update));
            }
        }

        long created = count(results, ImportResult.Status.CREATED);
        long updated = count(results, ImportResult.Status.UPDATED);
        long unchanged = count(results, ImportResult.Status.UNCHANGED);
        long failed = count(results, ImportResult.Status.FAILED);
        long differing =
                results.stream().filter(ImportResult::unappliedDifferences).count();
        String summary =
                created + " created, " + updated + " updated, " + unchanged + " unchanged, " + failed + " failed";
        System.out.println(
                dryRun
                        ? "Dry run completed (would be: " + summary + "). No " + noun() + " were written."
                        : "Import completed: " + summary + ".");
        if (differing > 0) {
            System.out.println(differing + " existing record(s) differ from their file and were left as they are"
                    + " — rerun with --update to write the file's values.");
        }

        return failed > 0L ? 1 : 0;
    }

    private static long count(final List<ImportResult> results, final ImportResult.Status status) {
        return results.stream().filter(r -> r.status() == status).count();
    }
}
