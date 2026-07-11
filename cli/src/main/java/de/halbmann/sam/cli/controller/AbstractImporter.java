package de.halbmann.sam.cli.controller;

import de.halbmann.sam.cli.CliErrorReporter;
import de.halbmann.sam.cli.entity.ImportResult;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Shared import flow for all entity types: parse the JSON file, run Jakarta Bean Validation
 * locally, then (outside dry-run) skip records that already exist on the server and create the
 * rest. Dry-run stops after validation and never touches the network.
 *
 * @param <T> the DTO type one file deserializes into
 */
public abstract class AbstractImporter<T> {

    @Inject
    Jsonb jsonb;

    @Inject
    Validator validator;

    @Inject
    CliErrorReporter errorReporter;

    /** DTO class one file deserializes into. */
    protected abstract Class<T> type();

    /** Human-readable identification of the record, e.g. its title or name. */
    protected abstract String describe(T dto);

    /** Whether an equivalent record already exists on the server (called outside dry-run only). */
    protected abstract boolean exists(T dto);

    /** Creates the record (and any dependent records) on the server. */
    protected abstract void create(T dto);

    /** Additional non-annotation validation; return one message per problem. */
    protected List<String> extraValidation(final T dto) {
        return List.of();
    }

    public ImportResult importFile(final File file, final boolean dryRun) {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getPath());
            return ImportResult.failed(file);
        }

        final T dto;
        try (var bis = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            dto = jsonb.fromJson(bis, type());
        } catch (IOException | RuntimeException e) {
            errorReporter.printError("  ✗ Error reading file " + file.getName(), e);
            return ImportResult.failed(file);
        }

        List<String> problems = new ArrayList<>();
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        violations.forEach(v -> problems.add(v.getPropertyPath() + " " + v.getMessage()));
        problems.addAll(extraValidation(dto));
        if (!problems.isEmpty()) {
            System.err.println("  ✗ Invalid file " + file.getName() + ":");
            problems.forEach(p -> System.err.println("      - " + p));
            return ImportResult.failed(file);
        }

        System.out.println("Processing " + file.getName() + " ...");
        if (dryRun) {
            System.out.println("  [DRY RUN] Would import: " + describe(dto));
            return ImportResult.imported(file);
        }

        try {
            if (exists(dto)) {
                System.out.println("  - Already exists, skipping: " + describe(dto));
                return ImportResult.skipped(file);
            }
            create(dto);
            return ImportResult.imported(file);
        } catch (Exception e) {
            errorReporter.printError("  ✗ Error importing file " + file.getName(), e);
            return ImportResult.failed(file);
        }
    }
}
