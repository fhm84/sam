package de.halbmann.sam.cli.commands;

import de.halbmann.sam.cli.controller.InstrumentImporter;
import de.halbmann.sam.cli.entity.ImportResult;
import io.quarkus.arc.Unremovable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import picocli.CommandLine;

@Unremovable
@Singleton
@CommandLine.Command(
        name = "importInstrument",
        description = "Import instrument(s) from JSON file(s)",
        mixinStandardHelpOptions = true)
public class ImportInstrumentCommand implements Callable<Integer> {

    @Inject
    InstrumentImporter instrumentImporter;

    @CommandLine.Parameters(description = "JSON file(s) or directory to import. Single instrument per file.")
    File[] files;

    @CommandLine.Option(
            names = {"-d", "--dry-run"},
            description = "Validate JSON without importing")
    boolean dryRun;

    @Override
    public Integer call() {
        final List<ImportResult> results = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                try (Stream<Path> fileStream = Files.walk(file.toPath(), 1)) {
                    fileStream
                            .map(Path::toFile)
                            .filter(File::isFile)
                            .forEach(f -> results.add(instrumentImporter.importFile(f, dryRun)));
                } catch (IOException e) {
                    System.err.println("Error reading directory " + file.getPath() + ": " + e.getMessage());
                    return 1;
                }
            } else {
                results.add(instrumentImporter.importFile(file, dryRun));
            }
        }

        Map<Boolean, Long> collected =
                results.stream().collect(Collectors.groupingBy(ImportResult::success, Collectors.counting()));
        Long successCount = Optional.ofNullable(collected.get(Boolean.TRUE)).orElse(0L);
        Long failureCount = Optional.ofNullable(collected.get(Boolean.FALSE)).orElse(0L);
        if (dryRun) {
            System.out.println("Dry run completed. No instruments were imported.");
        } else {
            System.out.println("Import completed: " + successCount + " succeeded, " + failureCount + " failed.");
        }

        return failureCount > 0L ? 1 : 0;
    }
}
