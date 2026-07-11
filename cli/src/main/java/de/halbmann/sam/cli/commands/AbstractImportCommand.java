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
 * delegates each file to the importer, and prints an imported/skipped/failed summary. Exit code is
 * 1 as soon as any file failed.
 */
public abstract class AbstractImportCommand implements Callable<Integer> {

    @CommandLine.Parameters(description = "JSON file(s) or directory to import. Single record per file.")
    File[] files;

    @CommandLine.Option(
            names = {"-d", "--dry-run"},
            description = "Parse and validate the JSON locally without importing (no network calls)")
    boolean dryRun;

    /** The importer handling a single file. */
    protected abstract AbstractImporter<?> importer();

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
                            .forEach(f -> results.add(importer().importFile(f, dryRun)));
                } catch (IOException e) {
                    System.err.println("Error reading directory " + file.getPath() + ": " + e.getMessage());
                    return 1;
                }
            } else {
                results.add(importer().importFile(file, dryRun));
            }
        }

        long imported = count(results, ImportResult.Status.IMPORTED);
        long skipped = count(results, ImportResult.Status.SKIPPED);
        long failed = count(results, ImportResult.Status.FAILED);
        if (dryRun) {
            System.out.println("Dry run completed (" + imported + " valid, " + failed + " invalid). No " + noun()
                    + " were imported.");
        } else {
            System.out.println(
                    "Import completed: " + imported + " imported, " + skipped + " skipped, " + failed + " failed.");
        }

        return failed > 0L ? 1 : 0;
    }

    private static long count(final List<ImportResult> results, final ImportResult.Status status) {
        return results.stream().filter(r -> r.status() == status).count();
    }
}
