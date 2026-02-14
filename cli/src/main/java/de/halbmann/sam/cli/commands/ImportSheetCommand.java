package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.SheetImporter;
import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "import",
        description = "Import music sheet(s) from JSON file(s)",
        mixinStandardHelpOptions = true
)
public class ImportSheetCommand implements Runnable {

    @Inject
    SheetImporter sheetImporter;

    @CommandLine.Parameters(
            description = "JSON file(s) or directory to import. Single sheet per file."
    )
    File[] files;

    @CommandLine.Option(
            names = {"-d", "--dry-run"},
            description = "Validate JSON without importing"
    )
    boolean dryRun;

    @Override
    public void run() {
        final List<ImportResult> results = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                try (final Stream<Path> fileStream = Files.walk(file.toPath(), 1)) {
                    fileStream.forEach(f -> results.add(sheetImporter.importFile(f.toFile(), dryRun)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                results.add(sheetImporter.importFile(file, dryRun));
            }
        }

        Map<Boolean, Long> collected = results.stream().collect(Collectors.groupingBy(ImportResult::success, Collectors.counting()));
        Long successCount = collected.get(Boolean.TRUE);
        Long failureCount = collected.get(Boolean.FALSE);
        System.out.println();
        if (dryRun) {
            System.out.println("Dry run completed. No sheets were imported.");
        } else {
            System.out.println("Import completed: " + successCount + " succeeded, " + failureCount + " failed.");
        }

        if (failureCount > 0L) {
            throw new CommandLine.ExecutionException(
                    new CommandLine(this),
                    "Some imports failed"
            );
        }
    }

}